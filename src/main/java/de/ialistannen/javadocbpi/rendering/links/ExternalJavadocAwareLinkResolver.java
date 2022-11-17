package de.ialistannen.javadocbpi.rendering.links;

import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference;
import java.util.List;

public class ExternalJavadocAwareLinkResolver implements LinkResolver {

  private final LinkResolver underlying;
  private final List<ExternalJavadocReference> externalJavadocReferences;

  public ExternalJavadocAwareLinkResolver(
      LinkResolver underlying,
      List<ExternalJavadocReference> externalJavadocReferences
  ) {
    this.underlying = underlying;
    this.externalJavadocReferences = List.copyOf(externalJavadocReferences);
  }

  @Override
  public String resolve(
      DocumentedElementReference reference,
      String baseUrl
  ) {
    String packageName = reference.getPackage().map(it -> it.segment().toString()).orElse("");

    for (ExternalJavadocReference externalReference : externalJavadocReferences) {
      if (externalReference.packages().contains(packageName)) {
        return underlying.resolve(reference, externalReference.baseUrl());
      }
    }

    return underlying.resolve(reference, baseUrl);
  }
}
