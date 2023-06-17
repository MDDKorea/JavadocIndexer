package de.ialistannen.javadocbpi.spoon;

import static de.ialistannen.javadocbpi.spoon.FluentFilter.forType;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtModule;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;

public class IndexerFilterChain {

  private final Set<String> packageWhitelist;

  public IndexerFilterChain(CtModel model, Set<String> packageOrModuleWhitelist) {
    this.packageWhitelist = packageOrModuleWhitelist.stream()
        .filter(it -> !it.endsWith("/"))
        .collect(Collectors.toCollection(HashSet::new));

    packageOrModuleWhitelist.stream()
        .filter(it -> it.endsWith("/"))
        .flatMap(it -> exportedPackages(model, it))
        .forEach(this.packageWhitelist::add);
  }

  public FluentFilter asFilter() {
    FluentFilter filter = element -> false;
    filter = filter.or(forType(CtType.class, CtModifiable::isPublic));
    filter = filter.or(forType(CtField.class, it -> it.isPublic() || it.isProtected()));
    filter = filter.or(forType(CtConstructor.class, it -> it.isProtected() || it.isPublic()));
    filter = filter.or(forType(CtMethod.class, it -> it.isProtected() || it.isPublic()));

    filter = filter.or(forType(CtPackage.class, this::includePackage));

    filter = filter.or(forType(CtModule.class, ignored -> true));

    return filter;
  }

  private Stream<String> exportedPackages(CtModel model, String moduleName) {
    return model.getAllModules()
        .stream()
        .filter(module -> module.getSimpleName().equals(moduleName.replace("/", "")))
        .flatMap(module -> module.getExportedPackages().stream())
        .filter(exportedPackage -> exportedPackage.getTargetExport().isEmpty())
        .map(exportedPackage -> exportedPackage.getPackageReference().getQualifiedName());
  }

  private boolean includePackage(CtPackage it) {
    if (packageWhitelist.contains("*")) {
      return true;
    }

    if (packageWhitelist.contains(it.getQualifiedName())) {
      return true;
    }

    for (String allowedPackage : packageWhitelist) {
      if (allowedPackage.startsWith(it.getQualifiedName())) {
        return true;
      }
    }
    return false;
  }
}
