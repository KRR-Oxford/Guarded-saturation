package uk.ac.ox.cs.gsat.satalg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;

import uk.ac.ox.cs.gsat.App;
import uk.ac.ox.cs.gsat.Log;
import uk.ac.ox.cs.gsat.api.SaturationAlgorithm;
import uk.ac.ox.cs.gsat.api.SaturationStatColumns;
import uk.ac.ox.cs.gsat.fol.GTGD;
import uk.ac.ox.cs.gsat.fol.Logic;
import uk.ac.ox.cs.gsat.fol.TGDFactory;
import uk.ac.ox.cs.gsat.statistics.NullStatisticsCollector;
import uk.ac.ox.cs.gsat.statistics.StatisticsCollector;
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
 * The abstract saturation class initialize the structures used by each
 * saturation algorithms following the chosen configuration. The tgds are
 * partitioned in two: the left tgds and the right tgds. The algorithm process
 * should unify the head atoms of the left tgds with the body atoms of the right
 * tgds to derive new tgds.
 */
public abstract class AbstractSaturation<Q extends GTGD> implements SaturationAlgorithm {
    protected final String saturationName;
    protected final TGDFactory<Q> factory;
    // New variable name for Universally Quantified Variables
    public String uVariable;
    // New variable name for Existentially Quantified Variables
    public String eVariable;
    // New variable name for renameVariable
    public String zVariable;
    // type of the left unification index
    protected final UnificationIndexType leftIndexType;
    // type of the right unification index
    protected final UnificationIndexType rightIndexType;
    protected final SaturationAlgorithmConfiguration config;
    protected StatisticsCollector<SaturationStatColumns> statsCollector = new NullStatisticsCollector<>();

    protected AbstractSaturation(String saturationName, TGDFactory<Q> factory, SaturationAlgorithmConfiguration config) {

        this(saturationName, factory, UnificationIndexType.PREDICATE_INDEX, UnificationIndexType.PREDICATE_INDEX,
             config);
    }

    protected AbstractSaturation(String saturationName, TGDFactory<Q> factory, UnificationIndexType leftIndexType,
                                 UnificationIndexType rightIndexType, SaturationAlgorithmConfiguration config) {
        this.saturationName = saturationName;
        this.uVariable = saturationName + "_u";
        this.eVariable = saturationName + "_e";
        this.zVariable = saturationName + "_z";
        this.factory = factory;
        this.config = config;
        // in case the unification index type is configurated forces the configuration
        // choice
        if (config.getUnificationIndexType() != null) {
            this.leftIndexType = config.getUnificationIndexType();
            this.rightIndexType = config.getUnificationIndexType();
        } else {
            this.leftIndexType = leftIndexType;
            this.rightIndexType = rightIndexType;
        }
    }

    public Collection<Q> run(Collection<? extends Dependency> allDependencies) {
        return run("", allDependencies);
    }
    
