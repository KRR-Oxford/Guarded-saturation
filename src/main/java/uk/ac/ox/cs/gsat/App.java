package uk.ac.ox.cs.gsat;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.lirmm.graphik.graal.api.core.AtomSet;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.io.owl.OWL2Parser;
import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.TGD;

public class App {

	static final Logger logger = LogManager.getLogger("Guarded saturation");

	public static void main(String[] args) throws Exception {

		System.out.println("Starting GSat...");

		try {
			Class.forName("uk.ac.ox.cs.pdq.fol.TGD");
		} catch (ClassNotFoundException e) {
			System.err.println("PDQ library not found. The system will now terminate.");
			System.exit(1);
		}

		try {

			if (args.length > 0)
				if (args[0].equals("cb"))
					if (args.length == 3 || args.length == 4) {

						String scenario = args[1];
						String basePath = args[2];
						if (!basePath.substring(basePath.length() - 1).equals(File.separator))
							basePath += File.separator; // Simplify usage
						String fact_querySize = args.length == 3 ? "" : args[3]; // Optional argument
						App.executeChaseBenchScenario(scenario, basePath, fact_querySize,
								Configuration.isFullGrounding());

					} else
						printHelp("Wrong number of parameters for cb");
				else if (args[0].equals("dlgp"))
					System.err.println("Not yet implemented!");
				else if (args[0].equals("owl"))
					if (args.length == 2) {
						fromOWL(args[1]);
					} else
						printHelp("Wrong number of parameters for owl");
				else
					printHelp("Wrong command (i.e. first argument)");
			else
				printHelp("No arguments provided");

		} catch (Throwable t) {
			System.err.println("Unknown error. The system will now terminate.");
			logger.debug(t);
			System.exit(1);
		}

	}

	private static void printHelp(String message) {

		System.err.println();
		System.err.println(message);
		System.err.println();
		System.err.println("Note that only these commands are currently supported:");
		System.err.println("cb \t for testing a ChaseBench scenario");
		System.err.println("dlgp \t for parsing a file in the DLGP format");
		System.err.println("owl \t for parsing a file in the OWL format");
		System.err.println();
		System.err.println("if cb is specified the following arguments must be provided, in this strict order:");
		System.err.println("<NAME OF THE SCENARIO> <PATH OF THE BASE FOLDER> [<FACT/QUERY SIZE>]");
		System.err.println();
		System.err.println("if dlgp is specified the following arguments must be provided, in this strict order:");
		System.err.println("<PATH OF THE DLGP FILE>");
		System.err.println();
		System.err.println("if owl is specified the following arguments must be provided, in this strict order:");
		System.err.println("<PATH OF THE OWL FILE>");
		System.err.println();

	}

	public static SolverOutput executeChaseBenchScenario(String scenario, String basePath, String fact_querySize,
			boolean fullGrounding) {

		System.out.println("Executing ChaseBench scenario: " + scenario + " " + basePath + " " + fact_querySize);

		logger.info("Reading from: '" + basePath + "'");

		Schema schema = null;
		Dependency[] allDependencies = null;
		Collection<TGD> queriesRules = null;
		try {

			schema = IO.readSchemaAndDependenciesChaseBench(basePath, scenario);
			allDependencies = schema.getAllDependencies();

			logger.info("# Dependencies: " + allDependencies.length);
			logger.trace(schema);

			queriesRules = IO.readQueriesChaseBench(basePath, fact_querySize, schema);
			logger.info("# Queries: " + queriesRules.size());
			logger.debug(queriesRules);

		} catch (Exception e) {
			System.err.println("Data loading failed. The system will now terminate.");
			logger.debug(e);
			System.exit(1);
		}

		Collection<TGD> guardedSaturation = null;
		try {

			guardedSaturation = GSat
					.runGSat(ArrayUtils.addAll(allDependencies, queriesRules.toArray(new TGD[queriesRules.size()])));
			logger.info("Rewriting completed!");
			System.out.println("Guarded saturation:");
			System.out.println("=========================================");
			guardedSaturation.forEach(System.out::println);
			System.out.println("=========================================");

		} catch (Exception e) {
			System.err.println("Guarded Saturation algorithm failed. The system will now terminate.");
			System.exit(1);
		}

		logger.info("Converting facts to Datalog");
		String baseOutputPath = "test" + File.separator + "datalog" + File.separator + scenario + File.separator
				+ fact_querySize + File.separator;
		try {
			new File(baseOutputPath).mkdirs();

			if (!new File(baseOutputPath + "datalog.data").exists()) {

				// Collection<Atom> facts = IO.readFactsChaseBench(basePath, fact_querySize,
				// schema);
				// logger.info("# Facts: " + facts.size());
				// logger.trace(facts);

				// IO.writeDatalogFacts(facts, baseOutputPath + "datalog.data");

				// For performance reasons
				IO.readFactsChaseBenchAndWriteToDatalog(basePath, fact_querySize, schema,
						baseOutputPath + "datalog.data");

			}

		} catch (Exception e) {
			System.err.println("Facts conversion to Datalog failed. The system will now terminate.");
			System.exit(1);
		}

		SolverOutput solverOutput = null;

		try {
			IO.writeDatalogRules(guardedSaturation, baseOutputPath + "datalog.rul");

			if (!queriesRules.isEmpty())
				System.out.println("Answering the queries...");

			for (TGD query : queriesRules) {

				IO.writeChaseBenchDatalogQueries(Arrays.asList(query), baseOutputPath + "queries.rul");

				SolverOutput solverOutputQuery = Logic.invokeSolver(Configuration.getSolverPath(),
						Configuration.getSolverOptionsQuery(), Arrays.asList(baseOutputPath + "datalog.rul",
								baseOutputPath + "datalog.data", baseOutputPath + "queries.rul"));

				// System.out.println(solverOutput2);
				System.out.println("Output size: " + solverOutputQuery.getOutput().length() + ", "
						+ solverOutputQuery.getErrors().length());
				IO.writeSolverOutput(solverOutputQuery, baseOutputPath + Configuration.getSolverName() + "."
						+ query.getHead().getAtoms()[0].getPredicate() + ".output");

			}

			if (fullGrounding) {

				System.out.println("Performing the full grounding...");

				solverOutput = Logic.invokeSolver(Configuration.getSolverPath(),
						Configuration.getSolverOptionsGrounding(),
						Arrays.asList(baseOutputPath + "datalog.rul", baseOutputPath + "datalog.data"));

				// System.out.println(solverOutput);
				System.out.println(
						"Output size: " + solverOutput.getOutput().length() + ", " + solverOutput.getErrors().length());
				IO.writeSolverOutput(solverOutput, baseOutputPath + Configuration.getSolverName() + ".output");

			}

		} catch (Exception e) {
			System.err.println("Datalog solver execution failed. The system will now terminate.");
			logger.debug(e);
			System.exit(1);
		}

		return solverOutput;

	}

