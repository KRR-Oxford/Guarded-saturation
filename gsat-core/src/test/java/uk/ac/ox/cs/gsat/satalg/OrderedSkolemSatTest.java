package uk.ac.ox.cs.gsat.satalg;

import uk.ac.ox.cs.gsat.fol.OrderedSkGTGD;
import uk.ac.ox.cs.gsat.satalg.OrderedSkolemSat;

public class OrderedSkolemSatTest extends AbstractSkolemSatTest<OrderedSkGTGD> {
    public OrderedSkolemSatTest() {
        super(OrderedSkolemSat.getInstance());
    }
}
