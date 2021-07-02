package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Term;

/**
 * Skolemized Saturation based on evolve This saturation is inspired by the
 * resolution algorithm The input guarded TGDs are first translated into single
 * head Skolemized TGDs.
 * 
 * The evolve function takes as inputs: - left: a TGD having an head atom with a
 * function term and a body without any - right: any TGD is either full or
 * contains at least one function term in its body
 */

public class SkolemizedSat extends EvolveBasedSat<SkGTGD> {

    protected static final TGDFactory<SkGTGD> FACTORY = TGDFactory.getSkGTGDInstance();
    private static final String NAME = "SkSat";
    private static final SkolemizedSat INSTANCE = new SkolemizedSat();

    private SkolemizedSat() {
        super(NAME);
    }

    public static SkolemizedSat getInstance() {
        return INSTANCE;
    }

    @Override
    protected Collection<SkGTGD> transformInputTGDs(Collection<SkGTGD> inputTGDs) {
        Collection<SkGTGD> result = new ArrayList<>();

        for (SkGTGD tgd : inputTGDs) {
            for (SkGTGD shnf : FACTORY.computeSHNF(tgd)) {
                result.add(FACTORY.computeVNF(shnf, eVariable, uVariable));
            }
        }
        return result;
    }

    @Override
    protected Collection<SkGTGD> getOutput(Collection<SkGTGD> rightTGDs) {
        Collection<SkGTGD> output = new HashSet<>();

        for (SkGTGD tgd : rightTGDs)
            if (isFullTGD(tgd))
                output.add(tgd);

        return output;
    }

    @Override
    protected Collection<Predicate> getUnifiableBodyPredicates(SkGTGD rightTGD) {
        Set<Predicate> result = new HashSet<>();

        if (rightTGD.isFunctional()) {
            for (Atom atom : rightTGD.getFunctionalBodyAtoms())
                result.add(atom.getPredicate());
        } else {
            result.add(rightTGD.computeGuard().getPredicate());
        }

        return result;
    }

    @Override
    public Collection<SkGTGD> evolveNew(SkGTGD leftTGD, SkGTGD rightTGD) {

        Collection<SkGTGD> results = new HashSet<>();

        rightTGD = evolveRename(rightTGD);

        Collection<Atom> selectedBodyAtoms = new ArrayList<>();
        if (rightTGD.isFunctional()) {
            for (Atom atom : rightTGD.getFunctionalBodyAtoms())
                selectedBodyAtoms.add(atom);
        } else {
            selectedBodyAtoms.add(rightTGD.computeGuard());
        }

        for (Atom B : selectedBodyAtoms) {
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

                    SkGTGD new_tgd = new SkGTGD(new_body, new_head);
                    new_tgd = FACTORY.computeVNF(new_tgd, eVariable, uVariable);

                    results.add(new_tgd);
                }
            }
        }
        return results;
    }

    /**
     * We only remove from the right side of evolve the TGDs having no function term
     * in body but having some in head
     */
    @Override
    protected boolean isRightTGD(SkGTGD newTGD) {
        return !newTGD.isNonFull();
    }

    @Override
    protected boolean isLeftTGD(SkGTGD newTGD) {
        return newTGD.isNonFull();
    }

    private boolean isFullTGD(SkGTGD tgd) {
        return !tgd.isNonFull() && !tgd.isFunctional();
    }

    @Override
    protected TGDFactory<SkGTGD> getTGDFactory() {
        return FACTORY;
    }

}
