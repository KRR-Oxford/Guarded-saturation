package uk.ac.ox.cs.gsat.subsumers;

import java.util.Collection;

import uk.ac.ox.cs.gsat.TGDGSat;

/*
 * A class to encapsulate subsumption methods
 */
public interface Subsumer {
    /*
     * Returns any formulas that subsume the given formula. Removes the found
     * formulas from the list of formula list.
     */
    public Collection<TGDGSat> subsumesAny(TGDGSat formula);

    public boolean subsumed(TGDGSat formula);

    public void add(TGDGSat formula);

    public Collection<TGDGSat> getAll();

    default public long getNumberSubsumed() {
        return 0;
    }

    default public long getFilterDiscarded() {
        return 0;
    }
}