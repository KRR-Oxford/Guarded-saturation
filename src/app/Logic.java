import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Conjunction;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.fol.Disjunction;
import uk.ac.ox.cs.pdq.fol.Formula;
import uk.ac.ox.cs.pdq.fol.Implication;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * Helper functions for logic-based operations
 * 
 * @author Stefano
 */
public class Logic {

	public static boolean isFull(TGD tgd) {

		return tgd.getExistential().length == 0;

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
				// we assume UNA also between variables and constants
				// if (term.isVariable() && substitution.containsKey(term))
				if (substitution.containsKey(term))
					nterms[termIndex] = substitution.get(term);
				else
					nterms[termIndex] = term;
			}
			Predicate predicate = ((Atom) formula).getPredicate(); // to Lower Case
			return Atom.create(Predicate.create(predicate.getName().toLowerCase(), predicate.getArity()), nterms);
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

	/**
	 * From EmbASP code, slightly modified
	 */
	static SolverOutput invokeSolver(String exe_path, String options, List<String> files) {
		String files_paths = new String();

		String final_program = new String();

		for (final String program_file : files) {
			File f = new File(program_file);
			if (f.exists() && !f.isDirectory()) {
				files_paths += program_file;
				files_paths += " ";
			} else
				App.logger.warn("The file " + f.getAbsolutePath() + " does not exists.");

		}

		final StringBuffer solverOutput = new StringBuffer();

		final StringBuffer solverError = new StringBuffer();

		try {

			final long startTime = System.nanoTime();

			final StringBuffer stringBuffer = new StringBuffer();

			if (exe_path == null)
				return new SolverOutput("", "Error: executable not found");

			stringBuffer.append(exe_path).append(" ").append(options).append(" ").append(files_paths);

			App.logger.debug(stringBuffer.toString());

			final Process solver_process = Runtime.getRuntime().exec(stringBuffer.toString());

			Thread threadOutput = new Thread() {
				@Override
				public void run() {
					try {
						final BufferedReader bufferedReaderOutput = new BufferedReader(
								new InputStreamReader(solver_process.getInputStream()));

						// Read output of the solver and store in solverOutput
						String currentLine;
						while ((currentLine = bufferedReaderOutput.readLine()) != null)
							solverOutput.append(currentLine + "\n");
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
			};

			threadOutput.start();
			threadOutput.join();

			Thread threadError = new Thread() {
				@Override
				public void run() {
					try {
						final BufferedReader bufferedReaderError = new BufferedReader(
								new InputStreamReader(solver_process.getErrorStream()));
						String currentErrLine;
						while ((currentErrLine = bufferedReaderError.readLine()) != null)
							solverError.append(currentErrLine + "\n");
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
			};

			threadError.start();
			threadError.join();

			final PrintWriter writer = new PrintWriter(solver_process.getOutputStream());
			writer.println(final_program);

			if (writer != null)
				writer.close();

			solver_process.waitFor();

			final long stopTime = System.nanoTime();

			App.logger.info("Solver total time : " + (stopTime - startTime) / 10E6 + " ms");

			return new SolverOutput(solverOutput.toString(), solverError.toString());

		} catch (final IOException e2) {
			e2.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		return new SolverOutput("", "");

	}

}
