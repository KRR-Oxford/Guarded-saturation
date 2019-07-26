package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Map.Entry;
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
    private boolean DEBUG_MODE = Configuration.isDebugMode();

    // New variable name for Universally Quantified Variables
    public String uVariable = "GSat_u";
    // New variable name for Existentially Quantified Variables
    public String eVariable = "GSat_e";
    // New variable name for evolveRename
    public String zVariable = "GSat_z";

    private static Comparator<? super TGDGSat> comparator = (tgd1, tgd2) -> {

        int numberOfHeadAtoms1 = tgd1.getNumberOfHeadAtoms();
        int numberOfHeadAtoms2 = tgd2.getNumberOfHeadAtoms();
        if (numberOfHeadAtoms1 != numberOfHeadAtoms2)
            return numberOfHeadAtoms2 - numberOfHeadAtoms1;

        int numberOfBodyAtoms1 = tgd1.getNumberOfBodyAtoms();
        int numberOfBodyAtoms2 = tgd2.getNumberOfBodyAtoms();
        if (numberOfBodyAtoms1 != numberOfBodyAtoms2)
            return numberOfBodyAtoms1 - numberOfBodyAtoms2;

        if (tgd1.equals(tgd2))
            return 0;

        int compareTo = tgd1.toString().compareTo(tgd2.toString());
        if (compareTo != 0)
            return compareTo;
        throw new RuntimeException();

    };

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
                selectedTGDs.add(new TGDGSat(Set.of(d.getBodyAtoms()), Set.of(d.getHeadAtoms())));
            else
                discarded++;

        App.logger.info("GSat discarded rules : " + discarded + "/" + allDependencies.size() + " = "
                + String.format(Locale.UK, "%.3f", (float) discarded / allDependencies.size() * 100) + "%");

        if (DEBUG_MODE)
            System.out.println("GSat discarded rules : " + discarded + "/" + allDependencies.size() + " = "
                    + String.format(Locale.UK, "%.3f", (float) discarded / allDependencies.size() * 100) + "%");

        while (checkRenameVariablesInTGDs(selectedTGDs)) {
            uVariable += "0";
            eVariable += "0";
            zVariable += "0";
        }

        Collection<TGDGSat> newFullTGDs = new HashSet<>();
        Collection<TGDGSat> newNonFullTGDs = new HashSet<>();

        if (Configuration.getOptimizationValue() >= 5) {

            newFullTGDs = new Stack<>();
            newNonFullTGDs = new Stack<>();

        } else if (Configuration.getOptimizationValue() >= 2) {

            newFullTGDs = new TreeSet<>(comparator);
            newNonFullTGDs = new TreeSet<>(comparator);

        }

        Map<Predicate, Set<TGDGSat>> nonFullTGDsMap = new HashMap<>();
        Map<Predicate, Set<TGDGSat>> fullTGDsMap = new HashMap<>();
        Set<TGDGSat> fullTGDsSet = new HashSet<>();
        Set<TGDGSat> nonFullTGDsSet = new HashSet<>();

        for (TGDGSat tgd : selectedTGDs)
            for (TGDGSat currentTGD : VNFs(HNF(tgd)))
                if (Logic.isFull(currentTGD))
                    addFullTGD(currentTGD, fullTGDsMap, fullTGDsSet);
                else
                    newNonFullTGDs.add(currentTGD);

        App.logger.fine("# initial TGDs: " + fullTGDsSet.size() + " , " + newNonFullTGDs.size());
        // newTGDs.forEach(tgd -> App.logger.fine(tgd.toString()));

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

        int counter = 100;
        while (!newFullTGDs.isEmpty() || !newNonFullTGDs.isEmpty()) {
            // System.out.print('.');

            App.logger.fine("# new TGDs: " + newFullTGDs.size() + " , " + newNonFullTGDs.size());
            // newTGDs.forEach(tgd -> App.logger.fine(tgd.toString()));

            if (DEBUG_MODE)
                if (counter % 100 == 0) {
                    counter = 1;
                    System.out.println("nonFullTGDs\t" + nonFullTGDsSet.size() + "\t\tfullTGDs\t" + fullTGDsSet.size()
                            + "\t\t\tnewNonFullTGDs\t" + newNonFullTGDs.size() + "\t\tnewFullTGDs\t"
                            + newFullTGDs.size());
                    // System.out.println(fullTGDs.keySet());
                    // System.out.println(newNonFullTGDs.iterator().next());
                } else
                    counter++;

            Collection<TGDGSat> toAdd = new ArrayList<>();

            if (!newNonFullTGDs.isEmpty()) {

                Iterator<TGDGSat> iterator = newNonFullTGDs.iterator();
                TGDGSat currentTGD = iterator.next();
                iterator.remove();
                App.logger.fine("current TGD: " + currentTGD);

                boolean added = addNonFullTGD(currentTGD, nonFullTGDsMap, nonFullTGDsSet);
                if (added)
                    for (TGDGSat ftgd : getFullTGDsToEvolve(fullTGDsMap, currentTGD))
                        if (Configuration.getOptimizationValue() >= 3) {
                            boolean subsumed = false;
                            for (TGDGSat newTGD : evolveNew(currentTGD, ftgd)) {
                                if (!currentTGD.equals(newTGD)) {
                                    toAdd.add(newTGD);
                                    subsumed = subsumed || subsumed(currentTGD, newTGD);
                                }
                                // subsumed = subsumed || subsumed(currentTGD, newTGD) && !subsumed(newTGD,
                                // currentTGD);
                            }
                            if (subsumed)
                                break;
                        } else
                            for (TGDGSat newTGD : evolveNew(currentTGD, ftgd))
                                toAdd.add(newTGD);

            } else {

                Iterator<TGDGSat> iterator = newFullTGDs.iterator();
                TGDGSat currentTGD = iterator.next();
                iterator.remove();
                App.logger.fine("current TGD: " + currentTGD);

                boolean added = addFullTGD(currentTGD, fullTGDsMap, fullTGDsSet);
                Set<TGDGSat> set = nonFullTGDsMap.get(currentTGD.getGuard().getPredicate());
                if (added && set != null)
                    for (TGDGSat nftgd : set)
                        if (Configuration.getOptimizationValue() >= 3) {
                            boolean subsumed = false;
                            for (TGDGSat newTGD : evolveNew(nftgd, currentTGD)) {
                                if (!currentTGD.equals(newTGD)) {
                                    toAdd.add(newTGD);
                                    subsumed = subsumed || subsumed(currentTGD, newTGD);
                                }
                                // subsumed = subsumed || subsumed(currentTGD, newTGD) && !subsumed(newTGD,
                                // currentTGD);
                            }
                            if (subsumed)
                                break;
                        } else
                            for (TGDGSat newTGD : evolveNew(nftgd, currentTGD))
                                toAdd.add(newTGD);

            }

            for (TGDGSat newTGD : toAdd)
                addNewTGD(newTGD, newFullTGDs, newNonFullTGDs, fullTGDsSet, nonFullTGDsSet, fullTGDsMap,
                        nonFullTGDsMap);

        }

        // System.out.println();

        final long stopTime = System.nanoTime();

        long totalTime = stopTime - startTime;

        App.logger.info("GSat total time : " + String.format(Locale.UK, "%.0f", totalTime / 1E6) + " ms = "
                + String.format(Locale.UK, "%.2f", totalTime / 1E9) + " s");

        return fullTGDsSet;

    }

    private Set<TGDGSat> getFullTGDsToEvolve(Map<Predicate, Set<TGDGSat>> fullTGDsMap, TGDGSat currentTGD) {

        Set<TGDGSat> result = new HashSet<>();

        if (Configuration.getOptimizationValue() >= 4)
            result = new TreeSet<>(comparator);

        for (Atom atom : currentTGD.getHeadSet()) {
            Set<TGDGSat> set = fullTGDsMap.get(atom.getPredicate());
            if (set != null)
                result.addAll(set);
        }

        // if (DEBUG_MODE)
        // System.out.println("toEvolve \t" + toEvolve.size() + "\t\tfullTGDs \t" +
        // fullTGDsSet.size());

        return result;

    }

    private boolean addFullTGD(TGDGSat currentTGD, Map<Predicate, Set<TGDGSat>> fullTGDsMap, Set<TGDGSat> fullTGDsSet) {

        fullTGDsMap.computeIfAbsent(currentTGD.getGuard().getPredicate(), k -> new HashSet<TGDGSat>()).add(currentTGD);

        return fullTGDsSet.add(currentTGD);

    }

    private boolean addNonFullTGD(TGDGSat currentTGD, Map<Predicate, Set<TGDGSat>> nonFullTGDsMap,
            Set<TGDGSat> nonFullTGDsSet) {

        if (Configuration.getOptimizationValue() >= 4)
            for (Atom atom : currentTGD.getHeadSet())
                nonFullTGDsMap.computeIfAbsent(atom.getPredicate(), k -> new TreeSet<TGDGSat>(comparator))
                        .add(currentTGD);
        else
            for (Atom atom : currentTGD.getHeadSet())
                nonFullTGDsMap.computeIfAbsent(atom.getPredicate(), k -> new HashSet<TGDGSat>()).add(currentTGD);

        return nonFullTGDsSet.add(currentTGD);

    }

    private void addNewTGD(TGDGSat newTGD, Collection<TGDGSat> newFullTGDs, Collection<TGDGSat> newNonFullTGDs,
            Set<TGDGSat> fullTGDsSet, Set<TGDGSat> nonFullTGDsSet, Map<Predicate, Set<TGDGSat>> fullTGDsMap,
            Map<Predicate, Set<TGDGSat>> nonFullTGDsMap) {

        final Collection<TGDGSat> newTGDs;
        final Set<TGDGSat> TGDsSet;
        final Map<Predicate, Set<TGDGSat>> TGDsMap;
        if (Logic.isFull(newTGD)) {
            newTGDs = newFullTGDs;
            TGDsSet = fullTGDsSet;
            TGDsMap = fullTGDsMap;
        } else {
            newTGDs = newNonFullTGDs;
            TGDsSet = nonFullTGDsSet;
            TGDsMap = nonFullTGDsMap;
        }

        if (Configuration.getOptimizationValue() >= 1) {

            if (subsumed(newTGD, newTGDs) || subsumed(newTGD, TGDsSet))
                return;

            Collection<TGDGSat> subsumed = subsumesAny(newTGD, newTGDs);
            newTGDs.removeAll(subsumed);

            // System.out.println("newTGD\t" + newTGD);
            // System.out.println("TGDsSet\t" + TGDsSet);
            // System.out.println("TGDsMap\t" + TGDsMap);

            subsumed = subsumesAny(newTGD, TGDsSet);
            TGDsSet.removeAll(subsumed);

            if (Logic.isFull(newTGD)) {
                // for (TGDGSat tgd : subsumed)
                // if (TGDsMap.get(tgd.getGuard().getPredicate()) == null)
                // System.out.println("NULL: " + tgd);

                subsumed.forEach(tgd -> TGDsMap.get(tgd.getGuard().getPredicate()).remove(tgd));
            } else
                for (TGDGSat tgd : subsumed)
                    for (Atom atom : tgd.getHeadSet())
                        TGDsMap.get(atom.getPredicate()).remove(tgd);

            TGDsMap.values().removeIf(v -> v.isEmpty());

        }

        if (Configuration.getOptimizationValue() >= 5 && newTGDs.contains(newTGD))
            return;

        newTGDs.add(newTGD);

    }

    private Collection<TGDGSat> subsumesAny(TGDGSat newTGD, Collection<TGDGSat> TGDs) {

        Collection<TGDGSat> subsumed = new HashSet<>();

        var bodyN = newTGD.getBodySet();
        var headN = newTGD.getHeadSet();

        for (TGDGSat tgd : TGDs) {
            var body = tgd.getBodySet();
            var head = tgd.getHeadSet();

            if (body.size() < bodyN.size() || headN.size() < head.size())
                continue;

            if (body.containsAll(bodyN) && headN.containsAll(head))
                subsumed.add(tgd);
        }

        return subsumed;

    }

    private boolean subsumed(TGDGSat newTGD, Collection<TGDGSat> TGDs) {

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

    private boolean subsumed(TGDGSat tgd1, TGDGSat tgd2) {

        var body1 = tgd1.getBodySet();
        var headN = tgd1.getHeadSet();

        var body = tgd2.getBodySet();
        var head = tgd2.getHeadSet();

        if (body1.size() < body.size() || head.size() < headN.size())
            return false;

        if (body1.containsAll(body) && head.containsAll(headN))
            return true;

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
    // private boolean containsSelfJoin(TGD tgd) {

    // Atom[] bodyAtoms = tgd.getBodyAtoms();
    // for (int i = 0; i < bodyAtoms.length; i++)
    // for (int j = i + 1; j < bodyAtoms.length; j++)
    // if (bodyAtoms[i].getPredicate().equals(bodyAtoms[j].getPredicate()))
    // return true;

    // Atom[] headAtoms = tgd.getHeadAtoms();
    // for (int i = 0; i < headAtoms.length; i++)
    // for (int j = i + 1; j < headAtoms.length; j++)
    // if (headAtoms[i].getPredicate().equals(headAtoms[j].getPredicate()))
    // return true;

    // return false;
    // }

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

        if (tgd == null)
            return new HashSet<>();

        Variable[] eVariables = tgd.getExistential();

        Set<Atom> eHead = new HashSet<>();
        Set<Atom> fHead = new HashSet<>();

        Set<Atom> bodyAtoms = tgd.getBodySet();
        for (Atom a : tgd.getHeadAtoms())
            if (a.equals(TGDGSat.Bottom))
                // remove all head atoms since ⊥ & S ≡ ⊥ for any conjunction S
                return Set.of(new TGDGSat(bodyAtoms, Set.of(TGDGSat.Bottom)));
            else if (Logic.containsAny(a, eVariables))
                eHead.add(a);
            else if (!bodyAtoms.contains(a))
                // Do not add atoms that already appear in the body.
                // This is only needed for fHead since we have no existentials in the body
                fHead.add(a);

        if (tgd.getHeadAtoms().length == eHead.size() || tgd.getHeadAtoms().length == fHead.size())
            return Set.of(tgd);

        Collection<TGDGSat> result = new HashSet<>();
        if (!eHead.isEmpty())
            result.add(new TGDGSat(bodyAtoms, eHead));
        if (!fHead.isEmpty())
            result.add(new TGDGSat(bodyAtoms, fHead));
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

    // public TGD evolve(TGD nftgd, TGD ftgd) {

    // ftgd = evolveRename(ftgd);

    // App.logger.fine("Composing:\n" + nftgd + "\nand\n" + ftgd);

    // Collection<Atom> joinAtoms = getJoinAtoms(nftgd.getHeadAtoms(),
    // ftgd.getBodyAtoms());
    // if (joinAtoms.isEmpty())
    // return null;
    // App.logger.fine("Join atoms:");
    // joinAtoms.forEach(tgd -> App.logger.fine(tgd.toString()));

    // // TGD evolveRule =
    // // if (existentialVariableCheck(evolveRule, joinAtoms))
    // // return evolveRule;
    // return getEvolveRule(nftgd, ftgd, joinAtoms);

    // }

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

    // private Collection<Atom> getJoinAtoms(Atom[] headAtoms, Atom[] bodyAtoms) {

    // Collection<Atom> result = new HashSet<>();

    // for (Atom bodyAtom : bodyAtoms)
    // for (Atom headAtom : headAtoms)
    // if (bodyAtom.getPredicate().equals(headAtom.getPredicate())) {
    // result.add(bodyAtom);
    // continue;
    // }

    // return result;

    // }

    // private TGD getEvolveRule(TGD nftgd, TGD ftgd, Collection<Atom> joinAtoms) {

    // Collection<Atom> nftgdBodyAtoms = new
    // HashSet<>(Arrays.asList(nftgd.getBodyAtoms()));
    // Collection<Atom> nftgdHeadAtoms = new
    // HashSet<>(Arrays.asList(nftgd.getHeadAtoms()));
    // Collection<Atom> ftgdBodyAtoms = new
    // HashSet<>(Arrays.asList(ftgd.getBodyAtoms()));
    // Collection<Atom> ftgdHeadAtoms = new
    // HashSet<>(Arrays.asList(ftgd.getHeadAtoms()));

    // ftgdBodyAtoms.removeAll(joinAtoms);
    // nftgdBodyAtoms.addAll(ftgdBodyAtoms);
    // nftgdHeadAtoms.addAll(ftgdHeadAtoms);

    // Map<Term, Term> mgu = getMGU(nftgd.getHeadAtoms(), ftgd.getBodyAtoms(),
    // joinAtoms,
    // Arrays.asList(nftgd.getExistential()));

    // App.logger.fine("MGU: " + mgu);

    // if (mgu != null) {
    // TGD newTGD = TGD.create(applyMGU(nftgdBodyAtoms, mgu),
    // applyMGU(nftgdHeadAtoms, mgu));
    // App.logger.fine("After applying MGU: " + newTGD);
    // return newTGD;
    // }

    // return null;

    // }

    // private Map<Term, Term> getMGU(Atom[] headAtoms, Atom[] bodyAtoms,
    // Collection<Atom> joinAtoms,
    // Collection<Variable> existentials) {
    // // it works only if there are no duplicate atoms in the 2 arrays

    // Map<Term, Term> result = new HashMap<>();

    // for (Atom bodyAtom : joinAtoms)
    // for (Atom headAtom : headAtoms)
    // if (bodyAtom.getPredicate().equals(headAtom.getPredicate()))
    // for (int i = 0; i < bodyAtom.getPredicate().getArity(); i++) {
    // Term currentTermBody = bodyAtom.getTerm(i);
    // Term currentTermHead = headAtom.getTerm(i);
    // if (currentTermBody.isVariable() && currentTermHead.isVariable())
    // if (result.containsKey(currentTermBody)) {
    // if (!result.get(currentTermBody).equals(currentTermHead))
    // return null;
    // } else
    // result.put(currentTermBody, currentTermHead);
    // else if (!currentTermBody.isVariable() && !currentTermHead.isVariable()) {
    // if (!currentTermBody.equals(currentTermHead)) // Clash
    // return null;
    // } else if (!currentTermBody.isVariable())// currentTermBody is the constant
    // if (existentials.contains(currentTermHead)) // Identity on y
    // return null;
    // else if (result.containsKey(currentTermHead)) {
    // if (!result.get(currentTermBody).equals(currentTermHead))
    // return null;
    // } else
    // result.put(currentTermHead, currentTermBody);
    // else // currentTermHead is the constant
    // if (result.containsKey(currentTermBody)) {
    // if (!result.get(currentTermBody).equals(currentTermHead))
    // return null;
    // } else
    // result.put(currentTermBody, currentTermHead);

    // }

    // // existential variable check (evc)
    // for (Atom a : bodyAtoms)
    // if (!joinAtoms.contains(a))
    // for (Term t : a.getTerms())
    // if (result.containsKey(t) && existentials.contains(result.get(t)))
    // return null;

    // return result;

    // }

    private Set<Atom> applyMGU(Collection<Atom> atoms, Map<Term, Term> mgu) {

        return atoms.stream().map(atom -> (Atom) Logic.applySubstitution(atom, mgu)).collect(Collectors.toSet());

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

        Atom guard = new TGDGSat(ftgd.getBodySet(), ftgd.getHeadSet()).getGuard();
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

                Set<Atom> new_body = new HashSet<>(new_ftgd_body_atoms);
                Atom new_guard = (Atom) Logic.applySubstitution(guard, guardMGU);
                new_body.remove(new_guard);
                List<Atom> Sbody = getSbody(new_body, new_nftgd_existentials);
                new_body.addAll(new_nftgd_body_atoms);
                Set<Atom> new_head = new HashSet<>(new_nftgd_head_atoms);
                new_head.addAll(new_ftgd_head_atoms);

                List<List<Atom>> Shead = getShead(new_nftgd_head_atoms, Sbody, new_nftgd_existentials);

                // if Sbody is empty, then Shead is empty, and we take this short-cut;
                // in fact, we should never have Shead == null and Sbody.isEmpty
                if (Shead == null || Shead.isEmpty()) {
                    if (Sbody.isEmpty())
                        results.addAll(VNFs(HNF(new TGDGSat(new_body, new_head))));
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

                    new_body.removeAll(Sbody);

                    if (mgu.isEmpty())
                        // no need to apply the MGU
                        results.addAll(VNFs(HNF(new TGDGSat(new_body, new_head))));
                    else
                        results.addAll(VNFs(HNF(applyMGU(new_body, new_head, mgu))));

                }

            }

        }

        return results;

    }

    // private Map<Term, Term> getMGU(List<Atom> s, List<Atom> sbody) {

    // Map<Term, Term> result = new HashMap<>();

    // int counter = 0;
    // for (Atom bodyAtom : sbody)
    // if (s.size() > counter &&
    // bodyAtom.getPredicate().equals(s.get(counter).getPredicate())) {
    // for (int i = 0; i < bodyAtom.getPredicate().getArity(); i++) {
    // Term currentTermBody = bodyAtom.getTerm(i);
    // Term currentTermHead = s.get(counter).getTerm(i);
    // if (currentTermBody.isVariable() && currentTermHead.isVariable())
    // if (result.containsKey(currentTermBody)) {
    // if (!result.get(currentTermBody).equals(currentTermHead))
    // return null;
    // } else
    // result.put(currentTermBody, currentTermHead);
    // else if (!currentTermBody.isVariable() && !currentTermHead.isVariable()) {
    // if (!currentTermBody.equals(currentTermHead)) // Clash
    // return null;
    // } else if (!currentTermBody.isVariable())// currentTermBody is the constant
    // if (result.containsKey(currentTermHead)) {
    // if (!result.get(currentTermBody).equals(currentTermHead))
    // return null;
    // } else
    // result.put(currentTermHead, currentTermBody);
    // else // currentTermHead is the constant
    // if (result.containsKey(currentTermBody)) {
    // if (!result.get(currentTermBody).equals(currentTermHead))
    // return null;
    // } else
    // result.put(currentTermBody, currentTermHead);

    // }
    // counter++;
    // }

    // return result;

    // }

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
                    ArrayList<Atom> resultList = new ArrayList<Atom>(1 + remainingList.size());
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
                        if (!bodyTerm.equals(headTerm) && (headTerm.isUntypedConstant() && bodyTerm.isUntypedConstant()
                                || eVariables.contains(headTerm) || eVariables.contains(bodyTerm))) {
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

    // private List<Atom> getSbody(Collection<Atom> bodyAtoms, Atom guard,
    // Collection<Variable> eVariables) {

    // List<Atom> results = new LinkedList<>();

    // // results.add(guard);
    // for (Atom atom : bodyAtoms)
    // if (!atom.equals(guard) && containsY(atom, eVariables))
    // results.add(atom);

    // return results;

    // }

    private List<Atom> getSbody(Collection<Atom> new_bodyAtoms, Collection<Variable> eVariables) {

        List<Atom> results = new LinkedList<>();

        // results.add(guard);
        for (Atom atom : new_bodyAtoms)
            if (containsY(atom, eVariables))// FIXME try one-line
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

    private TGDGSat applyMGU(Set<Atom> bodyAtoms, Set<Atom> headAtoms, Map<Term, Term> mgu) {

        return new TGDGSat(applyMGU(bodyAtoms, mgu), applyMGU(headAtoms, mgu));

    }

    private Map<Term, Term> getGuardMGU(Atom guard, Atom h) {

        // if (!guard.getPredicate().equals(h.getPredicate()))
        // return null;

        // Term[] guardTerms = guard.getTerms();
        // for (int i = 0; i < guardTerms.length; i++) {

        // Term guardTerm = guardTerms[i];
        // Term headTerm = h.getTerm(i);

        // if (result.containsKey(guardTerm)) {
        // if (!result.get(guardTerm).equals(headTerm))
        // return null;
        // } else
        // result.put(guardTerm, headTerm);

        // }

        Map<Term, Term> mgu = Logic.getMGU(guard, h);

        if (mgu == null)
            return null;

        for (Entry<Term, Term> entry : mgu.entrySet()) {

            Term key = entry.getKey();
            boolean isVariable = key.isVariable();
            if (isVariable) {
                String symbol = ((Variable) key).getSymbol();

                // identity on y
                if (symbol.startsWith(eVariable))
                    return null;

                // evc xθ ∩ y = ∅
                Term value = entry.getValue();
                if (value.isVariable() && symbol.startsWith(uVariable)
                        && ((Variable) value).getSymbol().startsWith(eVariable))
                    return null;

            }

        }

        return mgu;

    }

}
