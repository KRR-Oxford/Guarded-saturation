package uk.ac.ox.cs.gsat.api;

import uk.ac.ox.cs.gsat.statistics.StatisticsColumn;

public enum MaterializationStatColumns implements StatisticsColumn {

    // number of full tgds used for the materialization  
    MAT_FTGD_NB,
    // size of the generated input
    MAT_GEN_SIZE,
    // size of the materialization
    MAT_SIZE,
    // time required to generated the input
    MAT_GEN_TIME,
    // time required to initialize the materialization system
    MAT_INIT_TIME,
    // time required to load the data
    MAT_DATA_LOAD_TIME,
    // time of the materialization process (applying the rules)
    MAT_TIME,
    // time required to write the output
    MAT_WRITING_TIME,
    // total time of the materialization process
    MAT_TOTAL

}
