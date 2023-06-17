package de.ialistannen.javadocbpi;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import de.ialistannen.javadocbpi.classpath.GradleClasspathParser;
import de.ialistannen.javadocbpi.classpath.Pom;
import de.ialistannen.javadocbpi.classpath.PomClasspathDiscoverer;
import de.ialistannen.javadocbpi.classpath.PomParser;
import de.ialistannen.javadocbpi.model.elements.DocumentedElements;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.maven.shared.invoker.MavenInvocationException;
import spoon.Launcher;
import spoon.OutputType;
import spoon.reflect.CtModel;
import spoon.support.compiler.ProgressLogger;
import spoon.support.compiler.ZipFolder;

public class Indexer {

  public static void main(String[] args) throws IOException, SQLException {
    if (args.length != 1) {
      System.err.println("Usage: Indexer <path to config>");
      System.exit(1);
    }
    Timings timings = new Timings();

    System.out.println(heading("Parse config"));
    TomlMapper mapper = TomlMapper.builder().build();

    IndexerConfig config = mapper.readValue(
        Files.readString(Path.of(args[0])),
        IndexerConfig.class
    );

    System.out.println(heading("Configuring spoon"));
    timings.startTimer("configure-launcher");
    Launcher launcher = new Launcher();
    launcher.getEnvironment().setShouldCompile(false);
    launcher.getEnvironment().disableConsistencyChecks();
    launcher.getEnvironment().setOutputType(OutputType.NO_OUTPUT);
    launcher.getEnvironment().setSpoonProgress(new ConsoleProcessLogger(launcher));
    launcher.getEnvironment().setCommentEnabled(true);
    launcher.getEnvironment().setComplianceLevel(18);
    for (String path : config.resourcePaths()) {
      if (path.endsWith(".zip")) {
        launcher.addInputResource(new ZipFolder(new File(path)));
      } else {
        launcher.addInputResource(path);
      }
    }
    timings.stopTimer("configure-launcher");

    if (config.buildFiles() != null && !config.buildFiles().isEmpty()) {
      timings.measure(
          "build-classpath",
          () -> configureInputClassLoader(
              config.buildFiles(), config.mavenHome(), config.javaHome(), launcher
          )
      );
    }
    System.out.println("Spoon successfully configured\n");

    System.out.println(heading("Building spoon model"));
    CtModel model = timings.measure("build-model", launcher::buildModel);
    System.out.println("Model successfully built\n");

    System.out.println(heading("Converting Spoon Model "));
    Converter converter = new Converter();
    ParallelProcessor processor = new ParallelProcessor(
        new IndexerFilterChain(model, config.allowedPackages()).asFilter(),
        Runtime.getRuntime().availableProcessors() - 1
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

    if (!config.outputPath().isBlank()) {
      System.out.println(heading("Writing to output database"));
      Path dbPath = Path.of(config.outputPath());
      if (Files.exists(dbPath)) {
        Files.delete(dbPath);
      }
      SQLiteStorage storage = new SQLiteStorage(
          dbPath,
          new JsonSerializer()
      );
      storage.writeDatabase(elements);
    }

    // Gradle connector spawns non-daemon threads, nicely ask them to stop
    System.exit(0);
  }

  private static String heading(String text) {
    return heading(text, 0);
  }

  private static String heading(String text, int indent) {
    return "\n" + " ".repeat(indent) + "\033[94;1m==== \033[36;1m" + text
           + " \033[94;1m====\033[0m";
  }

  public static void configureInputClassLoader(
      List<Path> buildFiles, Path mavenHome, Path javaHome, Launcher launcher
  ) throws IOException {
    try {
      Pom pom = new Pom(Set.of(), Set.of(), Set.of());
      Set<Path> classpath = new HashSet<>();
      GradleClasspathParser classpathParser = new GradleClasspathParser();
      PomParser pomParser = new PomParser();
      for (Path buildFile : buildFiles) {
        if (buildFile.getFileName().toString().endsWith(".xml")) {
          System.out.println(heading("Parsing POM", 2));
          pom = pom.merge(pomParser.parsePom(Files.readString(buildFile)));
          System.out.println("  Successfully parsed POM");
        } else {
          System.out.println(heading("Parsing build.gradle", 2));
          for (Path path : classpathParser.getClasspath(javaHome, buildFile)) {
            if (Files.notExists(path) || !Files.isRegularFile(path)) {
              continue;
            }
            classpath.add(path);
          }
          System.out.println("  Successfully parsed build.gradle file");
        }
      }

      if (!pom.getDependencies().isEmpty()) {
        System.out.println(heading("Building classpath from POM", 2));
        Path outputPomFile = Files.createTempFile("Generatedpom", ".xml");
        outputPomFile.toFile().deleteOnExit();
        Files.writeString(outputPomFile, pom.format());
        classpath.addAll(new PomClasspathDiscoverer().findClasspath(outputPomFile, mavenHome));
      }

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

  public static class ConsoleProcessLogger extends ProgressLogger {

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
