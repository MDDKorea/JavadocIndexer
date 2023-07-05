#!/usr/bin/env bash

git clone https://github.com/junit-team/junit5 ../../../../indexes/junit5 --depth=1
curl -L https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/5.9.1/junit-jupiter-api-5.9.1.pom -o ../../../../indexes/junit5/pom.xml
