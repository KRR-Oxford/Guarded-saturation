package uk.ac.ox.cs.gsat;

import java.util.Locale;

import uk.ac.ox.cs.gsat.subsumers.Subsumer;

public class HyperResolutionStatistics<Q extends GTGD> extends SaturationStatistics<Q> {

    private int hyperResolutionCount;
    private long hyperResolutionTime;
    private int hyperResolutionFailureCount;

    public HyperResolutionStatistics(String saturationName) {
        super(saturationName);
    }

    public void log(Subsumer<Q> leftTGDsSubsumer, Subsumer<Q> rightTGDsSubsumer) {
        super.log(leftTGDsSubsumer, rightTGDsSubsumer);

        App.logger.info("hyperresolution time: " + String.format(Locale.UK, "%.0f", hyperResolutionTime / 1E6) + " ms = "
                + String.format(Locale.UK, "%.2f", hyperResolutionTime / 1E9) + " s");

        App.logger.info("hyperresolution count: " + hyperResolutionCount);
        App.logger.info("hyperresolution failure count: " + hyperResolutionFailureCount);
        
    }

    public void incrHyperResolutionCount() {
        hyperResolutionCount++;
    }

    public void incrHyperResolutionTime(long l) {
        hyperResolutionTime += l;
    }

    public void incrHyperResolutionFailureCount() {
        hyperResolutionFailureCount++;
    }

    public static <P extends GTGD> SaturationStatisticsFactory<P> getFactory() {
        return new Factory<P>();
    }

    private static class Factory<P extends GTGD> implements SaturationStatisticsFactory<P> {

        @Override
        public HyperResolutionStatistics<P> create(String name) {
            return new HyperResolutionStatistics<P>(name);
        }
    }

}
