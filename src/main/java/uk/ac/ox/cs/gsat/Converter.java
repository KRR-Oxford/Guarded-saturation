package uk.ac.ox.cs.gsat;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.TGD;

public class Converter {

	private static final Level level = Level.INFO;
	static final Logger logger = Logger.getLogger("Global Saturation");

	public static void main(String[] args) throws Exception {
		Handler handlerObj = new ConsoleHandler();
		handlerObj.setLevel(level);
		logger.addHandler(handlerObj);
		logger.setLevel(level);
		logger.setUseParentHandlers(false);

		System.out.println("Starting the OWL2TGD Converter...");

		try {
			Class.forName("uk.ac.ox.cs.pdq.fol.TGD");
		} catch (ClassNotFoundException e) {
			System.err.println("PDQ library not found. The system will now terminate.");
			System.exit(1);
		}

		try {

			if (args.length > 0)
				// if (args[0].equals("dlgp"))
				// if (args.length == 3) {
				// toDLGP(args[1], args[2]);
				// } else
				// printHelp("Wrong number of parameters for dlgp");
				// else
				if (args[0].equals("cb"))
					if (args.length == 3) {
						toChaseBench(args[1], args[2]);
					} else
						printHelp("Wrong number of parameters for cb");
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

	private static void toChaseBench(String input_path, String output_path) {
		OWLIO owlio = new OWLIO(input_path, true);
		try {
			Collection<Dependency> rules = owlio.getRules();
			Collection<TGDGSat> TGDRules = discardNonTGDRules(rules);
			IO.writeDatalogRules(TGDRules, output_path);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Collection<TGDGSat> discardNonTGDRules(Collection<Dependency> allDependencies) {

		System.out.println("Running GSat...");
		final long startTime = System.nanoTime();

		int discarded = 0;

		Collection<TGDGSat> selectedTGDs = new HashSet<>();
		for (Dependency d : allDependencies)
			if (d instanceof TGD && ((TGD) d).isGuarded())
				selectedTGDs.add(new TGDGSat((TGD) d));
			else
				discarded++;

		App.logger.info("GSat discarded rules : " + discarded + "/" + allDependencies.size() + " = "
				+ String.format(Locale.UK, "%.3f", (float) discarded / allDependencies.size() * 100) + "%");

		final long stopTime = System.nanoTime();

		long totalTime = stopTime - startTime;

		App.logger.info("GSat total time : " + String.format(Locale.UK, "%.0f", totalTime / 1E6) + " ms = "
				+ String.format(Locale.UK, "%.2f", totalTime / 1E9) + " s");

		return selectedTGDs;

	}

	// private static void toDLGP(String input_path, String output_path) {

	// }

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
		// System.err.println("dlgp \t to output a file in the DLGP format");
		System.err.println("cb \t to output a file in the ChaseBench format");
		System.err.println();
		// System.err.println("if dlgp is specified the following arguments must be
		// provided, in this strict order:");
		// System.err.println("<PATH OF THE INPUT FILE> <PATH OF THE OUTPUT FILE>");
		// System.err.println();
		System.err.println("if cb is specified the following arguments must be provided, in this strict order:");
		System.err.println("<PATH OF THE INPUT FILE> <PATH OF THE OUTPUT FILE>");
		System.err.println();

	}

}
