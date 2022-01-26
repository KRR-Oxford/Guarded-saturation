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

import uk.ac.ox.cs.gsat.api.ExecutionSteps;
import uk.ac.ox.cs.gsat.api.io.Serializer;
import uk.ac.ox.cs.gsat.io.ChaseBenchIO;
import uk.ac.ox.cs.gsat.io.DLGPIO;
import uk.ac.ox.cs.gsat.io.DLGPSerializer;
import uk.ac.ox.cs.gsat.io.IO;
import uk.ac.ox.cs.gsat.io.OWLIO;
import uk.ac.ox.cs.gsat.kaon2.KAON2StructuralTransformationIO;
import uk.ac.ox.cs.gsat.mat.SolverOutput;
import uk.ac.ox.cs.gsat.mat.Utils;
import uk.ac.ox.cs.gsat.satalg.GSat;
import uk.ac.ox.cs.gsat.satalg.HyperResolutionBasedSat;
import uk.ac.ox.cs.gsat.satalg.OrderedSkolemSat;
import uk.ac.ox.cs.gsat.satalg.SimpleSat;
import uk.ac.ox.cs.gsat.satalg.SkolemSat;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;

public class App {

	private static final Level level = Level.INFO;
	public static final Logger logger = Log.GLOBAL;

	public static void main(String[] args) throws Exception {
		Handler handlerObj = new ConsoleHandler();
		handlerObj.setLevel(level);
		logger.addHandler(handlerObj);
		logger.setLevel(level);
		logger.setUseParentHandlers(false);

		System.out.println("Starting Saturation...");

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
				else if (args[0].equals("owl")) {
                    if (Configuration.applyKAON2StructuralTransformation()) {
                        fromOWLWithStructuralTransformation(args[1]);
                    } else {
                        if (args.length == 2 || args.length == 3) {
                            String query = args.length == 2 ? "" : args[2]; // Optional argument
                            fromOWL(args[1], query);
                        } else
                            printHelp("Wrong number of parameters for owl");
                    }
                } else
					printHelp("Wrong command (i.e. first argument)");
			else
				printHelp("No arguments provided");

		} catch (Throwable t) {
			t.printStackTrace(System.err);
			System.err.println("Unknown error. The system will now terminate.");
			logger.severe(t.getLocalizedMessage());
			logger.severe(Arrays.toString(t.getStackTrace()));
			System.exit(1);
		}

	}

	/**
	 * Prints the help message of the program
	 * 
	 * @param message the error message
	 */
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
		System.err.println("<PATH OF THE OWL FILE> [<PATH OF THE SPARQL FILE>]");
		System.err.println();

	}

	/**
	 * Runs a scenario in the ChaseBench format
	 * 
	 * @param scenario       the name of the scenario
	 * @param basePath       the path of folder containing the scenario
	 * @param fact_querySize the size (i.e. the name of the subfolder) of data/query
	 *                       we want to run for the specific scenario (this is
	 *                       strictly related to the ChaseBench format)
	 * @return the results of `executeAllSteps`
	 */
	public static ExecutionOutput executeChaseBenchScenario(String scenario, String basePath, String fact_querySize) {

		System.out.println("Executing ChaseBench scenario: " + scenario + " " + basePath + " " + fact_querySize);

		return executeAllSteps(new ChaseBenchIO(scenario, basePath, fact_querySize));

	}

	/**
	 * Runs a file in the DLGP format
	 * 
	 * @param path the path of the DLGP file we want to process
	 * @return the results of `executeAllSteps`
	 */
	public static ExecutionOutput fromDLGP(String path) {

		System.out.println("Executing from DLGP files");

		return executeAllSteps(new DLGPIO(path, Configuration.isSaturationOnly(), Configuration.includeNegativeConstraint()));

	}

	public static ExecutionOutput fromOWLWithStructuralTransformation(String path) {

		System.out.println("Executing from OWL files with structural transformation");

		return executeAllSteps(new KAON2StructuralTransformationIO(path, false, Configuration.includeNegativeConstraint()));

	}
    
	/**
	 * Runs a file in the OWL format
	 * 
	 * @param path the path of the OWL file we want to process
	 * @return the results of `executeAllSteps`
	 */
	public static ExecutionOutput fromOWL(String path, String query) {

		System.out.println("Executing from OWL files");

		return executeAllSteps(new OWLIO(path, query, Configuration.isSaturationOnly(), Configuration.includeNegativeConstraint()));

	}

	/**
	 * Runs all the steps needed in order to get the saturation, the full
	 * grounding and the answers to all the queries
	 * 
	 * @param executionSteps the specific executor (implementing `ExecutionSteps`)
	 *                       to use for the basic operations
	 * @return the results of the saturation and, possibly, also of the full
	 *         gounding (if the specific option is enabled)
	 */
	private static ExecutionOutput executeAllSteps(ExecutionSteps executionSteps) {

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

            if (Configuration.getSaturationAlg().equals("gsat")) {

                System.out.println("Full TGD saturation algorithm: GSat");
                executionOutput.setFullTGDSaturation(GSat.getInstance().run(rules));
            } else if (Configuration.getSaturationAlg().equals("skolem_sat")) {

                System.out.println("Full TGD saturation algorithm: Skolem Sat");
                executionOutput.setFullTGDSaturation(SkolemSat.getInstance().run(rules));
            } else if (Configuration.getSaturationAlg().equals("ordered_skolem_sat")) {

                System.out.println("Full TGD saturation algorithm: ordered skolemized Sat");
                executionOutput.setFullTGDSaturation(OrderedSkolemSat.getInstance().run(rules));
            } else if (Configuration.getSaturationAlg().equals("hyper_sat")) {

                System.out.println("Full TGD saturation algorithm: hyperresolution Sat");
                executionOutput.setFullTGDSaturation(HyperResolutionBasedSat.getInstance().run(rules));
            } else if (Configuration.getSaturationAlg().equals("simple_sat")) {

                System.out.println("Full TGD saturation algorithm: Simple Sat");
                executionOutput.setFullTGDSaturation(SimpleSat.getInstance().run(rules));
            } else {
                throw new IllegalStateException("The saturation algorithm (saturation_alg) " + Configuration.getSaturationAlg() + " is not supported.");
            }

			logger.info("Rewriting completed!");
			System.out
					.println("Full TGD saturation: (" + executionOutput.getFullTGDSaturation().size() + " dependecies)");
            if (!Configuration.writeOutputDatalog()) {
                System.out.println("=========================================");
                executionOutput.getFullTGDSaturation().forEach(System.out::println);
                System.out.println("=========================================");
            }

		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.err.println("Full TGD saturation algorithm failed. The system will now terminate.");
			logger.severe(e.getLocalizedMessage());
			System.exit(1);
		}

        if (!Configuration.isSaturationOnly() || Configuration.writeOutputDatalog()) {

            String baseOutputPath = "test" + File.separator + "datalog" + File.separator
				+ executionSteps.getBaseOutputPath() + File.separator;
            if (!Configuration.isSaturationOnly()) {

                if (Files.notExists(Paths.get(baseOutputPath))) {
                    boolean mkdirs = new File(baseOutputPath).mkdirs();
                    if (!mkdirs)
                        throw new IllegalArgumentException("Output path not available: " + baseOutputPath);
                }
            }

            // write the output rules in datalog file
            try {
                String datalogFile = (Configuration.writeOutputDatalog())
					? executionSteps.getBaseOutputPath().replaceFirst("[.][^.]+$", "") + ".rul"
					: baseOutputPath + "datalog.rul";
                App.logger.info("Writing the saturation in the Datalog file " + datalogFile);
                // IO.writeDatalogRules(executionOutput.getFullTGDSaturation(), datalogFile);
                Serializer serializer = new DLGPSerializer();
                serializer.open(datalogFile);
                serializer.writeTGDs(executionOutput.getFullTGDSaturation());
                serializer.close();
            } catch (Exception e) {
                System.out.println("datalog writing failed");
				e.printStackTrace();
                System.exit(1);
            }
        }

		if (!Configuration.isSaturationOnly()) {

            String baseOutputPath = "test" + File.separator + "datalog" + File.separator
					+ executionSteps.getBaseOutputPath() + File.separator;

			logger.info("Converting facts to Datalog");

			try {

				if (Files.notExists(Paths.get(baseOutputPath, "datalog.data")))
					executionSteps.writeData(baseOutputPath + "datalog.data");

			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				System.err.println("Facts conversion to Datalog failed. The system will now terminate.");
				System.exit(1);
			}

			try {

				if (Configuration.isFullGrounding()) {

					System.out.println("Performing the full grounding...");

					executionOutput.setSolverOutput(
                                                    Utils.invokeSolver(Configuration.getSolverPath(), Configuration.getSolverOptionsGrounding(),
									Arrays.asList(baseOutputPath + "datalog.rul", baseOutputPath + "datalog.data")));

					// System.out.println(solverOutput);
					System.out.println("Output size: " + executionOutput.getSolverOutput().getOutput().length() + ", "
							+ executionOutput.getSolverOutput().getErrors().length() + "; number of lines (atoms): "
							+ executionOutput.getSolverOutput().getNumberOfLinesOutput());
					Utils.writeSolverOutput(executionOutput.getSolverOutput(),
							baseOutputPath + Configuration.getSolverName() + ".output");

				}

				Collection<Atom> queries = executionSteps.getQueries();
				// FIXME Get query rules for Conjunctive queries

				if (queries != null && !queries.isEmpty()) {

					System.out.println("Answering the queries...");

					for (Atom query : queries) {

						IO.writeDatalogQuery(query, baseOutputPath + "datalog.query");

						SolverOutput solverOutputQuery = Utils.invokeSolver(Configuration.getSolverPath(),
								Configuration.getSolverOptionsQuery(), Arrays.asList(baseOutputPath + "datalog.rul",
										baseOutputPath + "datalog.data", baseOutputPath + "datalog.query"));

						// System.out.println(solverOutput2);
						System.out.println("Output size: " + solverOutputQuery.getOutput().length() + ", "
								+ solverOutputQuery.getErrors().length() + "; number of lines (atoms): "
								+ solverOutputQuery.getNumberOfLinesOutput());
						Utils.writeSolverOutput(solverOutputQuery, baseOutputPath + Configuration.getSolverName() + "."
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
