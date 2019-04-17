package uk.ac.ox.cs.gsat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.AtomSet;
import fr.lirmm.graphik.graal.api.core.ConjunctiveQuery;
import fr.lirmm.graphik.graal.api.core.Query;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.io.dlp.DlgpParser;
import fr.lirmm.graphik.graal.io.owl.OWL2Parser;
import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.TGD;

public class App {

	private static final Level level = Level.INFO;
	static final Logger logger = Logger.getLogger("Global Saturation");

	public static void main(String[] args) throws Exception {
		Handler handlerObj = new ConsoleHandler();
		handlerObj.setLevel(level);
		logger.addHandler(handlerObj);
		logger.setLevel(level);
		logger.setUseParentHandlers(false);

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
					if (args.length == 2) {
						fromDLGP(args[1]);
					} else
						printHelp("Wrong number of parameters for dlgp");
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
			logger.severe(t.getLocalizedMessage());
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
			logger.finest(schema.toString());

			queriesRules = IO.readQueriesChaseBench(basePath, fact_querySize, schema);
			logger.info("# Queries: " + queriesRules.size());
			logger.fine(queriesRules.toString());

		} catch (Exception e) {
			System.err.println("Data loading failed. The system will now terminate.");
			logger.severe(e.getLocalizedMessage());
			System.exit(1);
		}

		Collection<TGDGSat> guardedSaturation = null;
		try {

			Collection<Dependency> allRules = new LinkedList<>();
			allRules.addAll(Arrays.asList(allDependencies));
			allRules.addAll(queriesRules);

			guardedSaturation = GSat.getInstance().runGSat(allRules.toArray(new Dependency[allRules.size()]));

			logger.info("Rewriting completed!");
			System.out.println("Guarded saturation:");
			System.out.println("=========================================");
			guardedSaturation.forEach(System.out::println);
			System.out.println("=========================================");

		} catch (Exception e) {
			System.err.println("Guarded Saturation algorithm failed. The system will now terminate.");
			logger.severe(e.getLocalizedMessage());
			System.exit(1);
		}

		if (!fullGrounding && queriesRules.isEmpty())
			return null;

		logger.info("Converting facts to Datalog");
		String baseOutputPath = "test" + File.separator + "datalog" + File.separator + scenario + File.separator
				+ fact_querySize + File.separator;
		try {

			if (Files.notExists(Paths.get(baseOutputPath))) {
				boolean mkdirs = new File(baseOutputPath).mkdirs();
				if (!mkdirs)
					throw new IllegalArgumentException("Output path not available: " + baseOutputPath);
			}

			if (Files.notExists(Paths.get(baseOutputPath, "datalog.data"))) {

				// Collection<Atom> facts = IO.readFactsChaseBench(basePath, fact_querySize,
				// schema);
				// logger.info("# Facts: " + facts.size());
				// logger.trace(facts);

				// IO.writeDatalogFacts(facts, baseOutputPath + "datalog.data");

				// For performance reasons
				IO.readFactsChaseBenchAndWriteToDatalog(basePath, fact_querySize, schema,
						baseOutputPath + "datalog.data");

			}

		} catch (RuntimeException e) {
			throw e;
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

		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			System.err.println("Datalog solver execution failed. The system will now terminate.");
			logger.severe(e.getLocalizedMessage());
			System.exit(1);
		}

