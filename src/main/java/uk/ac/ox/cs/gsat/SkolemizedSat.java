package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.FunctionTerm;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Term;

/**
 * Skolemized Saturation based on evolve
 * This saturation is inspired by the resolution algorithm
 * The input guarded TGDs are first translated into 
 * single head Skolemized TGDs.
 * 
 * The evolve function takes as inputs: 
 * - left: a TGD having an head atom with a function term and a body without any
 * - right: any TGD is either full or contains at least one function term in its body
 */

public class SkolemizedSat extends EvolveBasedSat {

    private static final String NAME = "SkSat";
    private static final SkolemizedSat INSTANCE = new SkolemizedSat();

    private SkolemizedSat() {
        super(NAME);
    }

    public static SkolemizedSat getInstance() {
        return INSTANCE;
    }

    @Override
    protected Collection<GTGD> transformInputTGDs(Collection<GTGD> inputTGDs) {
        Collection<GTGD> result = new ArrayList<>();

        for(GTGD tgd : inputTGDs)
            for (GTGD shnf : FACTORY.computeSHNF(tgd)) {
                GTGD currentGTGD = FACTORY.computeSkolemized(shnf);
                result.add(FACTORY.computeVNF(currentGTGD, eVariable, uVariable));
            }
        return result;
    }

    @Override
    protected Collection<GTGD> getOutput(Collection<GTGD> rightTGDs) {
        Collection<GTGD> output = new HashSet<>();

        for (GTGD tgd : rightTGDs)
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
    public Collection<GTGD> evolveNew(GTGD leftTGD, GTGD rightTGD) {

        Collection<GTGD> results = new HashSet<>();

        rightTGD = evolveRename(rightTGD);

        Collection<Atom> selectedBodyAtoms = new ArrayList<>();

        for (Atom a : rightTGD.getAtoms())
            if (isFunctional(a.getTerms()))
                selectedBodyAtoms.add(a);

        if (isFullTGD(rightTGD)) {
            Atom guard = rightTGD.getGuard();
            selectedBodyAtoms.add(guard);
        }

        for (Atom B : selectedBodyAtoms)
            for (Atom H : leftTGD.getHeadAtoms()) {

                Map<Term, Term> mgu = Logic.getMGU(B, H);

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
    protected boolean isRightTGD(GTGD newTGD) {
        return !isFunctional(newTGD.getHeadAtoms()) || isFunctional(newTGD.getBodyAtoms());
    }

    @Override
    protected boolean isLeftTGD(GTGD newTGD) {
        return isFunctional(newTGD.getHeadAtoms()) && !isFunctional(newTGD.getBodyAtoms());
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
