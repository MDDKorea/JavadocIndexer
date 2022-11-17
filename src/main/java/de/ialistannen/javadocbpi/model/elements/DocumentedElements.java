package de.ialistannen.javadocbpi.model.elements;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DocumentedElements {

  private final Map<DocumentedElementReference, DocumentedElement> elements;

  public DocumentedElements() {
    this.elements = new ConcurrentHashMap<>();
  }

  public void add(DocumentedElementReference reference, DocumentedElement element) {
    if (elements.put(reference, element) != null) {
      throw new IllegalArgumentException(reference + " was already added!");
    }
  }

  public void merge(DocumentedElements other) {
    elements.putAll(other.getElements());
  }

  public Map<DocumentedElementReference, DocumentedElement> getElements() {
    return elements;
  }
}
