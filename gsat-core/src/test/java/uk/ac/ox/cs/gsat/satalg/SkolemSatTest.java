package uk.ac.ox.cs.gsat.satalg;

import uk.ac.ox.cs.gsat.fol.SkGTGD;

public class SkolemSatTest extends AbstractSkolemSatTest<SkGTGD> {
    public SkolemSatTest() {
        super(new SkolemSat(new SaturationConfig()));
    }
}
