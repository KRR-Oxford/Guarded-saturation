package uk.ac.ox.cs.gsat;

public interface SaturationStatisticsFactory<Q extends GTGD>  {
    SaturationStatistics<Q> create(String name);
}
