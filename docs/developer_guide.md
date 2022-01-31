# 👩‍💻 Developer Guide

## Build

### Prerequisites

To build the software we require

- [Java](https://www.oracle.com/java)
- [Apache Maven](http://maven.apache.org)
- [PDQ](https://github.com/ProofDrivenQuerying/pdq)
- [KAON 2](http://kaon2.semanticweb.org)

KAON 2 is only relevant for some experiments, and is not part of the delivered product.

#### Install PDQ in Maven

1. Download from [PDQ releases](https://github.com/ProofDrivenQuerying/pdq/releases)
   - pdq-common-1.0.0.jar
2. Install as in [the official documentation](https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html)
   - `mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile="pdq-common-1.0.0.jar"`

#### Install KAON 2

Install the JAR of KAON2 using:

```shell
mvn install:install-file -Dfile=./src/main/resources/kaon2.jar -DgroupId=org.semanticweb.kaon2 -DartifactId=kaon2 -Dversion=2008-06-29 -Dpackaging=jar -DgeneratePom=true
```

### Install GSat

If you want to run the tests, you need to:

1. Download the `test-all.zip` file from [GSat test datasets](https://github.com/stefanogermano/Guarded-saturation/releases/tag/test-data)
2. Extract it in the root of the project

Otherwise, you can simply run the `download-test-datasets.sh` script.

Build the project:

```sh
mvn verify
```

That's it! If you look in the `target` subdirectory, you should find the build output.

### Useful Maven commands for developers

Check PDM violations and bugs in code:

```sh
mvn pmd:check

mvn spotbugs:check
```

Update Maven dependencies and Plugins:

```sh
mvn versions:display-dependency-updates

mvn versions:display-plugin-updates
```

Generate Javadoc:

```sh
mvn javadoc:javadoc
```
