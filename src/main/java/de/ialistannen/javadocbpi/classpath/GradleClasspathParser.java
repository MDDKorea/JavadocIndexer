package de.ialistannen.javadocbpi.classpath;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.eclipse.ClasspathAttribute;
import org.gradle.tooling.model.eclipse.EclipseExternalDependency;
import org.gradle.tooling.model.eclipse.EclipseProject;

public class GradleClasspathParser {

  public Set<Path> getClasspath(Path javaHome, Path projectFile) {
    GradleConnector connector = GradleConnector.newConnector()
        .forProjectDirectory(projectFile.toFile());

    try (ProjectConnection connection = connector.connect()) {
      ModelBuilder<EclipseProject> modelBuilder = connection.model(EclipseProject.class);
      if (javaHome != null) {
        modelBuilder.setJavaHome(javaHome.toFile());
      }

      return accumulateDeps(modelBuilder.get());
    } finally {
      connector.disconnect();
    }
  }

  private static Set<Path> accumulateDeps(EclipseProject project) {
    Set<Path> result = new HashSet<>();

    for (EclipseProject child : project.getChildren()) {
      result.addAll(accumulateDeps(child));
    }
    for (EclipseExternalDependency dependency : project.getClasspath()) {
      if (!dependency.isResolved()) {
        throw new RuntimeException("Unresolved dependency: " + dependency);
      }

      DomainObjectSet<? extends ClasspathAttribute> attributes = dependency.getClasspathAttributes();
      for (ClasspathAttribute attribute : attributes) {
        if (attribute.getName().equals("gradle_used_by_scope")) {
          if (attribute.getValue().contains("main")) {
            result.add(dependency.getFile().toPath());
          }
        }
      }
    }

    return result;
  }

}
