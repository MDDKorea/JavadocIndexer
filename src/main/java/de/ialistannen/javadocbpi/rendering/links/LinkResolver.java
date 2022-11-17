package de.ialistannen.javadocbpi.rendering.links;

import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference;
import java.util.regex.Pattern;

public interface LinkResolver {

  String resolve(DocumentedElementReference reference, String baseUrl);

  default String formatNamePart(DocumentedElementReference name) {
    String urlPart;

    if (!name.isMethod() && !name.isField()) {
      urlPart = name.asQualifiedName();
    } else {
      urlPart = name.getType().map(DocumentedElementReference::toString).orElse("N/A");
    }
    String moduleName = name.getModule().map(it -> it.segment().toString() + "/").orElse("");

    urlPart = urlPart
        .replaceFirst(Pattern.quote(moduleName), "")
        .replace(".", "/")
        .replace("$", ".");

    if (name.isMethod() || name.isField()) {
      String fullName = name.asQualifiedName();
      urlPart += "#" + fullName.substring(fullName.indexOf("#") + 1);
      if (name.isMethod()) {
        urlPart += ")";
      }
    }

    urlPart = urlPart.replace("#", ".html#");
    if (!urlPart.contains("#")) {
      urlPart += ".html";
    }

    return urlPart;
  }

}
