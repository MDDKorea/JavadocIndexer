package de.ialistannen.javadocbpi.rendering.links;

import java.util.Set;

public record ExternalJavadocReference(String baseUrl, Set<String> packages) {

  public ExternalJavadocReference {
    packages = Set.copyOf(packages);
  }
}
