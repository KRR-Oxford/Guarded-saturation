package uk.ac.ox.cs.gsat.satalg;

import uk.ac.ox.cs.gsat.fol.SkGTGD;

public class HyperResolutionBasedSatTest extends AbstractSkolemSatTest<SkGTGD> {
    public HyperResolutionBasedSatTest() {
        super(new HyperResolutionBasedSat(new SaturationAlgorithmConfiguration()));
    }
}
