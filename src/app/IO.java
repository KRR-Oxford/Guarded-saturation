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

import uk.ac.ox.cs.pdq.db.Relation;
import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Conjunction;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.fol.Constant;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.Formula;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.UntypedConstant;
import uk.ac.ox.cs.pdq.fol.Variable;
import uk.ac.ox.cs.pdq.regression.utils.CommonToPDQTranslator;

/**
 * Helper functions for I/O operations
 * 
 * @author Stefano
 */
public class IO {

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
        if (dataDir.exists())
            for (File f : dataDir.listFiles())
                if (f.getName().endsWith(".csv")) {
                    String name = f.getName().substring(0, f.getName().indexOf("."));
                    if (schema.getRelation(name) == null)
                        System.out.println("Can't process file: " + f.getAbsolutePath());
                    else
                        facts.addAll(CommonToPDQTranslator.importFacts(schema, name, f.getAbsolutePath()));
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

        if (queriesDir.exists())
            for (File f : queriesDir.listFiles())
                if (f.getName().endsWith(".txt")) {
                    App.logger.debug("Parsing: " + f.getAbsolutePath());
                    queries.addAll(CommonToPDQTranslator.parseDependencies(relations2, f.getAbsolutePath()));
                    // queries.add(CommonToPDQTranslator.parseQuery(relations2,
                    // f.getAbsolutePath()));
                }
        Collection<TGD> result = new ArrayList<>();
        for (Dependency d : queries) {
            if (d instanceof TGD && ((TGD) d).isGuarded()) {// Adding only Guarded TGDs
                result.add((TGD) d);
                if (!Logic.isFull((TGD) d))
                    System.out.println(d + "is not full!!");
            } else
                App.logger.error("We accept only Guarded TGDs. Error with query " + d);
        }
        return result;
    }

    public static void writeDatalogRules(Collection<TGD> guardedSaturation, String path) throws IOException {

        Collection<String> datalogRules = new LinkedList<>();

        for (TGD tgd : guardedSaturation)
            datalogRules.addAll(getDatalogRules(tgd));

        Files.write(Paths.get(path), datalogRules, StandardCharsets.UTF_8);

    }

    private static Collection<? extends String> getDatalogRules(TGD tgd) {

        assert !Logic.isFull(tgd);

        StringBuilder body = new StringBuilder();
        String to_append = ":-";
        for (Atom atom : tgd.getBodyAtoms()) {
            body.append(to_append);
            if (to_append == ":-")
                to_append = ",";
            App.logger.debug("Atom:" + renameVariablesAndConstantsDatalog(atom));
            body.append(renameVariablesAndConstantsDatalog(atom).toString());
        }
        body.append(".");

        String bodyString = body.toString();

        Collection<String> rules = new LinkedList<>();
        // if multiple atoms in the head, we have to return multiple rules
        for (Atom atom : tgd.getHeadAtoms()) {
            App.logger.debug("Atom:" + renameVariablesAndConstantsDatalog(atom));
            rules.add(renameVariablesAndConstantsDatalog(atom).toString() + bodyString);
        }

        return rules;

    }

    public static Atom renameVariablesAndConstantsDatalog(Atom atom) {
        // App.logger.info(atom);
        // App.logger.info(atom.getTypedAndUntypedConstants());

        Map<Term, Term> substitution = new HashMap<>();
        for (Variable v : atom.getVariables())
            substitution.put(v, Variable.create(v.getSymbol().toUpperCase()));
        for (Constant c : atom.getTypedAndUntypedConstants())
            substitution.put(c, UntypedConstant.create('"' + c.toString() + '"'));

        // App.logger.info(substitution);

        Atom newAtom = (Atom) Logic.applySubstitution(atom, substitution);
        Predicate predicate = newAtom.getPredicate();
        String name = predicate.getName();
        // First char to Lower Case
        if (name.substring(0, 1).matches("[A-Z]")) {
            App.logger.warn("Predicate starting with an upper-case letter. Transforming it to lower-case.");
            return Atom.create(
                    Predicate.create(name.substring(0, 1).toLowerCase() + name.substring(1), predicate.getArity()),
                    newAtom.getTerms());
        }

        return newAtom;

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
        if (dataDir.exists())
            for (File f : dataDir.listFiles())
                if (f.getName().endsWith(".csv")) {
                    String name = f.getName().substring(0, f.getName().indexOf("."));
                    if (schema.getRelation(name) == null)
                        System.out.println("Can't process file: " + f.getAbsolutePath());
                    else {
                        Collection<String> datalogFacts = new LinkedList<>();

                        for (Atom atom : CommonToPDQTranslator.importFacts(schema, name, f.getAbsolutePath()))
                            datalogFacts.add(renameVariablesAndConstantsDatalog(atom).toString() + '.');

                        Files.write(path, datalogFacts, StandardCharsets.UTF_8,
                                Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
                    }
                }
    }

    public static void writeDatalogFacts(Collection<Atom> facts, String path) throws IOException {

        Collection<String> datalogFacts = new LinkedList<>();

        for (Atom atom : facts)
            datalogFacts.add(renameVariablesAndConstantsDatalog(atom).toString() + '.');

        Files.write(Paths.get(path), datalogFacts, StandardCharsets.UTF_8);

    }

    public static void writeDatalogQueries(Collection<ConjunctiveQuery> queries, String path) throws IOException {

        Collection<String> datalogQueries = new LinkedList<>();

        for (ConjunctiveQuery query : queries)
            // System.out.println(query);
            datalogQueries.add(getDatalogQuery(query));

        Files.write(Paths.get(path), datalogQueries, StandardCharsets.UTF_8);

    }

    private static String getDatalogQuery(ConjunctiveQuery query) {

        StringBuilder querySB = new StringBuilder();
        String to_append = "";
        for (Formula f : query.getChildren()) {
            if (f instanceof Conjunction) {
                App.logger.warn("We only accept atomic queries");
                return "";
            }
            assert (f instanceof Atom);
            querySB.append(to_append);
            if (to_append == "")
                to_append = ",";
            querySB.append(renameVariablesAndConstantsDatalog((Atom) f).toString());
        }
        querySB.append(" ?");

        return querySB.toString();

    }

    public static void writeChaseBenchDatalogQueries(Collection<TGD> queriesRules, String path) throws IOException {

        Collection<String> datalogQueries = new LinkedList<>();

        for (TGD query : queriesRules) {
            // System.out.println(query);
            // datalogQueries.addAll(getDatalogRules(query));
            datalogQueries.add(getProjectedDatalogQuery(query));
        }

        Files.write(Paths.get(path), datalogQueries, StandardCharsets.UTF_8);

    }

    private static String getProjectedDatalogQuery(TGD query) {

        assert query.getHeadAtoms().length == 1;
        return renameVariablesAndConstantsDatalog(query.getHeadAtom(0)).toString() + " ?";

    }

    public static void writeSolverOutput(SolverOutput solverOutput, String path) throws IOException {

        if (!Configuration.isSolverOutputToFile())
            return;

        Files.write(Paths.get(path), Arrays.asList(solverOutput.getOutput(), solverOutput.getErrors()),
                StandardCharsets.UTF_8);

    }

}