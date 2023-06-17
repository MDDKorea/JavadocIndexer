package de.ialistannen.javadocbpi;

import static de.ialistannen.javadocbpi.Indexer.configureInputClassLoader;

import de.ialistannen.javadocbpi.Indexer.ConsoleProcessLogger;
import de.ialistannen.javadocbpi.model.elements.DocumentedElement;
import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference;
import de.ialistannen.javadocbpi.model.elements.DocumentedElements;
import de.ialistannen.javadocbpi.query.CaseSensitivity;
import de.ialistannen.javadocbpi.query.MatchingStrategy;
import de.ialistannen.javadocbpi.query.PrefixTrie;
import de.ialistannen.javadocbpi.query.QueryTokenizer;
import de.ialistannen.javadocbpi.rendering.DeclarationRenderer;
import de.ialistannen.javadocbpi.rendering.HtmlRenderVisitor;
import de.ialistannen.javadocbpi.rendering.links.Java11PlusLinkResolver;
import de.ialistannen.javadocbpi.spoon.Converter;
import de.ialistannen.javadocbpi.spoon.IndexerFilterChain;
import de.ialistannen.javadocbpi.spoon.ParallelProcessor;
import de.ialistannen.javadocbpi.storage.JsonSerializer;
import de.ialistannen.javadocbpi.storage.SQLiteStorage;
import de.ialistannen.javadocbpi.util.Timings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import spoon.Launcher;
import spoon.OutputType;
import spoon.javadoc.api.elements.JavadocElement;
import spoon.reflect.CtModel;
import spoon.support.compiler.ZipFolder;

public class Main {

  public static void main(String[] args) throws IOException, SQLException {
    Timings timings = new Timings();

    System.out.println(heading("Configuring spoon"));
    timings.startTimer("configure-launcher");
    Launcher launcher = new Launcher();
    launcher.getEnvironment().setShouldCompile(false);
    launcher.getEnvironment().disableConsistencyChecks();
    launcher.getEnvironment().setOutputType(OutputType.NO_OUTPUT);
    launcher.getEnvironment().setSpoonProgress(new ConsoleProcessLogger(launcher));
    launcher.getEnvironment().setCommentEnabled(true);
    launcher.getEnvironment().setComplianceLevel(18);
    for (String path : List.of("src/test")) {
      if (path.endsWith(".zip")) {
        launcher.addInputResource(new ZipFolder(new File(path)));
      } else {
        launcher.addInputResource(path);
      }
    }
    timings.stopTimer("configure-launcher");

    timings.measure(
        "build-classpath",
        () -> configureInputClassLoader(
            List.of(Path.of("pom.xml")), Path.of("/opt/maven"), null, launcher
        )
    );
    System.out.println("Spoon successfully configured\n");

    System.out.println(heading("Building spoon model"));
    CtModel model = timings.measure("build-model", launcher::buildModel);
    System.out.println("Model successfully built\n");

    System.out.println(heading("Converting Spoon Model "));
    Converter converter = new Converter();
    ParallelProcessor processor = new ParallelProcessor(
        new IndexerFilterChain(model, Set.of("*")).asFilter(),
        1
    );
    timings.measure("process-model", () -> {
      model.getAllModules()
          .forEach(it -> processor.process(
              it,
              element -> element.accept(converter))
          );
      processor.shutdown();
    });
    System.out.println("Model successfully converted\n");

    DocumentedElements elements = converter.getElements();

    System.out.println(heading("Writing to output database"));
    Path dbPath = Path.of("target/foo.db");
    if (Files.exists(dbPath)) {
      Files.delete(dbPath);
    }
    SQLiteStorage storage = new SQLiteStorage(
        dbPath,
        new JsonSerializer()
    );
    storage.writeDatabase(elements);

    for (DocumentedElementReference reference : storage.getAllReferences()) {
      System.out.println(reference.asQualifiedName());
    }

    System.out.println();
    for (DocumentedElement value : elements.getElements().values()) {
      System.out.println(new DeclarationRenderer().renderDeclaration(value));
    }

    System.out.println();
    PrefixTrie trie = PrefixTrie.forElements(elements);
    System.out.println(trie.find(
        MatchingStrategy.EXACT, CaseSensitivity.CONSIDER_CASE,
        new QueryTokenizer().tokenize("TestClass#first(int, String...)")
    ));
    System.out.println(trie.find(
        MatchingStrategy.EXACT, CaseSensitivity.CONSIDER_CASE,
        new QueryTokenizer().tokenize("analysable.pack.TestClass")
    ));
    System.out.println(trie.find(
        MatchingStrategy.PREFIX, CaseSensitivity.CONSIDER_CASE,
        new QueryTokenizer().tokenize("a.p.TestClass")
    ));
    System.out.println(trie.find(
        MatchingStrategy.PREFIX, CaseSensitivity.CONSIDER_CASE,
        new QueryTokenizer().tokenize("p.TestClass")
    ));
    System.out.println(trie.find(
        MatchingStrategy.EXACT, CaseSensitivity.CONSIDER_CASE,
        new QueryTokenizer().tokenize("TestClass")
    ));
    System.out.println(trie.find(
        MatchingStrategy.EXACT, CaseSensitivity.CONSIDER_CASE,
        new QueryTokenizer().tokenize("TestClass#foo(int, java.String")
    ));
    System.out.println(trie.find(
        MatchingStrategy.EXACT, CaseSensitivity.CONSIDER_CASE,
        new QueryTokenizer().tokenize("TestClass#foo(")
    ));
    System.out.println(trie.autocomplete(
        MatchingStrategy.PREFIX, CaseSensitivity.CONSIDER_CASE,
        new QueryTokenizer().tokenize("TestClass"),
        20
    ));
    System.out.println(trie.find(
        MatchingStrategy.EXACT, CaseSensitivity.CONSIDER_CASE,
        new QueryTokenizer().tokenize("TestClass#TestClass")
    ));
    System.out.println(trie.find(
        MatchingStrategy.PREFIX, CaseSensitivity.CONSIDER_CASE,
        new QueryTokenizer().tokenize("TestEnum#AYY")
    ));
    System.out.println(trie.find(
        MatchingStrategy.EXACT, CaseSensitivity.CONSIDER_CASE,
        new QueryTokenizer().tokenize("dsTestClass")
    ));
    System.out.println(trie.autocomplete(
        MatchingStrategy.PREFIX, CaseSensitivity.CONSIDER_CASE,
        new QueryTokenizer().tokenize("TestClass#"),
        10
    ));
    System.out.println(new QueryTokenizer().tokenize("java.base/java.lang.String#lines"));

    for (var entry : elements.getElements().entrySet()) {
      if (entry.getKey().asQualifiedName().contains(".TestClass")) {
        for (JavadocElement element : entry.getValue().javadoc()) {
          System.out.print(element.accept(
              new HtmlRenderVisitor(new Java11PlusLinkResolver(), "https://example.com")
          ));
        }
      }
    }
    System.out.println();
  }

  private static String heading(String text) {
    return heading(text, 0);
  }

  private static String heading(String text, int indent) {
    return "\n" + " ".repeat(indent) + "\033[94;1m==== \033[36;1m" + text
           + " \033[94;1m====\033[0m";
  }

}
