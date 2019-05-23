package uk.ac.ox.cs.gsat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;

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
						App.executeChaseBenchScenario(scenario, basePath, fact_querySize);

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

	public static ExecutionOutput executeChaseBenchScenario(String scenario, String basePath, String fact_querySize) {

		System.out.println("Executing ChaseBench scenario: " + scenario + " " + basePath + " " + fact_querySize);

		return executeAllSteps(new ChaseBenchIO(scenario, basePath, fact_querySize));

	}

	public static ExecutionOutput fromDLGP(String path) {

		System.out.println("Executing from DLGP files");

		return executeAllSteps(new DLGPIO(path, Configuration.isGSatOnly()));

	}

	public static ExecutionOutput fromOWL(String path) {

		System.out.println("Executing from OWL files");

		return executeAllSteps(new OWLIO(path, Configuration.isGSatOnly()));

	}

	public static ExecutionOutput executeAllSteps(ExecutionSteps executionSteps) {

		ExecutionOutput executionOutput = new ExecutionOutput(null, null);

		Collection<Dependency> rules = null;
		try {

			rules = executionSteps.getRules();

		} catch (Exception e) {
			System.err.println("Data loading failed. The system will now terminate.");
			logger.severe(e.getLocalizedMessage());
			System.exit(1);
		}

		try {

			executionOutput
					.setGuardedSaturation(GSat.getInstance().runGSat(rules.toArray(new Dependency[rules.size()])));

			logger.info("Rewriting completed!");
			System.out.println("Guarded saturation:");
			System.out.println("=========================================");
			executionOutput.getGuardedSaturation().forEach(System.out::println);
			System.out.println("=========================================");

		} catch (Exception e) {
			System.err.println("Guarded Saturation algorithm failed. The system will now terminate.");
			logger.severe(e.getLocalizedMessage());
			System.exit(1);
		}

		if (!Configuration.isGSatOnly()) {

			logger.info("Converting facts to Datalog");

			String baseOutputPath = "test" + File.separator + "datalog" + File.separator
					+ executionSteps.getBaseOutputPath() + File.separator;

			try {

				if (Files.notExists(Paths.get(baseOutputPath))) {
					boolean mkdirs = new File(baseOutputPath).mkdirs();
					if (!mkdirs)
						throw new IllegalArgumentException("Output path not available: " + baseOutputPath);
				}

				if (Files.notExists(Paths.get(baseOutputPath, "datalog.data")))
					executionSteps.writeData(baseOutputPath + "datalog.data");

			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				System.err.println("Facts conversion to Datalog failed. The system will now terminate.");
				System.exit(1);
			}

			try {
				IO.writeDatalogRules(executionOutput.getGuardedSaturation(), baseOutputPath + "datalog.rul");

				if (Configuration.isFullGrounding()) {

					System.out.println("Performing the full grounding...");

					executionOutput.setSolverOutput(
							Logic.invokeSolver(Configuration.getSolverPath(), Configuration.getSolverOptionsGrounding(),
									Arrays.asList(baseOutputPath + "datalog.rul", baseOutputPath + "datalog.data")));

					// System.out.println(solverOutput);
					System.out.println("Output size: " + executionOutput.getSolverOutput().getOutput().length() + ", "
							+ executionOutput.getSolverOutput().getErrors().length() + "; number of lines (atoms): "
							+ executionOutput.getSolverOutput().getNumberOfLinesOutput());
					IO.writeSolverOutput(executionOutput.getSolverOutput(),
							baseOutputPath + Configuration.getSolverName() + ".output");

				}

				Collection<Atom> queries = executionSteps.getQueries();

				if (queries != null && !queries.isEmpty()) {

					System.out.println("Answering the queries...");

					for (Atom query : queries) {

						IO.writeDatalogQuery(query, baseOutputPath + "query.rul");

						SolverOutput solverOutputQuery = Logic.invokeSolver(Configuration.getSolverPath(),
								Configuration.getSolverOptionsQuery(), Arrays.asList(baseOutputPath + "datalog.rul",
										baseOutputPath + "datalog.data", baseOutputPath + "query.rul"));

						// System.out.println(solverOutput2);
						System.out.println("Output size: " + solverOutputQuery.getOutput().length() + ", "
								+ solverOutputQuery.getErrors().length() + "; number of lines (atoms): "
								+ solverOutputQuery.getNumberOfLinesOutput());
						IO.writeSolverOutput(solverOutputQuery, baseOutputPath + Configuration.getSolverName() + "."
								+ query.getPredicate() + ".output");

					}

				}

			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				System.err.println("Datalog solver execution failed. The system will now terminate.");
				logger.severe(e.getLocalizedMessage());
				System.exit(1);
			}

		}

		return executionOutput;

	}

}
