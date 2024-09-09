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
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import uk.ac.ox.cs.gsat.Log;
import uk.ac.ox.cs.gsat.api.SaturationStatColumns;
import uk.ac.ox.cs.gsat.fol.Logic;
import uk.ac.ox.cs.gsat.fol.SkGTGD;
import uk.ac.ox.cs.gsat.fol.TGDFactory;
import uk.ac.ox.cs.gsat.subsumers.Subsumer;
import uk.ac.ox.cs.gsat.unification.UnificationIndex;
import uk.ac.ox.cs.gsat.unification.UnificationIndexType;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.FunctionTerm;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

public class HyperResolutionBasedSat extends AbstractSaturation<SkGTGD> {

    private static final String NAME = "HyperSat";


    private Map<SkGTGD, SkGTGD> renamingCache = new HashMap<>();

    protected HyperResolutionBasedSat(SaturationAlgorithmConfiguration config) {
        super(NAME, TGDFactory.getSkGTGDInstance(config.isSortedVNF()),
              UnificationIndexType.ATOM_PATH_INDEX,
              UnificationIndexType.ATOM_PATH_INDEX, config);
    }

    @Override
    protected void process(Set<SkGTGD> leftTGDsSet, Set<SkGTGD> rightTGDsSet, Collection<SkGTGD> newLeftTGDs,
            Collection<SkGTGD> newRightTGDs, UnificationIndex<SkGTGD> leftIndex, UnificationIndex<SkGTGD> rightIndex,
            Subsumer<SkGTGD> leftTGDsSubsumer, Subsumer<SkGTGD> rightTGDsSubsumer, Set<Predicate> bodyPredicates,
            String processName) throws TimeoutException {

        while (!newLeftTGDs.isEmpty() || !newRightTGDs.isEmpty()) {

            checkTimeout(statsCollector.total(processName));

            Collection<SkGTGD> resolved = new HashSet<>();

            if (!newRightTGDs.isEmpty()) {

                Iterator<SkGTGD> iterator = newRightTGDs.iterator();
                SkGTGD rightTGD = iterator.next();
                iterator.remove();
                addRightTGD(rightTGD, rightIndex, rightTGDsSet);

                if (Log.GLOBAL.isLoggable(Level.FINE))
                    Log.GLOBAL.fine("picked full TGD: " + rightTGD);

                statsCollector.tick(processName, SaturationStatColumns.OTHER_TIME);
                resolved.addAll(hyperresolveWithNewFullTGD(leftTGDsSet, leftIndex, rightTGD, processName));
                statsCollector.tick(processName, SaturationStatColumns.EVOL_TIME);
            } else if (!newLeftTGDs.isEmpty()) {
                Iterator<SkGTGD> iterator = newLeftTGDs.iterator();
                SkGTGD leftTGD = iterator.next();
                iterator.remove();
                addLeftTGD(leftTGD, leftIndex, leftTGDsSet);

                if (Log.GLOBAL.isLoggable(Level.FINE))
                    Log.GLOBAL.fine("picked non-full TGD: " + leftTGD);

                statsCollector.tick(processName, SaturationStatColumns.OTHER_TIME);
                resolved.addAll(hyperresolveWithNewNonFullTGD(leftTGDsSet, leftIndex, rightIndex, leftTGD, processName));
                statsCollector.tick(processName, SaturationStatColumns.EVOL_TIME);
            }

            // we update the structures with the TGDs to add
            for (SkGTGD newTGD : resolved) {
                if (Log.GLOBAL.isLoggable(Level.FINE))
                    Log.GLOBAL.fine("newTGD: " + newTGD);

                if (isRightTGD(newTGD)) {
                    reportNewRightTGD(processName, newTGD);
                    addNewTGD(newTGD, true, newRightTGDs, rightTGDsSubsumer, rightIndex, rightTGDsSet, processName);
                }

                if (isLeftTGD(newTGD)) {
                    reportNewLeftTGD(processName, newTGD);
                    addNewTGD(newTGD, false, newLeftTGDs, leftTGDsSubsumer, leftIndex, leftTGDsSet, processName);
                }

            }
            Log.GLOBAL.fine("======================== \n");

        }

    }

