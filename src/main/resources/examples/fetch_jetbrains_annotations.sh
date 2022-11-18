#!/usr/bin/env bash

curl -L https://repo1.maven.org/maven2/org/jetbrains/annotations/23.0.0/annotations-23.0.0-sources.jar -o /tmp/annotations.zip
unzip /tmp/annotations.zip -d ../../../../indexes/jetbrains-annotations/
rm -rf ../../../../indexes/jetbrains-annotations/META-INF
