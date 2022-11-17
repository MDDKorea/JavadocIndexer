package de.ialistannen.javadocbpi.rendering.links;

import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Java8LinkResolver implements LinkResolver {

  @Override
  public String resolve(
      DocumentedElementReference reference,
      String baseUrl
  ) {

    if (baseUrl.endsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }

    String urlPart = formatNamePart(reference);

    urlPart = urlPart
        .replace("(", "-")
        .replace(")", "-")
        .replace(",", "-");

    return baseUrl + "/" + urlPart;
  }

}
