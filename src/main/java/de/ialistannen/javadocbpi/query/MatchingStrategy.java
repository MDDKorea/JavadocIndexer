package de.ialistannen.javadocbpi.query;

public enum MatchingStrategy {
  PREFIX,
  EXACT;

  public boolean matches(String input, String reference) {
    return switch (this) {
      case PREFIX -> reference.startsWith(input);
      case EXACT -> reference.equals(input);
    };
  }
}
