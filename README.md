# Guarded-saturation
Resolution-based rewriting algorithm from Guarded Tuple Generating Dependencies (GTGDs) to Datalog

<!-- Description: A description of your project follows. A good description is clear, short, and to the point. Describe the importance of your project, and what it does. -->

## Getting Started (Installation and Usage)

### Prerequisites

It requires:
- [Java](https://www.oracle.com/java)
- [Apache Maven](http://maven.apache.org)
- [PDQ](https://github.com/ProofDrivenQuerying/pdq)
- [KAON 2](http://kaon2.semanticweb.org)

#### Installing PDQ in Maven

(This will no longer be required when PDQ is added to Maven repositories)

1. Download from [PDQ releases](https://github.com/ProofDrivenQuerying/pdq/releases)
   - pdq-common-1.0.0.jar
2. Install as in [the official documentation](https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html)
   - `mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile="pdq-common-1.0.0.jar"`

#### Installing KAON 2

1. Download the JAR file from [the KAON 2 website](http://kaon2.semanticweb.org)
2. Save it in the `src\main\resources` folder

### Installing

Build the project:

```
mvn verify
```

That's it! If you look in the `target` subdirectory, you should find the build output.

### Running

### Useful Maven commands for developers

Check PDM violations and bugs in code:

```
mvn pmd:check

mvn spotbugs:check
```

Update Maven dependencies and Plugins:

```
mvn versions:display-dependency-updates

mvn versions:display-plugin-updates
```

Generate Javadoc:

```
mvn javadoc:javadoc
```

<!-- Contributing: Larger projects often have sections on contributing to their project, in which contribution instructions are outlined. Sometimes, this is a separate file. If you have specific contribution preferences, explain them so that other developers know how to best contribute to your work. To learn more about how to help others contribute, check out the guide for setting guidelines for repository contributors. -->

## Credits

**[Information Systems Group](https://www.cs.ox.ac.uk/isg) - [Department of Computer Science](http://www.cs.ox.ac.uk) - [University of Oxford](www.ox.ac.uk)**

- [Michael Benedikt](http://www.cs.ox.ac.uk/people/michael.benedikt/home.html)
- [Stefano Germano](https://www.cs.ox.ac.uk/people/stefano.germano)
- [Kevin Kappelmann]()

## License

This project is licensed under the [MIT License](LICENSE)
