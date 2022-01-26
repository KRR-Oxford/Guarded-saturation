package uk.ac.ox.cs.gsat.satalg.stats;

import uk.ac.ox.cs.gsat.fol.GTGD;

public interface SaturationStatisticsFactory<Q extends GTGD>  {
    SaturationStatistics<Q> create(String name);
}
