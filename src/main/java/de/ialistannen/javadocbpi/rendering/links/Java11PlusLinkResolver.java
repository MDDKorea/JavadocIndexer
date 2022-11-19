package de.ialistannen.javadocbpi.rendering.links;

import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference;
import de.ialistannen.javadocbpi.model.elements.DocumentedElementType;

public class Java11PlusLinkResolver implements LinkResolver {

  @Override
  public String resolve(
      DocumentedElementReference reference,
      String baseUrl
  ) {
    if (baseUrl.endsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }

    String urlPart = formatNamePart(reference);
    String module = reference.getModule().orElse("");

    if (!module.isEmpty()) {
      if (reference.type() == DocumentedElementType.MODULE) {
        urlPart = "module-summary.html";
      }
      urlPart = module + "/" + urlPart;
    }

    return baseUrl + "/" + urlPart;
  }

}
