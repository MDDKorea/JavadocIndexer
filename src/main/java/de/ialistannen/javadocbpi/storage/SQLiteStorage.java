package de.ialistannen.javadocbpi.storage;

import de.ialistannen.javadocbpi.model.elements.DocumentedElement;
import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference;
import de.ialistannen.javadocbpi.model.elements.DocumentedElements;
import de.ialistannen.javadocbpi.util.Resources;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.JournalMode;
import org.sqlite.SQLiteDataSource;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

public class SQLiteStorage {

  private final SQLiteDataSource dataSource;
  private final JsonSerializer serializer;

  public SQLiteStorage(Path path, JsonSerializer serializer) {
    this.serializer = serializer;

    SQLiteConfig sqliteConfig = new SQLiteConfig();
    sqliteConfig.enforceForeignKeys(true);
    sqliteConfig.setJournalMode(JournalMode.WAL);

    dataSource = new SQLiteConnectionPoolDataSource(sqliteConfig);
    dataSource.setUrl("jdbc:sqlite:" + path);
  }

  public void writeDatabase(DocumentedElements elements) throws SQLException, IOException {
    try (Connection connection = dataSource.getConnection()) {
      PreparedStatement createTableStatement = connection.prepareStatement(
          Resources.readAsString("/db/Init.sql")
      );
      createTableStatement.execute();

      Set<String> names = new HashSet<>();
      PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO JavadocElements (qualified_name, full_reference, type, value) VALUES (?, ?, ?, ?);"
      );
      int processedCount = 0;
      connection.setAutoCommit(false);
      for (var entry : elements.getElements().entrySet()) {
        if (!names.add(entry.getKey().asQualifiedName())) {
          System.out.println("OH NO " + entry.getKey().asQualifiedName() + " " + entry.getKey());
        }
        statement.setString(1, entry.getKey().asQualifiedName());
        statement.setString(2, serializer.serializeReference(entry.getKey()));
        statement.setString(3, entry.getKey().type().name());
        statement.setString(4, serializer.serializeElement(entry.getValue()));
        statement.addBatch();
        if (processedCount++ % 1000 == 0) {
          System.out.println("Flushed batch " + processedCount);
          statement.executeBatch();
          connection.commit();
        }
      }
      statement.executeBatch();
      connection.commit();
      connection.setAutoCommit(true);
    }
  }

  public Set<DocumentedElementReference> getAllReferences() throws SQLException, IOException {
    Set<DocumentedElementReference> references = new HashSet<>();

    try (Connection connection = dataSource.getConnection()) {
      PreparedStatement statement = connection.prepareStatement(
          "SELECT full_reference FROM JavadocElements"
      );
      statement.execute();
      ResultSet resultSet = statement.getResultSet();
      while (resultSet.next()) {
        references.add(
            serializer.deserializeReference(resultSet.getString("full_reference"))
        );
      }
    }

    return references;
  }

  public Optional<FetchResult> get(String qualifiedName) throws SQLException, IOException {
    try (Connection connection = dataSource.getConnection()) {
      PreparedStatement statement = connection.prepareStatement(
          "SELECT full_reference, value FROM JavadocElements WHERE qualified_name = ?"
      );
      statement.setString(1, qualifiedName);
      statement.execute();
      ResultSet resultSet = statement.getResultSet();
      if (!resultSet.next()) {
        return Optional.empty();
      }
      return Optional.of(new FetchResult(
          serializer.deserializeReference(resultSet.getString("full_reference")),
          serializer.deserializeElement(resultSet.getString("value"))
      ));
    }
  }

  public DocumentedElements getAll() throws SQLException, IOException {
    try (Connection connection = dataSource.getConnection()) {
      PreparedStatement statement = connection.prepareStatement("SELECT * FROM JavadocElements");
      statement.execute();
      ResultSet resultSet = statement.getResultSet();

      DocumentedElements elements = new DocumentedElements();
      while (resultSet.next()) {
        DocumentedElement element = serializer.deserializeElement(resultSet.getString("value"));
        DocumentedElementReference reference = serializer.deserializeReference(
            resultSet.getString("full_reference")
        );
        elements.add(reference, element);
      }

      return elements;
    }
  }

  public record FetchResult(DocumentedElementReference reference, DocumentedElement element) {

  }

}
