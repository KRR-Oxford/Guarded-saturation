package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * GSat
 */
public class GSat {

    private static final GSat INSTANCE = new GSat();
    private boolean DEBUG_MODE = Configuration.isDebugMode();

    // New variable name for Universally Quantified Variables
    public String uVariable = "GSat_u";
    // New variable name for Existentially Quantified Variables
    public String eVariable = "GSat_e";
    // New variable name for evolveRename
    public String zVariable = "GSat_z";

    /**
     * Private construtor, we want this class to be a Singleton
     */
    private GSat() {
    }

    /**
     *
     * @return Singleton instace of GSat
     */
    public static GSat getInstance() {
        return INSTANCE;
    }

    /**
     *
     * Main method to run the Guarded Saturation algorithm
     *
     * @param allDependencies the Guarded TGDs to process
     * @return the Guarded Saturation of allDependencies
     */
    public Collection<TGDGSat> runGSat(Collection<Dependency> allDependencies) {

        System.out.println("Running GSat...");
        final long startTime = System.nanoTime();

        int discarded = 0;

        Collection<TGDGSat> selectedTGDs = new HashSet<>();
        for (Dependency d : allDependencies)
            if (isSupportedRule(d))
                selectedTGDs.add(new TGDGSat(d.getBodyAtoms(), d.getHeadAtoms()));
            else
                discarded++;

        App.logger.info("GSat discarded rules : " + discarded + "/" + allDependencies.size() + " = "
                + String.format(Locale.UK, "%.3f", (float) discarded / allDependencies.size() * 100) + "%");

        while (checkRenameVariablesInTGDs(selectedTGDs)) {
            uVariable += "0";
            eVariable += "0";
            zVariable += "0";
        }

        Collection<TGDGSat> newFullTGDs = new HashSet<>();
        Collection<TGDGSat> newNonFullTGDs = new HashSet<>();

        Collection<TGDGSat> nonFullTGDs = new HashSet<>();
        Collection<TGDGSat> fullTGDs = new HashSet<>();

        for (TGDGSat tgd : selectedTGDs)
            for (TGDGSat currentTGD : VNFs(HNF(tgd)))
                if (Logic.isFull(currentTGD))
                    fullTGDs.add(currentTGD);
                else
                    newNonFullTGDs.add(currentTGD);

        App.logger.fine("# initial TGDs: " + fullTGDs.size() + " , " + newNonFullTGDs.size());
        

        int counter = 100;
        while (!newFullTGDs.isEmpty() || !newNonFullTGDs.isEmpty()) {
            // System.out.print('.');

            App.logger.fine("# new TGDs: " + newFullTGDs.size() + " , " + newNonFullTGDs.size());
            // newTGDs.forEach(tgd -> App.logger.fine(tgd.toString()));

            if (DEBUG_MODE)
                if (counter % 100 == 0) {
                    counter = 1;
                    System.out.println("nonFullTGDs\t" + nonFullTGDs.size() + "\t\tfullTGDs\t" + fullTGDs.size()
                            + "\t\t\tnewNonFullTGDs\t" + newNonFullTGDs.size() + "\t\tnewFullTGDs\t"
                            + newFullTGDs.size());
                } else
                    counter++;

            if (!newNonFullTGDs.isEmpty()) {

                Iterator<TGDGSat> iterator = newNonFullTGDs.iterator();
                TGDGSat currentTGD = iterator.next();
                iterator.remove();
                App.logger.fine("current TGD: " + currentTGD);

                boolean added = nonFullTGDs.add(currentTGD);
                if (added)
                    for (TGDGSat ftgd : fullTGDs)
                        for (TGDGSat newTGD : evolveNew(currentTGD, ftgd))
                            addNewTGD(newTGD, newFullTGDs, newNonFullTGDs, fullTGDs, nonFullTGDs);

            } else {

                Iterator<TGDGSat> iterator = newFullTGDs.iterator();
                TGDGSat currentTGD = iterator.next();
                iterator.remove();
                App.logger.fine("current TGD: " + currentTGD);

                boolean added = fullTGDs.add(currentTGD);
                if (added)
                    for (TGDGSat nftgd : nonFullTGDs)
                        for (TGDGSat newTGD : evolveNew(nftgd, currentTGD))
                            addNewTGD(newTGD, newFullTGDs, newNonFullTGDs, fullTGDs, nonFullTGDs);

            }

        }

        // System.out.println();

        final long stopTime = System.nanoTime();

        long totalTime = stopTime - startTime;

        App.logger.info("GSat total time : " + String.format(Locale.UK, "%.0f", totalTime / 1E6) + " ms = "
                + String.format(Locale.UK, "%.2f", totalTime / 1E9) + " s");

        return fullTGDs;

    }

