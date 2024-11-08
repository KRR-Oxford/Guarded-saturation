package uk.ac.ox.cs.gsat.filters;

import java.util.Collection;

import uk.ac.ox.cs.gsat.fol.TGD;

/**
 * Stores and operates on a set of tgds S. Allows adding / removing elements
 * from S.
 * 
 * Given a tgd {@code a}, allows to get candidates that are subsumed or subsume
 * {@code a} in S.
 */
public interface FormulaFilter<Q extends TGD> {
    /**
     * Adds {@code tgds} to S.
     */
    default void addAll(Collection<Q> tgds) {
        for (Q tgd : tgds) {
            add(tgd);
        }
    }

    /**
     * Removes {@code tgds} from S.
     */
    default void removeAll(Collection<Q> tgds) {
        for (Q tgd : tgds) {
            remove(tgd);
        }
    }

    /**
     * Does not have to be efficient.
     * 
     * @return all tgds in S
     */
    Collection<Q> getAll();

    /**
     * Adds {@code tgd} to S.
     */
    void add(Q tgd);

    /**
     * initialize the structure of the filter with a set of tgds
     * no tgd is added to the filter by this method
    */    
    void init(Collection<Q> tgds);
    
    /**
     * Removes {@code tgd} from S.
     */
    void remove(Q tgd);

    /**
     * Returns elements in S that could subsume {@code tgd}
     */
    Iterable<Q> getSubsumedCandidates(Q tgd);

    /**
     * Returns elements in S that could be subsumed by {@code tgd}
     */
    Iterable<Q> getSubsumingCandidates(Q formula);
}
