package uk.ac.ox.cs.gsat.satalg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;

import uk.ac.ox.cs.gsat.Log;
import uk.ac.ox.cs.gsat.api.SaturationStatColumns;
import uk.ac.ox.cs.gsat.fol.GTGD;
import uk.ac.ox.cs.gsat.fol.TGDFactory;
import uk.ac.ox.cs.gsat.subsumers.Subsumer;
import uk.ac.ox.cs.gsat.unification.UnificationIndex;
import uk.ac.ox.cs.gsat.unification.UnificationIndexType;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Predicate;

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
public abstract class EvolveBasedSat<Q extends GTGD> extends AbstractSaturation<Q> {

    protected EvolveBasedSat(String saturationName, TGDFactory<Q> factory, SaturationAlgorithmConfiguration config) {

        this(saturationName, factory, UnificationIndexType.PREDICATE_INDEX, UnificationIndexType.PREDICATE_INDEX, config);
    }

    protected EvolveBasedSat(String saturationName, TGDFactory<Q> factory, UnificationIndexType leftIndexType,
            UnificationIndexType rightIndexType, SaturationAlgorithmConfiguration config) {
        super(saturationName, factory, leftIndexType, rightIndexType, config);
    }

    @Override
    protected void process(Set<Q> leftTGDsSet, Set<Q> rightTGDsSet, Collection<Q> newLeftTGDs,
            Collection<Q> newRightTGDs, UnificationIndex<Q> leftIndex, UnificationIndex<Q> rightIndex,
            Subsumer<Q> leftTGDsSubsumer, Subsumer<Q> rightTGDsSubsumer, Set<Predicate> bodyPredicates,
            String processName) throws TimeoutException {

        int counter = 100;

        while (!newRightTGDs.isEmpty() || !newLeftTGDs.isEmpty()) {

            checkTimeout(statsCollector.total(processName));

            Log.GLOBAL.fine("# new TGDs: " + newRightTGDs.size() + " , " + newLeftTGDs.size());

            if (config.isVerbose())
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

                Log.GLOBAL.fine("current TGD: " + currentTGD);

                boolean added = addLeftTGD(currentTGD, leftIndex, leftTGDsSet);
                if (added)
                    for (Q rightTGD : getRightTGDsToEvolveWith(currentTGD, rightIndex)) {
                        statsCollector.incr(processName, SaturationStatColumns.EVOL_COUNT);
                        boolean isCurrentTGDSubsumed = fillToAdd(toAdd, currentTGD, rightTGD, true, bodyPredicates,
                                processName);

                        if (isCurrentTGDSubsumed) {
                            statsCollector.incr(processName, SaturationStatColumns.STOP_BECAUSE_SUBSUMED);
                            break;
                        }
                    }

            } else {

                Iterator<Q> iterator = newRightTGDs.iterator();
                Q currentTGD = iterator.next();
                iterator.remove();
                Log.GLOBAL.fine("current TGD: " + currentTGD);

                boolean added = addRightTGD(currentTGD, rightIndex, rightTGDsSet);

                Set<Q> leftTGDsToEvolve = getLeftTGDsToEvolveWith(currentTGD, leftIndex);
                if (added && leftTGDsToEvolve != null)
                    for (Q leftTGD : leftTGDsToEvolve) {
                        statsCollector.incr(processName, SaturationStatColumns.EVOL_COUNT);
                        boolean isCurrentTGDSubsumed = fillToAdd(toAdd, currentTGD, leftTGD, false, bodyPredicates,
                                                                 processName);

                        if (isCurrentTGDSubsumed) {
                            statsCollector.incr(processName, SaturationStatColumns.STOP_BECAUSE_SUBSUMED);
                            break;
                        }
                    }
            }

            // we update the structures with the TGDs to add
            for (Q newTGD : toAdd) {

                if (isRightTGD(newTGD)) {
                    reportNewRightTGD(processName, newTGD);
                    addNewTGD(newTGD, true, newRightTGDs, rightTGDsSubsumer, rightIndex, rightTGDsSet, processName);
                }

                if (isLeftTGD(newTGD)) {
                    reportNewLeftTGD(processName, newTGD);
                    addNewTGD(newTGD, false, newLeftTGDs, leftTGDsSubsumer, leftIndex, leftTGDsSet, processName);
                }
            }
        }

    }

    private boolean fillToAdd(Collection<Q> toAdd, Q currentTGD, Q otherTGD, boolean isCurrentLeftTGD,
            Set<Predicate> bodyPredicates, String processName) {
        Q leftTGD = (isCurrentLeftTGD) ? currentTGD : otherTGD;
        Q rightTGD = (!isCurrentLeftTGD) ? currentTGD : otherTGD;
        statsCollector.tick(processName, SaturationStatColumns.OTHER_TIME);
        Collection<Q> evolvedTGDs = evolveNew(leftTGD, rightTGD);
        statsCollector.tick(processName, SaturationStatColumns.EVOL_TIME);
        if (this.config.isStopEvolvingIfSubsumedEnabled()) {
            boolean subsumed = false;
            for (Q newTGD : evolvedTGDs) {
                if (!currentTGD.equals(newTGD)) {
                    if (!this.config.isDiscardUselessTGDEnabled()
                            || isEvolvedTGDIsUseful(newTGD, rightTGD, bodyPredicates)) {
                        toAdd.add(newTGD);
                        subsumed = subsumed || SaturationUtils.subsumed(currentTGD, newTGD);
                        if (subsumed) {
                            Log.GLOBAL.fine("stop because sub :\n" + leftTGD + "\n + \n" + rightTGD + "\n = \n" + newTGD
                                    + "\n");
                            toAdd.clear();
                            toAdd.add(newTGD);
                            break;
                        }
                    } else {
                        Log.GLOBAL.fine(
                                "evolve useless :\n" + leftTGD + "\n + \n" + rightTGD + "\n = \n" + newTGD + "\n");
                    }
                } else {
                    Log.GLOBAL.fine("evolve equals :\n" + leftTGD + "\n + \n" + rightTGD + "\n = \n" + newTGD + "\n");
                    statsCollector.incr(processName, SaturationStatColumns.EVOL_STOPPED_BECAUSE_EQUAL);
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

    private Set<Q> getRightTGDsToEvolveWith(Q leftTGD, UnificationIndex<Q> rightIndex) {

        Set<Q> result = new HashSet<>();

        if (this.config.isEvolvingTGDOrderingEnabled())
            result = new TreeSet<>(SaturationUtils.comparator);

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

    /**
     * Apply the evolve function
     */
    protected abstract Collection<Q> evolveNew(Q leftTGD, Q rightTGD);

}
