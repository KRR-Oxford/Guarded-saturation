package uk.ac.ox.cs.gsat.api;

import java.util.Collection;

import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.statistics.StatisticsCollector;
import uk.ac.ox.cs.pdq.fol.Dependency;

/**
 * Interface of the saturation algorithm
 */
public interface SaturationAlgorithm {


    public Collection<? extends TGD> run(Collection<? extends Dependency> tgds);
    
    /**
     * Run the saturation algorithm on the input TGDs
     */
    public Collection<? extends TGD> run(String processName, Collection<? extends Dependency> tgds);

    public void setStatsCollector(StatisticsCollector<SaturationStatColumns> statsCollector);
}