	public static void fromOWL(String path) {

		// OWLAPI
		// OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		// try {
		// OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new
		// File(path));
		// ontology.saveOntology(new FunctionalSyntaxDocumentFormat(), System.out);
		// } catch (OWLOntologyCreationException | OWLOntologyStorageException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		// logger.debug("\n----\nGRAAL\n----\n");

		boolean fullGrounding = Configuration.isFullGrounding();

		List<Rule> rules = new LinkedList<>();
		List<AtomSet> atomSets = new LinkedList<>();

		try {

			OWL2Parser parser = new OWL2Parser(new File(path));

			while (parser.hasNext()) {
				Object o = parser.next();
				// logger.debug("Object:" + o);
				if (o instanceof Rule) {
					logger.debug("Rule: " + (Rule) o);
					rules.add((Rule) o);
				} else if (o instanceof AtomSet && fullGrounding) {
					logger.debug("Atom: " + (AtomSet) o);
					atomSets.add((AtomSet) o);
				}
			}

			parser.close();

		} catch (Exception e) {
			System.err.println("Data loading failed. The system will now terminate.");
			logger.debug(e);
			System.exit(1);
		}

		System.out.println("# Rules: " + rules.size() + "; # AtomSets: " + atomSets.size());

		Collection<TGD> guardedSaturation = null;
		try {

			List<TGD> tgds = IO.getPDQTGDsFromGraalRules(rules);
			rules = null;
			guardedSaturation = GSat.runGSat(tgds.toArray(new TGD[tgds.size()]));
			logger.info("Rewriting completed!");
			System.out.println("Guarded saturation:");
			System.out.println("=========================================");
			guardedSaturation.forEach(System.out::println);
			System.out.println("=========================================");

		} catch (Exception e) {
			System.err.println("Guarded Saturation algorithm failed. The system will now terminate.");
			System.exit(1);
		}

		SolverOutput solverOutput = null;

		if (fullGrounding) {

			logger.info("Converting facts to Datalog");
			// String testName =
			// FilenameUtils.removeExtension(Paths.get(path).getFileName().toString());
			String testName = Paths.get(path).getFileName().toString();
			String baseOutputPath = "test" + File.separator + "datalog" + File.separator + testName + File.separator;

			try {

				List<Atom> atoms = IO.getPDQAtomsFromGraalAtomSets(atomSets);
				atomSets = null;
				System.out.println("# PDQ Atoms: " + atoms.size());

				new File(baseOutputPath).mkdirs();

				// if (!new File(baseOutputPath + "datalog.data").exists())
				IO.writeDatalogFacts(atoms, baseOutputPath + "datalog.data");

			} catch (Exception e) {
				System.err.println("Facts conversion to Datalog failed. The system will now terminate.");
				System.exit(1);
			}

			try {

				IO.writeDatalogRules(guardedSaturation, baseOutputPath + "datalog.rul");

				System.out.println("Performing the full grounding...");

				solverOutput = Logic.invokeSolver(Configuration.getSolverPath(),
						Configuration.getSolverOptionsGrounding(),
						Arrays.asList(baseOutputPath + "datalog.rul", baseOutputPath + "datalog.data"));

				// System.out.println(solverOutput);
				System.out.println(
						"Output size: " + solverOutput.getOutput().length() + ", " + solverOutput.getErrors().length());
				IO.writeSolverOutput(solverOutput, baseOutputPath + Configuration.getSolverName() + ".output");

			} catch (Exception e) {
				System.err.println("Datalog solver execution failed. The system will now terminate.");
				logger.debug(e);
				System.exit(1);
			}
		}

	}

}
