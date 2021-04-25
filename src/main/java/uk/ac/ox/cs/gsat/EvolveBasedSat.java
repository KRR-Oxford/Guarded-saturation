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
 * Abstract saturation based on a evolve function. The evolve function takes two
 * TGDs as input, the first will be called the left TGD and the second the right
 * TGD applying evolve on them requires to find an unifier of some atoms from
 * the head of the left TGD and some of the body of the right TGD and it returns
 * some TGDs
 *
 * During the saturation process, we categorise the input and generated TGDs
 * into TGDs used as left or/and right input of the evolve function. This
 * categorization is handled by the methods isLeftTGD and isRightTGD
 *
 * The saturation process aims to compute the closure of these two (left and
 * right) sets of TGDs under the evolve inference.
 */
public abstract class EvolveBasedSat {

    protected static final TGDFactory<GTGD> FACTORY = TGDFactory.getGTGDInstance();
    protected final boolean DEBUG_MODE = Configuration.isDebugMode();
    protected final String saturationName;

    // New variable name for Universally Quantified Variables
    public String uVariable;
    // New variable name for Existentially Quantified Variables
    public String eVariable;
    // New variable name for evolveRename
    public String zVariable;

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

    protected EvolveBasedSat(String saturationName) {
        this.saturationName = saturationName;
        this.uVariable = saturationName + "_u";
        this.eVariable = saturationName + "_e";
        this.zVariable = saturationName + "_z";
    }

