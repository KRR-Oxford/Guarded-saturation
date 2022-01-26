package uk.ac.ox.cs.gsat.satalg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import uk.ac.ox.cs.gsat.App;
import uk.ac.ox.cs.gsat.Configuration;
import uk.ac.ox.cs.gsat.fol.Logic;
import uk.ac.ox.cs.gsat.fol.SkGTGD;
import uk.ac.ox.cs.gsat.fol.TGDFactory;
import uk.ac.ox.cs.gsat.satalg.stats.HyperResolutionStatistics;
import uk.ac.ox.cs.gsat.satalg.stats.SaturationStatistics;
import uk.ac.ox.cs.gsat.satalg.stats.SaturationStatisticsFactory;
import uk.ac.ox.cs.gsat.subsumers.Subsumer;
import uk.ac.ox.cs.gsat.unification.UnificationIndex;
import uk.ac.ox.cs.gsat.unification.UnificationIndexType;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.FunctionTerm;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

public class HyperResolutionBasedSat<Q extends SkGTGD> extends AbstractSaturation<Q> {

    private static final TGDFactory<SkGTGD> FACTORY = TGDFactory.getSkGTGDInstance(Configuration.isSortedVNF());
    private static final SaturationStatisticsFactory<SkGTGD> STAT_FACTORY = HyperResolutionStatistics.getFactory();
    private static final String NAME = "HyperSat";
    private static final HyperResolutionBasedSat<SkGTGD> INSTANCE = createInstance();

    private static HyperResolutionBasedSat<SkGTGD> createInstance() {
        return new HyperResolutionBasedSat<SkGTGD>(NAME, FACTORY, UnificationIndexType.ATOM_PATH_INDEX,
                UnificationIndexType.ATOM_PATH_INDEX, STAT_FACTORY);
    }

    public static HyperResolutionBasedSat<SkGTGD> getInstance() {
        return INSTANCE;
    }

    private Map<Q, Q> renamingCache = new HashMap<>();

    protected HyperResolutionBasedSat(String saturationName, TGDFactory<Q> factory, UnificationIndexType leftIndexType,
            UnificationIndexType rightIndexType, SaturationStatisticsFactory<Q> statFactory) {
        super(saturationName, factory, leftIndexType, rightIndexType, statFactory);
    }

    @Override
    protected void process(Set<Q> leftTGDsSet, Set<Q> rightTGDsSet, Collection<Q> newLeftTGDs,
            Collection<Q> newRightTGDs, UnificationIndex<Q> leftIndex, UnificationIndex<Q> rightIndex,
            Subsumer<Q> leftTGDsSubsumer, Subsumer<Q> rightTGDsSubsumer, Set<Predicate> bodyPredicates,
            SaturationStatistics<Q> s) {

        HyperResolutionStatistics<Q> stats = (HyperResolutionStatistics<Q>) s;

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

                if (App.logger.isLoggable(Level.FINE))
                    App.logger.fine("picked full TGD: " + rightTGD);

                long startTime = System.nanoTime();
                resolved.addAll(hyperresolveWithNewFullTGD(leftTGDsSet, leftIndex, rightTGD, stats));
                stats.incrHyperResolutionTime(System.nanoTime() - startTime);
            } else if (!newLeftTGDs.isEmpty()) {
                Iterator<Q> iterator = newLeftTGDs.iterator();
                Q leftTGD = iterator.next();
                iterator.remove();
                addLeftTGD(leftTGD, leftIndex, leftTGDsSet);

                if (App.logger.isLoggable(Level.FINE))
                    App.logger.fine("picked non-full TGD: " + leftTGD);

                long startTime = System.nanoTime();
                resolved.addAll(hyperresolveWithNewNonFullTGD(leftTGDsSet, leftIndex, rightIndex, leftTGD, stats));
                stats.incrHyperResolutionTime(System.nanoTime() - startTime);
            }

