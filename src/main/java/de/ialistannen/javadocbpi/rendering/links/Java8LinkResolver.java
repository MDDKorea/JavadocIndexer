package de.ialistannen.javadocbpi.rendering.links;

import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference;

public class Java8LinkResolver implements LinkResolver {

  @Override
  public String resolve(
      DocumentedElementReference reference,
      String baseUrl
  ) {
    if (baseUrl.endsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }

    String urlPart = formatNamePart(reference)
        .replace("(", "-")
        .replace(")", "-")
        .replace(",", "-");

    return baseUrl + "/" + urlPart;
  }

}
