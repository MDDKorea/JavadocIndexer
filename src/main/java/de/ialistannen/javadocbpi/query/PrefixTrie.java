package de.ialistannen.javadocbpi.query;

import static de.ialistannen.javadocbpi.model.elements.DocumentedElementType.FIELD;
import static de.ialistannen.javadocbpi.model.elements.DocumentedElementType.METHOD;

import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference;
import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference.ReferencePathElement;
import de.ialistannen.javadocbpi.model.elements.DocumentedElementType;
import de.ialistannen.javadocbpi.model.elements.DocumentedElements;
import de.ialistannen.javadocbpi.query.QueryTokenizer.Token;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PrefixTrie {

  private final TrieNode root;

  private PrefixTrie(TrieNode root) {
    this.root = root;
  }

  public Collection<String> find(
      MatchingStrategy matchingStrategy,
      CaseSensitivity caseSensitivity,
      List<Token> tokens
  ) {
    return root.find(matchingStrategy, caseSensitivity, tokens)
        .stream()
        .filter(it -> it instanceof InnerTrieNode)
        .flatMap(it -> ((InnerTrieNode) it).qualifiedNames().stream())
        .toList();
  }

  public Collection<String> autocomplete(
      MatchingStrategy matchingStrategy,
      CaseSensitivity caseSensitivity,
      List<Token> tokens,
      int targetSize
  ) {
    return root.autocomplete(matchingStrategy, caseSensitivity, tokens, targetSize);
  }

  public static PrefixTrie forElements(DocumentedElements elements) {
    RootTrieNode root = new RootTrieNode(new HashSet<>());

    for (var entry : elements.getElements().keySet()) {
      root.insert(entry);
    }

    return new PrefixTrie(root);
  }

  private interface TrieNode {

    Collection<PrefixTrie.TrieNode> find(
        MatchingStrategy matchingStrategy,
        CaseSensitivity caseSensitivity,
        List<Token> tokens
    );

    default void insertIntoInner(
        NavigableReference reference,
        Collection<InnerTrieNode> mutableChildren
    ) {
      Optional<DocumentedElementReference> next = reference.next();
      if (next.isEmpty()) {
        return;
      }
      String firstSegment = next.get().segment().toString();

      for (InnerTrieNode innerNode : mutableChildren) {
        for (String segmentName : innerNode.segmentNames) {
          if (segmentName.equals(firstSegment)) {
            if (reference.crossedFieldOrMethodPart()) {
              innerNode.qualifiedNames().add(reference.qualifiedName());
            }
            innerNode.insert(reference);
            return;
          }
        }
      }

      List<String> segments = List.of(firstSegment);
      if (firstSegment.equals("<init>") && reference.reference.getType().isPresent()) {
        segments = List.of(firstSegment, reference.reference.getType().get().segment().toString());
      }

      InnerTrieNode newNode = new InnerTrieNode(
          segments,
          new ArrayList<>(List.of(reference.qualifiedName())),
          new ArrayList<>(),
          next.get().type()
      );
      mutableChildren.add(newNode);
      newNode.insert(reference);
    }

    Collection<String> autocomplete(
        MatchingStrategy matchingStrategy,
        CaseSensitivity caseSensitivity,
        List<Token> tokens,
        int maxSize
    );
  }

  private record RootTrieNode(Collection<InnerTrieNode> innerNodes) implements TrieNode {

    @Override
    public Collection<TrieNode> find(
        MatchingStrategy matchingStrategy,
        CaseSensitivity caseSensitivity,
        List<Token> tokens
    ) {
      return innerNodes().stream()
          .flatMap(child -> child.find(matchingStrategy, caseSensitivity, tokens).stream())
          .toList();
    }

    @Override
    public Collection<String> autocomplete(
        MatchingStrategy matchingStrategy,
        CaseSensitivity caseSensitivity,
        List<Token> tokens,
        int maxSize
    ) {
      return innerNodes().stream()
          .flatMap(child -> child.autocomplete(matchingStrategy, caseSensitivity, tokens, maxSize)
              .stream())
          .limit(maxSize)
          .toList();
    }

    public void insert(DocumentedElementReference currentRef) {
      insertIntoInner(new NavigableReference(currentRef), innerNodes());
    }

  }

  private record InnerTrieNode(
      List<String> segmentNames,
      List<String> qualifiedNames,
      Collection<InnerTrieNode> children,
      DocumentedElementType type
  ) implements TrieNode {

    @Override
    public Collection<TrieNode> find(
        MatchingStrategy matchingStrategy,
        CaseSensitivity caseSensitivity,
        List<Token> tokens
    ) {
      if (tokens.isEmpty()) {
        return List.of();
      }
      Token myToken = tokens.get(0);
      List<Token> childTokens = tokens.subList(1, tokens.size());

      // Pass the buck down the line. This allows `java.util` to match even though no module is
      // provided.
      VisitResult visitResult = shouldDeferToChildren(matchingStrategy, caseSensitivity, myToken);
      if (visitResult == VisitResult.DEFER_TO_CHILDREN) {
        return children.stream()
            .flatMap(child -> child.find(matchingStrategy, caseSensitivity, tokens).stream())
            .collect(Collectors.toList());
      }
      if (visitResult != VisitResult.HANDLE) {
        return List.of();
      }
      if (childTokens.isEmpty()) {
        return List.of(this);
      }

      return children.stream()
          .flatMap(child -> child.find(matchingStrategy, caseSensitivity, childTokens).stream())
          .collect(Collectors.toList());
    }

    private VisitResult shouldDeferToChildren(
        MatchingStrategy matchingStrategy,
        CaseSensitivity caseSensitivity,
        Token myToken
    ) {
      // Only go down to methods when a method token was found
      // This prevents matching method parameters
      if (!myToken.hasType() && type() == METHOD) {
        return VisitResult.ABORT;
      }
      for (String segmentName : segmentNames) {
        if (myToken.matches(matchingStrategy, caseSensitivity, segmentName)) {
          return VisitResult.HANDLE;
        }
      }
      if (!myToken.hasType()) {
        return VisitResult.DEFER_TO_CHILDREN;
      }
      // We had the correct type but did not match. Assume this was meant for us
      // and abort our subtree here.
      // This will prevent "Foo#bar(" from going down to e.g. "Foo#FOO", as "FOO" will abort.
      if (myToken.isType(this.type)) {
        return VisitResult.ABORT;
      }
      return VisitResult.DEFER_TO_CHILDREN;
    }

    @Override
    public Collection<String> autocomplete(
        MatchingStrategy matchingStrategy,
        CaseSensitivity caseSensitivity,
        List<Token> tokens,
        int maxSize
    ) {
      Set<String> choices = new HashSet<>();
      for (TrieNode it : find(matchingStrategy, caseSensitivity, tokens)) {
        if (it instanceof InnerTrieNode child && child.type() == METHOD) {
          choices.addAll(child.subtreeNames(maxSize - choices.size()));
        } else if (it instanceof InnerTrieNode child) {
          choices.addAll(child.qualifiedNames());
        }
      }
      return choices;
    }

    private Set<String> subtreeNames(int maxSize) {
      Set<String> subtreeNames = new HashSet<>(qualifiedNames());
      for (InnerTrieNode child : children()) {
        if (subtreeNames.size() > maxSize) {
          break;
        }
        subtreeNames.addAll(child.qualifiedNames());
        subtreeNames.addAll(child.subtreeNames(maxSize));
      }
      return subtreeNames;
    }

    public void insert(NavigableReference reference) {
      insertIntoInner(reference, children());
    }

    private enum VisitResult {
      DEFER_TO_CHILDREN,
      HANDLE,
      ABORT
    }

  }

  private static final class NavigableReference {

    private final List<DocumentedElementReference> parts;
    private final DocumentedElementReference reference;
    private boolean crossedFieldOrMethodPart;
    private int currentPart;

    private NavigableReference(DocumentedElementReference reference) {
      this.reference = reference;
      this.parts = reference.toParts()
          .stream()
          .flatMap(it -> unwrapNestedReferences(it).stream())
          .toList();

      this.currentPart = -1;
    }

    public Optional<DocumentedElementReference> next() {
      if (currentPart >= parts.size() - 1) {
        return Optional.empty();
      }
      DocumentedElementReference next = parts.get(++currentPart);
      if (next.type() == FIELD || next.type() == METHOD) {
        crossedFieldOrMethodPart = true;
      }
      return Optional.of(next);
    }

    public String qualifiedName() {
      if (crossedFieldOrMethodPart) {
        return reference.asQualifiedName();
      }
      return parts.get(currentPart).asQualifiedName();
    }

    public boolean crossedFieldOrMethodPart() {
      return crossedFieldOrMethodPart;
    }

    private static List<DocumentedElementReference> unwrapNestedReferences(
        DocumentedElementReference reference
    ) {
      if (reference.segment() instanceof ReferencePathElement ref) {
        return ref.reference().toParts();
      }
      return List.of(reference);
    }
  }

}
