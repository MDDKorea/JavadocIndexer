package de.ialistannen.javadocbpi.rendering;

import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference;
import de.ialistannen.javadocbpi.model.javadoc.OurJavadocReference;
import de.ialistannen.javadocbpi.rendering.links.LinkResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.jsoup.nodes.Entities;
import spoon.javadoc.api.StandardJavadocTagType;
import spoon.javadoc.api.elements.JavadocBlockTag;
import spoon.javadoc.api.elements.JavadocElement;
import spoon.javadoc.api.elements.JavadocInlineTag;
import spoon.javadoc.api.elements.JavadocReference;
import spoon.javadoc.api.elements.JavadocText;
import spoon.javadoc.api.elements.JavadocVisitor;
import spoon.javadoc.api.elements.snippets.JavadocSnippetTag;

public class HtmlRenderVisitor implements JavadocVisitor<String> {

  private final LinkResolver linkResolver;
  private final String baseUrl;

  public HtmlRenderVisitor(LinkResolver linkResolver, String baseUrl) {
    this.linkResolver = linkResolver;
    this.baseUrl = baseUrl;
  }

  @Override
  public String defaultValue() {
    throw new UnsupportedOperationException("Default value called. Was a case missed?");
  }

  @Override
  public String visitInlineTag(JavadocInlineTag tag) {
    if (tag.getTagType() instanceof StandardJavadocTagType standardType) {
      return handleStandardInlineTag(tag, standardType);
    }

    return "&lt;" + tag.getTagType().getName() + "&gt;"
           + visitList(tag.getElements())
           + "&lt;/" + tag.getTagType().getName() + "&gt;";
  }

  private String handleStandardInlineTag(
      JavadocInlineTag tag,
      StandardJavadocTagType standardType
  ) {
    List<JavadocElement> arguments = tag.getElements();

    return switch (standardType) {
      case CODE -> {
        String text = Entities.escape(visitList(arguments));
        if (text.strip().lines().count() > 1) {
          yield "<pre><code class=\"language-java\">" + text + "</code></pre>";
        } else {
          yield "<code>" + text + "</code>";
        }
      }
      case DOC_ROOT -> baseUrl;
      case INDEX -> arguments.get(0).accept(this); // skip explanation
      case INHERIT_DOC -> throw new RuntimeException("TODO");
      case LINK, LINKPLAIN -> {
        if (arguments.get(0) instanceof OurJavadocReference ourJavadocReference) {
          DocumentedElementReference reference = ourJavadocReference.getDocumentedReference();
          if (arguments.size() == 1) {
            yield formatLink(Optional.empty(), reference);
          } else if (arguments.size() == 2) {
            yield formatLink(Optional.of(arguments.get(1).accept(this)), reference);
          } else {
            throw new RuntimeException("TODO: More than 2 arguments for link?");
          }
        } else {
          yield visitList(arguments);
        }
      }
      case LITERAL -> Entities.escape(visitList(arguments));
      case RETURN -> "Returns " + visitList(tag.getElements());
      case SNIPPET -> throw new RuntimeException("TODO: Snippets");
      case SUMMARY -> visitList(arguments);
      case SYSTEM_PROPERTY ->
          "&lt;systemProperty&gt;" + visitList(arguments) + "&lt;/systemProperty&gt";
      case VALUE -> "&lt;value&gt;" + visitList(arguments) + "&lt;/value&gt;";
      default -> throw new IllegalStateException("Unexpected value: " + standardType);
    };
  }

  @Override
  public String visitBlockTag(JavadocBlockTag tag) {
    if (tag.getTagType() instanceof StandardJavadocTagType standardType) {
      return handleStandardBlockTag(tag, standardType);
    }
    return JavadocVisitor.super.visitBlockTag(tag);
  }

  private String handleStandardBlockTag(JavadocBlockTag tag, StandardJavadocTagType type) {
    List<JavadocElement> elements = tag.getElements();

    return switch (type) {
      case AUTHOR,
          DEPRECATED,
          EXCEPTION,
          PROVIDES,
          RETURN,
          SEE,
          SERIAL,
          SERIAL_DATA,
          SERIAL_FIELD,
          SINCE,
          THROWS,
          USES,
          VERSION -> "@<strong>" + type.getName() + "</strong>: " + visitList(elements);
      case HIDDEN -> "<strong>Pssh</strong> This should be hidden";
      case PARAM -> "@<strong>" + elements.get(0).accept(this)
                    + "</strong> "
                    + visitList(elements.subList(1, elements.size()));
      default -> throw new IllegalStateException("Unexpected value: " + type);
    };
  }

  @Override
  public String visitSnippet(JavadocSnippetTag snippet) {
    // TODO: Don't be lazy
    return "Snippet support was not included because I am lazy";
  }

  @Override
  public String visitText(JavadocText text) {
    return text.getText();
  }

  @Override
  public String visitReference(JavadocReference reference) {
    return formatLink(
        Optional.empty(),
        ((OurJavadocReference) reference.getReference()).getDocumentedReference()
    );
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private String formatLink(Optional<String> label, DocumentedElementReference reference) {
    String url = linkResolver.resolve(reference, baseUrl);
    return "<a href=\"" + url + "\"><code>" +
           Entities.escape(label.orElse(getImplicitReferenceLabel(reference)))
           + "</code></a>";
  }

  private String getImplicitReferenceLabel(DocumentedElementReference reference) {
    if (reference.isField() || reference.isMethod()) {
      var parts = new ArrayList<>(reference.toParts());
      Collections.reverse(parts);

      String prefix = parts.stream()
          .filter(it -> !it.isField() && !it.isMethod())
          .findFirst()
          .map(DocumentedElementReference::asQualifiedName)
          .orElse("");
      String label = reference.asQualifiedName().replace(prefix, "");

      Optional<String> declaringType = reference.getType().map(it -> it.segment().toString());
      if (declaringType.isPresent() && label.contains("<init>")) {
        label = label.replace("<init>", declaringType.get());
      }

      if (reference.isMethod()) {
        return label + ")";
      }
      return label;
    }
    return reference.segment().toString();
  }

  private String visitList(List<JavadocElement> elements) {
    if (elements.isEmpty()) {
      return "";
    }
    StringBuilder result = new StringBuilder();
    for (JavadocElement element : elements) {
      result.append(element.accept(this));
    }
    return result.toString();
  }
}
