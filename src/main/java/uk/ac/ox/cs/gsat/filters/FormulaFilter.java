package uk.ac.ox.cs.gsat.filters;

import java.util.Collection;

import uk.ac.ox.cs.gsat.TGDGSat;

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
    default void addAll(Collection<TGDGSat> tgds) {
        for (TGDGSat tgd : tgds) {
            add(tgd);
        }
    }

    /**
     * Removes {@code tgds} from S.
     */
    default void removeAll(Collection<TGDGSat> tgds) {
        for (TGDGSat tgd : tgds) {
            remove(tgd);
        }
    }

    /**
     * Does not have to be efficient.
     * 
     * @return all tgds in S
     */
    Collection<TGDGSat> getAll();

    /**
     * Adds {@code tgd} to S.
     */
    void add(TGDGSat tgd);

    /**
     * Removes {@code tgd} from S.
     */
    void remove(TGDGSat tgd);

    /**
     * Returns elements in S that could subsume {@code tgd}
     */
    Iterable<TGDGSat> getSubsumedCandidates(TGDGSat tgd);

    /**
     * Returns elements in S that could be subsumed by {@code tgd}
     */
    Iterable<TGDGSat> getSubsumingCandidates(TGDGSat formula);
}