    private void addNewTGD(TGDGSat newTGD, Collection<TGDGSat> newFullTGDs, Collection<TGDGSat> newNonFullTGDs,
            Collection<TGDGSat> fullTGDs, Collection<TGDGSat> nonFullTGDs) {

        if (Configuration.isOptimizationEnabled())
            if (Logic.isFull(newTGD) && (subsumed(newTGD, newFullTGDs) || subsumed(newTGD, fullTGDs))
                    || !Logic.isFull(newTGD) && (subsumed(newTGD, newNonFullTGDs) || subsumed(newTGD, nonFullTGDs)))
                return;

        if (Logic.isFull(newTGD))
            newFullTGDs.add(newTGD);
        else
            newNonFullTGDs.add(newTGD);

    }

    private boolean subsumed(TGDGSat newTGD, Collection<TGDGSat> TGDs) {

        // FIXME We could keep these as sets in TGDGSat, so we avoid this step (and many
        // more in other functions and classes)
        var bodyN = newTGD.getBodySet();
        var headN = newTGD.getHeadSet();

        for (TGDGSat tgd : TGDs) {
            var body = tgd.getBodySet();
            var head = tgd.getHeadSet();

            if (bodyN.size() < body.size() || head.size() < headN.size())
                continue;

            if (bodyN.containsAll(body) && head.containsAll(headN))
                return true;
        }

        return false;

    }

    /**
     *
     * A checker for self-join
     *
     * @param tgd a TGD
     * @return true if the TGD contains 2 atoms in the body or in the head with the
     *         same predicate name (needed only until we implement a generic MGU)
     */
    

    private boolean isSupportedRule(Dependency d) {
        // if (d instanceof TGD && ((TGD) d).isGuarded() && !containsSelfJoin((TGD) d))
        // // Adding only Guarded TGDs
        return d instanceof TGD && ((TGD) d).isGuarded(); // Adding only Guarded TGDs
        // if (!(d instanceof EGD))
    }

    /**
     *
     * Check if the variables we use in the rename are already used in the input
     * TGDs
     *
     * @param TGDs
     * @return true if it founds a variable with the same name of one of our rename
     *         variables
     */
    private boolean checkRenameVariablesInTGDs(Collection<TGDGSat> TGDs) {

        for (TGDGSat tgd : TGDs)
            for (String symbol : tgd.getAllTermSymbols())
                if (symbol.startsWith(uVariable) || symbol.startsWith(eVariable) || symbol.startsWith(zVariable)) {
                    App.logger.info("Found rename variable: " + symbol);
                    return true;
                }

        return false;

    }

