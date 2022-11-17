package de.ialistannen.javadocbpi;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public record IndexerConfig(
    @JsonProperty("output_path") String outputPath,
    @JsonProperty("resource_paths") List<String> resourcePaths,
    @JsonProperty("allowed_packages") Set<String> allowedPackages,
    @JsonProperty("build_files") List<Path> buildFiles,
    @JsonProperty("maven_home") Path mavenHome
) {

}