    /**
     *
     * Main method to run the Guarded Saturation algorithm
     *
     * @param allDependencies the Guarded TGDs without function term to process
     * @return the Guarded Saturation of allDependencies
     */
    public Collection<GTGD> run(Collection<Dependency> allDependencies) {

        System.out.println(String.format("Running %s...", this.saturationName));
        final long startTime = System.nanoTime();

        int discarded = 0;

        Collection<GTGD> selectedTGDs = new HashSet<>();
        for (Dependency d : allDependencies)
            if (isSupportedRule(d))
                selectedTGDs.add(new GTGD(Set.of(d.getBodyAtoms()), Set.of(d.getHeadAtoms())));
            else
                discarded++;

        App.logger.info(this.saturationName + " discarded rules : " + discarded + "/" + allDependencies.size() + " = "
                + String.format(Locale.UK, "%.3f", (float) discarded / allDependencies.size() * 100) + "%");

        if (DEBUG_MODE)
            System.out.println(this.saturationName + " discarded rules : " + discarded + "/" + allDependencies.size()
                    + " = " + String.format(Locale.UK, "%.3f", (float) discarded / allDependencies.size() * 100) + "%");

        // we change the fesh variables prefixes we will use until
        // they do not appear into the selected TGDs
        while (checkRenameVariablesInTGDs(selectedTGDs)) {
            uVariable += "0";
            eVariable += "0";
            zVariable += "0";
        }

        Collection<GTGD> newRightTGDs = new HashSet<>();
        Collection<GTGD> newLeftTGDs = new HashSet<>();

        if (Configuration.getOptimizationValue() >= 5) {

            newRightTGDs = new Stack<>();
            newLeftTGDs = new Stack<>();

        } else if (Configuration.getOptimizationValue() >= 2) {

            newRightTGDs = new TreeSet<>(comparator);
            newLeftTGDs = new TreeSet<>(comparator);

        }

        Map<Predicate, Set<GTGD>> leftTGDsMap = new HashMap<>();
        Map<Predicate, Set<GTGD>> rightTGDsMap = new HashMap<>();
        Set<GTGD> rightTGDsSet = new HashSet<>();
        Set<GTGD> leftTGDsSet = new HashSet<>();

        App.logger.info("Subsumption method : " + Configuration.getSubsumptionMethod());
        Subsumer<GTGD> rightTGDsSubsumer = createSubsumer();
        Subsumer<GTGD> leftTGDsSubsumer = createSubsumer();

        // initialization of the structures
        initialization(selectedTGDs, rightTGDsSet, newLeftTGDs, rightTGDsMap, rightTGDsSubsumer, leftTGDsSubsumer);

        // copying the initial left TGD set for later comparison
        Collection<GTGD> initialRightTGDs = new HashSet<>(rightTGDsSet);

        App.logger.info("# initial TGDs: " + rightTGDsSet.size() + " , " + newLeftTGDs.size());
        int counter = 100;
        int newRightCount = 0;
        int newLeftCount = 0;
        while (!newRightTGDs.isEmpty() || !newLeftTGDs.isEmpty()) {

            App.logger.fine("# new TGDs: " + newRightTGDs.size() + " , " + newLeftTGDs.size());

            if (DEBUG_MODE)
                if (counter == 100) {
                    counter = 1;
                    System.out.println("nonFullTGDs\t" + leftTGDsSet.size() + "\t\tfullTGDs\t" + rightTGDsSet.size()
                            + "\t\t\tnewNonFullTGDs\t" + newLeftTGDs.size() + "\t\tnewFullTGDs\t"
                            + newRightTGDs.size());
                } else
                    counter++;

            Collection<GTGD> toAdd = new ArrayList<>();

            // if there is a new left TGD, we evolve it with all the processed right TGDs
            // else there is a new right TGD to evolve with all the processed left TGDs
            if (!newLeftTGDs.isEmpty()) {

                Iterator<GTGD> iterator = newLeftTGDs.iterator();
                GTGD currentTGD = iterator.next();
                iterator.remove();
                App.logger.fine("current TGD: " + currentTGD);

                boolean added = addLeftTGD(currentTGD, leftTGDsMap, leftTGDsSet);
                if (added)
                    for (GTGD rightTGD : getRightTGDsToEvolveWith(currentTGD, rightTGDsMap)) {
                        if (Configuration.getOptimizationValue() >= 3) {
                            boolean subsumed = false;
                            for (GTGD newTGD : evolveNew(currentTGD, rightTGD)) {
                                if (!currentTGD.equals(newTGD)) {
                                    toAdd.add(newTGD);
                                    subsumed = subsumed || subsumed(currentTGD, newTGD);
                                }
                            }
                            if (subsumed)
                                break;
                        } else
                            for (GTGD newTGD : evolveNew(currentTGD, rightTGD))
                                toAdd.add(newTGD);
                    }

            } else {

                Iterator<GTGD> iterator = newRightTGDs.iterator();
                GTGD currentTGD = iterator.next();
                iterator.remove();
                App.logger.fine("current TGD: " + currentTGD);

                boolean added = addRightTGD(currentTGD, rightTGDsMap, rightTGDsSet);

                Set<GTGD> leftTGDsToEvolve = getLeftTGDsToEvolveWith(currentTGD, leftTGDsMap);
                if (added && leftTGDsToEvolve != null)
                    for (GTGD leftTGD : leftTGDsToEvolve)
                        if (Configuration.getOptimizationValue() >= 3) {
                            boolean subsumed = false;
                            for (GTGD newTGD : evolveNew(leftTGD, currentTGD)) {
                                if (!currentTGD.equals(newTGD)) {
                                    toAdd.add(newTGD);
                                    subsumed = subsumed || subsumed(currentTGD, newTGD);
                                }
                            }
                            if (subsumed)
                                break;
                        } else
                            for (GTGD newTGD : evolveNew(leftTGD, currentTGD))
                                toAdd.add(newTGD);

            }

            // we update the structures with the TGDs to add
            for (GTGD newTGD : toAdd) {

                if (isRightTGD(newTGD)) {
                    newRightCount++;
                    addNewTGD(newTGD, true, newRightTGDs, rightTGDsSubsumer, rightTGDsMap, rightTGDsSet);
                }

                if (isLeftTGD(newTGD)) {
                    newLeftCount++;
                    addNewTGD(newTGD, false, newLeftTGDs, leftTGDsSubsumer, leftTGDsMap, leftTGDsSet);
                }
            }

        }

        final long stopTime = System.nanoTime();

        long totalTime = stopTime - startTime;

        App.logger.info("GSat total time : " + String.format(Locale.UK, "%.0f", totalTime / 1E6) + " ms = "
                + String.format(Locale.UK, "%.2f", totalTime / 1E9) + " s");
        App.logger.info("Subsumed elements : "
                + (rightTGDsSubsumer.getNumberSubsumed() + leftTGDsSubsumer.getNumberSubsumed()));
        App.logger.info("Filter discarded elements : "
                + (rightTGDsSubsumer.getFilterDiscarded() + leftTGDsSubsumer.getFilterDiscarded()));
        App.logger.info("Derived full/non full TGDs: " + newRightCount + " , " + newLeftCount);

        Collection<GTGD> output = getOutput(rightTGDsSubsumer.getAll());

        Collection<GTGD> outputCopy = new ArrayList<>(output);
        outputCopy.removeAll(initialRightTGDs);
        App.logger.info("ouptput full TGDs not contained in the input: " + outputCopy.size());

        return output;
    }

