package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

public class SimpleSat {

    // New variable name for Universally Quantified Variables
    public String uVariable = "GSat_u";
    // New variable name for Existentially Quantified Variables
    public String eVariable = "GSat_e";
    // New variable name for renamed Variables
    public String zVariable = "GSat_z";

    private static final SimpleSat INSTANCE = new SimpleSat();

    public static SimpleSat getInstance() {
        return INSTANCE;
    }

    /**
     * Private construtor, we want this class to be a Singleton
     */
    private SimpleSat() {
    }

    /**
     *
     * Main method to run the Guarded simple Saturation algorithm
     *
     * @param allDependencies the Guarded TGDs to process
     * @return the Guarded Saturation of allDependencies
     */
    public Collection<TGD> run(Collection<Dependency> allDependencies) {

        int discarded = 0;

        Collection<TGD> selectedTGDs = new HashSet<>();
        for (Dependency d : allDependencies)
            if (d instanceof TGD && ((TGD) d).isGuarded())
                selectedTGDs.add(new TGD(Set.of(d.getBodyAtoms()), Set.of(d.getHeadAtoms())));
            else
                discarded++;

        App.logger.info("GSat discarded rules : " + discarded + "/" + allDependencies.size() + " = "
                + String.format(Locale.UK, "%.3f", (float) discarded / allDependencies.size() * 100) + "%");

        // compute the set of full and non-full tgds in normal forms
        Set<TGD> fullTGDs = new HashSet<>();
        List<TGD> nonfullTGDs = new ArrayList<>();
        int width = 0;

        for (TGD tgd : selectedTGDs) {
            for (TGD hnf : tgd.computeHNF()) {
				TGD currentTGD = hnf.computeVNF(eVariable, uVariable);
                width = Math.max(currentTGD.getWidth(), width);
                if (Logic.isFull(currentTGD)) {
                    fullTGDs.add(currentTGD);
                } else {
                    nonfullTGDs.add(currentTGD);
                }
            }
        }

        App.logger.info("SimpleGSat width : " + width);

        Collection<TGD> resultingFullTDGs = new ArrayList<>(fullTGDs);

        do {

            List<TGD> currentFullTDGs = new ArrayList<>(resultingFullTDGs);

            resultingFullTDGs.clear();

			resultingFullTDGs.addAll(applyComposition(fullTGDs, currentFullTDGs, width));
			resultingFullTDGs.addAll(applyComposition(currentFullTDGs, fullTGDs, width));
            resultingFullTDGs.addAll(applyOriginal(nonfullTGDs, fullTGDs));

			System.out.println("step result" + resultingFullTDGs);

		} while (fullTGDs.addAll(resultingFullTDGs));

        return fullTGDs;
    }

    public Collection<TGD> applyOriginal(Collection<TGD> nonfullTGDs, Collection<TGD> fullTGDs) {
		Collection<TGD> resultingFullTGDs = new ArrayList<>();
        for (TGD nftgdbis : nonfullTGDs) {
            TGD nftgd = renameTgd(nftgdbis);
			Set<Variable> existentialVariables = Set.of(nftgd.getExistential());

            for (TGD ftgd : fullTGDs) {
                Atom[] head = nftgd.getHead().getAtoms();
                Atom[] body = ftgd.getBody().getAtoms();

                // check if there is a mgu between each atoms of the body and the head
                for (int k = 0; k < head.length; k++) {
                    for (int l = k; l < body.length; l++) {
                        Atom ha = head[k];
                        Atom ba = body[l];
						Map<Term, Term> mgu = Logic.getMGU(ba, ha);

						if (mgu != null) 
							mgu = makeUnifierIdentityOnVariables(mgu, existentialVariables);

                        if (mgu != null) {
                            // if there is a mgu with identify on existential variables
							// insert the result of the original inference rules on the tgds
                            TGD original = createOriginal(nftgd, ftgd, mgu);
                            if (original != null) {
                                resultingFullTGDs.add(original);
                            }
                        }
                    }
                }

            }
        }
		return resultingFullTGDs;
    }