    /**
     *
     * Returns the Head Normal Form (HNF) of the input TGD
     *
     * @param tgd an input TGD
     * @return Head Normal Form of tgd
     */
    public Collection<TGDGSat> HNF(final TGDGSat tgd) {

        Collection<TGDGSat> result = new HashSet<>();

        if (tgd == null)
            return result;

        Variable[] eVariables = tgd.getExistential();

        Collection<Atom> eHead = new HashSet<>();
        Collection<Atom> fHead = new HashSet<>();

        Atom[] bodyAtoms = tgd.getBodyAtoms();
        for (Atom a : tgd.getHeadAtoms())
            if (a.equals(TGDGSat.Bottom)) {
                // remove all head atoms since ⊥ & S ≡ ⊥ for any conjunction S
                result.add(new TGDGSat(bodyAtoms, new Atom[] { TGDGSat.Bottom }));
                return result;
            } else if (Logic.containsAny(a, eVariables))
                eHead.add(a);
            else
                fHead.add(a);

        // Remove atoms that already appear in the body.
        // This is only needed for fHead since we have no existentials in the body
        fHead.removeAll(Arrays.asList(bodyAtoms));// FIXME use getBodySet()

        if (tgd.getHeadAtoms().length == eHead.size() || tgd.getHeadAtoms().length == fHead.size())
            result.add(tgd);
        else {
            if (!eHead.isEmpty())
                result.add(new TGDGSat(bodyAtoms, eHead.toArray(new Atom[eHead.size()])));
            if (!fHead.isEmpty())
                result.add(new TGDGSat(bodyAtoms, fHead.toArray(new Atom[fHead.size()])));
        }

        return result;

    }

    /**
     *
     * Returns the Variable Normal Form (VNF) of all the input TGDs
     *
     * @param tgds a collection of TGDs
     * @return Variable Normal Form of tgds
     */
    public Collection<TGDGSat> VNFs(Collection<TGDGSat> tgds) {

        return tgds.stream().map((tgd) -> VNF(tgd)).collect(Collectors.toList());

    }

    /**
     *
     * Returns the Variable Normal Form (VNF) of the input TGD
     *
     * @param tgd an input TGD
     * @return Variable Normal Form of tgd
     */
    public TGDGSat VNF(TGDGSat tgd) {

        if (tgd == null)
            throw new IllegalArgumentException("Null TGD in VNF");

        Variable[] uVariables = tgd.getUniversal();
        Variable[] eVariables = tgd.getExistential();
        // App.logger.finest(uVariables.toString());
        // App.logger.finest(eVariables.toString());

        Map<Term, Term> substitution = new HashMap<>();
        int counter = 1;
        for (Variable v : uVariables)
            substitution.put(v, Variable.create(uVariable + counter++));
        counter = 1;
        for (Variable v : eVariables)
            substitution.put(v, Variable.create(eVariable + counter++));

        App.logger.fine("VNF substitution:\n" + substitution);

        TGDGSat applySubstitution = (TGDGSat) Logic.applySubstitution(tgd, substitution);
        App.logger.fine("VNF: " + tgd + "===>>>" + applySubstitution);
        return applySubstitution;

    }

    

    private TGDGSat evolveRename(TGDGSat ftgd) {

        Variable[] uVariables = ftgd.getUniversal();

        Map<Term, Term> substitution = new HashMap<>();
        int counter = 1;
        for (Variable v : uVariables) {
            if (!v.getSymbol().startsWith(uVariable))
                throw new IllegalArgumentException("TGD not valid in evolveRename: " + ftgd);
            substitution.put(v, Variable.create(zVariable + counter++));
        }

        return (TGDGSat) Logic.applySubstitution(ftgd, substitution);

    }

    

    private Atom[] applyMGU(Collection<Atom> atoms, Map<Term, Term> mgu) {

        Collection<Atom> result = new HashSet<>();

        atoms.forEach(atom -> result.add((Atom) Logic.applySubstitution(atom, mgu)));

        return result.toArray(new Atom[result.size()]);

    }