    protected void initialization(Collection<GTGD> selectedTGDs, Set<GTGD> rightTGDsSet, Collection<GTGD> newLeftTGDs,
            Map<Predicate, Set<GTGD>> rightTGDsMap, Subsumer<GTGD> rightTGDsSubsumer, Subsumer<GTGD> leftTGDsSubsumer) {

        for (GTGD transformedTGD : transformInputTGDs(selectedTGDs)) {

            if (isRightTGD(transformedTGD)) {
                addRightTGD(transformedTGD, rightTGDsMap, rightTGDsSet);
                rightTGDsSubsumer.add(transformedTGD);
            }
            if (isLeftTGD(transformedTGD)) {
                leftTGDsSubsumer.add(transformedTGD);
                newLeftTGDs.add(transformedTGD);
            }
        }
    }

    /**
     * Returns the input TGDs transformed into TGDs on which the evolve function
     * should be applied
     */
    protected abstract Collection<GTGD> transformInputTGDs(Collection<GTGD> inputTGDs);

    /**
     * select the ouput from the final right side TGDs
     */
    protected abstract Collection<GTGD> getOutput(Collection<GTGD> rightTGDs);

    /**
     * Returns true iff the tgd should be considered as a left input TGD for evolve
     */
    protected abstract boolean isLeftTGD(GTGD newTGD);

    /**
     * Returns true iff the tgd should be considered as a right input TGD for evolve
     */
    protected abstract boolean isRightTGD(GTGD newTGD);

    /**
     * Returns the predicates of the body atoms of the input right TGD that may be
     * unifiable in evolve
     */
    protected abstract Collection<Predicate> getUnifiableBodyPredicates(GTGD rightTGD);

    /**
     * Apply the evolve function
     */
    protected abstract Collection<GTGD> evolveNew(GTGD leftTGD, GTGD rightTGD);

    private Set<GTGD> getRightTGDsToEvolveWith(GTGD leftTGD, Map<Predicate, Set<GTGD>> rightTGDsMap) {

        Set<GTGD> result = new HashSet<>();

        if (Configuration.getOptimizationValue() >= 4)
            result = new TreeSet<>(comparator);

        for (Atom atom : leftTGD.getHeadSet()) {
            Set<GTGD> set = rightTGDsMap.get(atom.getPredicate());
            if (set != null)
                result.addAll(set);
        }

        return result;
    }

    protected Set<GTGD> getLeftTGDsToEvolveWith(GTGD rightTGD, Map<Predicate, Set<GTGD>> leftTGDsMap) {
        Set<GTGD> result = new HashSet<>();

        for (Predicate p : getUnifiableBodyPredicates(rightTGD)) {
            if (leftTGDsMap.containsKey(p))
                result.addAll(leftTGDsMap.get(p));
        }

        return result;
    }

    protected boolean addRightTGD(GTGD rightTGD, Map<Predicate, Set<GTGD>> rightTGDsMap, Set<GTGD> rightTGDsSet) {

        for (Predicate p : getUnifiableBodyPredicates(rightTGD))
            rightTGDsMap.computeIfAbsent(p, k -> new HashSet<GTGD>()).add(rightTGD);

        return rightTGDsSet.add(rightTGD);
    }

    protected boolean addLeftTGD(GTGD leftTGD, Map<Predicate, Set<GTGD>> leftTGDsMap, Set<GTGD> leftTGDsSet) {

        if (Configuration.getOptimizationValue() >= 4)
            for (Atom atom : leftTGD.getHeadSet())
                leftTGDsMap.computeIfAbsent(atom.getPredicate(), k -> new TreeSet<GTGD>(comparator)).add(leftTGD);
        else
            for (Atom atom : leftTGD.getHeadSet())
                leftTGDsMap.computeIfAbsent(atom.getPredicate(), k -> new HashSet<GTGD>()).add(leftTGD);

        return leftTGDsSet.add(leftTGD);
    }

    private void addNewTGD(GTGD newTGD, boolean asRightTGD, Collection<GTGD> newTGDs, Subsumer<GTGD> TGDsSubsumer,
            Map<Predicate, Set<GTGD>> TGDsMap, Set<GTGD> TGDsSet) {
        if (Configuration.getOptimizationValue() >= 1) {

            if (TGDsSubsumer.subsumed(newTGD))
                return;

            Collection<GTGD> sub = TGDsSubsumer.subsumesAny(newTGD);
            TGDsSet.removeAll(sub);
            newTGDs.removeAll(sub);
            for (GTGD tgd : sub) {
                if (asRightTGD) {
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
        // Adding only Guarded TGDs
        return d instanceof uk.ac.ox.cs.pdq.fol.TGD && ((uk.ac.ox.cs.pdq.fol.TGD) d).isGuarded(); // Adding only Guarded
                                                                                                  // TGDs
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