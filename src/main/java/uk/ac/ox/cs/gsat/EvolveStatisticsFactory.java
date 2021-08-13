package uk.ac.ox.cs.gsat;

public interface EvolveStatisticsFactory<Q extends GTGD>  {
    EvolveStatistics<Q> create(String name);
}
