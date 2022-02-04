package uk.ac.ox.cs.gsat.satalg;

import uk.ac.ox.cs.gsat.fol.OrderedSkGTGD;
import uk.ac.ox.cs.gsat.fol.TGDFactory;
import uk.ac.ox.cs.pdq.fol.Atom;

class OrderedSkolemSat extends AbstractSkolemSat<OrderedSkGTGD> {

    private static final String NAME = "OrderedSkSat";

    OrderedSkolemSat(SaturationConfig config) {
        super(TGDFactory.getOrderedSkGTGDInstance(config.isSortedVNF()), NAME, config);
    }

    @Override
	protected Atom[] getUnifiableBodyAtoms(OrderedSkGTGD rightTGD) {
        return rightTGD.getMaxOrSelectedAtoms();
	}

	@Override
    protected boolean isRightTGD(OrderedSkGTGD newTGD) {
        return newTGD.areMaxAtomInBody();
    }

    @Override
    protected boolean isLeftTGD(OrderedSkGTGD newTGD) {
        return !newTGD.areMaxAtomInBody();
    }
}
