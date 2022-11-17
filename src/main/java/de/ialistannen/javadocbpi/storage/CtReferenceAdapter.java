package de.ialistannen.javadocbpi.storage;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;
import spoon.javadoc.api.StandardJavadocTagType;
import spoon.reflect.reference.CtReference;

public class CtReferenceAdapter {

  @FromJson
  CtReference referenceFromJson(String ignored) {
    return null;
  }

  @ToJson
  String referenceToJson(CtReference reference) {
    throw new RuntimeException(":(");
  }

  @ToJson
  TagTypeRecord standardTagToJson(StandardJavadocTagType type) {
    return new TagTypeRecord(type.name());
  }

  @FromJson
  StandardJavadocTagType standardTagFromJson(TagTypeRecord type) {
    return StandardJavadocTagType.valueOf(type.name);
  }

  public record TagTypeRecord(String name) {

  }

}
