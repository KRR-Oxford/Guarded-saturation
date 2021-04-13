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
cb       for testing a ChaseBench scenario
dlgp     for parsing a file in the DLGP format
owl      for parsing a file in the OWL format

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

To test a ChaseBench scenario, you need to specify the `cb` option and add:

- `<NAME OF THE SCENARIO>` the name of the scenario as in the ChaseBench format
  - example: `doctors`
- `<PATH OF THE BASE FOLDER>` the directory that contains this scenario
  - example: `test/ChaseBench/scenarios/doctors`
- `[<FACT/QUERY SIZE>]` (optional) the size of the scenario that you want to test
  - example: `100k`

To test a file in DLGP format, you simply need to specify the `dlgp` option and add the path to the file (`<PATH OF THE DLGP FILE>`). Luckily, the DLGP format can contain everything in a single file.

To test a file in OWL format, you need to specify the `owl` option and add:

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
