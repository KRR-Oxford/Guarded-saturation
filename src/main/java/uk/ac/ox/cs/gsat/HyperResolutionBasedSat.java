package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import uk.ac.ox.cs.gsat.subsumers.Subsumer;
import uk.ac.ox.cs.gsat.unification.UnificationIndex;
import uk.ac.ox.cs.gsat.unification.UnificationIndexType;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Term;

public class HyperResolutionBasedSat<Q extends SkGTGD> extends AbstractSaturation<Q> {

    private static final TGDFactory<SkGTGD> FACTORY = TGDFactory.getSkGTGDInstance();
    private static final EvolveStatisticsFactory<SkGTGD> STAT_FACTORY = SkolemStatistics.getSkFactory();
    private static final String NAME = "HyperSat";
    private static final HyperResolutionBasedSat<SkGTGD> INSTANCE = createInstance();

    private static HyperResolutionBasedSat<SkGTGD> createInstance() {
        return new HyperResolutionBasedSat<SkGTGD>(NAME, FACTORY, UnificationIndexType.PREDICATE_INDEX,
                UnificationIndexType.PREDICATE_INDEX, STAT_FACTORY);
    }

    public static HyperResolutionBasedSat<SkGTGD> getInstance() {
        return INSTANCE;
    }

    protected HyperResolutionBasedSat(String saturationName, TGDFactory<Q> factory, UnificationIndexType leftIndexType,
            UnificationIndexType rightIndexType, EvolveStatisticsFactory<Q> statFactory) {
        super(saturationName, factory, leftIndexType, rightIndexType, statFactory);
    }

    @Override
    protected void process(Set<Q> leftTGDsSet, Set<Q> rightTGDsSet, Collection<Q> newLeftTGDs,
            Collection<Q> newRightTGDs, UnificationIndex<Q> leftIndex, UnificationIndex<Q> rightIndex,
            Subsumer<Q> leftTGDsSubsumer, Subsumer<Q> rightTGDsSubsumer, Set<Predicate> bodyPredicates,
            EvolveStatistics<Q> stats) {

        while (!newLeftTGDs.isEmpty() || !newRightTGDs.isEmpty()) {

            if (isTimeout(stats.getStartTime())) {
                stats.timeoutReached();
                break;
            }

            Collection<Q> resolved = new HashSet<>();

            if (!newRightTGDs.isEmpty()) {

                Iterator<Q> iterator = newRightTGDs.iterator();
                Q rightTGD = iterator.next();
                iterator.remove();
                addRightTGD(rightTGD, rightIndex, rightTGDsSet);
                System.out.println("rightTGD: " + rightTGD);
                stats.incrEvolveCount();
                resolved.addAll(hyperresolve(leftTGDsSet, leftIndex, rightTGD));

            } else if (!newLeftTGDs.isEmpty()) {
                Iterator<Q> iterator = newLeftTGDs.iterator();
                Q leftTGD = iterator.next();
                iterator.remove();
                addLeftTGD(leftTGD, leftIndex, leftTGDsSet);

                System.out.println("leftTGD: " + leftTGD);
                for (Q rightTGD : rightIndex.get(leftTGD.getHeadAtom(0))) {
                    System.out.println("rightTGD: " + rightTGD);
                    stats.incrEvolveCount();
                    Q resolvedRightTGD = resolveOn(leftTGD, rightTGD);
                    System.out.println("resolvedLeftTGD: " + resolvedRightTGD);
                    if (resolvedRightTGD != null && resolvedRightTGD.getFunctionalBodyAtoms().length > 0)
                        resolved.addAll(hyperresolve(leftTGDsSet, leftIndex, resolvedRightTGD));
                    else if (resolvedRightTGD != null)
                        resolved.add(factory.computeVNF(resolvedRightTGD, eVariable, uVariable));
                }
            }

            // we update the structures with the TGDs to add
            for (Q newTGD : resolved) {
                System.out.println("newTGD: " + newTGD);
                if (isRightTGD(newTGD)) {
                    stats.newRightTGD(newTGD);
                    addNewTGD(newTGD, true, newRightTGDs, rightTGDsSubsumer, rightIndex, rightTGDsSet, stats);
                }

                if (isLeftTGD(newTGD)) {
                    stats.newLeftTGD(newTGD);
                    addNewTGD(newTGD, false, newLeftTGDs, leftTGDsSubsumer, leftIndex, leftTGDsSet, stats);
                }
            }
            System.out.println("======================== \n");

        }

    }