    /**
     * hyperresolution with a new non-full TGD requires to try to hyperresolve using every full TGDs such that 
     * a body atom of full TGDs is resolvable with the non-full TGD's head. This body atom is not necessary a guard, so we have to retrieve the non-full TGDs to resolve the guard first.
     * There is an optimization to reduce the number of these last non-full TGDs using the new one.
     */    
    private Collection<? extends SkGTGD> hyperresolveWithNewNonFullTGD(Set<SkGTGD> nonFullTGDsSet,
            UnificationIndex<SkGTGD> nonFullTGDsIndex, UnificationIndex<SkGTGD> fullTGDsIndex, SkGTGD nonFullTGD,
            String processName) {
    
        Collection<SkGTGD> resolved = new HashSet<>();
    
        for (SkGTGD fullTGD : fullTGDsIndex.get(nonFullTGD.getHeadAtom(0))) {
            Set<Variable> fullTGDVariables = Set.of(fullTGD.getUniversal());
            for (Atom bodyAtom : fullTGD.getBodyAtoms()) {
                // two cases whether bodyAtom is a guard or not
    
                boolean isGuard = Set.of(bodyAtom.getVariables()).containsAll(fullTGDVariables);
    
                // 1. bodyAtom is a guard
                if (isGuard) {
                    resolved.addAll(hyperresolveStartingWith(nonFullTGDsSet, nonFullTGDsIndex, nonFullTGD,
                                                             fullTGD, bodyAtom, processName));
                } else {
                    // 2. bodyAtom is not a guard, so resolving on this atom will leave some Skolem functions
                    SkGTGD resolvedRightTGD = resolveOn(nonFullTGD, fullTGD, bodyAtom);
                    if (resolvedRightTGD != null) {
                        resolvedRightTGD = factory.computeVNF(resolvedRightTGD, eVariable, uVariable);
                        Atom resolvedGuard = resolvedRightTGD.getGuard();
    
                        for (SkGTGD nonFullTGDForGuard : nonFullTGDsIndex.get(resolvedGuard)) {
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
                                        nonFullTGDForGuard, resolvedRightTGD, resolvedGuard, processName));
                            }  else {
                                statsCollector.incr(processName, SaturationStatColumns.EVOL_COUNT);
                                statsCollector.incr(processName, SaturationStatColumns.HYPER_FAILURE);
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
    private Collection<? extends SkGTGD> hyperresolveWithNewFullTGD(Set<SkGTGD> nonFullTGDsSet,
            UnificationIndex<SkGTGD> nonFullTGDsIndex, SkGTGD fullTGD, String processName) {

        Collection<SkGTGD> resolved = new HashSet<>();
        for (SkGTGD nonFullTGD : nonFullTGDsIndex.get(fullTGD.getGuard()))
            resolved.addAll(hyperresolveStartingWith(nonFullTGDsSet, nonFullTGDsIndex, nonFullTGD, fullTGD, fullTGD.getGuard(), processName));

        return resolved;
    }

    /**
     * hyperresolving starting with a non full TGD and a full TGD or a partially resolved full TGD and an atom guarding the full TGD by:
     * 1. resolve the guard full TGDs with the head of the non full TGD 
     * 2. hyperresolving the resulting TGD, if it contains Skolem in body, otherwise return the resulting full TGD
     */

    private Collection<SkGTGD> hyperresolveStartingWith(Set<SkGTGD> nonFullTGDsSet, UnificationIndex<SkGTGD> nonFullTGDsIndex, SkGTGD nonFullTGD,
                                                   SkGTGD fullTGD, Atom fullTGDGuard, String processName) {
        Collection<SkGTGD> resolvedTGDs = new HashSet<>();
        SkGTGD resolvedRightTGD = resolveOn(nonFullTGD, fullTGD, fullTGDGuard);
        if (Log.GLOBAL.isLoggable(Level.FINE)) {
            Log.GLOBAL.fine("first resolved TGD: " + "\nnon full: " + nonFullTGD + "\nfull: " + fullTGD + "\nresolved: " + resolvedRightTGD);
        }

        if (resolvedRightTGD != null) {
            resolvedRightTGD = factory.computeVNF(resolvedRightTGD, eVariable, uVariable);

            List<Atom> skolemAtoms = Arrays.asList(resolvedRightTGD.getFunctionalBodyAtoms());
            if (skolemAtoms.size() > 0) {
                // the resolved TGD contains Skolem functions in its body 

                List<List<SkGTGD>> nonFullTGDcandidatesForSkolemAtoms = computeCandidatesForSkolemAtoms(nonFullTGDsIndex, skolemAtoms);
                if (Log.GLOBAL.isLoggable(Level.FINE)) {
                    Log.GLOBAL.fine("skolem atom candidates: " + nonFullTGDcandidatesForSkolemAtoms);
                }

                for (List<SkGTGD> candidateList : nonFullTGDcandidatesForSkolemAtoms) {
                    statsCollector.incr(processName, SaturationStatColumns.EVOL_COUNT);

                    List<Atom> headAtoms = new ArrayList<>();
                    List<Map<Term, Term>> candidateRenaming = new ArrayList<>();
                    int pos = 1;
                    for (SkGTGD candidate : candidateList) {
                        Map<Term, Term> renaming = getRenameVariableSubstitution(candidate, zVariable + pos);
                        candidateRenaming.add(renaming);
                        headAtoms.add((Atom) Logic.applySubstitution(candidate.getHeadAtom(0), renaming));
                        pos++;
                    }

                    Map<Term, Term> mgu = Logic.getVariableSubstitution(skolemAtoms, headAtoms);
                    if (mgu == null) {
                        statsCollector.incr(processName, SaturationStatColumns.HYPER_FAILURE);
                        // unification failed -> continue with next candidate list
                        continue;
                    }

                    // create the new body and head
                    Set<Atom> newBody = new HashSet<>(resolvedRightTGD.getBodySet());
                    newBody.removeAll(skolemAtoms);
                    pos = 0;
                    for (SkGTGD candidate : candidateList) {
                        Map<Term, Term> renaming = candidateRenaming.get(pos);
                        pos++;
                        for (Atom atom : candidate.getBodySet())
                            newBody.add((Atom) Logic.applySubstitution(atom, renaming));
                    }

                    Set<Atom> newHead = new HashSet<>();
                    newHead.add(resolvedRightTGD.getHeadAtom(0));

                    SkGTGD newTGD = factory.computeVNF(factory.create(Logic.applyMGU(newBody, mgu), Logic.applyMGU(newHead, mgu)), eVariable, uVariable);

                    resolvedTGDs.add(newTGD);

                    if (SaturationUtils.subsumed(resolvedRightTGD, newTGD)) {
                        break;
                    }
                }
            } else {
                statsCollector.incr(processName, SaturationStatColumns.EVOL_COUNT);
                resolvedTGDs.add(factory.computeVNF(resolvedRightTGD, eVariable, uVariable));
            }
        } else {
            statsCollector.incr(processName, SaturationStatColumns.EVOL_COUNT);
            statsCollector.incr(processName, SaturationStatColumns.HYPER_FAILURE);
        }

        return resolvedTGDs;
    }

    private List<List<SkGTGD>> computeCandidatesForSkolemAtoms(UnificationIndex<SkGTGD> nonFullTGDsIndex,
            List<Atom> skolemAtoms) {

        List<Collection<SkGTGD>> candidatesPerBodyAtoms = new ArrayList<>();

        for (Atom atom : skolemAtoms) {
            Collection<SkGTGD> candidates = nonFullTGDsIndex.get(atom);
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
    private SkGTGD resolveOn(SkGTGD leftTGD, SkGTGD rightTGD, Atom B) {

        Atom H = leftTGD.getHeadAtom(0);

        // quit if the atoms predicates are differents
        if (!H.getPredicate().equals(B.getPredicate()))
            return null;

        Map<Term, Term> renamingSubstitution = getRenameVariableSubstitution(leftTGD);
        Map<Term, Term> mgu = Logic.getMGU(B, (Atom) Logic.applySubstitution(H, renamingSubstitution));

        if (!renamingCache.containsKey(leftTGD))
            renamingCache.put(leftTGD, (SkGTGD) Logic.applySubstitution(leftTGD, renamingSubstitution));

        leftTGD = renamingCache.get(leftTGD);

        if (Log.GLOBAL.isLoggable(Level.FINE))
            Log.GLOBAL.fine("mgu of " + B + " and " + H + ": " + mgu);

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

            SkGTGD new_tgd = factory.create(new_body, new_head);

            return new_tgd;
        } else {
            return null;
        }
    }

    @Override
    protected Collection<SkGTGD> getOutput(Collection<SkGTGD> rightTGDs) {
        // ugly copy from AbstractSkolemSat
        Collection<SkGTGD> output = new HashSet<>();

        for (SkGTGD tgd : rightTGDs)
            if (!tgd.isFunctional())
                output.add(tgd);

        return output;

    }

    @Override
    protected Collection<SkGTGD> transformInputTGDs(Collection<SkGTGD> inputTGDs) {
        // ugly copy from AbstractSkolemSat
        Collection<SkGTGD> result = new ArrayList<>();

        for (SkGTGD tgd : inputTGDs) {

            Collection<SkGTGD> singleSkolemizedTGDs;
            switch (config.getSkolemizationType()) {
            case NAIVE:
                singleSkolemizedTGDs = factory.computeSingleHeadedSkolemized(tgd);
                break;
            case PROJ_ON_FRONTIER:
                singleSkolemizedTGDs = factory.computeSingleHeadSkolemizedOnFrontierVariable(tgd);
                break;
            default:
                String message = String.format("the skolemization type %s is not supported",
                        config.getSkolemizationType());
                throw new IllegalStateException(message);
            }

            for (SkGTGD shnf : singleSkolemizedTGDs) {
                result.add(factory.computeVNF(shnf, eVariable, uVariable));
            }
        }
        return result;
    }

    @Override
    protected boolean isLeftTGD(SkGTGD newTGD) {
        return newTGD.isNonFull();
    }

    @Override
    protected boolean isRightTGD(SkGTGD newTGD) {
        return !newTGD.isNonFull();
    }

    @Override
    protected Atom[] getUnifiableBodyAtoms(SkGTGD rightTGD) {
        return rightTGD.getBodyAtoms();
    }

}
