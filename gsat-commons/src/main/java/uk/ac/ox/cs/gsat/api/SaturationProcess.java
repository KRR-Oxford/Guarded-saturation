package uk.ac.ox.cs.gsat.api;

import java.util.Collection;

import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.statistics.StatisticsCollector;

/**
 * A saturation process includes a saturation algorithm and the preprocessing transformations that the input needs 
 */
public interface SaturationProcess {

    public Collection<? extends TGD> saturate(String processName, String inputPath) throws Exception;

    public Collection<? extends TGD> saturate(String inputPath) throws Exception;

    public void setStatisticCollector(StatisticsCollector<SaturationStatColumns> statisticsCollector);
}
