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
import uk.ac.ox.cs.gsat.unification.UnificationIndex;
import uk.ac.ox.cs.gsat.unification.UnificationIndexFactory;
import uk.ac.ox.cs.gsat.unification.UnificationIndexType;
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
public abstract class EvolveBasedSat<Q extends GTGD> {

    protected final boolean DEBUG_MODE = Configuration.isDebugMode();
    protected final Long TIME_OUT = Configuration.getTimeout();
    protected final String saturationName;
    protected final TGDFactory<Q> factory;

	// New variable name for Universally Quantified Variables
    public String uVariable;
    // New variable name for Existentially Quantified Variables
    public String eVariable;
    // New variable name for evolveRename
    public String zVariable;
	private UnificationIndexType leftIndexType;
	private UnificationIndexType rightIndexType;
    private EvolveStatisticsFactory<Q> statFactory;

    protected static Comparator<GTGD> comparator = (tgd1, tgd2) -> {

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

    protected EvolveBasedSat(String saturationName, TGDFactory<Q> factory, EvolveStatisticsFactory<Q> statFactory) {
        
        this(saturationName, factory, UnificationIndexType.PREDICATE_INDEX, UnificationIndexType.PREDICATE_INDEX, statFactory);
    }
    
    protected EvolveBasedSat(String saturationName, TGDFactory<Q> factory, UnificationIndexType leftIndexType, UnificationIndexType rightIndexType, EvolveStatisticsFactory<Q> statFactory) {
        this.saturationName = saturationName;
        this.uVariable = saturationName + "_u";
        this.eVariable = saturationName + "_e";
        this.zVariable = saturationName + "_z";
        this.factory = factory;
        this.leftIndexType = leftIndexType;
        this.rightIndexType = rightIndexType;
        this.statFactory = statFactory;
    }

    /**
     *
     * Main method to run the Guarded Saturation algorithm
     *
     * @param allDependencies the Guarded TGDs without function term to process
     * @return the Guarded Saturation of allDependencies
     */
    public Collection<Q> run(Collection<Dependency> allDependencies) {

        System.out.println(String.format("Running %s...", this.saturationName));
        EvolveStatistics<Q> stats = this.statFactory.create(saturationName);
        boolean timeoutReached = false;

        int discarded = 0;

        Collection<Q> selectedTGDs = new HashSet<>();
        for (Dependency d : allDependencies) {
            if (isSupportedRule(d)) {
                selectedTGDs.add(this.factory.create(Set.of(d.getBodyAtoms()), Set.of(d.getHeadAtoms())));
            } else {
                discarded++;
            }
        }

        App.logger.info(this.saturationName + " discarded rules : " + discarded + "/" + allDependencies.size() + " = "
                + String.format(Locale.UK, "%.3f", (float) discarded / allDependencies.size() * 100) + "%");

        if (DEBUG_MODE)
            System.out.println(this.saturationName + " discarded rules : " + discarded + "/" + allDependencies.size()
                    + " = " + String.format(Locale.UK, "%.3f", (float) discarded / allDependencies.size() * 100) + "%");

        // we start the time watch
        stats.start();
        
        // we change the fesh variables prefixes we will use until
        // they do not appear into the selected TGDs
        while (checkRenameVariablesInTGDs(selectedTGDs)) {
            uVariable += "0";
            eVariable += "0";
            zVariable += "0";
        }

        Collection<Q> newRightTGDs;
        Collection<Q> newLeftTGDs;

        switch (Configuration.getNewTGDStrusture()) {
        case ORDERED_BY_ATOMS_NB:
            newRightTGDs = new TreeSet<>(comparator);
            newLeftTGDs = new TreeSet<>(comparator);
            break;
        case SET:
            newRightTGDs = new HashSet<>();
            newLeftTGDs = new HashSet<>();
            break;
        case STACK:
            newRightTGDs = new Stack<>();
            newLeftTGDs = new Stack<>();
            break;
        default:
            throw new IllegalStateException();
        }

        UnificationIndex<Q> leftIndex = UnificationIndexFactory.getInstance().create(this.leftIndexType);
        UnificationIndex<Q> rightIndex;
        if (Configuration.isEvolvingTGDOrderingEnabled())
            rightIndex = UnificationIndexFactory.getInstance().create(this.rightIndexType, comparator);
        else
            rightIndex = UnificationIndexFactory.getInstance().create(this.rightIndexType);

        Set<Q> rightTGDsSet = new HashSet<>();
        Set<Q> leftTGDsSet = new HashSet<>();

        // initialization of the structures
        initialization(selectedTGDs, rightTGDsSet, newLeftTGDs, rightIndex);

        App.logger.info("Subsumption method : " + Configuration.getSubsumptionMethod());

        // hack
        Set<Q> allTGDSet = new HashSet<>();
        allTGDSet.addAll(rightTGDsSet);
        allTGDSet.addAll(newLeftTGDs);
        Subsumer<Q> rightTGDsSubsumer = createSubsumer(allTGDSet, newLeftTGDs);
        Subsumer<Q> leftTGDsSubsumer = createSubsumer(allTGDSet, rightTGDsSet);


        Set<Predicate> bodyPredicates = new HashSet<>();
        if (Configuration.isDiscardUselessTGDEnabled()) {
            bodyPredicates.addAll(getBodyPredicates(leftTGDsSet));
            bodyPredicates.addAll(getBodyPredicates(rightTGDsSet));
        }

        // copying the initial left TGD set for later comparison
        Collection<Q> initialRightTGDs = new HashSet<>(rightTGDsSet);

        stats.setInitialLeftTGDs(newLeftTGDs.size());
        stats.setInitialRightTGDs(rightTGDsSet.size());

        int counter = 100;

        while (!newRightTGDs.isEmpty() || !newLeftTGDs.isEmpty()) {

            if (isTimeout(stats.getStartTime())) {
                timeoutReached = true;
                break;
            }

            App.logger.fine("# new TGDs: " + newRightTGDs.size() + " , " + newLeftTGDs.size());

            if (DEBUG_MODE)
                if (counter == 100) {
                    counter = 1;
                    System.out.println("nonFullTGDs\t" + leftTGDsSet.size() + "\t\tfullTGDs\t" + rightTGDsSet.size()
                            + "\t\t\tnewNonFullTGDs\t" + newLeftTGDs.size() + "\t\tnewFullTGDs\t"
                            + newRightTGDs.size());
                } else
                    counter++;

            Collection<Q> toAdd = new ArrayList<>();

            // if there is a new left TGD, we evolve it with all the processed right TGDs
            // else there is a new right TGD to evolve with all the processed left TGDs
            if (!newLeftTGDs.isEmpty()) {

                Iterator<Q> iterator = newLeftTGDs.iterator();
                Q currentTGD = iterator.next();
                iterator.remove();

                App.logger.fine("current TGD: " + currentTGD);

                boolean added = addLeftTGD(currentTGD, leftIndex, leftTGDsSet);
                if (added)
                    for (Q rightTGD : getRightTGDsToEvolveWith(currentTGD, rightIndex)) {
                        stats.incrEvolveCount();
                        boolean isCurrentTGDSubsumed = fillToAdd(toAdd, currentTGD, rightTGD, true,                                                                  bodyPredicates, stats);

                        if (isCurrentTGDSubsumed) {
                            stats.incrStopBecauseSubsumedCount();
                            break;
                        }
                    }

            } else {

                Iterator<Q> iterator = newRightTGDs.iterator();
                Q currentTGD = iterator.next();
                iterator.remove();
                App.logger.fine("current TGD: " + currentTGD);

                boolean added = addRightTGD(currentTGD, rightIndex, rightTGDsSet);

                Set<Q> leftTGDsToEvolve = getLeftTGDsToEvolveWith(currentTGD, leftIndex);
                if (added && leftTGDsToEvolve != null)
                    for (Q leftTGD : leftTGDsToEvolve) {
                        stats.incrEvolveCount();
                        boolean isCurrentTGDSubsumed = fillToAdd(toAdd, currentTGD, leftTGD, false,                                                                  bodyPredicates, stats);

                        if (isCurrentTGDSubsumed) {
                            stats.incrStopBecauseSubsumedCount();
                            break;
                        }
                    }
            }

            // we update the structures with the TGDs to add
            for (Q newTGD : toAdd) {

                if (isRightTGD(newTGD)) {
                    stats.newRightTGD(newTGD);
                    addNewTGD(newTGD, true, newRightTGDs, rightTGDsSubsumer, rightIndex, rightTGDsSet, stats);
                }

                if (isLeftTGD(newTGD)) {
                    stats.newLeftTGD(newTGD);
                    addNewTGD(newTGD, false, newLeftTGDs, leftTGDsSubsumer, leftIndex, leftTGDsSet, stats);
                }
            }
        }

        Collection<Q> output = getOutput(rightTGDsSubsumer.getAll());

        stats.stop();
        stats.log(leftTGDsSubsumer, rightTGDsSubsumer);
        
        Collection<Q> outputCopy = new ArrayList<>(output);
        outputCopy.removeAll(initialRightTGDs);
        App.logger.info("ouptput full TGDs not contained in the input: " + outputCopy.size());
        // if (rightTGDsSubsumer instanceof SimpleSubsumer)
            // ((SimpleSubsumer<Q>)rightTGDsSubsumer).printIndex();


        if (timeoutReached)
            App.logger.info("!!! TIME OUT !!!");

        return output;
    }

    private boolean fillToAdd(Collection<Q> toAdd, Q currentTGD, Q otherTGD, boolean isCurrentLeftTGD, Set<Predicate> bodyPredicates, EvolveStatistics<Q> stats) {
        Q leftTGD = (isCurrentLeftTGD) ? currentTGD : otherTGD;
        Q rightTGD = (!isCurrentLeftTGD) ? currentTGD : otherTGD;
        final long startTime = System.nanoTime();
        Collection<Q> evolvedTGDs = evolveNew(leftTGD, rightTGD);
        stats.incrEvolveTime(System.nanoTime() - startTime);
        if (Configuration.isStopEvolvingIfSubsumedEnabled()) {
            boolean subsumed = false;
            for (Q newTGD : evolvedTGDs) {
                if (!currentTGD.equals(newTGD)) {
                    if (!Configuration.isDiscardUselessTGDEnabled() || isEvolvedTGDIsUseful(newTGD, rightTGD, bodyPredicates)) {
                        toAdd.add(newTGD);
                        subsumed = subsumed || subsumed(currentTGD, newTGD);
                        if (subsumed) {
                            App.logger.fine("stop because sub :\n" + leftTGD + "\n + \n" + rightTGD + "\n = \n"
                                        + newTGD + "\n");
                            toAdd.clear();
                            toAdd.add(newTGD);
                            break;
                        }
                    } else {
                            App.logger.fine("evolve useless :\n" + leftTGD + "\n + \n" + rightTGD + "\n = \n" + newTGD + "\n");
                    }
                } else {
                    App.logger.fine("evolve equals :\n" + leftTGD + "\n + \n" + rightTGD + "\n = \n" + newTGD + "\n");
                    stats.incrEvolvedEqualsCount();
                }
            }
            if (subsumed) {
                return true;
            }
        } else
            for (Q newTGD : evolvedTGDs)
                toAdd.add(newTGD);

        return false;
    }

    private boolean isEvolvedTGDIsUseful(Q evolvedTGD, Q rightTGD, Set<Predicate> bodyPredicates) {
        if (isRightTGD(evolvedTGD))
            return true;
        else
            for (Atom a : rightTGD.getHeadAtoms())
                if (bodyPredicates.contains(a.getPredicate()))
                    return true;
        return false;
    }

    protected void initialization(Collection<Q> selectedTGDs, Set<Q> rightTGDsSet, Collection<Q> newLeftTGDs,
            UnificationIndex<Q> rightIndex) {

        for (Q transformedTGD : transformInputTGDs(selectedTGDs)) {
            if (isRightTGD(transformedTGD)) {
                addRightTGD(transformedTGD, rightIndex, rightTGDsSet);
            }
            if (isLeftTGD(transformedTGD)) {
                newLeftTGDs.add(transformedTGD);
            }
        }
    }

    /**
     * Returns the input TGDs transformed into TGDs on which the evolve function
     * should be applied
     */
    protected abstract Collection<Q> transformInputTGDs(Collection<Q> inputTGDs);

    /**
     * select the ouput from the final right side TGDs
     */
    protected abstract Collection<Q> getOutput(Collection<Q> rightTGDs);

    /**
     * Returns true iff the tgd should be considered as a left input TGD for evolve
     */
    protected abstract boolean isLeftTGD(Q newTGD);

    /**
     * Returns true iff the tgd should be considered as a right input TGD for evolve
     */
    protected abstract boolean isRightTGD(Q newTGD);

    /**
     * Returns the atoms of the body of the input right TGD that may be
     * unifiable in evolve
     */
    protected abstract Atom[] getUnifiableBodyAtoms(Q rightTGD);

    /**
     * Apply the evolve function
     */
    protected abstract Collection<Q> evolveNew(Q leftTGD, Q rightTGD);

    private Set<Q> getRightTGDsToEvolveWith(Q leftTGD, UnificationIndex<Q> rightIndex) {

        Set<Q> result = new HashSet<>();

        if (Configuration.isEvolvingTGDOrderingEnabled())
            result = new TreeSet<>(comparator);

        for (Atom atom : leftTGD.getHeadSet()) {
            Set<Q> set = rightIndex.get(atom);
            result.addAll(set);
        }

        return result;
    }

    protected Set<Q> getLeftTGDsToEvolveWith(Q rightTGD, UnificationIndex<Q> leftIndex) {
        Set<Q> result = new HashSet<>();

        for (Atom atom : getUnifiableBodyAtoms(rightTGD)) {
            result.addAll(leftIndex.get(atom));
        }

        return result;
    }

    protected boolean addRightTGD(Q rightTGD, UnificationIndex<Q> rightIndex, Set<Q> rightTGDsSet) {

        for (Atom a : getUnifiableBodyAtoms(rightTGD))
            rightIndex.put(a, rightTGD);

        return rightTGDsSet.add(rightTGD);
    }

    protected boolean addLeftTGD(Q leftTGD, UnificationIndex<Q> leftIndex, Set<Q> leftTGDsSet) {

        for (Atom atom : leftTGD.getHeadSet())
            leftIndex.put(atom, leftTGD);

        return leftTGDsSet.add(leftTGD);
    }

    private void addNewTGD(Q newTGD, boolean asRightTGD, Collection<Q> newTGDs, Subsumer<Q> TGDsSubsumer,
                           UnificationIndex<Q> unificationIndex, Set<Q> TGDsSet, EvolveStatistics<Q> stats) {

        // discard if the newTGD is a tautology
        if (Configuration.isTautologyDiscarded() && newTGD.getBodySet().containsAll(newTGD.getHeadSet())) {
            stats.incrDiscardedTautologyCount();
            return;
        }

        long startTime = System.nanoTime();
        boolean isSubsumed = TGDsSubsumer.subsumed(newTGD);
        stats.incrForwardSubsumptionTime(System.nanoTime() - startTime);
        if (isSubsumed)
            return;

        startTime = System.nanoTime();
        Collection<Q> sub = TGDsSubsumer.subsumesAny(newTGD);
        stats.incrBackwardSubsumptionTime(System.nanoTime() - startTime);

        TGDsSet.removeAll(sub);
        newTGDs.removeAll(sub);
        for (Q tgd : sub) {
            if (asRightTGD) {
                for (Atom atom : getUnifiableBodyAtoms(newTGD)) {
                    unificationIndex.remove(atom, newTGD);
                }
            } else {
                for (Atom atom : tgd.getHeadSet()) {
                    unificationIndex.remove(atom, newTGD);
                }
            }
        }

        if (Configuration.getNewTGDStrusture().equals(newTGDStructure.STACK) && newTGDs.contains(newTGD))
            return;
        newTGDs.add(newTGD);
        TGDsSubsumer.add(newTGD);
    }

    private boolean subsumed(Q tgd1, Q tgd2) {

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

    private Collection<Predicate> getBodyPredicates(Collection<Q> tgds) {
        Collection<Predicate> result = new HashSet<>();

        for (Q tgd : tgds)
            for (Atom a : tgd.getBodyAtoms())
                result.add(a.getPredicate());

        return result;
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
    private boolean checkRenameVariablesInTGDs(Collection<Q> TGDs) {

        for (Q tgd : TGDs)
            for (String symbol : tgd.getAllTermSymbols())
                if (symbol.startsWith(uVariable) || symbol.startsWith(eVariable) || symbol.startsWith(zVariable)) {
                    App.logger.info("Found rename variable: " + symbol);
                    return true;
                }

        return false;

    }

    protected Q evolveRename(Q ftgd) {

        Variable[] uVariables = ftgd.getUniversal();

        Map<Term, Term> substitution = new HashMap<>();
        int counter = 1;
        for (Variable v : uVariables) {
            if (!v.getSymbol().startsWith(uVariable))
                throw new IllegalArgumentException("TGD not valid in evolveRename: " + ftgd);
            substitution.put(v, Variable.create(zVariable + counter++));
        }

        return (Q) Logic.applySubstitution(ftgd, substitution);

    }

    public TGDFactory<Q> getFactory() {
		return factory;
	}

    private boolean isTimeout(long startTime) {
        // from seconds to nano seconds
        Long timeout = (TIME_OUT != null) ? (long) (1000 * 1000 * 1000 * TIME_OUT) : null;

        if (timeout != null && timeout < (System.nanoTime() - startTime))
            return true;
        return false;
    }

    static <P extends TGD> Subsumer<P> createSubsumer(Set<P> initialTgds) {
        return createSubsumer(initialTgds, new HashSet<>());
    }
    
    static <P extends TGD> Subsumer<P> createSubsumer(Set<P> allTGDSet, Collection<P> newLeftTGDs) {

        String subsumptionMethod = Configuration.getSubsumptionMethod();
        Subsumer<P> subsumer;

        if (subsumptionMethod.equals("tree_atom")) {
            subsumer = new ExactAtomSubsumer<P>();
            for (P formula: allTGDSet)
                if (!newLeftTGDs.contains(formula))
                    subsumer.add(formula);
        } else {
            FormulaFilter<P> filter;
            if (subsumptionMethod.equals("min_predicate")) {
                filter = new MinPredicateFilter<P>();
            } else if (subsumptionMethod.equals("min_atom")) {
                filter = new MinAtomFilter<P>();
            } else if (subsumptionMethod.equals("tree_predicate")) {
                filter = new TreePredicateFilter<P>();
            } else if (subsumptionMethod.equals("identity")) {
                filter = new IdentityFormulaFilter<P>();
            } else {
                throw new IllegalStateException("Subsumption method " + subsumptionMethod + " is not supported.");
            }
            filter.init(allTGDSet);
            filter.removeAll(newLeftTGDs);
            subsumer = new SimpleSubsumer<P>(filter);
        }
        return subsumer;
    }

    public static enum newTGDStructure {
        STACK,
        /**
         * In evolved based algorithms, if true, the new TGDs (right and left) are stored 
         * in ordered sets such that the TGDs with smallest body and largest head
         * come first
         */
        ORDERED_BY_ATOMS_NB,
        SET
    }
}
