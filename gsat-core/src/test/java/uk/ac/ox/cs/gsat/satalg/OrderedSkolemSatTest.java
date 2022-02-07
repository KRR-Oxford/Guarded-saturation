package uk.ac.ox.cs.gsat.satalg;

import uk.ac.ox.cs.gsat.fol.OrderedSkGTGD;

public class OrderedSkolemSatTest extends AbstractSkolemSatTest<OrderedSkGTGD> {
    public OrderedSkolemSatTest() {
        super(new OrderedSkolemSat(new SaturationAlgorithmConfiguration()));
    }
}
