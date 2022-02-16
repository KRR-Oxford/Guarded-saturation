package uk.ac.ox.cs.gsat.mat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import tech.oxfordsemantic.jrdfox.Prefixes;
import tech.oxfordsemantic.jrdfox.logic.expression.IRI;
import tech.oxfordsemantic.jrdfox.logic.sparql.pattern.TriplePattern;
import uk.ac.ox.cs.gsat.Log;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Predicate;

public class Utils {
     
	/**
	 * From EmbASP code, slightly modified
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static SolverOutput invokeSolver(String exe_path, String options, List<String> files)
        throws InterruptedException, IOException {
		String files_paths = "";

		String final_program = "";

		for (final String program_file : files) {
			File f = new File(program_file);
			if (f.exists() && !f.isDirectory()) {
				files_paths += program_file;
				files_paths += " ";
			} else {
				Log.GLOBAL.warning("The file " + f.getAbsolutePath() + " does not exist.");
            }

		}

		final StringBuffer solverOutput = new StringBuffer();

		final StringBuffer solverError = new StringBuffer();

		final long startTime = System.nanoTime();

		final StringBuffer stringBuffer = new StringBuffer();

		if (exe_path == null)
			return new SolverOutput("", "Error: executable not found");

		stringBuffer.append(exe_path).append(" ").append(options).append(" ").append(files_paths);

		Log.GLOBAL.info(stringBuffer.toString());

		final Process solver_process = Runtime.getRuntime().exec(stringBuffer.toString());

		Thread threadOutput = new Thread() {
                @Override
                public void run() {
                    InputStreamReader in = new InputStreamReader(solver_process.getInputStream(), StandardCharsets.UTF_8);
                    final BufferedReader bufferedReaderOutput = new BufferedReader(in);

                    // Read output of the solver and store in solverOutput
                    String currentLine;
                    try {
                        while ((currentLine = bufferedReaderOutput.readLine()) != null)
                            solverOutput.append(currentLine + "\n");
                    } catch (IOException e) {
                        System.err.println("Error while reading the output of the solver.");
                    } finally {
                        if (bufferedReaderOutput != null)
                            try {
                                bufferedReaderOutput.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                    }
                }
            };

		threadOutput.start();
		threadOutput.join();

		Thread threadError = new Thread() {
                @Override
                public void run() {
                    InputStreamReader in = new InputStreamReader(solver_process.getErrorStream(), StandardCharsets.UTF_8);
                    final BufferedReader bufferedReaderError = new BufferedReader(in);
                    String currentErrLine;
                    try {
                        while ((currentErrLine = bufferedReaderError.readLine()) != null)
                            solverError.append(currentErrLine + "\n");
                    } catch (IOException e) {
                        System.err.println("Error while reading the output of the solver.");
                    } finally {
                        if (bufferedReaderError != null)
                            try {
                                bufferedReaderError.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                    }
                }
            };

		threadError.start();
		threadError.join();

		final PrintWriter writer = new PrintWriter(
                                                   new OutputStreamWriter(solver_process.getOutputStream(), StandardCharsets.UTF_8), true);
		writer.println(final_program);

		if (writer != null)
			writer.close();

		solver_process.waitFor();

		final long stopTime = System.nanoTime();

		long totalTime = stopTime - startTime;

		Log.GLOBAL.info("Solver total time : " + String.format(Locale.UK, "%.0f", totalTime / 1E6) + " ms = "
                        + String.format(Locale.UK, "%.2f", totalTime / 1E9) + " s");

		return new SolverOutput(solverOutput.toString(), solverError.toString());

	}

    public static void writeSolverOutput(SolverOutput solverOutput, String outputPath) throws IOException {

        Files.write(Paths.get(outputPath), Arrays.asList(solverOutput.getOutput(), solverOutput.getErrors()),
                    StandardCharsets.UTF_8);

    }


    /**
     * write a Ntriple files containing triples of the following forms 
     * c rdf:type P or c P c whether the predicate P is unary or binary
     * with c being a fixed IRI 
     * The used predicates P are those in the body of the tgd but in their head 
     */
    public static int generateNTriplesFromTGDs(Collection<TGD> tgds, String fileName) throws IOException {

        Set<Predicate> bodyPredicates = new HashSet<>();
        Set<Predicate> headPredicates = new HashSet<>();
        for (TGD tgd : tgds) {
            for (Atom atom : tgd.getBodyAtoms()) {
                bodyPredicates.add(atom.getPredicate());
            }

            for (Atom atom : tgd.getHeadAtoms()) {
                headPredicates.add(atom.getPredicate());
            }
        }

        // we consider only the predicates that appear in TGD bodies but in heads
        bodyPredicates.removeAll(headPredicates);

        Collection<TriplePattern> triples = new ArrayList<>();
        IRI constant = IRI.create("http://example.com/c");
        for (Predicate predicate : bodyPredicates) {
            TriplePattern triple;
            if (predicate.getArity() == 1) {
                triple = TriplePattern.create(constant, IRI.RDF_TYPE, RDFoxFactory.predicateAsIRI(predicate));
            } else if (predicate.getArity() == 2) {
                triple = TriplePattern.create(constant, RDFoxFactory.predicateAsIRI(predicate), constant);
            } else {
                String message = String.format("The predicate %s is neither unary nor binary", predicate);
                throw new IllegalStateException(message);
            }
            triples.add(triple);
        }

        // write the triples to a file
        File file = new File(fileName);
        file.delete();
        file.createNewFile();

        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));

        for (TriplePattern triple : triples) {
            writer.write(triple.toString(Prefixes.s_emptyPrefixes) + " .");
            writer.write("\n");
        }

        writer.close();
        return triples.size();
    }
}
