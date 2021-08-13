package uk.ac.ox.cs.gsat;

import java.util.Locale;

import uk.ac.ox.cs.gsat.subsumers.Subsumer;

public class EvolveStatistics<Q extends GTGD> {

    protected int initialRightTGDs = 0;
    protected int initialLeftTGDs = 0;
    protected int newRightCount = 0;
    protected int newLeftCount = 0;
    protected int evolvedEqualsCount = 0;
    protected int stopBecauseSubsumedCount = 0;
    protected long evolveTime = 0;
    protected int evolveCount = 0;
    protected long startTime = 0;
    protected String saturationName;
    protected long stopTime = 0;
    protected int discardedTautologyCount;
    protected long forwardSubsumptionTime;
    protected long backwardSubsumptionTime;

    public EvolveStatistics(String saturationName) {
        this.saturationName = saturationName;
    }

    public void log(Subsumer<Q> leftTGDsSubsumer, Subsumer<Q> rightTGDsSubsumer) {
        long totalTime = stopTime - startTime;

        App.logger.info("# initial TGDs: " + initialRightTGDs + " , " + initialLeftTGDs);
        App.logger.info(saturationName + " total time : " + String.format(Locale.UK, "%.0f", totalTime / 1E6) + " ms = "
                + String.format(Locale.UK, "%.2f", totalTime / 1E9) + " s");
        App.logger.info("Subsumed elements : "
                + (rightTGDsSubsumer.getNumberSubsumed() + leftTGDsSubsumer.getNumberSubsumed()));
        App.logger.info("Filter discarded elements : "
                + (rightTGDsSubsumer.getFilterDiscarded() + leftTGDsSubsumer.getFilterDiscarded()));
        App.logger.info("subsumption time: "
                + String.format(Locale.UK, "%.0f", (forwardSubsumptionTime + backwardSubsumptionTime) / 1E6) + " ms = "
                + String.format(Locale.UK, "%.2f", (forwardSubsumptionTime + backwardSubsumptionTime) / 1E9) + " s");
        App.logger.info("forward subsumption time: " + String.format(Locale.UK, "%.0f", forwardSubsumptionTime / 1E6)
                + " ms = " + String.format(Locale.UK, "%.2f", forwardSubsumptionTime / 1E9) + " s");
        App.logger.info("backward subsumption time: " + String.format(Locale.UK, "%.0f", backwardSubsumptionTime / 1E6)
                + " ms = " + String.format(Locale.UK, "%.2f", backwardSubsumptionTime / 1E9) + " s");

        App.logger.info("Derived full/non full TGDs: " + newRightCount + " , " + newLeftCount);
        App.logger.info("Stop because subsumed: " + stopBecauseSubsumedCount);
        App.logger.info("evolved equals to current: " + evolvedEqualsCount);
        App.logger.info("evolved time: " + String.format(Locale.UK, "%.0f", evolveTime / 1E6) + " ms = "
                + String.format(Locale.UK, "%.2f", evolveTime / 1E9) + " s");
        App.logger.info("evolve count: " + evolveCount);

        App.logger.info("discarded tautology count: " + discardedTautologyCount);
    }

    public void start() {
        startTime = System.nanoTime();
    }

    public void stop() {
        stopTime = System.nanoTime();
    }

    public long getStartTime() {
        return startTime;
    }

    public int getInitialLeftTGDs() {
        return initialLeftTGDs;
    }

    public void setInitialLeftTGDs(int initialLeftTGDs) {
        this.initialLeftTGDs = initialLeftTGDs;
    }

    public int getInitialRightTGDs() {
        return initialRightTGDs;
    }

    public void setInitialRightTGDs(int initialRightTGDs) {
        this.initialRightTGDs = initialRightTGDs;
    }

    public void incrStopBecauseSubsumedCount() {
        stopBecauseSubsumedCount++;
    }

    public void incrEvolveCount() {
        evolveCount++;
    }

    public void newRightTGD(Q tgd) {
        newRightCount++;
    }

    public void newLeftTGD(Q tgd) {
        newLeftCount++;
    }

    public void incrEvolveTime(long incr) {
        evolveTime += incr;
    }

    public void incrEvolvedEqualsCount() {
        evolvedEqualsCount++;
    }

    public void incrDiscardedTautologyCount() {
        discardedTautologyCount++;
    }

    public void incrForwardSubsumptionTime(long incr) {
        forwardSubsumptionTime += incr;
    }

    public void incrBackwardSubsumptionTime(long incr) {
        backwardSubsumptionTime += incr;
    }

    public static <P extends GTGD> EvolveStatisticsFactory<P> getFactory() {
        return new Factory<P>();
    }

    private static class Factory<P extends GTGD> implements EvolveStatisticsFactory<P> {

        @Override
        public EvolveStatistics<P> create(String name) {
            return new EvolveStatistics<P>(name);
        }
    }
}
