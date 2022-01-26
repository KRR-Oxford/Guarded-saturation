package uk.ac.ox.cs.gsat.satalg.stats;

import java.util.Locale;

import uk.ac.ox.cs.gsat.App;
import uk.ac.ox.cs.gsat.fol.GTGD;
import uk.ac.ox.cs.gsat.subsumers.Subsumer;

public class EvolveStatistics<Q extends GTGD> extends SaturationStatistics<Q>{

    protected int evolvedEqualsCount = 0;
    protected int stopBecauseSubsumedCount = 0;
    protected long evolveTime = 0;
    protected int evolveCount = 0;

    public EvolveStatistics(String saturationName) {
        super(saturationName);
    }

    public void log(Subsumer<Q> leftTGDsSubsumer, Subsumer<Q> rightTGDsSubsumer) {

        super.log(leftTGDsSubsumer, rightTGDsSubsumer);
        
        App.logger.info("Stop because subsumed: " + stopBecauseSubsumedCount);
        App.logger.info("evolved equals to current: " + evolvedEqualsCount);
        App.logger.info("evolved time: " + String.format(Locale.UK, "%.0f", evolveTime / 1E6) + " ms = "
                + String.format(Locale.UK, "%.2f", evolveTime / 1E9) + " s");
        App.logger.info("evolve count: " + evolveCount);

    }

    public void incrStopBecauseSubsumedCount() {
        stopBecauseSubsumedCount++;
    }

    public void incrEvolveCount() {
        evolveCount++;
    }

    public void incrEvolveTime(long incr) {
        evolveTime += incr;
    }

    public void incrEvolvedEqualsCount() {
        evolvedEqualsCount++;
    }

    public static <P extends GTGD> SaturationStatisticsFactory<P> getFactory() {
        return new Factory<P>();
    }

    private static class Factory<P extends GTGD> implements SaturationStatisticsFactory<P> {

        @Override
        public EvolveStatistics<P> create(String name) {
            return new EvolveStatistics<P>(name);
        }
    }
}