	/**
	 * @param unifier - an unifier
	 * @param variables - an set of variables
	 * @return an equivalent unifier, whose is the identity on variables, or null if a such unifier does not exist
	 */	
	private Map<Term, Term> makeUnifierIdentityOnVariables(Map<Term, Term> unifier, Set<Variable> variables) {
	
		Set<Term> domain = new HashSet<>(unifier.keySet());
		for(Term v : domain) {
			if (variables.contains(v)) {
				// if the unifier is the identity on the variable
				if (unifier.get(v).equals(v)) {
					unifier.remove(v);
				} else {
					Term img = unifier.get(v);
					// need to unify the image to the variable
					if (variables.contains(img) && !img.isVariable()) 
						return null;

					unifier.put(img, v);
					unifier.remove(v);

					// others variables of the domain having the same image
					// have to unify to the current variable
					for (Term u : domain) {
						if (unifier.containsKey(u) &&
							unifier.get(u).equals(img)) {

							if (variables.contains(u))
								return null;
							
							unifier.put(u, v);
						}
					}
				}
			}
		}
		return unifier;
	}

	/**
	 * @return the application of original on nftgd and ftgd wrt unifier, 
	 *         if possible otherwise null
	 */	
	public TGD createOriginal(TGD nftgd, TGD ftgd, Map<Term, Term> unifier) {
	    Set<Atom> body = Logic.applyMGU(ftgd.getBodySet(), unifier);
	    body.removeAll(Logic.applyMGU(nftgd.getHeadSet(), unifier));
	
	    Collection<Term> imageFullVariable = new HashSet<>();
	
	    for (Variable var : nftgd.getTopLevelQuantifiedVariables()) {
			Term img = unifier.get(var);
			if (img != null) {
				imageFullVariable.add(img);
			} else {
				imageFullVariable.add(var);
			}
	    }
	
	    boolean haveNotExistentialInBody = true;
	    for (Atom atom : body) {
	        for(Variable var : atom.getVariables())
	            haveNotExistentialInBody = haveNotExistentialInBody && imageFullVariable.contains(var);
	    }
	    
	    if (haveNotExistentialInBody) {
	
			System.out.println(unifier);
			System.out.println(nftgd);
			System.out.println(ftgd);
			
	        body.addAll(Logic.applyMGU(nftgd.getBodySet(), unifier));
	        Set<Atom> headBeforePruning = Logic.applyMGU(ftgd.getHeadSet(), unifier);
	        // remove atom from head, if they contain existential variables
	        Set<Atom> head = new HashSet<>();
	        Set<Variable> existentialVariables = Set.of(nftgd.getExistential());
	        for (Atom atom: headBeforePruning) {
	            boolean containsExistential = false;
	            for (Variable var : atom.getVariables()) {
	                if (existentialVariables.contains(var)) {
	                    containsExistential = true;
	                    break;
	                }
	            }
	            if (!containsExistential) {
	                head.add(atom);
	            }
	        }
			System.out.println("body : " +body);
			System.out.println("head : " +head);
	        //
			if (!head.isEmpty()) {
				System.out.println("-> " + new TGD(body, head));
				return new TGD(body, head);
			}
	    }
	    return null;
	}
	
	/**
	 * @param s1 - a set of full TGDs
	 * @param s2 - a set of full TGDs
	 * @return all the possible on tgds infered by applying COMPOSITION on t1 in s1 and t2 in s2
	 *         with respect to the width
	 */	
	public Collection<TGD> applyComposition(Collection<TGD> s1, Collection<TGD> s2, int width) {
		Collection<TGD> resultingFullTGDs = new ArrayList<>();
	    for (TGD t1bis : s1) {
	        TGD t1 = renameTgd(t1bis);
	        for (TGD t2 : s2) {
	            resultingFullTGDs.addAll(compose(t1, t2, width));
	        }
	    }
		return resultingFullTGDs;
	}
	
	/**
	 * create the result of COMPOSITION inference rule
	 * @param t1 - a full {@link TGD}
	 * @param t2 - another full {@link TGD} sharing no variable with t1
	 * @return the composition of t1 with t2 with respect to the width
	 */
    private Collection<TGD> compose(TGD t1, TGD t2, int width) {
		Collection<TGD> resultingFullTGDs = new ArrayList<>();
        Atom[] head = t1.getHead().getAtoms();
        Atom[] body = t2.getBody().getAtoms();

        // check if there is a mgu between each atoms of the body and the head
        for (int k = 0; k < head.length; k++) {
            for (int l = k; l < body.length; l++) {
                Atom ha = head[k];
                Atom ba = body[l];
                Map<Term, Term> mgu = Logic.getMGU(ha, ba);
                if (mgu != null) {
                    // if there is a mgu create the composition of the tgds
                    TGD composition = createComposition(t1, t2, mgu);
                    Variable[] variables = composition.getTopLevelQuantifiedVariables();
                    if (variables.length > width) {
                        // if the composition contains more universal variables
                        // than the width, we need to form partitions of the variables having $width parts.
                        for (Map<Term, Term> unifier : getPartitionUnifiers(variables, width)) {
							resultingFullTGDs.add(((TGD) Logic.applySubstitution(composition, unifier))
									.computeVNF(eVariable, uVariable));
                        }
                    } else {
                        resultingFullTGDs.add(composition.computeVNF(eVariable, uVariable));
                    }
                }
            }
        }
		return resultingFullTGDs;
    }

