# 💻 User Guide

## Saturating

The main functionality of GSat is to compute the saturation of a set of GTGDs. To do this, download the JAR file available with the [releases](https://github.com/KRR-Oxford/Guarded-saturation/releases) and use the command line:

```shell
java -jar GSat-1.0-SNAPSHOT-jar-with-dependencies.jar -i <TGD file> -o <saturation file>
```

`<TGD file>` is a file containing the input GTGDs and `<saturation file>` is the file in which saturation will be written.

### Example

Consider a DLGP file `example.dlgp` that contains the following two TGDs:

```prolog
b(X, Y), c(Y) :-  a(X).
d(X) :- b(X, Y), c(Y).
```

Calling:

```shell
java -jar GSat-1.0-SNAPSHOT-jar-with-dependencies.jar -i example.dlgp -o example-sat.dlgp
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
java -jar GSat-1.0-SNAPSHOT-jar-with-dependencies.jar -i example.owl -o example-sat.dlgp --kaon2
```

### Running the saturation on Chase Bench

```shell
java -jar GSat-1.0-SNAPSHOT-jar-with-dependencies.jar -i <scenario directory> -o example-sat.dlgp -cb
```

