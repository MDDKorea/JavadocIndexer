package de.ialistannen.javadocbpi.rendering.links;

import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference;
import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference.StringPathElement;
import de.ialistannen.javadocbpi.model.elements.DocumentedElementType;
import java.util.List;
import java.util.Optional;

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
    String packageName = reference.getPackage().orElse("");

    for (ExternalJavadocReference externalReference : externalJavadocReferences) {
      if (externalReference.packages().contains(packageName)) {
        Optional<String> module = externalReference.getModule(packageName);
        if (reference.getModule().isEmpty() && module.isPresent()) {
          return resolveWithAdjustedModule(reference, externalReference, module.get());
        }
        return underlying.resolve(reference, externalReference.baseUrl());
      }
    }

    return underlying.resolve(reference, baseUrl);
  }

  private String resolveWithAdjustedModule(
      DocumentedElementReference reference,
      ExternalJavadocReference externalReference,
      String newModule
  ) {
    DocumentedElementReference module = new DocumentedElementReference(
        null,
        new StringPathElement(newModule),
        DocumentedElementType.MODULE
    );
    return underlying.resolve(
        reference.withModule(module),
        externalReference.baseUrl()
    );
  }
}