    /**
     * Main method to run the saturation algorithm
     */
    public Collection<Q> run(String processName, Collection<? extends Dependency> allDependencies) {

        System.out.println(String.format("Running %s...", this.saturationName));

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

        // we start the time watch
        if (this.statsCollector != null)
            statsCollector.start(processName);

        // we change the fesh variables prefixes we will use until
        // they do not appear into the selected TGDs
        while (checkRenameVariablesInTGDs(selectedTGDs)) {
            uVariable += "0";
            eVariable += "0";
            zVariable += "0";
        }

        // the selected TGDs needs to be transformated  for the
        // algorithm. For the skolemised approches, it skolemises
        // the TGDs.
        Collection<Q> initialTGDs = transformInputTGDs(selectedTGDs);
        
        // right and left structures storing the newly derived TGDs; meaning unchecked
        Collection<Q> newRightTGDs;
        Collection<Q> newLeftTGDs;

        switch (config.getNewTGDStrusture()) {
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

        // right and left unification indexes of the checked TGDs
        UnificationIndex<Q> leftIndex = UnificationIndexFactory.getInstance().create(this.leftIndexType);
        UnificationIndex<Q> rightIndex;
        if (config.isEvolvingTGDOrderingEnabled())
            rightIndex = UnificationIndexFactory.getInstance().create(this.rightIndexType, SaturationUtils.comparator);
        else
            rightIndex = UnificationIndexFactory.getInstance().create(this.rightIndexType);

        // set of the checked TGDs
        Set<Q> rightTGDsSet = new HashSet<>();
        Set<Q> leftTGDsSet = new HashSet<>();

        App.logger.info("Subsumption method : " + config.getSubsumptionMethod());

        // creation of the right and left subsumers storing both the checked and unchecked TGDs
        Subsumer<Q> rightTGDsSubsumer = SaturationUtils.createSubsumer(initialTGDs, config);
        Subsumer<Q> leftTGDsSubsumer = SaturationUtils.createSubsumer(initialTGDs, config);

        // initialization of the structures
        initialization(initialTGDs, rightTGDsSet, leftTGDsSet, newLeftTGDs, rightIndex, leftIndex, rightTGDsSubsumer, leftTGDsSubsumer, processName);
        
        Set<Predicate> bodyPredicates = new HashSet<>();
        if (config.isDiscardUselessTGDEnabled()) {
            bodyPredicates.addAll(SaturationUtils.getBodyPredicates(leftTGDsSet));
            bodyPredicates.addAll(SaturationUtils.getBodyPredicates(rightTGDsSet));
        }

        // copying the initial left TGD set for later comparison
        Collection<Q> initialRightTGDs = new HashSet<>(rightTGDsSet);

        statsCollector.put(processName, SaturationStatColumns.NFTGD_NB, newLeftTGDs.size());
        statsCollector.put(processName, SaturationStatColumns.FTGD_NB, rightTGDsSet.size());

        try {
            // running the saturation process using the structures
            process(leftTGDsSet, rightTGDsSet, newLeftTGDs, newRightTGDs, leftIndex, rightIndex, leftTGDsSubsumer,
                    rightTGDsSubsumer, bodyPredicates, processName);
            statsCollector.stop(processName, SaturationStatColumns.TIME);
        } catch (TimeoutException e) {
            statsCollector.put(processName, SaturationStatColumns.TIME, "TIMEOUT");
        }

        // filter the outputed (full) TGD from the right ones
        Collection<Q> output = getOutput(rightTGDsSubsumer.getAll());

        statsCollector.put(processName, SaturationStatColumns.OUTPUT_SIZE, output.size());
        Collection<Q> outputCopy = new ArrayList<>(output);
        outputCopy.removeAll(initialRightTGDs);

        statsCollector.put(processName, SaturationStatColumns.NEW_OUTPUT_SIZE, outputCopy.size());
        statsCollector.put(processName, SaturationStatColumns.SUBSUMED, (leftTGDsSubsumer.getNumberSubsumed() + rightTGDsSubsumer.getNumberSubsumed()));

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
                                    String processName) throws TimeoutException;

    /**
     * select the ouput from the final right TGDs
     */
    protected abstract Collection<Q> getOutput(Collection<Q> rightTGDs);

    public TGDFactory<Q> getFactory() {
        return factory;
    }

    /**
     * Fill the structure holding TGDs with the initial TGDs.
     * The right TGDs structures are initialized as if every right TGD is checked.
     * While the left TGDs structures are initialized as if every left TGD is unchecked.
     */
    protected void initialization(Collection<Q> initialTGDs, Set<Q> rightTGDsSet, Set<Q> leftTGDsSet,
                                  Collection<Q> newLeftTGDs, UnificationIndex<Q> rightIndex, UnificationIndex<Q> leftIndex, Subsumer<Q> rightTGDsSubsumer, Subsumer<Q> leftTGDsSubsumer, String processName) {
        // we store the inserted right TGDs without redundancy.
        Set<Q> insertedRightTGDs = new HashSet<>();
        for (Q transformedTGD : initialTGDs) {
            if (isRightTGD(transformedTGD)) {
                addNewTGD(transformedTGD, true, insertedRightTGDs, rightTGDsSubsumer, rightIndex, rightTGDsSet, processName);
            }
            if (isLeftTGD(transformedTGD)) {
                addNewTGD(transformedTGD, false, newLeftTGDs, leftTGDsSubsumer, leftIndex, leftTGDsSet, processName);
            }
        }
        // we add every right tgd as checked
        for (Q insertedRightTGD : insertedRightTGDs) {
            addRightTGD(insertedRightTGD, rightIndex, rightTGDsSet);
        }
    }

    /**
     * Returns the inputted TGDs transformed into TGDs on which the saturation
     * process should be applied
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
                             UnificationIndex<Q> unificationIndex, Set<Q> TGDsSet, String processName) {

        // discard if the newTGD is a tautology
        if (config.isTautologyDiscarded() && newTGD.getBodySet().containsAll(newTGD.getHeadSet())) {
            statsCollector.incr(processName, SaturationStatColumns.DISCARDED_TAUTOLOGY);
            return;
        }

        statsCollector.tick(processName, SaturationStatColumns.OTHER_TIME);
        boolean isSubsumed = TGDsSubsumer.subsumed(newTGD);
        statsCollector.tick(processName, SaturationStatColumns.FORWARD_SUB_TIME);

        if (isSubsumed)
            return;

        statsCollector.tick(processName, SaturationStatColumns.OTHER_TIME);
        Collection<Q> sub = TGDsSubsumer.subsumesAny(newTGD);
        statsCollector.tick(processName, SaturationStatColumns.BACKWARD_SUB_TIME);

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

        if (config.getNewTGDStrusture().equals(NewTGDStructure.STACK) && newTGDs.contains(newTGD))
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

    protected Q renameVariable(Q ftgd) {

        return (Q) Logic.applySubstitution(ftgd, getRenameVariableSubstitution(ftgd));
    }

    protected Map<Term, Term> getRenameVariableSubstitution(Q ftgd) {
        return getRenameVariableSubstitution(ftgd, zVariable);
    }

    protected Map<Term, Term> getRenameVariableSubstitution(Q ftgd, String prefix) {

        Variable[] uVariables = ftgd.getUniversal();

        Map<Term, Term> substitution = new HashMap<>();
        int counter = 1;
        for (Variable v : uVariables) {
            if (!v.getSymbol().startsWith(uVariable))
                throw new IllegalArgumentException("TGD not valid in renameVariable: " + ftgd);
            substitution.put(v, Variable.create(prefix + counter++));
        }

        return substitution;
    }

    protected void checkTimeout(long currentDurationInMS) throws TimeoutException {
        if (config.getTimeout() != null && (1000 * config.getTimeout()) < currentDurationInMS) {
            throw new TimeoutException();
        }
    }

    public void setStatsCollector(StatisticsCollector<SaturationStatColumns> statsCollector) {
        this.statsCollector = statsCollector;
    }

    protected void reportNewLeftTGD(String processName, Q tgd) {
        statsCollector.incr(processName, SaturationStatColumns.NEW_NFTGD_NB);
    }

    protected void reportNewRightTGD(String processName, Q tgd) {
        statsCollector.incr(processName, SaturationStatColumns.NEW_FTGD_NB);
    }

}