    private Collection<? extends Q> hyperresolve(Set<Q> leftTGDsSet, UnificationIndex<Q> leftIndex, Q rightTGD) {
        System.out.println("hyperresolve: " + rightTGD);
        Collection<Q> result = new HashSet<>();

        Atom selectedAtom;
        if (rightTGD.getFunctionalBodyAtoms().length > 0) {
            selectedAtom = rightTGD.getFunctionalBodyAtoms()[0];
        } else {
            selectedAtom = rightTGD.getGuard();
        }

        Collection<Q> resolutionCandidates = leftIndex.get(selectedAtom);

        for (Q candidate : resolutionCandidates) {

            Q new_tgd = resolveOn(candidate, rightTGD, selectedAtom);

            if (new_tgd != null) {
                if (new_tgd.getFunctionalBodyAtoms().length > 0) {

                    System.out.println("rec call on " + new_tgd);
                    for (Q r : hyperresolve(leftTGDsSet, leftIndex, new_tgd))
                        result.add(r);
                } else {
                    result.add(factory.computeVNF(new_tgd, eVariable, uVariable));
                }
            }
        }
        return result;
    }

    // right and left are body-full
    // resolve right with left on the first atom of the body of right unifiable with
    // the head of left
    private Q resolveOn(Q leftTGD, Q rightTGD) {
        for (Atom B : rightTGD.getBodyAtoms()) {
            Q r = resolveOn(leftTGD, rightTGD, B);
            if (r != null)
                return r;
        }
        return null;
    }

    private Q resolveOn(Q leftTGD, Q rightTGD, Atom B) {

        leftTGD = renameVariable(leftTGD);
        Atom H = leftTGD.getHeadAtom(0);
        Map<Term, Term> mgu = Logic.getMGU(B, H);
        System.out.println("mgu of " + B + " and " + H + ": " + mgu);
        if (mgu != null) {
            Set<Atom> new_body = new HashSet<>();
            for (Atom batom : rightTGD.getBodySet())
                if (!batom.equals(B))
                    new_body.add((Atom) Logic.applySubstitution(batom, mgu));

            for (Atom batom : leftTGD.getBodySet())
                new_body.add((Atom) Logic.applySubstitution(batom, mgu));

            Set<Atom> new_head = new HashSet<>();
            for (Atom hatom : rightTGD.getHeadSet())
                new_head.add((Atom) Logic.applySubstitution(hatom, mgu));

            Q new_tgd = factory.create(new_body, new_head);

            return new_tgd;
        } else {
            return null;
        }
    }

    @Override
    protected Collection<Q> getOutput(Collection<Q> rightTGDs) {
        // ugly copy from AbstractSkolemSat
        Collection<Q> output = new HashSet<>();

        for (Q tgd : rightTGDs)
            if (!tgd.isFunctional())
                output.add(tgd);

        return output;

    }

    @Override
    protected Collection<Q> transformInputTGDs(Collection<Q> inputTGDs) {
        // ugly copy from AbstractSkolemSat
        Collection<Q> result = new ArrayList<>();

        for (Q tgd : inputTGDs) {

            Collection<Q> singleSkolemizedTGDs;
            switch (Configuration.getSkolemizationType()) {
            case NAIVE:
                singleSkolemizedTGDs = factory.computeSingleHeadedSkolemized(tgd);
                break;
            case PROJ_ON_FRONTIER:
                singleSkolemizedTGDs = factory.computeSingleHeadSkolemizedOnFrontierVariable(tgd);
                break;
            default:
                String message = String.format("the skolemization type %s is not supported",
                        Configuration.getSkolemizationType());
                throw new IllegalStateException(message);
            }

            for (Q shnf : singleSkolemizedTGDs) {
                result.add(factory.computeVNF(shnf, eVariable, uVariable));
            }
        }
        return result;
    }

    @Override
    protected boolean isLeftTGD(Q newTGD) {
        return newTGD.isNonFull();
    }

    @Override
    protected boolean isRightTGD(Q newTGD) {
        return !newTGD.isNonFull();
    }

    @Override
    protected Atom[] getUnifiableBodyAtoms(Q rightTGD) {
        return rightTGD.getBodyAtoms();
    }

}
