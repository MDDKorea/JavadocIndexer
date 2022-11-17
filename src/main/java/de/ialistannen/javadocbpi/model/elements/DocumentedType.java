package de.ialistannen.javadocbpi.model.elements;

import java.util.List;
import java.util.Set;
import spoon.javadoc.api.elements.JavadocElement;
import spoon.reflect.declaration.ModifierKind;

public record DocumentedType(
    String name,
    DocumentedElementReference packageRef,
    Type type,
    List<String> renderedTypeParameters,
    List<JavadocElement> javadoc,
    boolean isException,
    Set<ModifierKind> modifiers,
    String renderedSuperclass,
    List<String> renderedSuperInterfaces
) implements DocumentedElement {

  @Override
  public String pathSegment() {
    return name;
  }

  public boolean hasSuperclass() {
    return !renderedSuperclass.equals("Object");
  }

  public enum Type {
    ANNOTATION,
    ENUM,
    CLASS,
    INTERFACE,
    RECORD,
  }
}
