package uk.ac.ox.cs.gsat.subsumers;

import java.util.Collection;

import uk.ac.ox.cs.gsat.GTGD;

/**
 * A class to encapsulate subsumption methods for tgds. It stores a set of tgds
 * S that it operates on (initially empty). Also provides statistics for the
 * number of elements subsumed or
 */
public interface Subsumer {
    /**
     * Removes from S any tgds that are subsumed by {@code tgd}. Updates the counts
     * for number of filtered elements if an index is used. Increments number of
     * subsumed elements by the size of the returned set.
     * 
     * @return a collection of the removed tgds.
     */
    public Collection<GTGD> subsumesAny(GTGD tgd);

    /**
     * Should not modify S. Updates the counts for number of filtered elements if an
     * index is used. Increments number of subsumed elements by 1 if the return
     * value is true.
     * 
     * @return whether {@code tgd} is subsumed by something in S.
     */
    public boolean subsumed(GTGD tgd);

    /**
     * Adds {@code tgd} to S
     */
    public void add(GTGD tgd);

    /**
     * This does not need to be efficient. It is intended to be called at most once
     * at the end of the algorithm, or for debugging.
     * 
     * @return S
     */
    public Collection<GTGD> getAll();

    /**
     * 
     * @return the number of identified subsumptions by calls to {@code subsumesAny}
     *         and {@code subsumed}
     */
    default public long getNumberSubsumed() {
        return 0;
    }

    /**
     * Does not need to be efficient.
     * 
     * @return the number of elements that were identified by an index as possible
     *         subsumption candidates, but then discarded in the final answer. If no
     *         index is used, returns 0.
     */
    default public long getFilterDiscarded() {
        return 0;
    }
}
