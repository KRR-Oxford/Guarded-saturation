#!/bin/bash

set -e

# go into KAON2 source directory from Boris 
cd KAON2
rm -r classes
mkdir -p classes

javac $(find src -name "*.java") -d classes

cd classes
jar cf ../kaon2.jar $(find . -name "*.class")
