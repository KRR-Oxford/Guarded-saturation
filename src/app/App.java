import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.TGD;

public class App {

	static final Logger logger = LogManager.getLogger("Guarded saturation");

	public static void main(String[] args) throws Exception {

		System.out.println("Starting GSat...");

		if (args.length > 0)
			if (args[0].equals("cb"))
				if (args.length == 3)
					App.executeChaseBenchScenario(args[1], args[2], "");
				else if (args.length == 4)
					App.executeChaseBenchScenario(args[1], args[2], args[3]);
				else
					printHelp("Wrong number of parameters for cb");
			else if (args[0].equals("dlgp"))
				System.err.println("Not yet implemented!");
			else
				printHelp("Wrong command (i.e. first argument)");
		else
			printHelp("No arguments provided");

	}

	private static void printHelp(String message) {

		System.err.println();
		System.err.println(message);
		System.err.println();
		System.err.println("Note that only these commands are currently supported:");
		System.err.println("cb \t for testing a ChaseBench scenario");
		System.err.println("dlgp \t for parsing a file in the DLGP format");
		System.err.println();
		System.err.println("if cb is specified the following arguments must be provided, in this strict order:");
		System.err.println("<NAME OF THE SCENARIO> <PATH OF THE BASE FOLDER> [<FACT/QUERY SIZE>]");
		System.err.println();
		System.err.println("if dlgp is specified the following arguments must be provided, in this strict order:");
		System.err.println("<PATH OF THE DLGP FILE>");

	}

	public static SolverOutput executeChaseBenchScenario(String baseTest, String basePath, String fact_querySize) {

		System.out.println("Executing ChaseBench scenario: " + baseTest + " " + basePath + " " + fact_querySize);

		logger.info("Reading from: '" + basePath + "'");

		Schema schema = IO.readSchemaAndDependenciesChaseBench(basePath, baseTest);
		Dependency[] allDependencies = schema.getAllDependencies();

		logger.info("# Dependencies: " + allDependencies.length);
		logger.trace(schema);

		Collection<Atom> facts = IO.readFactsChaseBench(basePath, fact_querySize, schema);
		logger.info("# Facts: " + facts.size());
		logger.trace(facts);

		Collection<TGD> queriesRules = IO.readQueriesChaseBench(basePath, fact_querySize, schema);
		logger.info("# Queries: " + queriesRules.size());
		logger.debug(queriesRules);

		Collection<TGD> guardedSaturation = GSat
				.runGSat(ArrayUtils.addAll(allDependencies, queriesRules.toArray(new TGD[queriesRules.size()])));
		logger.info("Rewriting completed!");
		System.out.println("Guarded saturation:");
		System.out.println("=========================================");
		guardedSaturation.forEach(System.out::println);
		System.out.println("=========================================");

		String baseOutputPath = "test" + File.separator + "datalog" + File.separator;
		new File(baseOutputPath).mkdirs();
		IO.writeDatalogRules(guardedSaturation, baseOutputPath + baseTest + ".rul");
		IO.writeDatalogFacts(facts, baseOutputPath + baseTest + ".data");

		System.out.println("Performing the full grounding...");
		SolverOutput solverOutput = Logic.invokeSolver("executables" + File.separator + "idlv_1.1.3_windows_x86-64.exe",
				"--t --no-facts --check-edb-duplication", // "dlv.mingw.exe", "-nofacts",
				Arrays.asList(baseOutputPath + baseTest + ".rul", baseOutputPath + baseTest + ".data"));
		// System.out.println(solverOutput);
		System.out.println(
				"Output size: " + solverOutput.getOutput().length() + ", " + solverOutput.getErrors().length());
		IO.writeSolverOutput(solverOutput, baseOutputPath + baseTest + ".idlv.output" // ".dlv.output"
		);

		for (TGD query : queriesRules) {

			IO.writeChaseBenchDatalogQueries(Arrays.asList(query), baseOutputPath + baseTest + "_queries.rul");

			SolverOutput solverOutputQuery = Logic.invokeSolver(
					"executables" + File.separator + "idlv_1.1.3_windows_x86-64.exe", "--query", // "dlv.mingw.exe",
					// "-nofacts",
					Arrays.asList(baseOutputPath + baseTest + ".rul", baseOutputPath + baseTest + ".data",
							baseOutputPath + baseTest + "_queries.rul"));
			// System.out.println(solverOutput2);
			System.out.println("Output size: " + solverOutputQuery.getOutput().length() + ", "
					+ solverOutputQuery.getErrors().length());
			IO.writeSolverOutput(solverOutputQuery,
					baseOutputPath + baseTest + "." + query.getHead().getAtoms()[0].getPredicate() + ".idlv.output" // ".dlv.output"
			);

		}

		return solverOutput;

	}

}
