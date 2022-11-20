package de.ialistannen.javadocbpi.rendering.links;

import static de.ialistannen.javadocbpi.model.elements.DocumentedElementType.METHOD;
import static de.ialistannen.javadocbpi.model.elements.DocumentedElementType.MODULE;
import static de.ialistannen.javadocbpi.model.elements.DocumentedElementType.PACKAGE;
import static de.ialistannen.javadocbpi.model.elements.DocumentedElementType.TYPE;

import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference;
import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference.ReferencePathElement;
import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference.StringPathElement;
import de.ialistannen.javadocbpi.model.elements.DocumentedElementType;

public interface LinkResolver {

  String resolve(DocumentedElementReference reference, String baseUrl);

  default String formatNamePart(DocumentedElementReference name) {
    String urlPart = formatUrl(name);
    if (name.isMethod()) {
      urlPart += ")";
    }

    // fixup method names as the parameters are an anchor
    urlPart = urlPart.replace("#", ".html#");
    if (!urlPart.contains(".html")) {
      urlPart += ".html";
    }

    // Fixup summaries
    if (name.type() == DocumentedElementType.PACKAGE) {
      urlPart = urlPart.replace(".html", "/package-summary.html");
    } else if (name.type() == DocumentedElementType.MODULE) {
      urlPart = urlPart.replace(".html", "/module-summary.html");
    }

    return urlPart;
  }

  private String formatUrl(DocumentedElementReference reference) {
    // Do not print "unnamed module"
    if (reference.type() == MODULE && reference.getModule().isEmpty()) {
      return "";
    }

    // First parameter has no preceding separator
    if (reference.isMethodParameter() && reference.isFirstMethodParameter()) {
      return formatUrl(reference.nullableParent()) + formatParameterType(reference);
    }
    // Further parameters are separated by commas
    if (reference.isMethodParameter()) {
      return formatUrl(reference.nullableParent()) + "," + formatParameterType(reference);
    }

    String result;
    if (reference.nullableParent() != null) {
      String separator;
      if (reference.isMethod() || reference.isField()) {
        // Methods and fields are anchors
        separator = "#";
      } else if (reference.nullableParent().type() == TYPE) {
        // Inner types are separated by dots
        separator = ".";
      } else if (reference.nullableParent().type() == PACKAGE) {
        // Packages are sub-folders
        separator = "/";
      } else if (reference.nullableParent().type() == MODULE) {
        // Module names are kept as-is but the module is separated by a slash
        separator = "/";
      } else {
        throw new IllegalArgumentException("Unknown parent type: " + this);
      }

      // Format the parent and attach us
      result = formatUrl(reference.nullableParent()) + separator + reference.segment().toString();
    } else {
      result = reference.segment().toString();
    }

    // Methods need a "(" even if they do not have any children (i.e. parameters), so do it here
    if (reference.type() == METHOD) {
      result += "(";
    }
    return result;
  }

  private String formatParameterType(DocumentedElementReference param) {
    return switch (param.segment()) {
      case StringPathElement path -> path.segment();
      case ReferencePathElement path -> {
        // Strip module as it is not path of the parameters for some reason...
        String fqn = path.reference().asQualifiedName();
        if (path.reference().getModule().isPresent()) {
          fqn = fqn.replace(path.reference().getModule().get() + "/", "");
        }
        yield fqn;
      }
    };
  }
}
