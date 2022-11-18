package de.ialistannen.javadocbpi.model.elements;

import static de.ialistannen.javadocbpi.model.elements.DocumentedElementType.FIELD;
import static de.ialistannen.javadocbpi.model.elements.DocumentedElementType.METHOD;
import static de.ialistannen.javadocbpi.model.elements.DocumentedElementType.MODULE;
import static de.ialistannen.javadocbpi.model.elements.DocumentedElementType.PACKAGE;
import static de.ialistannen.javadocbpi.model.elements.DocumentedElementType.TYPE;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public record DocumentedElementReference(
    DocumentedElementReference nullableParent,
    PathElement segment,
    DocumentedElementType type
) {

  public DocumentedElementReference {
    Objects.requireNonNull(segment);
    Objects.requireNonNull(type);
  }

  public Optional<DocumentedElementReference> parent() {
    return Optional.ofNullable(nullableParent);
  }

  public DocumentedElementReference andThen(String next, DocumentedElementType type) {
    return andThen(new StringPathElement(next), type);
  }

  public DocumentedElementReference andThen(PathElement next, DocumentedElementType type) {
    return new DocumentedElementReference(this, next, type);
  }

  @Override
  public String toString() {
    return asQualifiedName();
  }

  public boolean isMethod() {
    return type() == METHOD || isMethodParameter();
  }

  public boolean isField() {
    return type() == FIELD;
  }

  public Optional<DocumentedElementReference> getModule() {
    if (type() == MODULE) {
      return Optional.of(this);
    }
    return parent().flatMap(DocumentedElementReference::getModule);
  }

  public Optional<DocumentedElementReference> getPackage() {
    if (type() == PACKAGE) {
      return Optional.of(this);
    }
    return parent().flatMap(DocumentedElementReference::getModule);
  }

  public Optional<DocumentedElementReference> getType() {
    if (type() == MODULE || type() == PACKAGE) {
      return Optional.empty();
    }
    if (type() == FIELD) {
      return parent();
    }
    if (type() == METHOD) {
      return parent();
    }
    if (type() == TYPE && !isMethodParameter()) {
      return Optional.of(this);
    }
    return parent().flatMap(DocumentedElementReference::getType);
  }

  public String asQualifiedName() {
    if (isMethodParameter() && isFirstMethodParameter()) {
      return nullableParent.asQualifiedName() + segment.toString();
    }
    if (isMethodParameter()) {
      return nullableParent.asQualifiedName() + "," + segment.toString();
    }
    String result;
    if (nullableParent != null) {
      String separator;
      if (isMethod() || isField()) {
        separator = "#";
      } else if (nullableParent.type() == TYPE || nullableParent.type() == PACKAGE) {
        separator = ".";
      } else if (nullableParent.type() == MODULE) {
        separator = "/";
      } else {
        throw new IllegalArgumentException("Unknown parent type: " + this);
      }
      result = nullableParent.asQualifiedName() + separator + segment.toString();
    } else {
      result = segment.toString();
    }
    if (type() == METHOD) {
      result += "(";
    }
    return result;
  }

  private boolean isFirstMethodParameter() {
    return parent().isPresent() && parent().get().type() == METHOD;
  }

  private boolean isMethodParameter() {
    return anyParentMatches(it -> it.type() == METHOD);
  }

  private boolean anyParentMatches(Predicate<DocumentedElementReference> predicate) {
    return parent().map(it -> predicate.test(it) || it.anyParentMatches(predicate)).orElse(false);
  }

  public static DocumentedElementReference root(DocumentedElementType type, String name) {
    return new DocumentedElementReference(null, new StringPathElement(name), type);
  }

  public List<DocumentedElementReference> toParts() {
    List<DocumentedElementReference> parts = parent()
        .map(DocumentedElementReference::toParts)
        .orElse(new ArrayList<>());

    parts.add(this);

    return parts;
  }

  public DocumentedElementReference withModule(DocumentedElementReference module) {
    if (module.type() != MODULE) {
      throw new IllegalArgumentException("Type of parent must be module, was " + module);
    }
    if (nullableParent == null || nullableParent.type() == MODULE) {
      return new DocumentedElementReference(module, segment(), type());
    }

    return new DocumentedElementReference(
        nullableParent.withModule(module),
        segment(),
        type()
    );
  }

  public sealed interface PathElement {

  }

  public record StringPathElement(String segment) implements PathElement {

    @Override
    public String toString() {
      return segment;
    }
  }

  public record ReferencePathElement(DocumentedElementReference reference) implements PathElement {

    @Override
    public String toString() {
      return reference.asQualifiedName();
    }
  }
}
