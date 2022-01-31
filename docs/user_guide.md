# 💻 User Guide

## Saturating

The main functionality of GSat is to compute the saturation of a set of GTGDs. To do this, download the JAR file available with the [releases](https://github.com/KRR-Oxford/Guarded-saturation/releases) and use the command line:

```shell
java -jar guarded-saturation-1.0.0-jar-with-dependencies.jar <syntax> <TGD file>
```

where `<syntax>` is the one of the syntax `dlgp`, `owl` and `<TGD file>` is a file containing the input GTGDs in the syntax `<syntax>`.

By default, the output saturation is printed in the console, it can be written to a file instead by setting `write_output` to `true` in the file `config.properties`.

### Example

Consider a DLGP file `example.dlgp` that contains the following two TGDs:

```prolog
b(X, Y), c(Y) :-  a(X).
d(X) :- b(X, Y), c(Y).
```

Calling:

```shell
java -jar guarded-saturation-1.0.0-jar-with-dependencies.jar dlgp example.dlgp
```

returns the saturation containing:

```prolog
b(X, Y), c(Y) :-  a(X).
d(X) :- a(X).
```

### The choice of the saturation algorithm

This project implements different saturation algorithms. You can set the algorithm to use by changing `saturation_alg`'s value in the file `config.properties` to either:

- `gsat` (default)
  - corresponding to `ExbDR` in the article
- `skolem_sat`
  - corresponding to `SkolemDR`
- `hyper_sat`
  - corresponding to `HyperDR`
- `simple_sat`
  - corresponding to `SimDR`
- `ordered_skolem_sat`
  - also called `KAON3`

Additionally, KAON2 can be used to get the saturation of the set of TGDs in an OWL file using the following command:

```shell
java -cp guarded-saturation-1.0.0-jar-with-dependencies.jar 'uk.ac.ox.cs.gsat.ExecutorOWL' <OWL file> <timeout>
```

where the `<timeout>` is expressed in seconds.

### Running the saturation on Chase Bench

Moreover, the [ChaseBench](https://dbunibas.github.io/chasebench) format can also be used for the input.

To run on a ChaseBench scenario, you need to specify the `cb` option and add:

- `<NAME OF THE SCENARIO>` the name of the scenario as in the ChaseBench format
  - example: `doctors`
- `<PATH OF THE BASE FOLDER>` the directory that contains this scenario
  - example: `test/ChaseBench/scenarios/doctors`
- `[<FACT/QUERY SIZE>]` (optional) the size of the scenario that you want to test
  - example: `100k`
