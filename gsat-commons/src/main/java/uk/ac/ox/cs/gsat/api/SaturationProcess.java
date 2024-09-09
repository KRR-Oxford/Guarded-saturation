package uk.ac.ox.cs.gsat.api;

import java.util.Collection;

import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.statistics.StatisticsCollector;

/**
 * A saturation process includes a {@link SaturationAlgorithm} and a {@link uk.ac.ox.cs.gsat.api.io.TGDProcessor} that the input needs.
 */
public interface SaturationProcess {

    /**
     * Parse and transform the TGDs from the input file and compute its saturation.
     */
    public Collection<? extends TGD> saturate(String processName, String inputPath) throws Exception;

    public Collection<? extends TGD> saturate(String inputPath) throws Exception;

    public void setStatisticCollector(StatisticsCollector<SaturationStatColumns> statisticsCollector);
}
