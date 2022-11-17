package de.ialistannen.javadocbpi.model.javadoc;

import static de.ialistannen.javadocbpi.model.javadoc.ReferenceConversions.getReference;

import java.util.ArrayList;
import java.util.List;
import spoon.javadoc.api.elements.JavadocBlockTag;
import spoon.javadoc.api.elements.JavadocElement;
import spoon.javadoc.api.elements.JavadocInlineTag;
import spoon.javadoc.api.elements.JavadocReference;
import spoon.javadoc.api.elements.JavadocText;
import spoon.javadoc.api.elements.JavadocVisitor;
import spoon.javadoc.api.elements.snippets.JavadocSnippetTag;

public class ReferenceConversionVisitor implements JavadocVisitor<JavadocElement> {

  @Override
  public JavadocElement defaultValue() {
    throw new UnsupportedOperationException("Should not be called");
  }

  @Override
  public JavadocElement visitReference(JavadocReference reference) {
    try {
      return new OurJavadocReference(getReference(reference.getReference()));
    } catch (Exception e) {
      System.out.println(
          "Reference conversion failed for " + reference + " (" + e.getMessage() + ")"
      );
      return new JavadocText(reference.getReference().toString());
    }
  }

  @Override
  public JavadocElement visitInlineTag(JavadocInlineTag tag) {
    return new JavadocInlineTag(convertElements(tag.getElements()), tag.getTagType());
  }

  @Override
  public JavadocElement visitBlockTag(JavadocBlockTag tag) {
    return new JavadocBlockTag(convertElements(tag.getElements()), tag.getTagType());
  }

  @Override
  public JavadocElement visitSnippet(JavadocSnippetTag snippet) {
    return snippet;
  }

  @Override
  public JavadocElement visitText(JavadocText text) {
    return text;
  }

  public List<JavadocElement> convertElements(List<JavadocElement> toConvert) {
    List<JavadocElement> elements = new ArrayList<>();
    for (JavadocElement tagElement : toConvert) {
      elements.add(tagElement.accept(this));
    }
    return elements;
  }
}
