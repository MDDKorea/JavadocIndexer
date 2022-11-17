package de.ialistannen.javadocbpi;

import de.ialistannen.javadocbpi.classpath.GradleParser;
import de.ialistannen.javadocbpi.classpath.Pom;
import de.ialistannen.javadocbpi.classpath.PomClasspathDiscoverer;
import de.ialistannen.javadocbpi.classpath.PomParser;
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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.maven.shared.invoker.MavenInvocationException;
import spoon.Launcher;
import spoon.OutputType;
import spoon.javadoc.api.elements.JavadocElement;
import spoon.reflect.CtModel;
import spoon.support.compiler.ProgressLogger;
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
            List.of(Path.of("pom.xml")), Path.of("/opt/maven"), launcher
        )
    );
    System.out.println("Spoon successfully configured\n");

    System.out.println(heading("Building spoon model"));
    CtModel model = timings.measure("build-model", launcher::buildModel);
    System.out.println("Model successfully built\n");

    System.out.println(heading("Converting Spoon Model "));
    Converter converter = new Converter();
    ParallelProcessor processor = new ParallelProcessor(
        new IndexerFilterChain(Set.of("*")).asFilter(),
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
    System.exit(0);

    System.out.println();
    PrefixTrie trie = PrefixTrie.forElements(elements);
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
        new QueryTokenizer().tokenize("dsTestClass")
    ));
    System.out.println(new QueryTokenizer().tokenize("java.base/java.lang.String#lines"));

    for (var entry : elements.getElements().entrySet()) {
      if (entry.getKey().asQualifiedName().endsWith(".TestClass")) {
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

  private static void configureInputClassLoader(List<Path> buildFiles, Path mavenHome,
      Launcher launcher)
      throws IOException {
    try {
      Pom pom = new Pom(Set.of(), Set.of(), Set.of());
      GradleParser gradleParser = new GradleParser();
      PomParser pomParser = new PomParser();
      for (Path buildFile : buildFiles) {
        if (buildFile.getFileName().toString().contains("gradle")) {
          System.out.println(heading("Parsing build.gradle", 2));
          pom = pom.merge(gradleParser.parseGradleFile(Files.readString(buildFile)));
          System.out.println("  Successfully parsed build.gradle file");
        } else {
          System.out.println(heading("Parsing POM", 2));
          pom = pom.merge(pomParser.parsePom(Files.readString(buildFile)));
          System.out.println("  Successfully parsed POM");
        }
      }

      System.out.println(heading("Building classpath from POM", 2));

      Path outputPomFile = Files.createTempFile("Generatedpom", ".xml");
      outputPomFile.toFile().deleteOnExit();
      Files.writeString(outputPomFile, pom.format());

      List<Path> classpath = new PomClasspathDiscoverer().findClasspath(outputPomFile, mavenHome);

      List<URL> urls = new ArrayList<>();
      for (Path path : classpath) {
        System.out.println("    " + path);
        urls.add(path.toUri().toURL());
      }
      launcher.getEnvironment().setInputClassLoader(new URLClassLoader(urls.toArray(URL[]::new)));

      System.out.println("  Classpath successfully built\n");
    } catch (MavenInvocationException e) {
      throw new IOException("Error invoking maven", e);
    }
  }

  private static class ConsoleProcessLogger extends ProgressLogger {

    public int touchedClasses;

    public ConsoleProcessLogger(Launcher launcher) {
      super(launcher.getEnvironment());
      touchedClasses = 0;
    }

    @Override
    public void start(Process process) {
      System.out.println("Starting phase " + process);
      touchedClasses = 0;
    }

    @Override
    public void step(Process process, String task, int taskId, int nbTask) {
      touchedClasses++;
      if (touchedClasses % 1000 == 0) {
        System.out.println(
            "Phase " + process + " has discovered " + touchedClasses
            + " classes so far. Currently working on " + task
        );
      }
    }

    @Override
    public void end(Process process) {
      System.out.println("Phase " + process + " done! Discovered Classes: " + touchedClasses);
    }
  }
}
