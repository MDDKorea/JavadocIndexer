package de.ialistannen.javadocbpi.model.javadoc;

import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference;
import spoon.javadoc.api.elements.JavadocReference;
import spoon.reflect.reference.CtReference;

public class OurJavadocReference extends JavadocReference {

  private final DocumentedElementReference documentedReference;

  public OurJavadocReference(DocumentedElementReference documentedReference) {
    super(null);
    this.documentedReference = documentedReference;
  }

  @Override
  public CtReference getReference() {
    throw new UnsupportedOperationException("Please use getDocumentedReference");
  }

  public DocumentedElementReference getDocumentedReference() {
    return documentedReference;
  }

  @Override
  public String toString() {
    return "OurJavadocReference{" + documentedReference + '}';
  }
}
