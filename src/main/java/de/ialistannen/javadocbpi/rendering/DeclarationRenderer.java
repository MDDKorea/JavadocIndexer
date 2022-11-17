package de.ialistannen.javadocbpi.rendering;

import de.ialistannen.javadocbpi.model.elements.DocumentedElement;
import de.ialistannen.javadocbpi.model.elements.DocumentedField;
import de.ialistannen.javadocbpi.model.elements.DocumentedMethod;
import de.ialistannen.javadocbpi.model.elements.DocumentedModule;
import de.ialistannen.javadocbpi.model.elements.DocumentedPackage;
import de.ialistannen.javadocbpi.model.elements.DocumentedType;
import de.ialistannen.javadocbpi.model.elements.DocumentedType.Type;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import spoon.reflect.declaration.ModifierKind;

public class DeclarationRenderer {

  public String renderDeclaration(DocumentedElement element) {
    return switch (element) {
      case DocumentedModule module -> render(module);
      case DocumentedPackage pack -> render(pack);
      case DocumentedType type -> render(type);
      case DocumentedField field -> render(field);
      case DocumentedMethod method -> render(method);
    };
  }

  private String render(DocumentedModule module) {
    return "module " + module.name();
  }

  private String render(DocumentedPackage pack) {
    return "package " + pack.name();
  }

  private String render(DocumentedType type) {
    String result = renderModifiers(filterModifiers(type.type(), type.modifiers()));
    if (!type.modifiers().isEmpty()) {
      result += " ";
    }
    result += switch (type.type()) {
      case ANNOTATION -> "@interface";
      case ENUM -> "enum";
      case CLASS -> "class";
      case INTERFACE -> "interface";
      case RECORD -> "record";
    };

    result += " " + type.name();
    if (!type.renderedTypeParameters().isEmpty()) {
      result += "<" + String.join(", ", type.renderedTypeParameters()) + ">";
    }

    if (type.type() == Type.CLASS && type.hasSuperclass()) {
      result += " extends " + type.renderedSuperclass();
    }
    if (!type.renderedSuperInterfaces().isEmpty()) {
      result += " implements ";
      result += String.join(", ", type.renderedSuperInterfaces());
    }

    return result;
  }

  private Collection<ModifierKind> filterModifiers(
      DocumentedType.Type type,
      Set<ModifierKind> modifiers
  ) {
    return switch (type) {
      case ANNOTATION, INTERFACE ->
          modifiers.stream().filter(it -> it != ModifierKind.ABSTRACT).toList();
      case ENUM, RECORD -> modifiers.stream().filter(it -> it != ModifierKind.FINAL).toList();
      case CLASS -> modifiers;
    };
  }

  private String render(DocumentedField field) {
    return renderModifiers(field.modifiers())
           + " " + field.renderedType()
           + " " + field.name();
  }

  private String render(DocumentedMethod method) {
    String result = renderModifiers(method.modifiers());
    if (!method.renderedTypeParameters().isEmpty()) {
      result += " <" + String.join(", ", method.renderedTypeParameters()) + ">";
    }
    result += " " + method.renderedReturnType();
    result += " " + method.name();
    result += method.parameters().stream()
        .map(it -> it.renderedType() + " " + it.name())
        .collect(Collectors.joining(", ", "(", ")"));

    if (!method.throwsClause().isEmpty()) {
      result += " throws ";
      result += method.throwsClause()
          .stream()
          .map(it -> it.segment().toString())
          .collect(Collectors.joining(", "));
    }

    return result;
  }

  private String renderModifiers(Collection<ModifierKind> modifiers) {
    return modifiers.stream()
        .sorted()
        .map(it -> it.name().toLowerCase(Locale.ROOT))
        .collect(Collectors.joining(" "));
  }
}
