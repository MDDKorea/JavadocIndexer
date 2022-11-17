package de.ialistannen.javadocbpi.query;

import static de.ialistannen.javadocbpi.model.elements.DocumentedElementType.FIELD;
import static de.ialistannen.javadocbpi.model.elements.DocumentedElementType.METHOD;
import static de.ialistannen.javadocbpi.model.elements.DocumentedElementType.MODULE;

import de.ialistannen.javadocbpi.model.elements.DocumentedElementType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryTokenizer {

  public static final Pattern SEPARATOR = Pattern.compile("([/.#$(,]?)([^/.#$(,]+)");

  public List<Token> tokenize(String query) {
    List<Token> tokens = new ArrayList<>();
    // Our qualified method names do not end with a )
    String normalizedQuery = query.replace(")", "");

    if (normalizedQuery.indexOf('/') > 0) {
      int moduleEnd = normalizedQuery.indexOf('/');
      tokens.add(new Token(normalizedQuery.substring(0, moduleEnd), Set.of(MODULE)));
      normalizedQuery = normalizedQuery.substring(moduleEnd + 1);
    }
    Matcher matcher = SEPARATOR.matcher(normalizedQuery);

    while (matcher.find()) {
      String foundSeparator = matcher.group(1);
      String tokenText = matcher.group(2);
      if (foundSeparator.equals("#")) {
        tokens.add(new Token(tokenText, Set.of(METHOD, FIELD)));
      } else {
        tokens.add(new Token(tokenText, Set.of()));
      }
    }

    return tokens;
  }

  public record Token(String str, Set<DocumentedElementType> potentialTypes) {

    public Token {
      str = str.strip();
    }

    public boolean matches(
        MatchingStrategy matchingStrategy,
        CaseSensitivity caseSensitivity,
        String reference
    ) {
      return matchingStrategy.matches(
          caseSensitivity.normalize(str()),
          caseSensitivity.normalize(reference)
      );
    }

    public boolean hasType() {
      return !potentialTypes.isEmpty();
    }

    public boolean isType(DocumentedElementType type) {
      return potentialTypes.contains(type);
    }
  }

}
