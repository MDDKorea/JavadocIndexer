package de.ialistannen.javadocbpi.model.elements;

import java.util.List;
import java.util.Set;
import spoon.javadoc.api.elements.JavadocElement;
import spoon.reflect.declaration.ModifierKind;

public record DocumentedField(
    DocumentedElementReference enclosingTypeRef,
    String name,
    DocumentedElementReference typeRef,
    String renderedType,
    List<JavadocElement> javadoc,
    Set<ModifierKind> modifiers
) implements DocumentedElement {

  @Override
  public String pathSegment() {
    return name;
  }

}
