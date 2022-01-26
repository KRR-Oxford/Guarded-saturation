package uk.ac.ox.cs.gsat.io;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import uk.ac.ox.cs.gsat.App;
import uk.ac.ox.cs.gsat.ExecutionSteps;
import uk.ac.ox.cs.gsat.fol.Logic;
import uk.ac.ox.cs.pdq.db.Relation;
import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.io.CommonToPDQTranslator;

public class ChaseBenchIO implements ExecutionSteps {

    private String scenario;
    private String basePath;
    private String fact_querySize;
    private Schema schema;
    Collection<TGD> queriesRules;

    public ChaseBenchIO(String scenario, String basePath, String fact_querySize) {
        this.scenario = scenario;
        this.basePath = basePath;
        this.fact_querySize = fact_querySize;
    }

    @Override
    public Collection<Dependency> getRules() {

        App.logger.info("Reading from: '" + basePath + "'");

        Dependency[] allDependencies = null;
        Collection<Dependency> allRules = new LinkedList<>();

        schema = readSchemaAndDependenciesChaseBench(basePath, scenario);
        allDependencies = schema.getAllDependencies();

        App.logger.info("# Dependencies: " + allDependencies.length);
        App.logger.finest(schema.toString());

        queriesRules = readQueriesChaseBench(basePath, fact_querySize, schema);
        App.logger.info("# Queries: " + queriesRules.size());
        App.logger.fine(queriesRules.toString());

        allRules.addAll(Arrays.asList(allDependencies));
        // allRules.addAll(queriesRules); FIXME

        return allRules;

    }

    @Override
    public void writeData(String path) throws IOException {
        // Collection<Atom> facts = IO.readFactsChaseBench(basePath, fact_querySize,
        // schema);
        // logger.info("# Facts: " + facts.size());
        // logger.trace(facts);

        // IO.writeDatalogFacts(facts, path);

        // For performance reasons
        readFactsChaseBenchAndWriteToDatalog(basePath, fact_querySize, schema, path);
    }

    @Override
    public Collection<Atom> getQueries() {
        return getQueryAtoms(queriesRules);
    }

    @Override
    public String getBaseOutputPath() {
        return scenario + File.separator + fact_querySize;
    }

    /**
     * From PDQ testing code, slightly modified
     */
    static Schema readSchemaAndDependenciesChaseBench(String basePath, String testName) {
        File schemaDir = new File(basePath + "schema");
        File dependencyDir = new File(basePath + "dependencies");
        Map<String, Relation> tables = CommonToPDQTranslator
                .parseTables(schemaDir.getAbsolutePath() + File.separator + testName + ".s-schema.txt");
        Map<String, Relation> tables1 = CommonToPDQTranslator
                .parseTables(schemaDir.getAbsolutePath() + File.separator + testName + ".t-schema.txt");

        Map<String, Relation> relations = new HashMap<>();
        relations.putAll(tables);
        relations.putAll(tables1);
        List<Dependency> dependencies = CommonToPDQTranslator.parseDependencies(relations,
                dependencyDir.getAbsolutePath() + File.separator + testName + ".st-tgds.txt");
        if (new File(dependencyDir.getAbsolutePath() + File.separator + testName + ".t-tgds.txt").exists())
            dependencies.addAll(CommonToPDQTranslator.parseDependencies(relations,
                    dependencyDir.getAbsolutePath() + File.separator + testName + ".t-tgds.txt"));
        Schema schema = new Schema(relations.values().toArray(new Relation[relations.size()]),
                dependencies.toArray(new Dependency[dependencies.size()]));
        return schema;
    }

    /**
     * From PDQ testing code, slightly modified
     * 
     * @param fact_querySize
     */
    static Collection<Atom> readFactsChaseBench(String basePath, String fact_querySize, Schema schema) {
        File dataDir = new File(basePath + "data" + File.separator + fact_querySize);
        Collection<Atom> facts = new ArrayList<>();
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
                        App.logger.fine("Parsing: " + f.getAbsolutePath());
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
            } else
                App.logger.warning("We accept only Guarded TGDs. Error with query " + d);
        }
        return result;
    }

    /**
     * From PDQ testing code, slightly modified
     * 
     * @param fact_querySize
     * @throws IOException
     */
    static void readFactsChaseBenchAndWriteToDatalog(String basePath, String fact_querySize, Schema schema,
            String outputPath) throws IOException {
        Path path = Paths.get(outputPath);
        File dataDir = new File(basePath + "data" + File.separator + fact_querySize);
        if (dataDir.exists()) {
            File[] listFiles = dataDir.listFiles();
            if (listFiles != null)
                for (File f : listFiles)
                    if (f.getName().endsWith(".csv")) {
                        String name = f.getName().substring(0, f.getName().indexOf("."));
                        if (schema.getRelation(name) == null)
                            System.err.println("Can't process file: " + f.getAbsolutePath());
                        else {
                            Collection<String> datalogFacts = new LinkedList<>();

                            for (Atom atom : CommonToPDQTranslator.importFacts(schema, name, f.getAbsolutePath()))
                                datalogFacts.add(IO.renameVariablesAndConstantsDatalog(atom).toString() + '.');

                            Files.write(path, datalogFacts, StandardCharsets.UTF_8,
                                    Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
                        }
                    }
        }
    }

    public static void writeChaseBenchDatalogQueries(Collection<TGD> queriesRules, Collection<TGD> queriesRules2,
            String path) throws IOException {

        Collection<String> datalogQueries = new LinkedList<>();

        for (TGD query : queriesRules) {
            // System.out.println(query);
            // datalogQueries.addAll(getDatalogRules(query));
            datalogQueries.add(getProjectedDatalogQuery(query));
        }

        queriesRules2.forEach((tgd) -> datalogQueries.addAll(IO.getDatalogRules(tgd)));

        Files.write(Paths.get(path), datalogQueries, StandardCharsets.UTF_8);

    }

    public static String getProjectedDatalogQuery(TGD query) {

        if (query.getHeadAtoms().length != 1)
            throw new IllegalArgumentException("The query is not atomic");

        return IO.renameVariablesAndConstantsDatalog(query.getHeadAtom(0)).toString() + " ?";

    }

    public static Collection<Atom> getQueryAtoms(Collection<TGD> queriesRules) {

        Collection<Atom> queryAtoms = new LinkedList<>();

        for (TGD query : queriesRules) {
            if (query.getHeadAtoms().length != 1)
                throw new IllegalArgumentException("The query is not atomic");

            queryAtoms.add(query.getHeadAtoms()[0]);
        }

        return queryAtoms;

    }

}
