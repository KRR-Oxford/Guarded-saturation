package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import uk.ac.ox.cs.gsat.subsumers.Subsumer;
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
            if (GSat.isSupportedRule(d))
                selectedTGDs.add(new TGD(Set.of(d.getBodyAtoms()), Set.of(d.getHeadAtoms())));
            else
                discarded++;

        App.logger.info("GSat discarded rules : " + discarded + "/" + allDependencies.size() + " = "
                + String.format(Locale.UK, "%.3f", (float) discarded / allDependencies.size() * 100) + "%");

        // compute the set of full and non-full tgds in normal forms
        Subsumer<TGD> fullTGDSubsumer = GSat.createSubsumer();
        Collection<TGD> fullTGDs = new ArrayList<>();
        List<TGD> nonfullTGDs = new ArrayList<>();
        int width = 0;

        for (TGD tgd : selectedTGDs) {
            for (TGD hnf : tgd.computeHNF()) {
				TGD currentTGD = hnf.computeVNF(eVariable, uVariable);
                width = Math.max(currentTGD.getWidth(), width);
                if (Logic.isFull(currentTGD) && !fullTGDSubsumer.subsumed(tgd)) {
                    fullTGDs.add(currentTGD);
                    fullTGDSubsumer.subsumesAny(tgd);
                    fullTGDSubsumer.add(tgd);
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
            resultingFullTDGs.addAll(applyComposition(fullTGDs, fullTGDSubsumer, currentFullTDGs, width, true));
            resultingFullTDGs.addAll(applyComposition(fullTGDs, fullTGDSubsumer, currentFullTDGs, width, false));
            resultingFullTDGs.addAll(applyOriginal(nonfullTGDs, fullTGDs, fullTGDSubsumer));



        } while (fullTGDs.addAll(resultingFullTDGs));

        return fullTGDs;
    }


    /**
     * Warning : it has side effects on fullTGDSubsumer and on fullTGDs
     * @param nonfullTGDs
     * @param fullTGDs
     * @param fullTGDSubsumer
     * @return all the ORIGINAL compositions between a collection of non full TGDs and a collection of full TGDs
     */    
    private Collection<TGD> applyOriginal(Collection<TGD> nonfullTGDs, Collection<TGD> fullTGDs, Subsumer<TGD> fullTGDSubsumer) {
        Collection<TGD> resultingFullTGDs = new ArrayList<>();
        Collection<TGD> fullTGDsCopy = new ArrayList<>(fullTGDs);

        for (TGD nftgdbis : nonfullTGDs) {
            for (TGD ftgd : fullTGDsCopy) {
                for (TGD o : originalNew(nftgdbis, ftgd)) {
                    if (!fullTGDSubsumer.subsumed(o)) {
                        resultingFullTGDs.add(o);
                        fullTGDs.removeAll(fullTGDSubsumer.subsumesAny(o));
                        fullTGDSubsumer.add(o);
                    }
                }
            }
        }
        return resultingFullTGDs;
    }
    
    /**
     * @param nftgd - Non full TGD
     * @param ftgd - Full TGD
     * @return the collection of full TGDs resulting from the ORIGINAL compositions of nftgd with ftgd
     */
    Collection<TGD> originalNew(TGD nftgd, TGD ftgd) {
    
        ftgd = renameTgd(ftgd);
    
        App.logger.fine("apply original on:\n" + nftgd + "\nand\n" + ftgd);
    
    
        Collection<TGD> results = new ArrayList<>();
    
        // if the ftgd is guarded it is sufficient to all unify the guard
        Collection<Atom> bodyAtomsToUnify;
        if (ftgd.isGuarded()) {
            bodyAtomsToUnify = List.of(GTGD.fromTGD(ftgd).getGuard());
        } else {
            bodyAtomsToUnify = Arrays.asList(ftgd.getBodyAtoms());
        }
    
        for (Atom B : bodyAtomsToUnify) {
            for (Atom H : nftgd.getHeadAtoms()) {
    
                Map<Term, Term> guardMGU = GSat.getGuardMGU(B, H, eVariable, uVariable);
    
                if (guardMGU != null && !guardMGU.isEmpty()) {
    
                    final TGD new_nftgd = (TGD) Logic.applySubstitution(nftgd, guardMGU);
                    final TGD new_ftgd = (TGD) Logic.applySubstitution(ftgd, guardMGU);
    
                    final Set<Variable> new_nftgd_existentials = Set.of(new_nftgd.getExistential());
    
                    var new_nftgd_head_atoms = new_nftgd.getHeadSet();
                    var new_nftgd_body_atoms = new_nftgd.getBodySet();
                    var new_ftgd_head_atoms = new_ftgd.getHeadSet();
                    var new_ftgd_body_atoms = new_ftgd.getBodySet();
    
                    Set<Atom> new_body = new HashSet<>(new_ftgd_body_atoms);
                    Atom new_B = (Atom) Logic.applySubstitution(B, guardMGU);
                    new_body.remove(new_B);
                    List<Atom> Sbody = GSat.getSbody(new_body, new_nftgd_existentials);
                    new_body.addAll(new_nftgd_body_atoms);
    
                    TGD original_tgd = createOriginal(new_body, new_ftgd_head_atoms, new_nftgd_existentials);
                    // if the head of the original tgd is empty, no need to look for further mgu of Sbody
                    if (original_tgd == null) {
                        continue;
                    }
    
                    // variable of Sbody not included in the image of x variables, for any mgu
                    Set<Variable> SbodyVariables = new HashSet<>();
                    SbodyVariables.addAll(Set.of(new_B.getVariables()));
                    for (Atom a : Sbody)
                        for (Variable v : a.getVariables())
                            SbodyVariables.add(v);
    
                    if (!SbodyVariables.containsAll(Set.of(new_ftgd.getUniversal())))
                        continue;
    
                    List<List<Atom>> Shead = GSat.getShead(new_nftgd_head_atoms, Sbody, new_nftgd_existentials);
    
                    // if Sbody is empty, then Shead is empty, and we take this short-cut;
                    // in fact, we should never have Shead == null and Sbody.isEmpty
                    if (Shead == null || Shead.isEmpty()) {
                        if (Sbody.isEmpty()) {
                            results.add(original_tgd);
                        }
                        // no matching head atom for some atom in Sbody -> continue
                        continue;
                    }
    
                    App.logger.fine("Shead:" + Shead.toString());
    
                    for (List<Atom> S : GSat.getProduct(Shead)) {
    
                        App.logger.fine("Non-Full:" + new_nftgd.toString() + "\nFull:" + new_ftgd.toString() + "\nSbody:"
                                        + Sbody + "\nS:" + S);
    
                        Map<Term, Term> mgu = GSat.getVariableSubstitution(S, Sbody);
    
                        if (mgu == null)
                            // unification failed -> continue with next sequence
                            continue;
    
                        new_body.removeAll(Sbody);
    
                        if (mgu.isEmpty()) {
                            // no need to apply the MGU
                            TGD new_tgd = createOriginal(Logic.applyMGU(new_body, mgu), Logic.applyMGU(new_ftgd_head_atoms, mgu), new_nftgd_existentials);
                            results.add(new_tgd);
                        } else {
                            TGD new_tgd = createOriginal(Logic.applyMGU(new_body, mgu), Logic.applyMGU(new_ftgd_head_atoms, mgu), new_nftgd_existentials);
                            results.add(new_tgd);
                        }
                    }
                }
            }
        }
        return results;
    }
    
    
    /**
     * @param body - a set of atoms
     * @param headWithExistential - a collection of atoms with possibly some variables that are not in body
     * @param existentialVariable - the set of variable in headWithExistential, not in body
     * @return the full TGDs with body and headWithExistential from which the atoms 
     *         containing existential variables have been removed, or 
     *         null if all atom in headWithExistential contain at least an existential variable
     */    
    TGD createOriginal(Set<Atom> body, Collection<Atom> headWithExistential, Collection<Variable> existentialVariable) {
    	// define the head of the resulting tgd 
    	// remove atom from head, if they contain existential variables
    	Set<Atom> new_head = new HashSet<>();
    	for (Atom atom: headWithExistential) {
    		boolean containsExistential = false;
    		for (Variable var : atom.getVariables()) {
    			if (existentialVariable.contains(var)) {
    				containsExistential = true;
    				break;
    			}
    		}
    		if (!containsExistential) {
    			new_head.add(atom);
    		}
    	}
    
    	if(!new_head.isEmpty()) {
    		return new TGD(body, new_head).computeVNF(eVariable, uVariable);
    	} else {
    		return null;
    	}
    }
    
    /**
     * Warning : it has side effects on fullTGDSubsumer and on fullTGDs
     *
     * @param fullTGDs - a set of full TGDs
     * @param fullTGDSubsumer
     * @param otherFullTGDs - a set of full TGDs
     * @param reversed - a boolean
     * @return all the possible on tgds infered by applying COMPOSITION on t1 in fullTDGs and t2 in otherFullTGDs
     *         with respect to the width except if they are subsumed 
     *         or the other way around, if reversed is true
     */	
	public Collection<TGD> applyComposition(Collection<TGD> fullTGDs, Subsumer<TGD> fullTGDSubsumer, Collection<TGD> otherFullTGDs, int width, boolean reversed) {
		Collection<TGD> resultingFullTGDs = new ArrayList<>();
        Collection<TGD> s1 = (!reversed) ? new ArrayList<>(fullTGDs): otherFullTGDs;
        Collection<TGD> s2 = (!reversed) ? otherFullTGDs : new ArrayList<>(fullTGDs);

	    for (TGD t1bis : s1) {
	        TGD t1 = renameTgd(t1bis);
	        for (TGD t2 : s2) {
                for (TGD o : compose(t1, t2, width)) {
                    if (!fullTGDSubsumer.subsumed(o)) {
                        resultingFullTGDs.add(o);
                        fullTGDs.removeAll(fullTGDSubsumer.subsumesAny(o));
                        fullTGDSubsumer.add(o);
                    }
                }
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
		Collection<TGD> resultingFullTGDs = new HashSet<>();
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
        // for(TGD tgd : resultingFullTGDs)
        //     System.out.println(tgd);
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