            // we update the structures with the TGDs to add
            for (Q newTGD : resolved) {
                if (App.logger.isLoggable(Level.FINE))
                    App.logger.fine("newTGD: " + newTGD);

                if (isRightTGD(newTGD)) {
                    stats.newRightTGD(newTGD);
                    addNewTGD(newTGD, true, newRightTGDs, rightTGDsSubsumer, rightIndex, rightTGDsSet, stats);
                }

                if (isLeftTGD(newTGD)) {
                    stats.newLeftTGD(newTGD);
                    addNewTGD(newTGD, false, newLeftTGDs, leftTGDsSubsumer, leftIndex, leftTGDsSet, stats);
                }

            }
            App.logger.fine("======================== \n");

        }

    }

    /**
     * hyperresolution with a new non-full TGD requires to try to hyperresolve using every full TGDs such that 
     * a body atom of full TGDs is resolvable with the non-full TGD's head. This body atom is not necessary a guard, so we have to retrieve the non-full TGDs to resolve the guard first.
     * There is an optimization to reduce the number of these last non-full TGDs using the new one.
     */    
    private Collection<? extends Q> hyperresolveWithNewNonFullTGD(Set<Q> nonFullTGDsSet,
            UnificationIndex<Q> nonFullTGDsIndex, UnificationIndex<Q> fullTGDsIndex, Q nonFullTGD,
            HyperResolutionStatistics<Q> stats) {
    
        Collection<Q> resolved = new HashSet<>();
    
        for (Q fullTGD : fullTGDsIndex.get(nonFullTGD.getHeadAtom(0))) {
            Set<Variable> fullTGDVariables = Set.of(fullTGD.getUniversal());
            for (Atom bodyAtom : fullTGD.getBodyAtoms()) {
                // two cases whether bodyAtom is a guard or not
    
                boolean isGuard = Set.of(bodyAtom.getVariables()).containsAll(fullTGDVariables);
    
                // 1. bodyAtom is a guard
                if (isGuard) {
                    resolved.addAll(hyperresolveStartingWith(nonFullTGDsSet, nonFullTGDsIndex, nonFullTGD,
                                                             fullTGD, bodyAtom, stats));
                } else {
                    // 2. bodyAtom is not a guard, so resolving on this atom will leave some Skolem functions
                    Q resolvedRightTGD = resolveOn(nonFullTGD, fullTGD, bodyAtom);
                    if (resolvedRightTGD != null) {
                        resolvedRightTGD = factory.computeVNF(resolvedRightTGD, eVariable, uVariable);
                        Atom resolvedGuard = resolvedRightTGD.getGuard();
    
                        for (Q nonFullTGDForGuard : nonFullTGDsIndex.get(resolvedGuard)) {
                            // check if the head is compatible meaning it covers the skolem function of the resolved guard
                            // this initialisation is necessary in the case where the unification index is disabled.
                            boolean isCompatible = resolvedGuard.getPredicate().equals(nonFullTGDForGuard.getHeadAtom(0).getPredicate());
                            for (int pos = 0; pos < resolvedGuard.getPredicate().getArity(); pos++) {
                                if (!isCompatible)
                                    break;
                                isCompatible = isCompatible && (!(resolvedGuard.getTerm(pos) instanceof FunctionTerm)
                                        || ((nonFullTGDForGuard.getHeadAtom(0).getTerm(pos) instanceof FunctionTerm)
                                                && ((FunctionTerm) resolvedGuard.getTerm(pos)).getFunction().equals(
                                                        ((FunctionTerm) nonFullTGDForGuard.getHeadAtom(0).getTerm(pos))
                                                                .getFunction())));
                            }
    
                            if (isCompatible) {
                                resolved.addAll(hyperresolveStartingWith(nonFullTGDsSet, nonFullTGDsIndex,
                                        nonFullTGDForGuard, resolvedRightTGD, resolvedGuard, stats));
                            }  else {
                                stats.incrHyperResolutionCount();
                                stats.incrHyperResolutionFailureCount();
                            }
                        }
                    }
                }
            }
        }
        return resolved;
    }
    
    /**
     * hyperresolution with a new full TGD gathering the non-full TGDs to resolve a guard of the full TGD
     */
    private Collection<? extends Q> hyperresolveWithNewFullTGD(Set<Q> nonFullTGDsSet,
            UnificationIndex<Q> nonFullTGDsIndex, Q fullTGD, HyperResolutionStatistics<Q> stats) {

        Collection<Q> resolved = new HashSet<>();
        for (Q nonFullTGD : nonFullTGDsIndex.get(fullTGD.getGuard()))
            resolved.addAll(hyperresolveStartingWith(nonFullTGDsSet, nonFullTGDsIndex, nonFullTGD, fullTGD, fullTGD.getGuard(), stats));

        return resolved;
    }

    /**
     * hyperresolving starting with a non full TGD and a full TGD or a partially resolved full TGD and an atom guarding the full TGD by:
     * 1. resolve the guard full TGDs with the head of the non full TGD 
     * 2. hyperresolving the resulting TGD, if it contains Skolem in body, otherwise return the resulting full TGD
     */

    private Collection<Q> hyperresolveStartingWith(Set<Q> nonFullTGDsSet, UnificationIndex<Q> nonFullTGDsIndex, Q nonFullTGD,
                                                   Q fullTGD, Atom fullTGDGuard, HyperResolutionStatistics<Q> stats) {
        Collection<Q> resolvedTGDs = new HashSet<>();
        Q resolvedRightTGD = resolveOn(nonFullTGD, fullTGD, fullTGDGuard);
        if (App.logger.isLoggable(Level.FINE)) {
            App.logger.fine("first resolved TGD: " + "\nnon full: " + nonFullTGD + "\nfull: " + fullTGD + "\nresolved: " + resolvedRightTGD);
        }

        if (resolvedRightTGD != null) {
            resolvedRightTGD = factory.computeVNF(resolvedRightTGD, eVariable, uVariable);

            List<Atom> skolemAtoms = Arrays.asList(resolvedRightTGD.getFunctionalBodyAtoms());
            if (skolemAtoms.size() > 0) {
                // the resolved TGD contains Skolem functions in its body 

                List<List<Q>> nonFullTGDcandidatesForSkolemAtoms = computeCandidatesForSkolemAtoms(nonFullTGDsIndex, skolemAtoms);
                if (App.logger.isLoggable(Level.FINE)) {
                    App.logger.fine("skolem atom candidates: " + nonFullTGDcandidatesForSkolemAtoms);
                }

                for (List<Q> candidateList : nonFullTGDcandidatesForSkolemAtoms) {
                    stats.incrHyperResolutionCount();

                    List<Atom> headAtoms = new ArrayList<>();
                    List<Map<Term, Term>> candidateRenaming = new ArrayList<>();
                    int pos = 1;
                    for (Q candidate : candidateList) {
                        Map<Term, Term> renaming = getRenameVariableSubstitution(candidate, zVariable + pos);
                        candidateRenaming.add(renaming);
                        headAtoms.add((Atom) Logic.applySubstitution(candidate.getHeadAtom(0), renaming));
                        pos++;
                    }

                    Map<Term, Term> mgu = Logic.getVariableSubstitution(skolemAtoms, headAtoms);
                    if (mgu == null) {
                        stats.incrHyperResolutionFailureCount();
                        // unification failed -> continue with next candidate list
                        continue;
                    }

                    // create the new body and head
                    Set<Atom> newBody = new HashSet<>(resolvedRightTGD.getBodySet());
                    newBody.removeAll(skolemAtoms);
                    pos = 0;
                    for (Q candidate : candidateList) {
                        Map<Term, Term> renaming = candidateRenaming.get(pos);
                        pos++;
                        for (Atom atom : candidate.getBodySet())
                            newBody.add((Atom) Logic.applySubstitution(atom, renaming));
                    }

                    Set<Atom> newHead = new HashSet<>();
                    newHead.add(resolvedRightTGD.getHeadAtom(0));

                    Q newTGD = factory.computeVNF(factory.create(Logic.applyMGU(newBody, mgu), Logic.applyMGU(newHead, mgu)), eVariable, uVariable);

                    resolvedTGDs.add(newTGD);

                    if (SaturationUtils.subsumed(resolvedRightTGD, newTGD)) {
                        break;
                    }
                }
            } else {
                stats.incrHyperResolutionCount();
                resolvedTGDs.add(factory.computeVNF(resolvedRightTGD, eVariable, uVariable));
            }
        } else {
            stats.incrHyperResolutionCount();
            stats.incrHyperResolutionFailureCount();
        }

        return resolvedTGDs;
    }

    private List<List<Q>> computeCandidatesForSkolemAtoms(UnificationIndex<Q> nonFullTGDsIndex,
            List<Atom> skolemAtoms) {

        List<Collection<Q>> candidatesPerBodyAtoms = new ArrayList<>();

        for (Atom atom : skolemAtoms) {
            Collection<Q> candidates = nonFullTGDsIndex.get(atom);
            if (!candidates.isEmpty())
                candidatesPerBodyAtoms.add(candidates);
            else
                return new ArrayList<>();
        }

        return SaturationUtils.getProduct(candidatesPerBodyAtoms);
    }

    /**
     * @param leftTGD  rule in VNF
     * @param rightTGD rule in VNF
     * @param B        - an atom in rightTGD's body
     */
    private Q resolveOn(Q leftTGD, Q rightTGD, Atom B) {

        Atom H = leftTGD.getHeadAtom(0);

        // quit if the atoms predicates are differents
        if (!H.getPredicate().equals(B.getPredicate()))
            return null;

        Map<Term, Term> renamingSubstitution = getRenameVariableSubstitution(leftTGD);
        Map<Term, Term> mgu = Logic.getMGU(B, (Atom) Logic.applySubstitution(H, renamingSubstitution));

        if (!renamingCache.containsKey(leftTGD))
            renamingCache.put(leftTGD, (Q) Logic.applySubstitution(leftTGD, renamingSubstitution));

        leftTGD = renamingCache.get(leftTGD);

        if (App.logger.isLoggable(Level.FINE))
            App.logger.fine("mgu of " + B + " and " + H + ": " + mgu);

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
