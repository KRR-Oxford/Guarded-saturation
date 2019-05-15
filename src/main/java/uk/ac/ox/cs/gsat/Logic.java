package uk.ac.ox.cs.gsat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Conjunction;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.fol.Disjunction;
import uk.ac.ox.cs.pdq.fol.Formula;
import uk.ac.ox.cs.pdq.fol.Implication;
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
			for (int termIndex = 0; termIndex < nterms.length; ++termIndex) {
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
			return Atom.create(((Atom) formula).getPredicate(), nterms);
		}
		throw new RuntimeException("Unsupported formula type: " + formula);
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
			} else
				App.logger.warning("The file " + f.getAbsolutePath() + " does not exists.");

		}

		final StringBuffer solverOutput = new StringBuffer();

		final StringBuffer solverError = new StringBuffer();

		final long startTime = System.nanoTime();

		final StringBuffer stringBuffer = new StringBuffer();

		if (exe_path == null)
			return new SolverOutput("", "Error: executable not found");

		stringBuffer.append(exe_path).append(" ").append(options).append(" ").append(files_paths);

		App.logger.fine(stringBuffer.toString());

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

		App.logger.info("Solver total time : " + String.format(Locale.UK, "%.0f", totalTime / 1E6) + " ms = "
				+ String.format(Locale.UK, "%.2f", totalTime / 1E9) + " s");

		return new SolverOutput(solverOutput.toString(), solverError.toString());

	}

	public static Map<Term, Term> getMGU(Atom s, Atom t) {

		return getMGU(s, t, null);

	}

	public static Map<Term, Term> getMGU(Atom s, Atom t, Map<Term, Term> renaming) {

		if (!s.getPredicate().equals(t.getPredicate()))
			// throw new IllegalArgumentException("Cannot compute MGU of atoms with
			// different predicate names or arity: "+ s + " and "+ t);
			return null;

		Map<Term, Term> sigma;

		if (renaming != null) {
			sigma = new HashMap<>(renaming);
			s = (Atom) applySubstitution(s, sigma);
			t = (Atom) applySubstitution(t, sigma);
		} else
			sigma = new HashMap<>();

		while (!s.equals(t)) {

			int different_position = -1;
			for (int i = 0; i < s.getTerms().length; i++)
				if (!s.getTerm(i).equals(t.getTerm(i))) {
					different_position = i;
					break;
				}

			if (different_position == -1)
				throw new RuntimeException("Unexpected error while computing the MGU of " + s + " and " + t);

			Term s_term = s.getTerm(different_position);
			Term t_term = t.getTerm(different_position);
			if (s_term.isVariable() || t_term.isVariable()) {

				if (s_term.isVariable())
					if (sigma.containsKey(s_term) || sigma.containsKey(t_term))
						return null;
					else {
						sigma.put(s_term, t_term);
						// s = (Atom) applySubstitution(s, Map.of(s_term, t_term));
						// t = (Atom) applySubstitution(t, Map.of(s_term, t_term));
					}

				else if (sigma.containsKey(t_term) || sigma.containsKey(s_term))
					return null;
				else {
					sigma.put(t_term, s_term);
					// s = (Atom) applySubstitution(s, Map.of(t_term, s_term));
					// t = (Atom) applySubstitution(t, Map.of(t_term, s_term));
				}

				s = (Atom) applySubstitution(s, sigma);
				t = (Atom) applySubstitution(t, sigma);

			} else
				return null;

		}

		boolean changed = true;
		while (changed) {
			changed = false;
			for (Entry<Term, Term> entry : sigma.entrySet())
				if (sigma.containsKey(entry.getValue())) {
					sigma.put(entry.getKey(), sigma.get(entry.getValue()));
					changed = true;
				}
		}

		return sigma;

	}

	// public static Map<Term, Term> getMGU(List<Atom> s, List<Atom> t) {

	// for (int i = 0; i < s.size(); i++) {
	// Atom s_atom = s.get(i);
	// boolean unified = false;

	// for (int j = 0; j < t.size(); j++) {
	// Atom t_atom = t.get(j);

	// Map<Term, Term> mgu = getMGU(s_atom, t_atom);

	// if (mgu != null) {
	// unified = true;
	// Map<Term, Term> mgu2 =
	// getMGU(s.stream().skip(i).collect(Collectors.toList()),
	// t.stream().skip(j).collect(Collectors.toList()));

	// if (mgu2 == null) {
	// return null;
	// }

	// Set<Term> keySet = mgu.keySet();
	// Set<Term> keySet2 = mgu2.keySet();
	// keySet.retainAll(keySet2);
	// if (!keySet2.isEmpty()) {

	// }

	// mgu.putAll(mgu2);
	// return mgu;
	// }

	// }

	// if (!unified)
	// return null;

	// }

	// return new HashMap<>();

	// }

}
