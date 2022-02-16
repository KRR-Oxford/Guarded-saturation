package uk.ac.ox.cs.gsat.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.ox.cs.gsat.api.io.Parser;
import uk.ac.ox.cs.gsat.api.io.ParserResult;
import uk.ac.ox.cs.gsat.fol.Logic;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.pdq.db.Relation;
import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.io.CommonToPDQTranslator;

public class ChaseBenchParser implements Parser {

    /**
     * parse a chase bench scenario
     */
    @Override
    public ParserResult parse(String path) throws Exception {

        return new ChaseBenchParserResult(path);
    }

    static class ChaseBenchParserResult implements ParserResult {

        private String path;
        private Schema schema;

        public ChaseBenchParserResult(String path) {
            this.path = path;
            this.schema = readSchemaAndDependenciesChaseBench(path);
        }

        @Override
        public Set<TGD> getTGDs() throws Exception {
            Set<TGD> tgds = new HashSet<>();
            for (Dependency dep : schema.getAllDependencies()) {
                if (dep instanceof uk.ac.ox.cs.pdq.fol.TGD)
                    tgds.add(TGD.create(((uk.ac.ox.cs.pdq.fol.TGD) dep).getBodyAtoms(),
                            ((uk.ac.ox.cs.pdq.fol.TGD) dep).getHeadAtoms()));
            }
            return tgds;
        }

        @Override
        public Set<Atom> getAtoms() throws Exception {
            return readFactsChaseBench(path, "", schema);
        }

        @Override
        public Set<Atom> getConjunctiveQueries() throws Exception {
            // TODO insert the querysize
            return getQueryAtoms(readQueriesChaseBench(path, "", schema));
        }
    }

    /**
     * From PDQ testing code, slightly modified
     */
    static Schema readSchemaAndDependenciesChaseBench(String basePath) {
        File schemaDir = new File(basePath + "schema");
        File dependencyDir = new File(basePath + "dependencies");
        File[] schemaFiles = schemaDir.listFiles();
        File[] dependencyFiles = dependencyDir.listFiles();

        Map<String, Relation> relations = new HashMap<>();
        for (File schemaFile : schemaFiles) {
            relations.putAll(CommonToPDQTranslator.parseTables(schemaFile.getAbsolutePath()));
        }

        List<Dependency> dependencies = new ArrayList<>();
        for (File dependencyFile : dependencyFiles) {
            dependencies.addAll(CommonToPDQTranslator.parseDependencies(relations, dependencyFile.getAbsolutePath()));
        }

        Schema schema = new Schema(relations.values().toArray(new Relation[relations.size()]),
                dependencies.toArray(new Dependency[dependencies.size()]));
        return schema;
    }

    /**
     * From PDQ testing code, slightly modified
     * 
     * @param fact_querySize
     */
    static Collection<TGD> readQueriesChaseBench(String basePath, String fact_querySize, Schema schema) {
        File queriesDir = new File(basePath + "queries" + File.separator + fact_querySize);
        Collection<Dependency> queries = new ArrayList<>();
        Map<String, Relation> relations2 = new HashMap<>();
        for (Relation r : schema.getRelations())
            relations2.put(r.getName(), r);

        if (queriesDir.exists()) {
            File[] listFiles = queriesDir.listFiles();
            if (listFiles != null)
                for (File f : listFiles)
                    if (f.getName().endsWith(".txt")) {
                        // App.logger.fine("Parsing: " + f.getAbsolutePath());
                        queries.addAll(CommonToPDQTranslator.parseDependencies(relations2, f.getAbsolutePath()));
                        // queries.add(CommonToPDQTranslator.parseQuery(relations2,
                        // f.getAbsolutePath()));
                    }
        }
        Collection<TGD> result = new ArrayList<>();
        for (Dependency d : queries) {
            if (d instanceof TGD && ((TGD) d).isGuarded()) {// Adding only Guarded TGDs
                result.add((TGD) d);
                if (!Logic.isFull((TGD) d))
                    System.err.println(d + "is not full!!");
            } else {
                // App.logger.warning("We accept only Guarded TGDs. Error with query " + d);
            }
        }
        return result;
    }

    public static Set<Atom> getQueryAtoms(Collection<TGD> queriesRules) {

        Set<Atom> queryAtoms = new HashSet<>();

        for (TGD query : queriesRules) {
            if (query.getHeadAtoms().length != 1)
                throw new IllegalArgumentException("The query is not atomic");

            queryAtoms.add(query.getHeadAtoms()[0]);
        }

        return queryAtoms;

    }

    /**
     * From PDQ testing code, slightly modified
     * 
     * @param fact_querySize
     */
    static Set<Atom> readFactsChaseBench(String basePath, String fact_querySize, Schema schema) {
        File dataDir = new File(basePath + "data" + File.separator + fact_querySize);
        Set<Atom> facts = new HashSet<>();
        if (dataDir.exists()) {
            File[] listFiles = dataDir.listFiles();
            if (listFiles != null)
                for (File f : listFiles)
                    if (f.getName().endsWith(".csv")) {
                        String name = f.getName().substring(0, f.getName().indexOf("."));
                        if (schema.getRelation(name) == null)
                            System.err.println("Can't process file: " + f.getAbsolutePath());
                        else
                            facts.addAll(CommonToPDQTranslator.importFacts(schema, name, f.getAbsolutePath()));
                    }
        }
        return facts;
    }

}
