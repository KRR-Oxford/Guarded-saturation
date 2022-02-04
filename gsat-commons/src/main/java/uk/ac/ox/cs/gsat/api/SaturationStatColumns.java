package uk.ac.ox.cs.gsat.api;

import uk.ac.ox.cs.gsat.statistics.StatisticsColumn;

public enum SaturationStatColumns implements StatisticsColumn {
    /**
     * initial number of non-full TGDs 
     */
    NFTGD_NB,
    /**
     * initial number of full TGDs 
     */
    FTGD_NB,
    /**
     * number of subsumed TGDs
     */
    SUBSUMED,
    /**
     * number of discarded tautology
     */
    DISCARDED_TAUTOLOGY,
    /**
     * number of loop stopped because a TGD is subsumed
     */
    STOP_BECAUSE_SUBSUMED,
    /**
     * number of derived non-full TGDs 
     */
    NEW_NFTGD_NB,
    /**
     * number of derived full TGDs 
     */
    NEW_FTGD_NB,
    /**
     * number of derived full TGDs in the output
     */
    NEW_OUTPUT_SIZE,
    /**
     * number of full TGDs in the output
     */
    OUTPUT_SIZE,
    /**
     * number of derived TGDs with Skolem terms in the body
     */
    NEW_RTGD_BSK,
    /**
     * maximum number of Skolem terms in the TGD's body
     */
    BODY_SK_ATOMS_MAX,
    /**
     * number of evolve/hypperesolution applications 
     */
    EVOL_COUNT,
    /**
     * number of evolve stopped because equals 
     */
    EVOL_STOPPED_BECAUSE_EQUAL,
    /**
     * time (ms) spent for the evolve applications
     */
    EVOL_TIME,
    /**
     * number of hypperesolution failure
     */
    HYPER_FAILURE,
    /**
     * time (ms) spent for the forward subsumption
     */
    FORWARD_SUB_TIME,
    /**
     * time (ms) spent for the backward subsumption
     */
    BACKWARD_SUB_TIME,
    /**
     * time (ms) spent for the subsumption (both forward and backward)
     */
    SUMB_TIME,
    /**
     * time (ms) spent for the rest
     */
    OTHER_TIME,
    /**
     * total time (ms)
     */
    TIME
}