		return solverOutput;

	}

	public static int fromDLGP(String path) {

		// boolean fullGrounding = Configuration.isFullGrounding();

		Collection<Atom> atoms = new HashSet<>();
		Collection<Rule> rules = new HashSet<>();
		Collection<Query> queries = new HashSet<>();

		try {

			DlgpParser parser = new DlgpParser(new File(path));

			while (parser.hasNext()) {
				Object o = parser.next();
				if (o instanceof Atom) {
					logger.fine("Atom: " + ((Atom) o));
					atoms.add((Atom) o);
				} else if (o instanceof Rule) {
					logger.fine("Rule: " + ((Rule) o));
					rules.add((Rule) o);
				} else if (o instanceof ConjunctiveQuery) {
					logger.fine("ConjunctiveQuery: " + ((Query) o));
					queries.add((Query) o);
				}
			}

			parser.close();

		} catch (Exception e) {
			System.err.println("Data loading failed. The system will now terminate.");
			logger.severe(e.getLocalizedMessage());
			System.exit(1);
		}

		System.out
				.println("# Rules: " + rules.size() + "; # Atoms: " + atoms.size() + "; # Queries: " + queries.size());

		Collection<TGDGSat> guardedSaturation = null;
		try {

			Collection<TGD> tgds = IO.getPDQTGDsFromGraalRules(rules);
			rules = null;

			guardedSaturation = GSat.getInstance().runGSat(tgds.toArray(new TGD[tgds.size()]));

			logger.info("Rewriting completed!");
			System.out.println("Guarded saturation:");
			System.out.println("=========================================");
			guardedSaturation.forEach(System.out::println);
			System.out.println("=========================================");

		} catch (Exception e) {
			System.err.println("Guarded Saturation algorithm failed. The system will now terminate.");
			logger.severe(e.getLocalizedMessage());
			System.exit(1);
		}

		return guardedSaturation.size();

	}

	public static int fromOWL(String path) {

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

		Collection<Rule> rules = new HashSet<>();
		Collection<AtomSet> atomSets = new HashSet<>();

		try {

			OWL2Parser parser = new OWL2Parser(new File(path));

			while (parser.hasNext()) {
				Object o = parser.next();
				// logger.debug("Object:" + o);
				if (o instanceof Rule) {
					logger.fine("Rule: " + (Rule) o);
					rules.add((Rule) o);
				} else if (o instanceof AtomSet && fullGrounding) {
					logger.fine("Atom: " + (AtomSet) o);
					atomSets.add((AtomSet) o);
				}
			}

			parser.close();

		} catch (Exception e) {
			System.err.println("Data loading failed. The system will now terminate.");
			logger.severe(e.getLocalizedMessage());
			System.exit(1);
		}

		System.out.println("# Rules: " + rules.size() + "; # AtomSets: " + atomSets.size());

		Collection<TGDGSat> guardedSaturation = null;
		try {

			Collection<TGD> tgds = IO.getPDQTGDsFromGraalRules(rules);
			rules = null;

			guardedSaturation = GSat.getInstance().runGSat(tgds.toArray(new TGD[tgds.size()]));

			logger.info("Rewriting completed!");
			System.out.println("Guarded saturation:");
			System.out.println("=========================================");
			guardedSaturation.forEach(System.out::println);
			System.out.println("=========================================");

		} catch (Exception e) {
			System.err.println("Guarded Saturation algorithm failed. The system will now terminate.");
			logger.severe(e.getLocalizedMessage());
			System.exit(1);
		}

		return guardedSaturation.size();

		// FIXME find a way to convert IRI into valid predicate names

		// SolverOutput solverOutput = null;

		// if (fullGrounding) {

		// logger.info("Converting facts to Datalog");
		// // String testName =
		// // FilenameUtils.removeExtension(Paths.get(path).getFileName().toString());
		// String testName = Paths.get(path).getFileName().toString();
		// String baseOutputPath = "test" + File.separator + "datalog" + File.separator
		// + testName + File.separator;

		// try {

		// Collection<Atom> atoms = IO.getPDQAtomsFromGraalAtomSets(atomSets);
		// atomSets = null;
		// System.out.println("# PDQ Atoms: " + atoms.size());

		// if (Files.notExists(Paths.get(baseOutputPath))) {
		// boolean mkdirs = new File(baseOutputPath).mkdirs();
		// if (!mkdirs)
		// throw new IllegalArgumentException("Output path not available: " +
		// baseOutputPath);
		// }

		// if (Files.notExists(Paths.get(baseOutputPath, "datalog.data"))) {
		// IO.writeDatalogFacts(atoms, baseOutputPath + "datalog.data");

		// } catch (Exception e) {
		// System.err.println("Facts conversion to Datalog failed. The system will now
		// terminate.");
		// logger.debug(e);
		// System.exit(1);
		// }

		// try {

		// IO.writeDatalogRules(guardedSaturation, baseOutputPath + "datalog.rul");

		// System.out.println("Performing the full grounding...");

		// solverOutput = Logic.invokeSolver(Configuration.getSolverPath(),
		// Configuration.getSolverOptionsGrounding(),
		// Arrays.asList(baseOutputPath + "datalog.rul", baseOutputPath +
		// "datalog.data"));

		// // System.out.println(solverOutput);
		// System.out.println(
		// "Output size: " + solverOutput.getOutput().length() + ", " +
		// solverOutput.getErrors().length());
		// IO.writeSolverOutput(solverOutput, baseOutputPath +
		// Configuration.getSolverName() + ".output");

		// } catch (Exception e) {
		// System.err.println("Datalog solver execution failed. The system will now
		// terminate.");
		// logger.debug(e);
		// System.exit(1);
		// }

		// }

	}

}
