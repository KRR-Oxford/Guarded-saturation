package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

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
 * Abstract Gsat
 */
public abstract class AbstractGSat {

    protected static final TGDFactory<GTGD> FACTORY = TGDFactory.getGTGDInstance();
    protected boolean DEBUG_MODE = Configuration.isDebugMode();

    // New variable name for Universally Quantified Variables
    public String uVariable = "GSat_u";
    // New variable name for Existentially Quantified Variables
    public String eVariable = "GSat_e";
    // New variable name for evolveRename
    public String zVariable = "GSat_z";

    protected static Comparator<? super GTGD> comparator = (tgd1, tgd2) -> {

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
     *
     * Main method to run the Guarded Saturation algorithm
     *
     * @param allDependencies the Guarded TGDs to process
     * @return the Guarded Saturation of allDependencies
     */
    public Collection<GTGD> run(Collection<Dependency> allDependencies) {

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

        // initialization of the structure
        initialization(selectedTGDs, fullTGDsSet, newNonFullTGDs, fullTGDsMap, fullTGDsSubsumer, nonFullTGDsSubsumer);

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
                iterator.remove();
                App.logger.fine("current TGD: " + currentTGD);

                boolean added = addNonFullTGD(currentTGD, nonFullTGDsMap, nonFullTGDsSet);
                if (added)
                    for (GTGD ftgd : getFullTGDsToEvolve(fullTGDsMap, currentTGD)) {
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
                    }

            } else {

                Iterator<GTGD> iterator = newFullTGDs.iterator();
                GTGD currentTGD = iterator.next();
                iterator.remove();
                App.logger.fine("current TGD: " + currentTGD);

                boolean added = addFullTGD(currentTGD, fullTGDsMap, fullTGDsSet);

                Set<GTGD> set = getNonFullTGDsToEvolve(nonFullTGDsMap, currentTGD);
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
                if (isFull(newTGD))
                    addNewTGD(newTGD, true, newFullTGDs, fullTGDsSubsumer, fullTGDsMap, fullTGDsSet);

                if (isNonFull(newTGD))
                    addNewTGD(newTGD, false, newNonFullTGDs, nonFullTGDsSubsumer,
                        nonFullTGDsMap, nonFullTGDsSet);

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

	protected abstract void initialization(Collection<GTGD> selectedTGDs, Set<GTGD> fullTGDsSet,
                                           Collection<GTGD> newNonFullTGDs, Map<Predicate, Set<GTGD>> fullTGDsMap,
                                           Subsumer<GTGD> fullTGDsSubsumer, Subsumer<GTGD> nonFullTGDsSubsumer);

    /**
     * Returns true iff the tgd should be added to the fullTGD set
     */
    protected abstract boolean isFull(GTGD newTGD);

    /**
     * Returns true iff the tgd should be added to the nonfullTGD set
     */
    protected abstract boolean isNonFull(GTGD newTGD);

    protected abstract Collection<Predicate> getUnifiableBodyPredicates(GTGD tgd);

    private Set<GTGD> getFullTGDsToEvolve(Map<Predicate, Set<GTGD>> fullTGDsMap, GTGD currentTGD) {

        Set<GTGD> result = new HashSet<>();

        if (Configuration.getOptimizationValue() >= 4)
            result = new TreeSet<>(comparator);

        for (Atom atom : currentTGD.getHeadSet()) {
            Set<GTGD> set = fullTGDsMap.get(atom.getPredicate());
            if (set != null)
                result.addAll(set);
        }

        return result;
    }

    protected Set<GTGD> getNonFullTGDsToEvolve(Map<Predicate, Set<GTGD>> nonFullTGDsMap, GTGD currentTGD) {
        Set<GTGD> result = new HashSet<>();

        for (Predicate p : getUnifiableBodyPredicates(currentTGD)) {
            if (nonFullTGDsMap.containsKey(p))
                result.addAll(nonFullTGDsMap.get(p));
        }

        return result;
	}

    protected boolean addFullTGD(GTGD currentTGD, Map<Predicate, Set<GTGD>> fullTGDsMap, Set<GTGD> fullTGDsSet) {

        for (Predicate p : getUnifiableBodyPredicates(currentTGD))
            fullTGDsMap.computeIfAbsent(p, k -> new HashSet<GTGD>()).add(currentTGD);

        return fullTGDsSet.add(currentTGD);
    }

    protected boolean addNonFullTGD(GTGD currentTGD, Map<Predicate, Set<GTGD>> nonFullTGDsMap,
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

    private void addNewTGD(GTGD newTGD, boolean asFullTGD, Collection<GTGD> newTGDs,
                                  Subsumer<GTGD> TGDsSubsumer, Map<Predicate, Set<GTGD>> TGDsMap,
                                  Set<GTGD> TGDsSet) {
        if (Configuration.getOptimizationValue() >= 1) {

            if (TGDsSubsumer.subsumed(newTGD))
                return;

            Collection<GTGD> sub = TGDsSubsumer.subsumesAny(newTGD);
            TGDsSet.removeAll(sub);
            newTGDs.removeAll(sub);
            for (GTGD tgd : sub) {
                if (asFullTGD) {
                    for (Predicate p : getUnifiableBodyPredicates(newTGD)) {
                        Set<GTGD> set = TGDsMap.get(p);
                        if (set != null)
                            set.remove(tgd);
                    }
                } else {
                    for (Atom atom : tgd.getHeadSet()) {
                        Set<GTGD> set = TGDsMap.get(atom.getPredicate());
                        if (set != null)
                            set.remove(tgd);
                    }
                }
            }

            TGDsMap.values().removeIf(v -> v.isEmpty());
        }

        if (Configuration.getOptimizationValue() >= 5 && newTGDs.contains(newTGD))
            return;
        newTGDs.add(newTGD);
        TGDsSubsumer.add(newTGD);
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

    static boolean isSupportedRule(Dependency d) {
        // // Adding only Guarded TGDs
        return d instanceof uk.ac.ox.cs.pdq.fol.TGD && ((uk.ac.ox.cs.pdq.fol.TGD) d).isGuarded(); // Adding only Guarded TGDs
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

    protected GTGD evolveRename(GTGD ftgd) {

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

    protected abstract Collection<GTGD> evolveNew(GTGD currentTGD, GTGD ftgd);

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