    /**
     *
     * @param nftgd non-full TGD (guarded)
     * @param ftgd  full TGD (guarded)
     * @return the derived rules of nftgd and ftgd according to the EVOLVE inference
     *         rule
     */
    public Collection<TGDGSat> evolveNew(TGDGSat nftgd, TGDGSat ftgd) {

        ftgd = evolveRename(ftgd);

        App.logger.fine("Composing:\n" + nftgd + "\nand\n" + ftgd);

        Atom guard = new TGDGSat(ftgd).getGuard();
        Collection<TGDGSat> results = new HashSet<>();

        for (Atom H : nftgd.getHeadAtoms()) {

            Map<Term, Term> guardMGU = getGuardMGU(guard, H);

            if (guardMGU != null && !guardMGU.isEmpty()) {

                final TGDGSat new_nftgd = applyMGU(nftgd, guardMGU);
                final TGDGSat new_ftgd = applyMGU(ftgd, guardMGU);

                final List<Variable> new_nftgd_existentials = Arrays.asList(new_nftgd.getExistential());

                var new_nftgd_head_atoms = new_nftgd.getHeadSet();
                var new_nftgd_body_atoms = new_nftgd.getBodySet();
                var new_ftgd_head_atoms = new_ftgd.getHeadSet();
                var new_ftgd_body_atoms = new_ftgd.getBodySet();

                Atom new_guard = (Atom) Logic.applySubstitution(guard, guardMGU);
                List<Atom> Sbody = getSbody(new_ftgd_body_atoms, new_guard, new_nftgd_existentials);

                List<List<Atom>> Shead = getShead(new_nftgd_head_atoms, Sbody, new_nftgd_existentials);

                // if Sbody is empty, then Shead is empty, and we take this short-cut;
                // in fact, we should never have Shead == null and Sbody.isEmpty
                if (Shead == null || Shead.isEmpty()) {
                    if (Sbody.isEmpty()) {
                        Collection<Atom> new_body = new HashSet<>();
                        new_body.addAll(new_nftgd_body_atoms);
                        new_body.addAll(new_ftgd_body_atoms);
                        new_body.remove(new_guard);

                        Collection<Atom> new_head = new HashSet<>();
                        new_head.addAll(new_nftgd_head_atoms);
                        new_head.addAll(new_ftgd_head_atoms);

                        results.addAll(VNFs(HNF(new TGDGSat(new_body.toArray(new Atom[new_body.size()]),
                                new_head.toArray(new Atom[new_head.size()])))));
                    }
                    // no matching head atom for some atom in Sbody -> continue
                    continue;
                }

                App.logger.fine("Shead:" + Shead.toString());

                for (List<Atom> S : getProduct(Shead)) {

                    App.logger.fine("Non-Full:" + new_nftgd.toString() + "\nFull:" + new_ftgd.toString() + "\nSbody:"
                            + Sbody + "\nS:" + S);

                    Map<Term, Term> mgu = getVariableSubstitution(S, Sbody);
                    if (mgu == null)
                        // unification failed -> continue with next sequence
                        continue;

                    Collection<Atom> new_body = new HashSet<>();
                    new_body.addAll(new_nftgd_body_atoms);
                    new_body.addAll(new_ftgd_body_atoms);
                    new_body.removeAll(Sbody);
                    new_body.remove(new_guard);

                    Collection<Atom> new_head = new HashSet<>();
                    new_head.addAll(new_nftgd_head_atoms);
                    new_head.addAll(new_ftgd_head_atoms);

                    if (mgu.isEmpty())
                        // no need to apply the MGU
                        results.addAll(VNFs(HNF(new TGDGSat(new_body.toArray(new Atom[new_body.size()]),
                                new_head.toArray(new Atom[new_head.size()])))));
                    else
                        results.addAll(VNFs(HNF(applyMGU(new_body, new_head, mgu))));

                }

            }

        }

        return results;

    }

   

    public Map<Term, Term> getVariableSubstitution(List<Atom> head, List<Atom> body) {

        Map<Term, Term> sigma = new HashMap<>();

        if (head.size() != body.size())
            throw new IllegalArgumentException();

        // assume they are all in the same order
        for (int i = 0; i < head.size(); i++) {
            Atom atom_h = head.get(i);
            Atom atom_b = body.get(i);

            if (!atom_h.getPredicate().equals(atom_b.getPredicate()))
                throw new IllegalArgumentException();

            sigma = Logic.getMGU(atom_h, atom_b, sigma);

        }

        return sigma;
    }

