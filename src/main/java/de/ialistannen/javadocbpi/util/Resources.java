package de.ialistannen.javadocbpi.util;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Resources {

  public static String readAsString(String path) throws IOException {
    try (InputStream inputStream = requireNonNull(Resources.class.getResourceAsStream(path))) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      inputStream.transferTo(outputStream);

      return outputStream.toString(StandardCharsets.UTF_8);
    }
  }
}
