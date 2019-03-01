import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import uk.ac.ox.cs.pdq.fol.Disjunction;
import uk.ac.ox.cs.pdq.fol.Formula;
import uk.ac.ox.cs.pdq.fol.Implication;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;
import uk.ac.ox.cs.pdq.regression.utils.CommonToPDQTranslator;

/**
 * Utility
 */
public class Utility {

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
	 */
	static Collection<Atom> readFactsChaseBench(String basePath, Schema schema) {
		File dataDir = new File(basePath + "data");
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
	 */
	static Collection<ConjunctiveQuery> readQueriesChaseBench(String basePath, Schema schema) {
		File queriesDir = new File(basePath + "queries");
		Collection<ConjunctiveQuery> queries = new ArrayList<>();
		Map<String, Relation> relations2 = new HashMap<>();
		for (Relation r : schema.getRelations())
			relations2.put(r.getName(), r);

		if (queriesDir.exists())
			for (File f : queriesDir.listFiles())
				if (f.getName().endsWith(".txt"))
					try {
						queries.add(CommonToPDQTranslator.parseQuery(relations2, f.getAbsolutePath()));
					} catch (IOException e) {
						e.printStackTrace();
					}
		return queries;
	}

	/**
	 * From PDQ code, slightly modified
	 */
	static Formula applySubstitution(Formula formula, Map<Term, Term> substitution) {
		if (formula instanceof Conjunction) {
			Formula child1 = applySubstitution(((Conjunction) formula).getChildren()[0], substitution);
			Formula child2 = applySubstitution(((Conjunction) formula).getChildren()[1], substitution);
			return Conjunction.create(child1, child2);
		} else if (formula instanceof Disjunction) {
			Formula child1 = applySubstitution(((Disjunction) formula).getChildren()[0], substitution);
			Formula child2 = applySubstitution(((Disjunction) formula).getChildren()[1], substitution);
			return Disjunction.of(child1, child2);
		} else if (formula instanceof Implication) {
			Formula child1 = applySubstitution(((Implication) formula).getChildren()[0], substitution);
			Formula child2 = applySubstitution(((Implication) formula).getChildren()[1], substitution);
			return Implication.of(child1, child2);
		} else if (formula instanceof ConjunctiveQuery) {
			Atom[] atoms = ((ConjunctiveQuery) formula).getAtoms();
			Formula[] bodyAtoms = new Formula[atoms.length];
			for (int atomIndex = 0; atomIndex < atoms.length; ++atomIndex)
				bodyAtoms[atomIndex] = applySubstitution(atoms[atomIndex], substitution);
			return Conjunction.create(bodyAtoms);
		} else if (formula instanceof TGD) {
			Atom[] headAtoms = ((TGD) formula).getHeadAtoms();
			Atom[] headAtomsF = new Atom[headAtoms.length];
			Atom[] bodyAtoms = ((TGD) formula).getBodyAtoms();
			Atom[] bodyAtomsF = new Atom[bodyAtoms.length];
			for (int atomIndex = 0; atomIndex < headAtoms.length; ++atomIndex)
				headAtomsF[atomIndex] = (Atom) applySubstitution(headAtoms[atomIndex], substitution);
			for (int atomIndex = 0; atomIndex < bodyAtoms.length; ++atomIndex)
				bodyAtomsF[atomIndex] = (Atom) applySubstitution(bodyAtoms[atomIndex], substitution);
			return TGD.create(bodyAtomsF, headAtomsF);
		} else if (formula instanceof Atom) {
			Term[] nterms = new Term[((Atom) formula).getNumberOfTerms()];
			for (int termIndex = 0; termIndex < ((Atom) formula).getNumberOfTerms(); ++termIndex) {
				Term term = ((Atom) formula).getTerm(termIndex);
				// System.out.println(term);
				// System.out.println(term.isVariable());
				// System.out.println(substitution.containsKey(term));
				// System.out.println(substitution.get(term));
				if (term.isVariable() && substitution.containsKey(term))
					nterms[termIndex] = substitution.get(term);
				else
					nterms[termIndex] = term;
			}
			return Atom.create(((Atom) formula).getPredicate(), nterms);
		}
		throw new java.lang.RuntimeException("Unsupported formula type: " + formula);
	}

	static boolean containsAny(Atom atom, Variable[] eVariables) {
		for (Variable v : eVariables)
			for (Variable va : atom.getVariables())
				if (v.equals(va))
					return true;
		return false;
	}

	public static void writeDatalogRules(Collection<TGD> guardedSaturation, String path) {

		Collection<String> datalogRules = new LinkedList<>();

		for (TGD tgd : guardedSaturation)
			datalogRules.addAll(getDatalogRules(tgd));

		try {
			Files.write(Paths.get(path), datalogRules, Charset.forName("UTF-8"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static Collection<? extends String> getDatalogRules(TGD tgd) {

		assert !App.isFull(tgd);

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

		Map<Term, Term> substitution = new HashMap<>();
		for (Variable v : atom.getVariables())
			substitution.put(v, Variable.create(v.getSymbol().toUpperCase()));
		for (Constant c : atom.getTypedAndUntypedConstants())
			substitution.put(c, Variable.create(c.toString().toLowerCase()));

		return (Atom) Utility.applySubstitution(atom, substitution);

	}

	public static void writeDatalogFacts(Collection<Atom> facts, String path) {

		Collection<String> datalogFacts = new LinkedList<>();

		for (Atom atom : facts)
			datalogFacts.add(renameVariablesAndConstantsDatalog(atom).toString() + '.');

		try {
			Files.write(Paths.get(path), datalogFacts, Charset.forName("UTF-8"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void writeDatalogQueries(Collection<ConjunctiveQuery> queries, String path) {

		Collection<String> datalogQueries = new LinkedList<>();

		for (ConjunctiveQuery query : queries)
			// System.out.println(query);
			datalogQueries.add(getDatalogQuery(query));

		try {
			Files.write(Paths.get(path), datalogQueries, Charset.forName("UTF-8"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static String getDatalogQuery(ConjunctiveQuery query) {

		StringBuilder querySB = new StringBuilder();
		String to_append = "";
		for (Formula f : query.getChildren()) {
			assert !(f instanceof Conjunction);
			for (Atom atom : ((Conjunction) f).getAtoms()) {
				querySB.append(to_append);
				if (to_append == "")
					to_append = ",";
				querySB.append(renameVariablesAndConstantsDatalog(atom).toString());
			}
		}
		querySB.append(" ?");

		return querySB.toString();

	}

}
