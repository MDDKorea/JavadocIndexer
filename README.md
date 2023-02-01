# JavadocIndexer

This is an indexer for java code, extracting information about javadoc elements
directly from the source code. This allows it to build a database with an
easier to parse and render representation of javadoc, acting as the backend for
other tools ([DocTor](https://github.com/I-Al-Istannen/DocTor)).

# Usage

## Setup
To run the indexer, you need to first set up its dependencies:
- clone my fork of [Spoon](https://github.com/I-Al-Istannen/spoon) and checkout
  the `feat/javadoc` branch.
- `cd spoon/spoon-pom && mvn clean install`

Afterwards you can build the indexer using `mvn package`.

## Running
`java19 --enable-preview -jar target/JavadocIndexer.jar <config file>`

You can find example config files in `src/main/resources/examples`.
An example config could look like
```toml
# Where to store the database
output_path = "target/Paper-Api-Index.db"
# What files to index
resource_paths = ["indexes/paper/Paper-API/src/main/java"]
# The packages to include in the index.
# A single star matches everything, but no support for globbing exists.
# Subpackages are automatically allowed as well.
allowed_packages = [
    "*",
]
# The files containing dependencies. Maven is supported by specifying the
# pom.xml files. Gradle is a bit more complicated but might work if you specify
# your project *directories*
build_files = [
    "indexes/paper/",
]
# Path to your maven binary
maven_home = "/opt/maven"
# Java home to use for gradle. Can be omitted, then the JVM running your
# indexer will be chosen
java_home = "/usr/lib/jvm/java-17-openjdk"
```
