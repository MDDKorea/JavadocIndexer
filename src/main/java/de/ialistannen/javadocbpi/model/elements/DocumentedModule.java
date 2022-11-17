package de.ialistannen.javadocbpi.model.elements;

import java.util.List;
import spoon.javadoc.api.elements.JavadocElement;

public record DocumentedModule(
    String name,
    List<JavadocElement> javadoc
) implements DocumentedElement {

  @Override
  public String pathSegment() {
    return name;
  }
}
