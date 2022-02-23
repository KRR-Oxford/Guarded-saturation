# Graph of Rule Dependencies Visualizer

It draws a rule dependencies graph based on the predicates. Alternatively, a rule dependencies graph as introduced in An Introduction to Ontology-Based Query Answering with Existential Rules can be used. This latter graph is built using the tools [Kiabora](https://graphik-team.github.io/graal/downloads/kiabora).

These graph also allows to highlight the descendent or the ancestor of a TGD in the graph. You can also [visit a blog post](http://www.cs.ox.ac.uk/people/maxime.buron/blog/2021-04-28.html), that show an example of the use of this tools on the ontology ISG-00729.

## How to use it

Build the predicate graph writer:
```bash
mvn clean install
```

```bash
java -jar target/<JAR> dlgp_file output_file
```


You have to run a static server in this directory ([see alternatives](https://gist.github.com/willurd/5720255)): 

```bash
## python 3
python -m http.server 8080
## visit for example: http://localhost:8080?path=analyzes/ISG-guarded_kaon2-bak/00729.txt
```


