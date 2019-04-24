package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * GSat
 */
public class GSat {

    private static final GSat INSTANCE = new GSat();

    // New variable name for Universally Quantified Variables
    public String uVariable = "GSat_u";
    // New variable name for Existentially Quantified Variables
    public String eVariable = "GSat_e";
    // New variable name for evolveRename
    public String zVariable = "GSat_z";

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
    public Collection<TGDGSat> runGSat(Dependency[] allDependencies) {

        System.out.println("Running GSat...");
        final long startTime = System.nanoTime();

        Collection<TGDGSat> newTGDs = new HashSet<>();

        int discarded = 0;

        for (Dependency d : allDependencies)
            // should this use TGDGSat.isGuarded()?
            // Moreoever, I think the self join problem should be fixed
            // if the implementation corresponds to the pseudo code
            if (d instanceof TGD && ((TGD) d).isGuarded() && !containsSelfJoin((TGD) d)) // Adding only Guarded TGDs
                // if (!(d instanceof EGD))
                newTGDs.addAll(VNFs(HNF((TGD) d)));
            else
                discarded++;

        while (checkRenameVariablesInTGDs(newTGDs)) {
            uVariable += "0";
            eVariable += "0";
            zVariable += "0";
        }

        App.logger.info("GSat discarded rules : " + discarded + "/" + allDependencies.length + " = "
                + String.format(Locale.UK, "%.3f", (float) discarded / allDependencies.length * 100) + "%");

        App.logger.fine("# initial TGDs: " + newTGDs.size());
        newTGDs.forEach(tgd -> App.logger.fine(tgd.toString()));

        Collection<TGDGSat> nonFullTGDs = new HashSet<>();
        Collection<TGDGSat> fullTGDs = new HashSet<>();

        // for (TGD d : allDependencies)
        // if (isFull(d))
        // fullTGDs.add(VNF(d));
        // else {
        // Collection<TGD> hnf = HNF(d);
        // TGD[] dh = hnf.toArray(new TGD[hnf.size()]);
        // if (hnf.size() == 1)
        // nonFullTGDs.add(VNF(dh[0]));
        // else {
        // nonFullTGDs.add(VNF(dh[0]));
        // fullTGDs.add(VNF(dh[1]));
        // }
        // }
        //
        // App.logger.fine("# nonFullTGDs: " + nonFullTGDs.size());
        // nonFullTGDs.forEach(App.logger.fine);
        // App.logger.fine("# fullTGDs: " + fullTGDs.size());
        // fullTGDs.forEach(App.logger.fine);
        // if (!nonFullTGDs.isEmpty())
        // App.logger.fine("First non full TGD: " + nonFullTGDs.toArray()[0]);
        // if (!fullTGDs.isEmpty())
        // App.logger.fine("First full TGD: " + fullTGDs.toArray()[0]);
        //
        // newTGDs.addAll(nonFullTGDs);
        // newTGDs.addAll(fullTGDs);

        while (!newTGDs.isEmpty()) {
            // System.out.print('.');

            App.logger.fine("# new TGDs: " + newTGDs.size());
            newTGDs.forEach(tgd -> App.logger.fine(tgd.toString()));

            TGDGSat currentTGD = newTGDs.iterator().next();
            App.logger.fine("current TGD: " + currentTGD);
            newTGDs.remove(currentTGD);

            Collection<TGDGSat> tempTGDsSet = new ArrayList<>();

            if (Logic.isFull(currentTGD)) {
                fullTGDs.add(currentTGD);
                for (TGDGSat nftgd : nonFullTGDs)
                    // tempTGDsSet.addAll(VNFs(HNF(evolve(nftgd, currentTGD))));
                    tempTGDsSet.addAll(evolveNew(nftgd, currentTGD));
            } else {
                nonFullTGDs.add(currentTGD);
                for (TGDGSat ftgd : fullTGDs)
                    // tempTGDsSet.addAll(VNFs(HNF(evolve(currentTGD, ftgd))));
                    tempTGDsSet.addAll(evolveNew(currentTGD, ftgd));
            }

            for (TGDGSat d : tempTGDsSet)
                if (Logic.isFull(d) && !fullTGDs.contains(d) || !Logic.isFull(d) && !nonFullTGDs.contains(d)) {
                    App.logger.fine("adding new TGD: " + d + "\t" + d.equals(currentTGD) + ": "
                            + nonFullTGDs.contains(currentTGD) + ": " + nonFullTGDs.contains(d) + ": "
                            + Objects.equals(d, currentTGD));
                    newTGDs.add(d);
                }

        }
        // System.out.println();

        final long stopTime = System.nanoTime();

        long totalTime = stopTime - startTime;

        App.logger.info("GSat total time : " + String.format(Locale.UK, "%.0f", totalTime / 1E6) + " ms = "
                + String.format(Locale.UK, "%.2f", totalTime / 1E9) + " s");

        return fullTGDs;

    }

