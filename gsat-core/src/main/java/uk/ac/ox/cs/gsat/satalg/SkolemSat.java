package uk.ac.ox.cs.gsat.satalg;

import uk.ac.ox.cs.gsat.fol.SkGTGD;
import uk.ac.ox.cs.gsat.fol.TGDFactory;
import uk.ac.ox.cs.pdq.fol.Atom;

/**
 * Skolemized Saturation based on evolve as defined in Kevin Kappelmann's thesis
 * This saturation is inspired by the resolution algorithm without ordering 
 * The input guarded TGDs are first translated into single head Skolemized TGDs.
 * 
 * The evolve function takes as inputs: 
 * - left: a TGD having an head atom with a function term and a body without any 
 * - right: any TGD is either full or contains at least one function term in its body
 */

public class SkolemSat extends AbstractSkolemSat<SkGTGD> {

    private static final TGDFactory<SkGTGD> FACTORY = TGDFactory.getSkGTGDInstance();
    private static final String NAME = "SkSat";
    private static final SkolemSat INSTANCE = new SkolemSat();

    private SkolemSat() {
        super(FACTORY, NAME);
    }

    public static SkolemSat getInstance() {
        return INSTANCE;
    }

    @Override
    protected Atom[] getUnifiableBodyAtoms(SkGTGD rightTGD) {
        if (rightTGD.isFunctional()) {
            return rightTGD.getFunctionalBodyAtoms();
        } else {
            return new Atom[] { rightTGD.computeGuard() };
        }
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
}