	/**
	 * @param t1 - a full TGD 
	 * @param t2 - a full TGD
	 * @param unifier - an unifier of an head atom of t1 with a body atom of t2
	 * @return the composition of t1 and t2 wrt the unifier
	 */
    private TGD createComposition(TGD t1, TGD t2, Map<Term, Term> unifier) {
        Set<Atom> body = Logic.applyMGU(t2.getBodySet(), unifier);
        body.removeAll(Logic.applyMGU(t1.getHeadSet(), unifier));
        body.addAll(Logic.applyMGU(t1.getBodySet(), unifier));
        Set<Atom> head = Logic.applyMGU(t2.getHeadSet(), unifier);

        return new TGD(body, head);
    }

	/**
	 * This algorithm returns substitutions based on the different partitions of a set of variables
	 * having a fixed number of parts.
	 * This algorithm is highly inspired by the first algorithm presented in 
	 * "A fast iterative algorithm for generating set partitions"
	 * see https://academic.oup.com/comjnl/article/32/3/281/331557
	 *	
	 * @param variables - a array of variables
	 * @param partNumber - an integer
	 * @return a list of all u subtitutions of the variables such that 
	 *         domain(u) = {variables}
	 *         range(u)  included in variables	
	 *         |range(u)| = partNumber
	 *         whitout u1 and u2 in the results such that u1 != u2 
	 *                 and {u1^-1(y) | y in range(u1) } and {u2^-1(y) | y in range(u2) } are the same partition of {variables}	
	 */
    public static List<Map<Term, Term>> getPartitionUnifiers(Variable[] variables, int partNumber) {

        List<Map<Term, Term>> results = new ArrayList<>();
        int n = variables.length;
        // actual code position
        int r = -1;
        // actual partition code
        int[] code = new int[n];
        // g[i] = max(c_0, ..., c_{i-1})
        int[] g = new int[n];
        g[0] = 1;
        // intil we have increase the first code position
        while (r != 0) {
            // initialization of the end of the code
            // update of g
            while (r < n - 2) {
                r = r + 1;
                code[r] = 1;
                g[r + 1] = g[r];
            }

            // exploring the possible values for the last code value
            // i.e. c[n-1]
            for (int j = 1; j <= Math.min(g[n - 1] + 1, partNumber); j++) {
                code[n - 1] = j;
                if (g[n - 1] == partNumber || j == partNumber) {
                    results.add(createUniferFromPartition(variables, code));
                }
            }
            // backtrack to find a position to increase
            while (r > 0 && (code[r] > g[r] || code[r] >= partNumber)) {
                r = r - 1;
            }
            // increase the position
            code[r] = code[r] + 1;

            if (code[r] > g[r + 1]) {
                g[r + 1] = code[r];
            }
        }

        return results;
    }

	/**
	 * @param variables - array of variables
	 * @param partitionCode - array of integer of length variables.length
	 * @return a substitution of the variables where each variable is mapped 
	 *         to a representative of the partition of the part it belongs
	 */
    public static Map<Term, Term> createUniferFromPartition(Variable[] variables, int[] partitionCode) {
        Map<Term, Term> unifier = new HashMap<>();

        for (int pos = 0; pos < partitionCode.length; pos++) {
            Term key = variables[pos];
            Term value = variables[partitionCode[pos]];
            unifier.put(key, value);
        }

        return unifier;
    }

    private TGD renameTgd(TGD tgd) {

        Variable[] uVariables = tgd.getTopLevelQuantifiedVariables();

        Map<Term, Term> substitution = new HashMap<>();
        int counter = 1;
        for (Variable v : uVariables) {
            substitution.put(v, Variable.create(zVariable + counter++));
        }

        return (TGD) Logic.applySubstitution(tgd, substitution);

    }

}
