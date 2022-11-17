package de.ialistannen.javadocbpi.model.elements;

import java.util.List;
import java.util.Set;
import spoon.javadoc.api.elements.JavadocElement;
import spoon.reflect.declaration.ModifierKind;

public record DocumentedMethod(
    DocumentedElementReference enclosingTypeRef,
    String name,
    List<String> renderedTypeParameters,
    DocumentedElementReference returnTypeRef,
    String renderedReturnType,
    List<DocumentedParameter> parameters,
    List<DocumentedElementReference> throwsClause,
    List<JavadocElement> javadoc,
    Set<ModifierKind> modifiers
) implements DocumentedElement {

  @Override
  public String pathSegment() {
    return name;
  }

  public record DocumentedParameter(
      DocumentedElementReference typeRef,
      String renderedType,
      String name
  ) {

  }

}
