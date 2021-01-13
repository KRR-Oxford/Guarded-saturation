package uk.ac.ox.cs.gsat.filters;

import java.util.Collection;

import uk.ac.ox.cs.gsat.TGDGSat;

public interface FormulaFilter {
    default void addAll(Collection<TGDGSat> formulas) {
        for (TGDGSat formula : formulas) {
            add(formula);
        }
    }

    default void removeAll(Collection<TGDGSat> formulas) {
        for (TGDGSat formula : formulas) {
            remove(formula);
        }
    }

    Collection<TGDGSat> getAll();

    void add(TGDGSat formula);

    void remove(TGDGSat formula);

    // returns a collection of elements that may be subsumed by formula
    Iterable<TGDGSat> getSubsumedCandidates(TGDGSat formula);

    // returns a collection of elements that may subsume a formula
    Iterable<TGDGSat> getSubsumingCandidates(TGDGSat formula);
}