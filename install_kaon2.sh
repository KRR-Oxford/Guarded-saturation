#!/bin/bash

set -e

# go into KAON2 source directory from Boris 
cd ../KAON2
rm -fr classes
mkdir -p classes

javac $(find src -name "*.java") -d classes

cd classes
jar cf ../kaon2.jar $(find . -name "*.class")

cd ..
mvn install:install-file -Dfile=kaon2.jar -DgroupId=org.semanticweb.kaon2 -DartifactId=kaon2 -Dversion=2008-06-29 -Dpackaging=jar -DgeneratePom=true
