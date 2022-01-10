# Guarded-saturation

The software implements a Resolution-based rewriting algorithm from Guarded Tuple Generating Dependencies (GTGDs) to Datalog, along with some functions
for running the resulting Datalog rules.

This README is aimed at developers.


<!-- Description: A description of your project follows. A good description is clear, short, and to the point. Describe the importance of your project, and what it does. -->

## Saturating 

The main functionality of GSat is to compute the saturation of a set of TGDs. The corresponding command line is (the JAR file is available with the [releases](https://github.com/KRR-Oxford/Guarded-saturation/releases)):
```bash
java -jar guarded-saturation-1.0.0-jar-with-dependencies.jar <syntax> <TGD file>
```
where `<syntax>` is the one of the syntax `dlgp`, `owl`, `cb` (for chasebench) and `<TGD file>` is a file containing the input TGDs in the syntax `<syntax>`.

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

## Getting Started (Installation and Usage)

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

### Running

### Guarded saturation

The entry point of the project is the `App` class.
You can run it directly or through the JAR.

If no arguments or wrong arguments are provided, it prints this useful help message:

```txt
Note that only these commands are currently supported:
cb       for processing a scenario for the ''ChaseBench'' -- seee the chasebench github for information on this format. A scenario is a directory with several files in it.
dlgp     for processing a file of TGDs in the DLGP format
owl      for processing a file of TGDs in the OWL format

if cb is specified the following arguments must be provided, in this strict order:
<NAME OF THE SCENARIO> <PATH OF THE BASE FOLDER> [<FACT/QUERY SIZE>]

if dlgp is specified the following arguments must be provided, in this strict order:
<PATH OF THE DLGP FILE>

if owl is specified the following arguments must be provided, in this strict order:
<PATH OF THE OWL FILE> [<PATH OF THE SPARQL FILE>]
```

As specified in this message, at the moment we support 3 input formats:

- [ChaseBench](https://dbunibas.github.io/chasebench)
- [DLGP](https://graphik-team.github.io/graal)
- [OWL](https://www.w3.org/OWL)

Since they are completely different, each of them needs different options.

To run on a ChaseBench scenario, you need to specify the `cb` option and add:

- `<NAME OF THE SCENARIO>` the name of the scenario as in the ChaseBench format
  - example: `doctors`
- `<PATH OF THE BASE FOLDER>` the directory that contains this scenario
  - example: `test/ChaseBench/scenarios/doctors`
- `[<FACT/QUERY SIZE>]` (optional) the size of the scenario that you want to test
  - example: `100k`

To run on a file in DLGP format, you simply need to specify the `dlgp` option and add the path to the file (`<PATH OF THE DLGP FILE>`). Luckily, the DLGP format can contain everything in a single file.

To run on a file in OWL format, you need to specify the `owl` option and add:

- `<PATH OF THE OWL FILE>` the path to the file
- `[<PATH OF THE SPARQL FILE>]` (optional) the path to the SPARQL file containing the query

### Configuration file

In addition to the input parameters that can be specified on the command line, it is possible to configure the behaviour of Gsat by editing the properties in the configuration file (`config.properties`).

These are the allowed values:

- Datalog solver options:
  - `solver.name` the name of the Datalog solver
    - example: `solver.name=idlv`
  - `solver.path` the path to the Datalog solver
    - example: `solver.path=executables/idlv_1.1.6_linux_x86-64`
  - `solver.options.grounding` the options for the grounding/materialisation phase
    - example: `solver.options.grounding=--t --no-facts --check-edb-duplication`
  - `solver.options.query` the options for the solving/query phase
    - example: `solver.options.query=--query`
- Execution options:
  - `solver.output.to_file` [boolean (`true` or `false`)] if the output of the solver should be saved to file
    - example: `solver.output.to_file=false`
  - `solver.full_grounding` [boolean (`true` or `false`)] if it should compute the full grounding/materialisation
    - example: `solver.full_grounding=true`
  - `gsat_only` [boolean (`true` or `false`)] get only the Guarded Saturation, or also the full grounding (if `solver.full_grounding` is set to `true`) and the answers to all the queries
    - example: `gsat_only=true`
  - `debug` [boolean (`true` or `false`)] turn on or of the debug mode (mostly debug prints)
    - example: `debug=true`
  - `optimization` number (between 0 and 5) to specify the level of optimization (bigger should correspond to a "better" version)
    - example: `optimization=3`
    - values allowed:
      - **0** no optimizations
      - **1** subsumption check
      - **2** ordered sets to store the new TGDs to evaluate (stored in `newFullTGDs` and `newNonFullTGDs`)
      - **3** stop to evolve a TGD if we found a new one that subsumes it
      - **4** ordered sets to store the "possible evolving" TGDs (stored in `fullTGDsMap` and `nonFullTGDsMap`)
      - **5** stacks to store the new TGDs to evaluate
    - Note that the order of 2 and 4 is conceived to evaluate earlier rules with bigger heads and smaller bodies (i.e. rules that can easily subsume other rules), see `comparator` in `GSat` for more details

### Executors

Executors are utility apps that allow comparing Gsat with other systems.

We have 2 different versions at the moment:

- the "KAON2-GSat Executor" that compares KAON2 and Gsat on DLGP files
  - the only parameter is the path to the file (`<PATH OF THE DLGP FILE>`)
- the "KAON2-GSat Executor (from OWL)" that compares KAON2 and Gsat on OWL files
  - it has 2 parameters: the path to the file (`<PATH OF THE DLGP FILE>`) and a timeout value (`<TIMEOUT (sec)>`)

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
