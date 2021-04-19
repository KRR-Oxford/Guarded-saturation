package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uk.ac.ox.cs.gsat.subsumers.Subsumer;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.FunctionTerm;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Term;

public class SkolemizedSat extends AbstractGSat {

    private static final SkolemizedSat INSTANCE = new SkolemizedSat();

    private SkolemizedSat() {
    }

    public static SkolemizedSat getInstance() {
        return INSTANCE;
    }

    @Override
    protected void initialization(Collection<GTGD> selectedTGDs, Set<GTGD> fullTGDsSet, Collection<GTGD> newNonFullTGDs,
            Map<Predicate, Set<GTGD>> fullTGDsMap, Subsumer<GTGD> fullTGDsSubsumer,
            Subsumer<GTGD> nonFullTGDsSubsumer) {
        for (GTGD tgd : selectedTGDs)
            for (GTGD shnf : FACTORY.computeSHNF(tgd)) {
                GTGD currentGTGD = FACTORY.computeSkolemized(shnf);
                currentGTGD = FACTORY.computeVNF(currentGTGD, eVariable, uVariable);

                if (!isFunctional(currentGTGD)) {
                    addFullTGD(currentGTGD, fullTGDsMap, fullTGDsSet);
                    fullTGDsSubsumer.add(currentGTGD);
                }

                if (isFunctional(currentGTGD)) {
                    nonFullTGDsSubsumer.add(currentGTGD);
                    newNonFullTGDs.add(currentGTGD);
                }
            }
    }

    @Override
    protected Collection<GTGD> getOutput(Collection<GTGD> rightSideTgds) {
        Collection<GTGD> output = new HashSet<>();

        for (GTGD tgd : rightSideTgds)
            if (isFullTGD(tgd))
                output.add(tgd);

        return output;
    }

    @Override
    protected Collection<Predicate> getUnifiableBodyPredicates(GTGD tgd) {
        Set<Predicate> result = new HashSet<>();

        if (isFullTGD(tgd))
            result.add(tgd.getGuard().getPredicate());

        // also need to consider the predicate of the functional atoms of the TGD body
        for (Atom atom : tgd.getBodyAtoms())
            if (isFunctional(atom.getTerms()))
                result.add(atom.getPredicate());

        return result;
    }

    @Override
    protected boolean addFullTGD(GTGD currentTGD, Map<Predicate, Set<GTGD>> fullTGDsMap, Set<GTGD> fullTGDsSet) {

        if (isFullTGD(currentTGD))
            fullTGDsMap.computeIfAbsent(currentTGD.getGuard().getPredicate(), k -> new HashSet<GTGD>()).add(currentTGD);

        // also need to consider the predicate of the functional atoms of the TGD body
        for (Atom atom : currentTGD.getBodyAtoms())
            if (isFunctional(atom.getTerms()))
                fullTGDsMap.computeIfAbsent(atom.getPredicate(), k -> new HashSet<GTGD>()).add(currentTGD);

        return fullTGDsSet.add(currentTGD);
    }

    @Override
    public Collection<GTGD> evolveNew(GTGD nftgd, GTGD ftgd) {

        Collection<GTGD> results = new HashSet<>();

        ftgd = evolveRename(ftgd);

        Collection<Atom> selectedBodyAtoms = new ArrayList<>();

        for (Atom a : ftgd.getAtoms())
            if (isFunctional(a.getTerms()))
                selectedBodyAtoms.add(a);

        if (isFullTGD(ftgd)) {
            Atom guard = ftgd.getGuard();
            selectedBodyAtoms.add(guard);
        }

        for (Atom B : selectedBodyAtoms)
            for (Atom H : nftgd.getHeadAtoms()) {

                Map<Term, Term> mgu = Logic.getMGU(B, H);

                if (mgu != null) {

                    Set<Atom> new_body = new HashSet<>();
                    for (Atom batom : ftgd.getBodySet())
                        if (!batom.equals(B))
                            new_body.add((Atom) Logic.applySubstitution(batom, mgu));

                    for (Atom batom : nftgd.getBodySet())
                        new_body.add((Atom) Logic.applySubstitution(batom, mgu));

                    Set<Atom> new_head = new HashSet<>();
                    for (Atom hatom : ftgd.getHeadSet())
                        new_head.add((Atom) Logic.applySubstitution(hatom, mgu));

                    GTGD new_tgd = new GTGD(new_body, new_head);
                    new_tgd = FACTORY.computeVNF(new_tgd, eVariable, uVariable);

                    results.add(new_tgd);
                }
            }

        return results;
    }

    /**
     * We only remove from the right side of evolve the TGDs having no function term in body but having some in head
     */
    @Override
    protected boolean isFull(GTGD newTGD) {
        return !isFunctional(newTGD.getHeadAtoms()) || isFunctional(newTGD.getBodyAtoms());
    }

    @Override
    protected boolean isNonFull(GTGD newTGD) {
        return isFunctional(newTGD.getHeadAtoms());
    }

    private boolean isFullTGD(GTGD tgd) {
        return !isFunctional(tgd);
    }

    private boolean isFunctional(GTGD currentGTGD) {
        return isFunctional(currentGTGD.getTerms());
    }

    private boolean isFunctional(Atom[] atoms) {
        for (Atom atom : atoms)
            if (isFunctional(atom.getTerms()))
                return true;

        return false;
    }

    private boolean isFunctional(Term[] terms) {
        for (Term term : terms)
            if (term instanceof FunctionTerm)
                return true;

        return false;
    }

}
