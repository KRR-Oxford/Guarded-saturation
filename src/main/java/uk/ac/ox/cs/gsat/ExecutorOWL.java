package uk.ac.ox.cs.gsat;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.semanticweb.kaon2.api.DefaultOntologyResolver;
import org.semanticweb.kaon2.api.KAON2Connection;
import org.semanticweb.kaon2.api.KAON2Exception;
import org.semanticweb.kaon2.api.KAON2Manager;
import org.semanticweb.kaon2.api.Ontology;
import org.semanticweb.kaon2.api.logic.Rule;
import org.semanticweb.kaon2.api.reasoner.Reasoner;
import org.semanticweb.kaon2.saturation.TheoremProverMonitor;

public class ExecutorOWL {

	/**
	 *
	 */
	private static final Level level = Level.INFO;
	static final Logger logger = Logger.getLogger("ExecutorOWL");

	public static void main(String[] args) throws Exception {
		Handler handlerObj = new ConsoleHandler();
		handlerObj.setLevel(level);
		logger.addHandler(handlerObj);
		logger.setLevel(level);
		logger.setUseParentHandlers(false);

		System.out.println("Starting the KAON2 Executor (from OWL)...");

		try {
			Class.forName("uk.ac.ox.cs.pdq.fol.TGD");
		} catch (ClassNotFoundException e) {
			System.err.println("PDQ library not found. The system will now terminate.");
			System.exit(1);
		}

		try {

			if (args.length > 0)
					if (args.length == 2)
						if (args[1].matches("\\d+"))
							run(args[0], Integer.parseInt(args[1]));
						else
							printHelp("The TIMEOUT must be a positive number!");
					else
						printHelp("requires two arguments");
			else
				printHelp("No arguments provided");

		} catch (Throwable t) {
			System.err.println("Unknown error. The system will now terminate.");
			logger.severe(t.getLocalizedMessage());
			System.exit(1);
		}

	}

	private static void run(String input_file, final int timeout) {

		Handler handlerObj = new ConsoleHandler();
		handlerObj.setLevel(Level.WARNING);
		App.logger.addHandler(handlerObj);
		App.logger.setLevel(Level.WARNING);
		App.logger.setUseParentHandlers(false);

		System.out.println("KAON2 Start");
		final long startTimeKAON2 = System.nanoTime();
		final Collection<Rule> runKAON2 = new LinkedList<>();

		ExecutorService executorKAON2 = Executors.newSingleThreadExecutor();
        KAON2Statistics monitor = new KAON2Statistics(new PrintWriter(System.out));
		Future<String> futureKAON2 = executorKAON2.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				try {
					runKAON2.addAll(runKAON2(input_file, monitor));
				} catch (Exception e) {
					e.printStackTrace();
					return "Failed!";
				}
				return "Completed!";
			}
		});

		try {
			System.out.println("Started..");
			System.out.println(futureKAON2.get(timeout, TimeUnit.SECONDS));
			System.out.println("Finished!");
        } catch (TimeoutException e) {
            futureKAON2.cancel(true);
            System.err.println("!!! TIME OUT !!!");
		} catch (InterruptedException | ExecutionException e) {
			futureKAON2.cancel(true);
            System.err.println("!!! ERROR !!!");
			System.err.println("Terminated!");
		}

		executorKAON2.shutdownNow();

		final long stopTimeKAON2 = System.nanoTime();
		final long totalTimeKAON2 = stopTimeKAON2 - startTimeKAON2;
        monitor.print();
		System.out.println("KAON2 End; # rules: " + runKAON2.size());
		System.out.println("KAON2 total time : " + String.format(Locale.UK, "%.0f", totalTimeKAON2 / 1E6) + " ms = "
				+ String.format(Locale.UK, "%.2f", totalTimeKAON2 / 1E9) + " s");
		System.out.println("Full TGD saturation: (" + runKAON2.size() + " dependecies)");

        System.out.println("\n====================");
		runKAON2.forEach(rule -> IO.getDatalogRules(rule).forEach(System.out::println));
		System.out.println("====================");

		System.exit(0); // needed because sometimes the program does not terminate
	}

	private static Collection<Rule> runKAON2(String input_file, KAON2Statistics monitor) throws KAON2Exception, InterruptedException {
        KAON2Connection connection=KAON2Manager.newConnection();


		DefaultOntologyResolver resolver = new DefaultOntologyResolver();
		resolver.registerReplacement("http://bkhigkhghjbhgiyfgfhgdhfty", "file:" + input_file.replace("\\", "/"));
        connection.setOntologyResolver(resolver);

		Ontology ontology = connection.openOntology("http://bkhigkhghjbhgiyfgfhgdhfty",
				new HashMap<String, Object>());
		System.out.println("Initial axioms in the ontology: " + ontology.createAxiomRequest().sizeAll());
		Reasoner reasoner = ontology.createReasoner();

        reasoner.setParameter("theoremProverMonitor", monitor);
        reasoner.setParameter("predicateOrdering", "none");
        reasoner.setParameter("selectionFunction", "empty");
        reasoner.setParameter("useMagicSets", false);
		// reasoner.getOntology().saveOntology(OntologyFileFormat.OWL_RDF, System.out,
		// "UTF-8");
		// reasoner.setTrace("theoremProver", true,
		// new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8)),
		// new Namespaces());
		Collection<Rule> reductionToDLP = new LinkedList<>();
		try {
			reductionToDLP = reasoner.getReductionToDisjunctiveDatalog(false, false, false, true);
		} finally {
			reasoner.dispose();
		}
		return reductionToDLP;
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
		System.err.println();
		System.err.println("the following arguments must be provided, in this strict order:");
		System.err.println("<PATH OF THE OWL FILE> <TIMEOUT (sec)>");
		System.err.println();

	}

}
