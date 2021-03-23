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
import java.util.stream.Collector;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

public class SimpleGSat {

    // New variable name for Universally Quantified Variables
    public String uVariable = "GSat_u";
    // New variable name for Existentially Quantified Variables
    public String eVariable = "GSat_e";
    // New variable name for renamed Variables
    public String zVariable = "GSat_z";

    private static final SimpleGSat INSTANCE = new SimpleGSat();

    public static SimpleGSat getInstance() {
        return INSTANCE;
    }

    /**
     * Private construtor, we want this class to be a Singleton
     */
    private SimpleGSat() {
    }

    /**
     *
     * Main method to run the Guarded simple Saturation algorithm
     *
     * @param allDependencies the Guarded TGDs to process
     * @return the Guarded Saturation of allDependencies
     */
    public Collection<GTGD> run(Collection<Dependency> allDependencies) {

        int discarded = 0;

        Collection<GTGD> selectedTGDs = new HashSet<>();
        for (Dependency d : allDependencies)
            if (d instanceof TGD && ((TGD) d).isGuarded())
                selectedTGDs.add(new GTGD(Set.of(d.getBodyAtoms()), Set.of(d.getHeadAtoms())));
            else
                discarded++;

        App.logger.info("GSat discarded rules : " + discarded + "/" + allDependencies.size() + " = "
                + String.format(Locale.UK, "%.3f", (float) discarded / allDependencies.size() * 100) + "%");

        // compute the set of full and non-full tgds in normal forms
        List<GTGD> fullTGDs = new ArrayList<>();
        List<GTGD> nonfullTGDs = new ArrayList<>();
        int width = 0;

        for (GTGD tgd : selectedTGDs) {
            for (GTGD currentTGD : Logic.VNFs(Logic.HNF(tgd), eVariable, uVariable)) {
                width = Math.max(currentTGD.getWidth(), width);
                if (Logic.isFull(currentTGD)) {
                    fullTGDs.add(currentTGD);
                } else {
                    nonfullTGDs.add(currentTGD);
                }
            }
        }

        App.logger.info("SimpleGSat width : " + width);

        Collection<GTGD> resultingFullTDGs = new ArrayList<>(fullTGDs);

        while (!resultingFullTDGs.isEmpty()) {

            List<GTGD> currentFullTDGs = new ArrayList<>(resultingFullTDGs);

            resultingFullTDGs.clear();

            insertAllComposition(fullTGDs, currentFullTDGs, width, resultingFullTDGs);
            insertAllComposition(currentFullTDGs, fullTGDs, width, resultingFullTDGs);
            insertAllOriginal(nonfullTGDs, fullTGDs, resultingFullTDGs);

            fullTGDs.addAll(resultingFullTDGs);
        }
        return fullTGDs;

    }

    public void insertAllOriginal(Collection<GTGD> nonfullTGDs, Collection<GTGD> fullTGDs,
            Collection<GTGD> resultingFullTGDs) {
        for (GTGD nftgdbis : nonfullTGDs) {
            GTGD nftgd = renameTgd(nftgdbis);

            Map<Term, Term> identityOnExistential = new HashMap<>();
            for(Variable variable : nftgd.getExistential()) {
                identityOnExistential.put(variable, variable);
            }
            System.out.println(identityOnExistential);
            
            for (GTGD ftgd : fullTGDs) {
                Atom[] head = nftgd.getHead().getAtoms();
                Atom[] body = ftgd.getBody().getAtoms();

                // check if there is a mgu between each atoms of the body and the head
                for (int k = 0; k < head.length; k++) {
                    for (int l = k; l < body.length; l++) {
                        Atom ha = head[k];
                        Atom ba = body[l];
                        Map<Term, Term> mgu = Logic.getMGU(ha, ba, identityOnExistential);
                        if (mgu != null) {
                            System.out.println("mgu : " + mgu + "\n");
                            System.out.println(nftgd);
                            System.out.println(ftgd);
                            // if there is a mgu create the composition of the tgds
                            GTGD original = createOriginal(nftgd, ftgd, mgu);
                            if (original != null) {
                                resultingFullTGDs.add(original);
                            }
                        }
                    }
                }

            }
        }
    }

    public GTGD createOriginal(GTGD nftgd, GTGD ftgd, Map<Term, Term> unifier) {
        Set<Atom> body = Logic.applyMGU(ftgd.getBodySet(), unifier);
        body.removeAll(Logic.applyMGU(nftgd.getHeadSet(), unifier));

        Collection<Term> imageFullVariable = new HashSet<>();

        for (Variable var : ftgd.getTopLevelQuantifiedVariables()) {
            imageFullVariable.add(unifier.get(var));
        }

        boolean isPieceUnification = true;
        for (Atom atom : body) {
            for(Variable var : atom.getVariables())
                isPieceUnification = isPieceUnification && imageFullVariable.contains(var);
        }
        
        if (isPieceUnification) {
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
            
            //
            return new GTGD(body, head);
        }
        return null;
    }

    public void insertAllComposition(Collection<GTGD> s1, Collection<GTGD> s2, int width,
            Collection<GTGD> resultingFullTGDs) {
        for (GTGD t1bis : s1) {
            GTGD t1 = renameTgd(t1bis);
            for (GTGD t2 : s2) {
                insertComposition(t1, t2, width, resultingFullTGDs);
            }
        }
    }

    /*
     * assume that t1 and t2 do not share variables insert to resultingFullTGDs the
     * composition of t1 with t2 according to width
     */
    private void insertComposition(GTGD t1, GTGD t2, int width, Collection<GTGD> resultingFullTGDs) {
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
                    GTGD composition = createComposition(t1, t2, mgu, width);
                    Variable[] variables = composition.getTopLevelQuantifiedVariables();
                    if (variables.length > width) {
                        // if the composition contains more universal variables
                        // than the width, we need to form partitions the variables having $width parts.
                        for (Map<Term, Term> unifier : getUnifiersWith(variables, width)) {
                            resultingFullTGDs.add(Logic.VNF((GTGD) Logic.applySubstitution(composition, unifier),
                                    eVariable, uVariable));
                        }
                    } else {
                        resultingFullTGDs.add(composition);
                    }
                }
            }
        }
    }

    private GTGD createComposition(GTGD t1, GTGD t2, Map<Term, Term> mgu, int width) {
        Set<Atom> body = Logic.applyMGU(t2.getBodySet(), mgu);
        body.removeAll(Logic.applyMGU(t1.getHeadSet(), mgu));
        body.addAll(Logic.applyMGU(t1.getBodySet(), mgu));
        Set<Atom> head = Logic.applyMGU(t2.getHeadSet(), mgu);

        return new GTGD(body, head);
    }

    public static List<Map<Term, Term>> getUnifiersWith(Term[] variables, int partNumber) {

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
                    results.add(createUniferFromPart(variables, code));
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

    public static Map<Term, Term> createUniferFromPart(Term[] variables, int[] part) {
        Map<Term, Term> unifier = new HashMap<>();

        for (int pos = 0; pos < part.length; pos++) {
            Term key = variables[pos];
            Term value = variables[part[pos]];
            unifier.put(key, value);
        }

        return unifier;
    }

    private GTGD renameTgd(GTGD tgd) {

        Variable[] uVariables = tgd.getTopLevelQuantifiedVariables();

        Map<Term, Term> substitution = new HashMap<>();
        int counter = 1;
        for (Variable v : uVariables) {
            substitution.put(v, Variable.create(zVariable + counter++));
        }

        return (GTGD) Logic.applySubstitution(tgd, substitution);

    }

}
