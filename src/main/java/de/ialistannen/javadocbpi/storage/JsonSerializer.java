package de.ialistannen.javadocbpi.storage;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Moshi.Builder;
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory;
import de.ialistannen.javadocbpi.model.elements.DocumentedElement;
import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference;
import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference.PathElement;
import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference.ReferencePathElement;
import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference.StringPathElement;
import de.ialistannen.javadocbpi.model.elements.DocumentedField;
import de.ialistannen.javadocbpi.model.elements.DocumentedMethod;
import de.ialistannen.javadocbpi.model.elements.DocumentedModule;
import de.ialistannen.javadocbpi.model.elements.DocumentedPackage;
import de.ialistannen.javadocbpi.model.elements.DocumentedType;
import de.ialistannen.javadocbpi.model.javadoc.OurJavadocReference;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import spoon.javadoc.api.JavadocTagCategory;
import spoon.javadoc.api.JavadocTagType;
import spoon.javadoc.api.StandardJavadocTagType;
import spoon.javadoc.api.elements.JavadocBlockTag;
import spoon.javadoc.api.elements.JavadocElement;
import spoon.javadoc.api.elements.JavadocInlineTag;
import spoon.javadoc.api.elements.JavadocText;
import spoon.javadoc.api.elements.snippets.JavadocSnippetTag;

public class JsonSerializer {

  private final Moshi moshi;

  public JsonSerializer() {
    moshi = new Builder()
        .add(
            PolymorphicJsonAdapterFactory.of(DocumentedElement.class, "poly-type")
                .withSubtype(DocumentedField.class, "field")
                .withSubtype(DocumentedMethod.class, "method")
                .withSubtype(DocumentedType.class, "type")
                .withSubtype(DocumentedPackage.class, "package")
                .withSubtype(DocumentedModule.class, "module")
        ).add(
            PolymorphicJsonAdapterFactory.of(JavadocElement.class, "poly-type")
                .withSubtype(JavadocText.class, "text")
                .withSubtype(JavadocInlineTag.class, "inline-tag")
                .withSubtype(JavadocBlockTag.class, "inline-block")
                .withSubtype(OurJavadocReference.class, "reference")
                .withSubtype(JavadocSnippetTag.class, "snippet")
        )
        .add(
            PolymorphicJsonAdapterFactory.of(JavadocTagType.class, "poly-type")
                .withSubtype(StandardJavadocTagType.class, "standard")
                .withFallbackJsonAdapter(new UnknownTagTypeAdapter())
        )
        .add(
            PolymorphicJsonAdapterFactory.of(PathElement.class, "poly-type")
                .withSubtype(StringPathElement.class, "string")
                .withSubtype(ReferencePathElement.class, "reference")
        )
        .add(new CtReferenceAdapter())
        .build();
  }

  public String serializeReference(DocumentedElementReference reference) {
    return moshi.adapter(DocumentedElementReference.class).toJson(reference);
  }

  public String serializeElement(DocumentedElement element) {
    return moshi.adapter(DocumentedElement.class).toJson(element);
  }

  public DocumentedElementReference deserializeReference(String json) throws IOException {
    return moshi.adapter(DocumentedElementReference.class).fromJson(json);
  }

  public DocumentedElement deserializeElement(String json) throws IOException {
    return moshi.adapter(DocumentedElement.class).fromJson(json);
  }

  private static class UnknownTagTypeAdapter extends JsonAdapter<Object> {

    @SuppressWarnings("unchecked")
    @Override
    public Object fromJson(JsonReader reader) throws IOException {
      Map<String, Object> value = (Map<String, Object>) reader.readJsonValue();
      String name = (String) Objects.requireNonNull(value).get("name");
      JavadocTagCategory[] categories = ((List<String>) value.get("categories"))
          .stream()
          .map(JavadocTagCategory::valueOf)
          .toArray(JavadocTagCategory[]::new);
      return JavadocTagType.unknown(name, categories);
    }

    @Override
    public void toJson(JsonWriter writer, Object value) throws IOException {
      JavadocTagType type = (JavadocTagType) value;
      Map<String, Object> map = new HashMap<>();
      map.put("name", type.getName());
      map.put("categories", type.categories().stream().map(Enum::name).toList());
      map.put("poly-type", "custom");
      writer.jsonValue(map);
    }
  }
}
