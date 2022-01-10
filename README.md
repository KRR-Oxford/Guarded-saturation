# Guarded-saturation

The software implements a Resolution-based rewriting algorithm from Guarded Tuple Generating Dependencies (GTGDs) to Datalog, along with some functions
for running the resulting Datalog rules.

<!-- Description: A description of your project follows. A good description is clear, short, and to the point. Describe the importance of your project, and what it does. -->

## Saturating 

The main functionality of GSat is to compute the saturation of a set of TGDs. The corresponding command line is (the JAR file is available with the [releases](https://github.com/KRR-Oxford/Guarded-saturation/releases)):
```bash
java -jar guarded-saturation-1.0.0-jar-with-dependencies.jar <syntax> <TGD file>
```
where `<syntax>` is the one of the syntax `dlgp`, `owl` and `<TGD file>` is a file containing the input GTGDs in the syntax `<syntax>`.

By default, the output saturation is printed in the console, it can be written to a file instead by setting `write_output` to `true` in the file `config.properties`.

### Example

Consider a DLGP file `example.dlgp` that contains the following two TGDs:
```
b(X, Y), c(Y) :-  a(X).
d(X) :- b(X, Y), c(Y).
```

Calling:
```bash
java -jar guarded-saturation-1.0.0-jar-with-dependencies.jar dlgp example.dlgp
```
returns the saturation corresponding to:
```
b(X, Y), c(Y) :-  a(X).
d(X) :- a(X).
```

### Choice the saturation algorithm

This project implements different saturation algorithms. You can set the algorithm to use by changing `saturation_alg`'s value in the file `config.properties` to either:

- `gsat` (default) corresponding to `ExbDR` in the article
- `skolem_sat` corresponding to `SkolemDR` 
- `hyper_sat` corresponding to `HyperDR`
- `ordered_skolem_sat` also called `KAON3`
- `simple_sat` a naive saturation algorithm

Additionally, Gsat can be use to get the saturation of the set of TGDs in a OWL file using the following command:
```bash
java -cp guarded-saturation-1.0.0-jar-with-dependencies.jar "uk.ac.ox.cs.gsat.ExecutorOWL" <OWL file> <timeout>
```
where the `<timeout>` is expressed in seconds.

## Compilation (For developers)

### Prerequisites

To build the software we require

- [Java](https://www.oracle.com/java)
- [Apache Maven](http://maven.apache.org)
- [PDQ](https://github.com/ProofDrivenQuerying/pdq)
- [KAON 2](http://kaon2.semanticweb.org)

Kaon2 is only relevant for some experiments, and is not part of the delivered product.

#### Installing PDQ in Maven

1. Download from [PDQ releases](https://github.com/ProofDrivenQuerying/pdq/releases)
   - pdq-common-1.0.0.jar
2. Install as in [the official documentation](https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html)
   - `mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile="pdq-common-1.0.0.jar"`

#### Installing KAON 2

Install the JAR of KAON2 using 
```
mvn install:install-file -Dfile=./src/main/resources/kaon2.jar -DgroupId=org.semanticweb.kaon2 -DartifactId=kaon2 -Dversion=2008-06-29 -Dpackaging=jar -DgeneratePom=true
```

### Installing

If you are a developer and you want to run the tests, you need to:

1. Download the "test-all.zip" file from [GSat test datasets](https://github.com/stefanogermano/Guarded-saturation/releases/tag/test-data)
2. Extract it in the root of the project

Otherwise, you can simply run the `download-test-datasets.sh` script.

Build the project:

```sh
mvn verify
```

That's it! If you look in the `target` subdirectory, you should find the build output.

### Running the saturation on Chase Bench

In addition to what have been said in the usage section [ChaseBench](https://dbunibas.github.io/chasebench) format can also be used for the input.

To run on a ChaseBench scenario, you need to specify the `cb` option and add:

- `<NAME OF THE SCENARIO>` the name of the scenario as in the ChaseBench format
  - example: `doctors`
- `<PATH OF THE BASE FOLDER>` the directory that contains this scenario
  - example: `test/ChaseBench/scenarios/doctors`
- `[<FACT/QUERY SIZE>]` (optional) the size of the scenario that you want to test
  - example: `100k`

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

<!-- Contributing: Larger projects often have sections on contributing to their project, in which contribution instructions are outlined. Sometimes, this is a separate file. If you have specific contribution preferences, explain them so that other developers know how to best contribute to your work. To learn more about how to help others contribute, check out the guide for setting guidelines for repository contributors. -->

## Experiments

Experiments are available in a submodule, which may be large. You can initialize the experiments folder using:
```sh
git submodule init
```

## Credits

**[Information Systems Group](https://www.cs.ox.ac.uk/isg) - [Department of Computer Science](http://www.cs.ox.ac.uk) - [University of Oxford](www.ox.ac.uk)**

- [Michael Benedikt](http://www.cs.ox.ac.uk/people/michael.benedikt/home.html)
- [Stefano Germano](https://www.cs.ox.ac.uk/people/stefano.germano)
- [Kevin Kappelmann](https://www21.in.tum.de/team/kappelmk)

## License

This project is licensed under the [MIT License](LICENSE)
