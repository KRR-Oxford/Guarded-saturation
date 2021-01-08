package uk.ac.ox.cs.gsat.filters;

import java.util.Collection;

import uk.ac.ox.cs.gsat.TGDGSat;

public interface FormulaFilter {
    // public FormulaFilter() {

    // }
    default void addAll(Collection<TGDGSat> formulas) {
        for (TGDGSat formula : formulas) {
            add(formula);
        }
    }

    default void removeAll(Collection<TGDGSat> formulas) {
        for (TGDGSat formula : formulas) {
            add(formula);
        }
    }

    Collection<TGDGSat> getAll();

    void add(TGDGSat formula);

    void remove(TGDGSat formula);

    // returns a collection of elements that may be subsumed by formula
    Collection<TGDGSat> getSubsumedCandidates(TGDGSat formula);

    Collection<TGDGSat> getSubsumingCandidates(TGDGSat formula);
}