    /**
     * 
     * A checker for self-join
     * 
     * @param tgd a TGD
     * @return true if the TGD contains 2 atoms in the body or in the head with the
     *         same predicate name (needed only until we implement a generic MGU)
     */
    private boolean containsSelfJoin(TGD tgd) {

        Atom[] bodyAtoms = tgd.getBodyAtoms();
        for (int i = 0; i < bodyAtoms.length; i++)
            for (int j = i + 1; j < bodyAtoms.length; j++)
                if (bodyAtoms[i].getPredicate().equals(bodyAtoms[j].getPredicate()))
                    return true;

        Atom[] headAtoms = tgd.getHeadAtoms();
        for (int i = 0; i < headAtoms.length; i++)
            for (int j = i + 1; j < headAtoms.length; j++)
                if (headAtoms[i].getPredicate().equals(headAtoms[j].getPredicate()))
                    return true;

        return false;
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
                if (symbol.equals(uVariable) || symbol.equals(eVariable) || symbol.equals(zVariable))
                    return true;

        return false;

    }

    /**
     * 
     * Returns the Head Normal Form (HNF) of the input TGD
     * 
     * @param tgd an input TGD
     * @return Head Normal Form of tgd
     */
    public Collection<TGD> HNF(TGD tgd) {

        Collection<TGD> result = new HashSet<>();

        if (tgd == null)
            return result;

        Variable[] eVariables = tgd.getExistential();

        Collection<Atom> eHead = new HashSet<>();
        Collection<Atom> fHead = new HashSet<>();

        for (Atom a : tgd.getHeadAtoms())
            if (Logic.containsAny(a, eVariables))
                eHead.add(a);
            else
                fHead.add(a);

        if (eHead.isEmpty() || fHead.isEmpty())
            result.add(tgd);
        else {
            result.add(TGD.create(tgd.getBodyAtoms(), eHead.toArray(new Atom[eHead.size()])));
            result.add(TGD.create(tgd.getBodyAtoms(), fHead.toArray(new Atom[fHead.size()])));
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
    public Collection<TGDGSat> VNFs(Collection<TGD> tgds) {

        return tgds.stream().map((tgd) -> VNF(tgd)).collect(Collectors.toList());

    }

    /**
     * 
     * Returns the Variable Normal Form (VNF) of the input TGD
     * 
     * @param tgd an input TGD
     * @return Variable Normal Form of tgd
     */
    public TGDGSat VNF(TGD tgd) {

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

        TGDGSat applySubstitution = new TGDGSat((TGD) Logic.applySubstitution(tgd, substitution));
        App.logger.fine("VNF: " + tgd + "===>>>" + applySubstitution);
        return applySubstitution;

    }

    public TGD evolve(TGD nftgd, TGD ftgd) {

        ftgd = evolveRename(ftgd);

        App.logger.fine("Composing:\n" + nftgd + "\nand\n" + ftgd);

        Collection<Atom> joinAtoms = getJoinAtoms(nftgd.getHeadAtoms(), ftgd.getBodyAtoms());
        if (joinAtoms.isEmpty())
            return null;
        App.logger.fine("Join atoms:");
        joinAtoms.forEach(tgd -> App.logger.fine(tgd.toString()));

        // TGD evolveRule =
        // if (existentialVariableCheck(evolveRule, joinAtoms))
        // return evolveRule;
        return getEvolveRule(nftgd, ftgd, joinAtoms);

    }

    private TGD evolveRename(TGD ftgd) {

        Variable[] uVariables = ftgd.getUniversal();

        Map<Term, Term> substitution = new HashMap<>();
        int counter = 1;
        for (Variable v : uVariables) {
            if (!v.getSymbol().startsWith(uVariable))
                throw new IllegalArgumentException("TGD not valid in evolveRename");
            substitution.put(v, Variable.create(zVariable + counter++));
        }

        return (TGD) Logic.applySubstitution(ftgd, substitution);

    }

    private Collection<Atom> getJoinAtoms(Atom[] headAtoms, Atom[] bodyAtoms) {

        Collection<Atom> result = new HashSet<>();

        for (Atom bodyAtom : bodyAtoms)
            for (Atom headAtom : headAtoms)
                if (bodyAtom.getPredicate().equals(headAtom.getPredicate())) {
                    result.add(bodyAtom);
                    continue;
                }

        return result;

    }

    private TGD getEvolveRule(TGD nftgd, TGD ftgd, Collection<Atom> joinAtoms) {

        Collection<Atom> nftgdBodyAtoms = new HashSet<>(Arrays.asList(nftgd.getBodyAtoms()));
        Collection<Atom> nftgdHeadAtoms = new HashSet<>(Arrays.asList(nftgd.getHeadAtoms()));
        Collection<Atom> ftgdBodyAtoms = new HashSet<>(Arrays.asList(ftgd.getBodyAtoms()));
        Collection<Atom> ftgdHeadAtoms = new HashSet<>(Arrays.asList(ftgd.getHeadAtoms()));

        ftgdBodyAtoms.removeAll(joinAtoms);
        nftgdBodyAtoms.addAll(ftgdBodyAtoms);
        nftgdHeadAtoms.addAll(ftgdHeadAtoms);

        Map<Term, Term> mgu = getMGU(nftgd.getHeadAtoms(), ftgd.getBodyAtoms(), joinAtoms,
                Arrays.asList(nftgd.getExistential()));

        App.logger.fine("MGU: " + mgu);

        if (mgu != null) {
            TGD newTGD = TGD.create(applyMGU(nftgdBodyAtoms, mgu), applyMGU(nftgdHeadAtoms, mgu));
            App.logger.fine("After applying MGU: " + newTGD);
            return newTGD;
        }

        return null;

    }

    private Map<Term, Term> getMGU(Atom[] headAtoms, Atom[] bodyAtoms, Collection<Atom> joinAtoms,
            Collection<Variable> existentials) {
        // it works only if there are no duplicate atoms in the 2 arrays

        Map<Term, Term> result = new HashMap<>();

        for (Atom bodyAtom : joinAtoms)
            for (Atom headAtom : headAtoms)
                if (bodyAtom.getPredicate().equals(headAtom.getPredicate()))
                    for (int i = 0; i < bodyAtom.getPredicate().getArity(); i++) {
                        Term currentTermBody = bodyAtom.getTerm(i);
                        Term currentTermHead = headAtom.getTerm(i);
                        if (currentTermBody.isVariable() && currentTermHead.isVariable())
                            if (result.containsKey(currentTermBody)) {
                                if (!result.get(currentTermBody).equals(currentTermHead))
                                    return null;
                            } else
                                result.put(currentTermBody, currentTermHead);
                        else if (!currentTermBody.isVariable() && !currentTermHead.isVariable()) {
                            if (!currentTermBody.equals(currentTermHead)) // Clash
                                return null;
                        } else if (!currentTermBody.isVariable())// currentTermBody is the constant
                            if (existentials.contains(currentTermHead)) // Identity on y
                                return null;
                            else if (result.containsKey(currentTermHead)) {
                                if (!result.get(currentTermBody).equals(currentTermHead))
                                    return null;
                            } else
                                result.put(currentTermHead, currentTermBody);
                        else // currentTermHead is the constant
                        if (result.containsKey(currentTermBody)) {
                            if (!result.get(currentTermBody).equals(currentTermHead))
                                return null;
                        } else
                            result.put(currentTermBody, currentTermHead);

                    }

        // existential variable check (evc)
        for (Atom a : bodyAtoms)
            if (!joinAtoms.contains(a))
                for (Term t : a.getTerms())
                    if (result.containsKey(t) && existentials.contains(result.get(t)))
                        return null;

        return result;

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
    public Collection<TGDGSat> evolveNew(TGD nftgd, TGD ftgd) {

        ftgd = evolveRename(ftgd);

        App.logger.fine("Composing:\n" + nftgd + "\nand\n" + ftgd);

        Atom guard = new TGDGSat(ftgd).getGuard();
        Collection<TGDGSat> results = new HashSet<>();

        for (Atom H : nftgd.getHeadAtoms()) {

            Map<Term, Term> guardMGU = getGuardMGU(guard, H);

            if (guardMGU != null && !guardMGU.isEmpty()) {

                TGD new_nftgd = applyMGU(nftgd, guardMGU);
                TGD new_ftgd = applyMGU(ftgd, guardMGU);

                List<Atom> Sbody = getSbody(new_ftgd.getBodyAtoms(), applyMGU(Arrays.asList(guard), guardMGU)[0],
                        // maybe create a local variable for the existentials since it is used again
                        // below
                        Arrays.asList(new_nftgd.getExistential()));

                List<List<Atom>> Shead = getShead(new_nftgd.getHeadAtoms(), Sbody,
                        Arrays.asList(new_nftgd.getExistential()));

                if (Shead == null)
                    continue;

                App.logger.fine("Shead:" + Shead.toString());

                for (List<Atom> S : getProduct(Shead)) {

                    App.logger.fine("Non-Full:" + new_nftgd.toString() + "\nFull:" + new_ftgd.toString() + "\nSbody:"
                            + Sbody + "\nS:" + S);

                    Map<Term, Term> mgu = getMGU(S, Sbody);

                    Collection<Atom> new_body = new HashSet<>();
                    new_body.addAll(Arrays.asList(new_nftgd.getBodyAtoms()));
                    new_body.addAll(Arrays.asList(new_ftgd.getBodyAtoms()));
                    new_body.removeAll(Sbody);

                    Collection<Atom> new_head = new HashSet<>();
                    new_head.addAll(Arrays.asList(new_nftgd.getHeadAtoms()));
                    new_head.addAll(Arrays.asList(new_ftgd.getHeadAtoms()));

                    results.addAll(VNFs(HNF(applyMGU(TGD.create(new_body.toArray(new Atom[new_body.size()]),
                            new_head.toArray(new Atom[new_head.size()])), mgu))));

                }

            }

        }

        return results;

    }

    private Map<Term, Term> getMGU(List<Atom> s, List<Atom> sbody) {

        Map<Term, Term> result = new HashMap<>();

        int counter = 0;
        for (Atom bodyAtom : sbody)
            // this seems way too complicated and buggy; you can either consolidate the
            // fixed version of getGuardMGU
            // with this function (just call the former here for each pair) or write a
            // simpler version of this function.
            // At this point of the algorithm, we will not have to rename any existentials
            // anymore and the unification will
            // always work (as we only have to map universals to universals)
            if (s.size() > counter && bodyAtom.getPredicate().equals(s.get(counter).getPredicate())) {
                for (int i = 0; i < bodyAtom.getPredicate().getArity(); i++) {
                    Term currentTermBody = bodyAtom.getTerm(i);
                    Term currentTermHead = s.get(counter).getTerm(i);
                    if (currentTermBody.isVariable() && currentTermHead.isVariable())
                        if (result.containsKey(currentTermBody)) {
                            if (!result.get(currentTermBody).equals(currentTermHead))
                                return null;
                        } else
                            result.put(currentTermBody, currentTermHead);
                    else if (!currentTermBody.isVariable() && !currentTermHead.isVariable()) {
                        if (!currentTermBody.equals(currentTermHead)) // Clash
                            return null;
                    } else if (!currentTermBody.isVariable())// currentTermBody is the constant
                        if (result.containsKey(currentTermHead)) {
                            if (!result.get(currentTermBody).equals(currentTermHead))
                                return null;
                        } else
                            result.put(currentTermHead, currentTermBody);
                    else // currentTermHead is the constant
                    if (result.containsKey(currentTermBody)) {
                        if (!result.get(currentTermBody).equals(currentTermHead))
                            return null;
                    } else
                        result.put(currentTermBody, currentTermHead);

                }
                counter++;
            }

        return result;

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

    private List<List<Atom>> getShead(Atom[] headAtoms, Collection<Atom> sbody, List<Variable> eVariables) {

        List<List<Atom>> resultLists = new ArrayList<List<Atom>>();

        for (Atom bodyAtom : sbody) {

            List<Atom> temp = new ArrayList<>();

            for (Atom headAtom : headAtoms)
                if (headAtom.getPredicate().equals(bodyAtom.getPredicate())) {
                    boolean valid = true;

                    Term[] headTerms = headAtom.getTerms();
                    for (int i = 0; i < headTerms.length; i++) {
                        Term headTerm = headTerms[i];
                        // Term bodyTerm = bodyAtom.getTerm(i)
                        if (eVariables.contains(headTerm) && !bodyAtom.getTerm(i).equals(headTerm)) {
                            // wrong condition; should be:
                            // (eVariables.contains(headTerm) || eVariables.contains(bodyTerm)) &&
                            // !bodyTerm.equals(headTerm)
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

    private List<Atom> getSbody(Atom[] bodyAtoms, Atom guard, List<Variable> eVariables) {

        List<Atom> results = new LinkedList<>();

        results.add(guard);
        for (Atom atom : bodyAtoms)
            // why not Logic.containsAny(atom, eVariables) ?
            if (!atom.equals(guard) && containsY(atom, eVariables))
                results.add(atom);

        return results;

    }

    private boolean containsY(Atom atom, List<Variable> eVariables) {

        for (Term term : atom.getTerms())
            if (eVariables.contains(term))
                return true;

        return false;

    }

    private TGD applyMGU(TGD tgd, Map<Term, Term> mgu) {

        return TGD.create(applyMGU(Arrays.asList(tgd.getBodyAtoms()), mgu),
                applyMGU(Arrays.asList(tgd.getHeadAtoms()), mgu));

    }

    private Map<Term, Term> getGuardMGU(Atom guard, Atom h) {

        if (!guard.getPredicate().equals(h.getPredicate()))
            return null;

        Map<Term, Term> result = new HashMap<>();

        Term[] guardTerms = guard.getTerms();
        for (int i = 0; i < guardTerms.length; i++) {

            Term guardTerm = guardTerms[i];
            Term headTerm = h.getTerm(i);

            if (result.containsKey(guardTerm)) {
                if (result.get(guardTerm) != headTerm)
                    return null;
            } else
                result.put(guardTerm, headTerm);

        }

        return result;

    }

    // some test cases for the MGU computation
    public static void main(String[] args) {
        // getGuardMGU failures:
        Variable x1 = Variable.create("x1");
        Variable x2 = Variable.create("x2");
        Variable y = Variable.create("y");
        Variable z1 = Variable.create("z1");
        Variable z2 = Variable.create("z2");
        GSat gsat = GSat.getInstance();
        Atom Rx = Atom.create(Predicate.create("R", 3), x1, x2, y);
        Atom Rz = Atom.create(Predicate.create("R", 3), z1, z1, z2);
        Map<Term, Term> expected = new HashMap<>();
        expected.put(z1, x2);
        expected.put(z2, y);
        expected.put(x1, x2);
        // should be true, but the result is actually null
        System.out.println(expected.equals(gsat.getGuardMGU(Rz, Rx)));

        // Another one:
        Variable z3 = Variable.create("z2");
        Rx = Atom.create(Predicate.create("R", 4), x1, x1, x2, y);
        Rz = Atom.create(Predicate.create("R", 4), z1, z2, z1, z3);
        expected = new HashMap<>();
        expected.put(z1, x2);
        expected.put(z2, x2);
        expected.put(z3, y);
        expected.put(x1, x2);
        System.out.println(expected.equals(gsat.getGuardMGU(Rz, Rx)));

        // getMGU also fails:
        System.out.println(expected.equals(gsat.getMGU(Arrays.asList(Rx), Arrays.asList(Rz))));
    }

}