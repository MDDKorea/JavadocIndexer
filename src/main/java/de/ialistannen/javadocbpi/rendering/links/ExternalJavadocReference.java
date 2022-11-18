package de.ialistannen.javadocbpi.rendering.links;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record ExternalJavadocReference(
    String baseUrl,
    Set<String> packages,
    Map<String, String> packageToModuleMap
) {

  public ExternalJavadocReference(String baseUrl, Set<String> packages) {
    this(baseUrl, packages, Map.of());
  }

  public ExternalJavadocReference(String baseUrl, Map<String, String> packageToModuleMap) {
    this(baseUrl, packageToModuleMap.keySet(), packageToModuleMap);
  }

  public Optional<String> getModule(String packageName) {
    return Optional.ofNullable(packageToModuleMap.get(packageName));
  }

  public ExternalJavadocReference {
    packages = Set.copyOf(packages);
    packageToModuleMap = Map.copyOf(packageToModuleMap);
  }
}
