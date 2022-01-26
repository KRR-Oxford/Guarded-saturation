package uk.ac.ox.cs.gsat.satalg;

import uk.ac.ox.cs.gsat.fol.OrderedSkGTGD;
import uk.ac.ox.cs.gsat.fol.TGDFactory;
import uk.ac.ox.cs.gsat.satalg.AbstractSkolemSat;
import uk.ac.ox.cs.pdq.fol.Atom;

public class OrderedSkolemSat extends AbstractSkolemSat<OrderedSkGTGD> {

    private static final TGDFactory<OrderedSkGTGD> FACTORY = TGDFactory.getOrderedSkGTGDInstance();
    private static final String NAME = "OrderedSkSat";
    private static final OrderedSkolemSat INSTANCE = new OrderedSkolemSat();

    private OrderedSkolemSat() {
        super(FACTORY, NAME);
    }

    public static OrderedSkolemSat getInstance() {
        return INSTANCE;
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
