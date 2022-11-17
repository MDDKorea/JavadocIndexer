package de.ialistannen.javadocbpi.query;

import java.util.Locale;

public enum CaseSensitivity {
  CONSIDER_CASE,
  IGNORE_CASE;

  public String normalize(String input) {
    return switch (this) {
      case CONSIDER_CASE -> input;
      case IGNORE_CASE -> input.toLowerCase(Locale.ROOT);
    };
  }
}
