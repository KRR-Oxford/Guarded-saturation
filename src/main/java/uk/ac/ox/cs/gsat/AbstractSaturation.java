package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import uk.ac.ox.cs.gsat.subsumers.Subsumer;
import uk.ac.ox.cs.gsat.unification.UnificationIndex;
import uk.ac.ox.cs.gsat.unification.UnificationIndexFactory;
import uk.ac.ox.cs.gsat.unification.UnificationIndexType;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.Predicate;

/**
 * The abstract saturation class initialize the structures used by each
 * saturation algorithms following the chosen configuration. The tgds are 
 * partitioned in two: the left tgds and the right tgds. The algorithm process
 * should unify the head atoms of the left tgds with the body atoms of the right tgds to 
 * derive new tgds.
 */
public abstract class AbstractSaturation<Q extends GTGD> {
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
    // type of the left unification index
    protected final UnificationIndexType leftIndexType;
    // type of the right unification index
    protected final UnificationIndexType rightIndexType;
    // factory of the statistics record
    protected final EvolveStatisticsFactory<Q> statFactory;

    protected AbstractSaturation(String saturationName, TGDFactory<Q> factory, EvolveStatisticsFactory<Q> statFactory) {

        this(saturationName, factory, UnificationIndexType.PREDICATE_INDEX, UnificationIndexType.PREDICATE_INDEX,
                statFactory);
    }

    protected AbstractSaturation(String saturationName, TGDFactory<Q> factory, UnificationIndexType leftIndexType,
            UnificationIndexType rightIndexType, EvolveStatisticsFactory<Q> statFactory) {
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
     * Main method to run the saturation algorithm
     */
    public Collection<Q> run(Collection<Dependency> allDependencies) {

        System.out.println(String.format("Running %s...", this.saturationName));
        EvolveStatistics<Q> stats = this.statFactory.create(saturationName);

        int discarded = 0;

        Collection<Q> selectedTGDs = new HashSet<>();
        for (Dependency d : allDependencies) {
            if (SaturationUtils.isSupportedRule(d)) {
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
            newRightTGDs = new TreeSet<>(SaturationUtils.comparator);
            newLeftTGDs = new TreeSet<>(SaturationUtils.comparator);
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
            rightIndex = UnificationIndexFactory.getInstance().create(this.rightIndexType, SaturationUtils.comparator);
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
        Subsumer<Q> rightTGDsSubsumer = SaturationUtils.createSubsumer(allTGDSet, newLeftTGDs);
        Subsumer<Q> leftTGDsSubsumer = SaturationUtils.createSubsumer(allTGDSet, rightTGDsSet);

        Set<Predicate> bodyPredicates = new HashSet<>();
        if (Configuration.isDiscardUselessTGDEnabled()) {
            bodyPredicates.addAll(SaturationUtils.getBodyPredicates(leftTGDsSet));
            bodyPredicates.addAll(SaturationUtils.getBodyPredicates(rightTGDsSet));
        }

        // copying the initial left TGD set for later comparison
        Collection<Q> initialRightTGDs = new HashSet<>(rightTGDsSet);

        stats.setInitialLeftTGDs(newLeftTGDs.size());
        stats.setInitialRightTGDs(rightTGDsSet.size());

        // running the saturation process using the structures
        process(leftTGDsSet, rightTGDsSet, newLeftTGDs, newRightTGDs, leftIndex, rightIndex, leftTGDsSubsumer, rightTGDsSubsumer, bodyPredicates, stats);
        
        // filter the outputed (full) TGD from the right ones
        Collection<Q> output = getOutput(rightTGDsSubsumer.getAll());

        stats.stop();
        stats.log(leftTGDsSubsumer, rightTGDsSubsumer);

        Collection<Q> outputCopy = new ArrayList<>(output);
        outputCopy.removeAll(initialRightTGDs);
        App.logger.info("ouptput full TGDs not contained in the input: " + outputCopy.size());
        // if (rightTGDsSubsumer instanceof SimpleSubsumer)
        // ((SimpleSubsumer<Q>)rightTGDsSubsumer).printIndex();

        return output;
    }

    /**
     * saturation process of the algorithm 
     * 
     * @param leftTGDsSet 
     * @param rightTGDsSet 
     * @param newLeftTGDs 
     * @param newRightTGDs 
     * @param leftIndex 
     * @param rightIndex 
     * @param bodyPredicates 
     * @param stats 
     * @param leftTGDsSubsumer 
     * @param rightTGDsSubsumer 
     *
     */
    protected abstract void process(Set<Q> leftTGDsSet, Set<Q> rightTGDsSet, Collection<Q> newLeftTGDs,
                Collection<Q> newRightTGDs, UnificationIndex<Q> leftIndex, UnificationIndex<Q> rightIndex,
                Subsumer<Q> leftTGDsSubsumer, Subsumer<Q> rightTGDsSubsumer, Set<Predicate> bodyPredicates,
                EvolveStatistics<Q> stats);

    /**
     * select the ouput from the final right TGDs
     */
    protected abstract Collection<Q> getOutput(Collection<Q> rightTGDs);

    public TGDFactory<Q> getFactory() {
        return factory;
    }

    /**
     * Fill the structure holding TGDs with the selected TGDs transformed for the
     * algorithm. For the skolemised approches, this is the step, which skolemises
     * the TGDs
     */
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
     * Returns the inputted TGDs transformed into TGDs on which the saturation process
     * should be applied
     */
    protected abstract Collection<Q> transformInputTGDs(Collection<Q> inputTGDs);

    /**
     * Returns true iff the tgd should be considered as a left input TGD
     */
    protected abstract boolean isLeftTGD(Q newTGD);

    /**
     * Returns true iff the tgd should be considered as a right input TGD
     */
    protected abstract boolean isRightTGD(Q newTGD);

    /**
     * Returns the atoms of the body of the input right TGD that may be unifiable
     */
    protected abstract Atom[] getUnifiableBodyAtoms(Q rightTGD);

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

    protected void addNewTGD(Q newTGD, boolean asRightTGD, Collection<Q> newTGDs, Subsumer<Q> TGDsSubsumer,
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

        if (Configuration.getNewTGDStrusture().equals(SaturationUtils.newTGDStructure.STACK) && newTGDs.contains(newTGD))
            return;
        newTGDs.add(newTGD);
        TGDsSubsumer.add(newTGD);
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

    protected boolean isTimeout(long startTime) {
        // from seconds to nano seconds
        Long timeout = (TIME_OUT != null) ? (long) (1000 * 1000 * 1000 * TIME_OUT) : null;

        if (timeout != null && timeout < (System.nanoTime() - startTime))
            return true;
        return false;
    }

}