package uk.ac.ox.cs.gsat.filters;

import java.util.Collection;

import uk.ac.ox.cs.gsat.GTGD;

/**
 * Stores and operates on a set of tgds S. Allows adding / removing elements
 * from S.
 * 
 * Given a tgd {@code a}, allows to get candidates that are subsumed or subsume
 * {@code a} in S.
 */
public interface FormulaFilter {
    /**
     * Adds {@code tgds} to S.
     */
    default void addAll(Collection<GTGD> tgds) {
        for (GTGD tgd : tgds) {
            add(tgd);
        }
    }

    /**
     * Removes {@code tgds} from S.
     */
    default void removeAll(Collection<GTGD> tgds) {
        for (GTGD tgd : tgds) {
            remove(tgd);
        }
    }

    /**
     * Does not have to be efficient.
     * 
     * @return all tgds in S
     */
    Collection<GTGD> getAll();

    /**
     * Adds {@code tgd} to S.
     */
    void add(GTGD tgd);

    /**
     * Removes {@code tgd} from S.
     */
    void remove(GTGD tgd);

    /**
     * Returns elements in S that could subsume {@code tgd}
     */
    Iterable<GTGD> getSubsumedCandidates(GTGD tgd);

    /**
     * Returns elements in S that could be subsumed by {@code tgd}
     */
    Iterable<GTGD> getSubsumingCandidates(GTGD formula);
}
