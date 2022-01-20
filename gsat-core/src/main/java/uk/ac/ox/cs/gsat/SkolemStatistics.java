package uk.ac.ox.cs.gsat;

import uk.ac.ox.cs.gsat.subsumers.Subsumer;

public class SkolemStatistics<Q extends SkGTGD> extends EvolveStatistics<Q> {

    protected int maxSkolemAtomInBody = 0;
    protected int leftTGDWithSkolemAtomInBodyCount = 0;
    protected int rightTGDWithSkolemAtomInBodyCount = 0;
    protected int tgdWithMultipleSkolemAtomInBodyCount = 0;
    
    public SkolemStatistics(String saturationName) {
        super(saturationName);
    }

    @Override
    public void newRightTGD(Q tgd) {
        super.newRightTGD(tgd);
        
        if (tgd.getFunctionalBodyAtoms().length > 0) {
            rightTGDWithSkolemAtomInBodyCount++;
            maxSkolemAtomInBody = Math.max(maxSkolemAtomInBody, tgd.getFunctionalBodyAtoms().length);
            if (tgd.getFunctionalBodyAtoms().length > 1)
                tgdWithMultipleSkolemAtomInBodyCount++;

        }
    }

    
    @Override
    public void newLeftTGD(Q tgd) {
        super.newLeftTGD(tgd);

        if (tgd.getFunctionalBodyAtoms().length > 0) {
            leftTGDWithSkolemAtomInBodyCount++;
            maxSkolemAtomInBody = Math.max(maxSkolemAtomInBody, tgd.getFunctionalBodyAtoms().length);
            if (tgd.getFunctionalBodyAtoms().length > 1)
                tgdWithMultipleSkolemAtomInBodyCount++;
        }
    }

    @Override
    public void log(Subsumer<Q> leftTGDsSubsumer, Subsumer<Q> rightTGDsSubsumer) {
        super.log(leftTGDsSubsumer, rightTGDsSubsumer);
        App.logger.info("maximum number of Skolem atoms in body: " + maxSkolemAtomInBody);
        App.logger.info("left derived TGD with Skolem in the body: " + leftTGDWithSkolemAtomInBodyCount);
        App.logger.info("right derived TGD with Skolem in the body: " + rightTGDWithSkolemAtomInBodyCount);
        App.logger.info("derived TGD with more than two Skolem atoms in the body: " + tgdWithMultipleSkolemAtomInBodyCount);
    }

    public static <P extends SkGTGD> SaturationStatisticsFactory<P> getSkFactory() {
        return new Factory<P>();
    }

    private static class Factory<P extends SkGTGD> implements SaturationStatisticsFactory<P> {
        @Override
        public EvolveStatistics<P> create(String name) {
            return new SkolemStatistics<P>(name);
        }
    }
}
