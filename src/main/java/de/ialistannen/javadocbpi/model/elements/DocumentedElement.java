package de.ialistannen.javadocbpi.model.elements;

import java.util.List;
import spoon.javadoc.api.elements.JavadocElement;

public sealed interface DocumentedElement permits DocumentedField, DocumentedMethod,
    DocumentedModule, DocumentedPackage, DocumentedType {

  String pathSegment();

  List<JavadocElement> javadoc();
}
