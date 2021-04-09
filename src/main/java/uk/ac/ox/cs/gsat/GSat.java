package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.stream.Collectors;

import uk.ac.ox.cs.gsat.filters.FormulaFilter;
import uk.ac.ox.cs.gsat.filters.IdentityFormulaFilter;
import uk.ac.ox.cs.gsat.filters.MinAtomFilter;
import uk.ac.ox.cs.gsat.filters.MinPredicateFilter;
import uk.ac.ox.cs.gsat.filters.TreePredicateFilter;
import uk.ac.ox.cs.gsat.subsumers.ExactAtomSubsumer;
import uk.ac.ox.cs.gsat.subsumers.SimpleSubsumer;
import uk.ac.ox.cs.gsat.subsumers.Subsumer;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.Predicate;
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

    private static Comparator<? super GTGD> comparator = (tgd1, tgd2) -> {

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
    public Collection<GTGD> runGSat(Collection<Dependency> allDependencies) {

        System.out.println("Running GSat...");
        final long startTime = System.nanoTime();

        int discarded = 0;

        Collection<GTGD> selectedTGDs = new HashSet<>();
        for (Dependency d : allDependencies)
            if (isSupportedRule(d))
                selectedTGDs.add(new GTGD(Set.of(d.getBodyAtoms()), Set.of(d.getHeadAtoms())));
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

        Collection<GTGD> newFullTGDs = new HashSet<>();
        Collection<GTGD> newNonFullTGDs = new HashSet<>();

        if (Configuration.getOptimizationValue() >= 5) {

            newFullTGDs = new Stack<>();
            newNonFullTGDs = new Stack<>();

        } else if (Configuration.getOptimizationValue() >= 2) {

            newFullTGDs = new TreeSet<>(comparator);
            newNonFullTGDs = new TreeSet<>(comparator);

        }

        Map<Predicate, Set<GTGD>> nonFullTGDsMap = new HashMap<>();
        Map<Predicate, Set<GTGD>> fullTGDsMap = new HashMap<>();
        Set<GTGD> fullTGDsSet = new HashSet<>();
        Set<GTGD> nonFullTGDsSet = new HashSet<>();

        App.logger.info("Subsumption method : " + Configuration.getSubsumptionMethod());
        Subsumer<GTGD> fullTGDsSubsumer = createSubsumer();
        Subsumer<GTGD> nonFullTGDsSubsumer = createSubsumer();

        for (GTGD tgd : selectedTGDs)
            for (GTGD hnf : tgd.computeHNF()) {
				GTGD currentGTGD = hnf.computeVNF(eVariable, uVariable);
                if (Logic.isFull(currentGTGD)) {
                    addFullTGD(currentGTGD, fullTGDsMap, fullTGDsSet);
                    fullTGDsSubsumer.add(currentGTGD);
                } else {
                    nonFullTGDsSubsumer.add(currentGTGD);
                    newNonFullTGDs.add(currentGTGD);
                }
            }

        // copying the input full TGD set of comparison
        Collection<GTGD> inputFullTGDs = new HashSet<>(fullTGDsSet);

        App.logger.info("# initial TGDs: " + fullTGDsSet.size() + " , " + newNonFullTGDs.size());
        int counter = 100;
        int newFullCount = 0;
        int newNonFullCount = 0;
        while (!newFullTGDs.isEmpty() || !newNonFullTGDs.isEmpty()) {

            App.logger.fine("# new TGDs: " + newFullTGDs.size() + " , " + newNonFullTGDs.size());
            newFullCount += newFullTGDs.size();
            newNonFullCount += newNonFullTGDs.size();

            if (DEBUG_MODE)
                if (counter == 100) {
                    counter = 1;
                    System.out.println("nonFullTGDs\t" + nonFullTGDsSet.size() + "\t\tfullTGDs\t" + fullTGDsSet.size()
                            + "\t\t\tnewNonFullTGDs\t" + newNonFullTGDs.size() + "\t\tnewFullTGDs\t"
                            + newFullTGDs.size());
                } else
                    counter++;

            Collection<GTGD> toAdd = new ArrayList<>();

            if (!newNonFullTGDs.isEmpty()) {

                Iterator<GTGD> iterator = newNonFullTGDs.iterator();
                GTGD currentTGD = iterator.next();
                // System.out.println("currentTGD " + currentTGD);
                iterator.remove();
                App.logger.fine("current TGD: " + currentTGD);

                boolean added = addNonFullTGD(currentTGD, nonFullTGDsMap, nonFullTGDsSet);
                if (added)
                    for (GTGD ftgd : getFullTGDsToEvolve(fullTGDsMap, currentTGD))
                        if (Configuration.getOptimizationValue() >= 3) {
                            boolean subsumed = false;
                            for (GTGD newTGD : evolveNew(currentTGD, ftgd)) {
                                if (!currentTGD.equals(newTGD)) {
                                    toAdd.add(newTGD);
                                    subsumed = subsumed || subsumed(currentTGD, newTGD);
                                }
                            }
                            if (subsumed)
                                break;
                        } else
                            for (GTGD newTGD : evolveNew(currentTGD, ftgd))
                                toAdd.add(newTGD);

            } else {

                Iterator<GTGD> iterator = newFullTGDs.iterator();
                GTGD currentTGD = iterator.next();
                iterator.remove();
                App.logger.fine("current TGD: " + currentTGD);

                boolean added = addFullTGD(currentTGD, fullTGDsMap, fullTGDsSet);
                Set<GTGD> set = nonFullTGDsMap.get(currentTGD.getGuard().getPredicate());
                if (added && set != null)
                    for (GTGD nftgd : set)
                        if (Configuration.getOptimizationValue() >= 3) {
                            boolean subsumed = false;
                            for (GTGD newTGD : evolveNew(nftgd, currentTGD)) {
                                if (!currentTGD.equals(newTGD)) {
                                    toAdd.add(newTGD);
                                    subsumed = subsumed || subsumed(currentTGD, newTGD);
                                }
                            }
                            if (subsumed)
                                break;
                        } else
                            for (GTGD newTGD : evolveNew(nftgd, currentTGD))
                                toAdd.add(newTGD);

            }
            for (GTGD newTGD : toAdd) {
                addNewTGD(newTGD, newFullTGDs, newNonFullTGDs, fullTGDsSubsumer, nonFullTGDsSubsumer, fullTGDsMap,
                        nonFullTGDsMap, fullTGDsSet, nonFullTGDsSet);

            }

        }

        final long stopTime = System.nanoTime();

        long totalTime = stopTime - startTime;

        App.logger.info("GSat total time : " + String.format(Locale.UK, "%.0f", totalTime / 1E6) + " ms = "
                + String.format(Locale.UK, "%.2f", totalTime / 1E9) + " s");
        App.logger.info("Subsumed elements : "
                + (fullTGDsSubsumer.getNumberSubsumed() + nonFullTGDsSubsumer.getNumberSubsumed()));
        App.logger.info("Filter discarded elements : "
                + (fullTGDsSubsumer.getFilterDiscarded() + nonFullTGDsSubsumer.getFilterDiscarded()));
        App.logger.info("Derived full/non full TGDs: "+ newFullCount + " , " + newNonFullCount);

        Collection<GTGD> output = fullTGDsSubsumer.getAll();

        Collection<GTGD> outputCopy = new ArrayList<>(output);
        outputCopy.removeAll(inputFullTGDs);
        App.logger.info("ouptput full TGDs not contained in the input: " + outputCopy.size());

        return output;
    }

    private Set<GTGD> getFullTGDsToEvolve(Map<Predicate, Set<GTGD>> fullTGDsMap, GTGD currentTGD) {

        Set<GTGD> result = new HashSet<>();

        if (Configuration.getOptimizationValue() >= 4)
            result = new TreeSet<>(comparator);

        for (Atom atom : currentTGD.getHeadSet()) {
            Set<GTGD> set = fullTGDsMap.get(atom.getPredicate());
            if (set != null)
                result.addAll(set);
        }

        // if (DEBUG_MODE)
        // System.out.println("toEvolve \t" + toEvolve.size() + "\t\tfullTGDs \t" +
        // fullTGDsSet.size());

        return result;

    }

    private boolean addFullTGD(GTGD currentTGD, Map<Predicate, Set<GTGD>> fullTGDsMap, Set<GTGD> fullTGDsSet) {

        fullTGDsMap.computeIfAbsent(currentTGD.getGuard().getPredicate(), k -> new HashSet<GTGD>()).add(currentTGD);

        return fullTGDsSet.add(currentTGD);

    }

    private boolean addNonFullTGD(GTGD currentTGD, Map<Predicate, Set<GTGD>> nonFullTGDsMap,
            Set<GTGD> nonFullTGDsSet) {

        if (Configuration.getOptimizationValue() >= 4)
            for (Atom atom : currentTGD.getHeadSet())
                nonFullTGDsMap.computeIfAbsent(atom.getPredicate(), k -> new TreeSet<GTGD>(comparator))
                        .add(currentTGD);
        else
            for (Atom atom : currentTGD.getHeadSet())
                nonFullTGDsMap.computeIfAbsent(atom.getPredicate(), k -> new HashSet<GTGD>()).add(currentTGD);

        return nonFullTGDsSet.add(currentTGD);

    }

    private void addNewTGD(GTGD newTGD, Collection<GTGD> newFullTGDs, Collection<GTGD> newNonFullTGDs,
            Subsumer<GTGD> fullTGDsSubsumer, Subsumer<GTGD> nonFullTGDsSubsumer, Map<Predicate, Set<GTGD>> fullTGDsMap,
            Map<Predicate, Set<GTGD>> nonFullTGDsMap, Set<GTGD> fullTGDsSet, Set<GTGD> nonFullTGDsSet) {
        final Collection<GTGD> newTGDs;
        final Map<Predicate, Set<GTGD>> TGDsMap;
        final Subsumer<GTGD> TGDsSubsumer;
        final Set<GTGD> TGDsSet;
        if (Logic.isFull(newTGD)) {
            newTGDs = newFullTGDs;
            TGDsMap = fullTGDsMap;
            TGDsSubsumer = fullTGDsSubsumer;
            TGDsSet = fullTGDsSet;
        } else {
            newTGDs = newNonFullTGDs;
            TGDsMap = nonFullTGDsMap;
            TGDsSubsumer = nonFullTGDsSubsumer;
            TGDsSet = nonFullTGDsSet;
        }

        if (Configuration.getOptimizationValue() >= 1) {

            if (TGDsSubsumer.subsumed(newTGD))
                return;

            Collection<GTGD> sub = TGDsSubsumer.subsumesAny(newTGD);
            TGDsSet.removeAll(sub);
            newTGDs.removeAll(sub);
            if (Logic.isFull(newTGD)) {
                sub.forEach(tgd -> {
                    Set<GTGD> set = TGDsMap.get(tgd.getGuard().getPredicate());
                    if (set != null)
                        set.remove(tgd);
                });
            } else
                for (GTGD tgd : sub) {
                    for (Atom atom : tgd.getHeadSet()) {
                        Set<GTGD> set = TGDsMap.get(atom.getPredicate());
                        if (set != null)
                            set.remove(tgd);
                    }
                }

            TGDsMap.values().removeIf(v -> v.isEmpty());
        }

        if (Configuration.getOptimizationValue() >= 5 && newTGDs.contains(newTGD))
            return;
        newTGDs.add(newTGD);
        TGDsSubsumer.add(newTGD);
    }

    // use this for correctness testing
    // same as normal addNewTGD, except it uses 2 subsumers, and asserts that they
    // return the same results
    private void addNewTGD(GTGD newTGD, Collection<GTGD> newFullTGDs, Collection<GTGD> newNonFullTGDs,
            Subsumer<GTGD> fullTGDsSubsumer, Subsumer<GTGD> nonFullTGDsSubsumer, Subsumer<GTGD> fullTGDsSubsumer2,
            Subsumer<GTGD> nonFullTGDsSubsumer2, Map<Predicate, Set<GTGD>> fullTGDsMap,
            Map<Predicate, Set<GTGD>> nonFullTGDsMap) {
        final Collection<GTGD> newTGDs;
        final Map<Predicate, Set<GTGD>> TGDsMap;
        final Subsumer<GTGD> TGDsSubsumer, TGDsSubsumer2;
        if (Logic.isFull(newTGD)) {
            newTGDs = newFullTGDs;
            TGDsMap = fullTGDsMap;
            TGDsSubsumer = fullTGDsSubsumer;
            TGDsSubsumer2 = fullTGDsSubsumer2;
        } else {
            newTGDs = newNonFullTGDs;
            TGDsMap = nonFullTGDsMap;
            TGDsSubsumer = nonFullTGDsSubsumer;
            TGDsSubsumer2 = nonFullTGDsSubsumer2;
        }

        if (Configuration.getOptimizationValue() >= 1) {
            if (TGDsSubsumer.subsumed(newTGD) != TGDsSubsumer2.subsumed(newTGD))
                System.out.println("boolean");
            assert TGDsSubsumer.subsumed(newTGD) == TGDsSubsumer2.subsumed(newTGD);
            if (TGDsSubsumer.subsumed(newTGD))
                return;

            Collection<GTGD> sub = TGDsSubsumer.subsumesAny(newTGD);
            Collection<GTGD> sub2 = TGDsSubsumer2.subsumesAny(newTGD);
            for (GTGD tgd : sub) {
                if (!subsumed(tgd, newTGD))
                    System.out.println("not subsumed");
                assert (subsumed(tgd, newTGD));
            }
            for (GTGD tgd : sub2) {
                if (!subsumed(tgd, newTGD))
                    System.out.println("not subsumed");
                assert (subsumed(tgd, newTGD));
            }
            // System.out.println("sub" + sub);
            // System.out.println("sub2" + sub2);
            assert sub.equals(sub2);

            newTGDs.removeAll(sub);
            // System.out.println("removing from filter " + subsumed.size() + " elements");
            // System.out.println("original " + newTGD);
            // System.out.println("subsumed " + subsumed);
            // subsumed.forEach(tgd ->
            // System.out.println(TGDsMap.get(tgd.getGuard().getPredicate())));
            // System.out.println("subsumed\t" + subsumed);
            // System.out.println("newTGD\t" + newTGD);
            // System.out.println("TGDsFilter\t" + TGDsFilter.getAll());
            if (Logic.isFull(newTGD)) {
                // for (TGDGSat tgd : subsumed)
                // if (TGDsMap.get(tgd.getGuard().getPredicate()) == null)
                // System.out.println("NULL: " + tgd);
                // System.out.println("going to do for each");
                // subsumed.forEach(tgd ->
                // System.out.println(TGDsMap.get(tgd.getGuard().getPredicate())));
                // System.out.println("full");
                sub.forEach(tgd -> {
                    Set<GTGD> set = TGDsMap.get(tgd.getGuard().getPredicate());
                    if (set != null)
                        set.remove(tgd);
                });
                // System.out.println("done with for each");
                // System.out.println("full end");
            } else
                for (GTGD tgd : sub) {
                    // System.out.println(tgd);
                    for (Atom atom : tgd.getHeadSet()) {
                        // System.out.println("not full" + atom);
                        Set<GTGD> set = TGDsMap.get(atom.getPredicate());
                        if (set != null)
                            set.remove(tgd);
                        // System.out.println("not full end");
                    }
                }

            TGDsMap.values().removeIf(v -> v.isEmpty());
        }

        if (Configuration.getOptimizationValue() >= 5 && newTGDs.contains(newTGD))
            return;
        newTGDs.add(newTGD);
        TGDsSubsumer.add(newTGD);
        TGDsSubsumer2.add(newTGD);
        assert TGDsSubsumer.getAll().equals(TGDsSubsumer2.getAll());
    }

    private boolean subsumed(GTGD tgd1, GTGD tgd2) {

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

    static boolean isSupportedRule(Dependency d) {
        // if (d instanceof TGD && ((TGD) d).isGuarded() && !containsSelfJoin((TGD) d))
        // // Adding only Guarded TGDs
        return d instanceof uk.ac.ox.cs.pdq.fol.TGD && ((uk.ac.ox.cs.pdq.fol.TGD) d).isGuarded(); // Adding only Guarded TGDs
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
    private boolean checkRenameVariablesInTGDs(Collection<GTGD> TGDs) {

        for (GTGD tgd : TGDs)
            for (String symbol : tgd.getAllTermSymbols())
                if (symbol.startsWith(uVariable) || symbol.startsWith(eVariable) || symbol.startsWith(zVariable)) {
                    App.logger.info("Found rename variable: " + symbol);
                    return true;
                }

        return false;

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

    private GTGD evolveRename(GTGD ftgd) {

        Variable[] uVariables = ftgd.getUniversal();

        Map<Term, Term> substitution = new HashMap<>();
        int counter = 1;
        for (Variable v : uVariables) {
            if (!v.getSymbol().startsWith(uVariable))
                throw new IllegalArgumentException("TGD not valid in evolveRename: " + ftgd);
            substitution.put(v, Variable.create(zVariable + counter++));
        }

        return (GTGD) Logic.applySubstitution(ftgd, substitution);

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

    /**
     *
     * @param nftgd non-full TGD (guarded)
     * @param ftgd  full TGD (guarded)
     * @return the derived rules of nftgd and ftgd according to the EVOLVE inference
     *         rule
     */
    public Collection<GTGD> evolveNew(GTGD nftgd, GTGD ftgd) {

        ftgd = evolveRename(ftgd);

        App.logger.fine("Composing:\n" + nftgd + "\nand\n" + ftgd);

        Atom guard = new GTGD(ftgd.getBodySet(), ftgd.getHeadSet()).getGuard();
        Collection<GTGD> results = new HashSet<>();

        for (Atom H : nftgd.getHeadAtoms()) {

            Map<Term, Term> guardMGU = getGuardMGU(guard, H);

            if (guardMGU != null && !guardMGU.isEmpty()) {

                final GTGD new_nftgd = Logic.applyMGU(nftgd, guardMGU);
                final GTGD new_ftgd = Logic.applyMGU(ftgd, guardMGU);

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
                    if (Sbody.isEmpty()) {
						for (GTGD hnf : new GTGD(new_body, new_head).computeHNF())
							results.add(hnf.computeVNF(eVariable, uVariable));
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

                    new_body.removeAll(Sbody);

                    if (mgu.isEmpty())
                        // no need to apply the MGU
						for (GTGD hnf : new GTGD(new_body, new_head).computeHNF())
							results.add(hnf.computeVNF(eVariable, uVariable));
                    else
						for (GTGD hnf : Logic.applyMGU(new_body, new_head, mgu).computeHNF())
							results.add(hnf.computeVNF(eVariable, uVariable));

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

    static Map<Term, Term> getVariableSubstitution(List<Atom> head, List<Atom> body) {

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
    static List<List<Atom>> getProduct(List<List<Atom>> shead) {

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

    static List<List<Atom>> getShead(Collection<Atom> headAtoms, Collection<Atom> sbody, Collection<Variable> eVariables) {

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

    static List<Atom> getSbody(Collection<Atom> new_bodyAtoms, Collection<Variable> eVariables) {

        return new_bodyAtoms.stream().filter(atom -> containsY(atom, eVariables)).collect(Collectors.toList());

    }

    private static boolean containsY(Atom atom, Collection<Variable> eVariables) {

        for (Term term : atom.getTerms())
            if (eVariables.contains(term))
                return true;

        return false;

    }


    private Map<Term, Term> getGuardMGU(Atom guard, Atom h) {
        return getGuardMGU(guard, h, eVariable, uVariable);
    }
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
    static Map<Term, Term> getGuardMGU(Atom guard, Atom h, String eVariable, String uVariable) {
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

    static <Q extends TGD> Subsumer<Q> createSubsumer() {

        String subsumptionMethod = Configuration.getSubsumptionMethod();
        Subsumer<Q> subsumer;

        if (subsumptionMethod.equals("tree_atom")) {
            subsumer = new ExactAtomSubsumer<Q>();
        } else {
            FormulaFilter<Q> filter;
            if (subsumptionMethod.equals("min_predicate")) {
                filter = new MinPredicateFilter<Q>();
            } else if (subsumptionMethod.equals("min_atom")) {
                filter = new MinAtomFilter<Q>();
            } else if (subsumptionMethod.equals("tree_predicate")) {
                filter = new TreePredicateFilter<Q>();
            } else {
                filter = new IdentityFormulaFilter<Q>();
            }
            subsumer = new SimpleSubsumer<Q>(filter);
        }
		return subsumer;
    }
}