    /**
     * Mainly from https://stackoverflow.com/a/9496234
     *
     * @param shead
     * @return
     */
    private List<List<Atom>> getProduct(List<List<Atom>> shead) {

        List<List<Atom>> resultLists = new ArrayList<List<Atom>>();
        if (shead.size() == 0) {
            resultLists.add(new ArrayList<Atom>());
            return resultLists;
        } else {
            List<Atom> firstList = shead.get(0);
            List<List<Atom>> remainingLists = getProduct(shead.subList(1, shead.size()));
            for (Atom condition : firstList) {
                for (List<Atom> remainingList : remainingLists) {
                    ArrayList<Atom> resultList = new ArrayList<Atom>();
                    resultList.add(condition);
                    resultList.addAll(remainingList);
                    resultLists.add(resultList);
                }
            }
        }
        return resultLists;
    }

    private List<List<Atom>> getShead(Collection<Atom> headAtoms, Collection<Atom> sbody, List<Variable> eVariables) {

        List<List<Atom>> resultLists = new ArrayList<List<Atom>>();

        for (Atom bodyAtom : sbody) {

            List<Atom> temp = new ArrayList<>();

            for (Atom headAtom : headAtoms)
                if (headAtom.getPredicate().equals(bodyAtom.getPredicate())) {
                    boolean valid = true;
                    Term[] headTerms = headAtom.getTerms();
                    for (int i = 0; i < headTerms.length; i++) {
                        Term bodyTerm = bodyAtom.getTerm(i);
                        Term headTerm = headTerms[i];
                        // check if constants and existentials match
                        if ((headTerm.isUntypedConstant() && bodyTerm.isUntypedConstant()
                                || eVariables.contains(headTerm) || eVariables.contains(bodyTerm))
                                && !bodyTerm.equals(headTerm)) {
                            valid = false;
                            break;
                        }
                    }

                    if (valid)
                        temp.add(headAtom);

                }

            if (temp.isEmpty())
                return null;

            resultLists.add(temp);

        }

        return resultLists;

    }

    private List<Atom> getSbody(Collection<Atom> bodyAtoms, Atom guard, Collection<Variable> eVariables) {

        List<Atom> results = new LinkedList<>();

        // results.add(guard);
        for (Atom atom : bodyAtoms)
            if (!atom.equals(guard) && containsY(atom, eVariables))
                results.add(atom);

        return results;

    }

    private boolean containsY(Atom atom, Collection<Variable> eVariables) {

        for (Term term : atom.getTerms())
            if (eVariables.contains(term))
                return true;

        return false;

    }

    private TGDGSat applyMGU(TGDGSat tgd, Map<Term, Term> mgu) {

        return applyMGU(tgd.getBodySet(), tgd.getHeadSet(), mgu);

    }

    private TGDGSat applyMGU(Collection<Atom> bodyAtoms, Collection<Atom> headAtoms, Map<Term, Term> mgu) {

        return new TGDGSat(applyMGU(bodyAtoms, mgu), applyMGU(headAtoms, mgu));

    }

    private Map<Term, Term> getGuardMGU(Atom guard, Atom h) {

       

        Map<Term, Term> mgu = Logic.getMGU(guard, h);

        if (mgu == null)
            return null;

        for (Entry<Term, Term> entry : mgu.entrySet()) {

            // identity on y
            if (entry.getKey().isVariable() && ((Variable) entry.getKey()).getSymbol().startsWith(eVariable))
                return null;

            // evc xθ ∩ y = ∅
            if (entry.getKey().isVariable() && ((Variable) entry.getKey()).getSymbol().startsWith(uVariable)
                    && entry.getValue().isVariable() && ((Variable) entry.getValue()).getSymbol().startsWith(eVariable))
                return null;

        }

        return mgu;

    }

}
