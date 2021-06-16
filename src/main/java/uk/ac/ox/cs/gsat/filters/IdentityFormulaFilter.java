package uk.ac.ox.cs.gsat.filters;

import java.util.Collection;
import java.util.HashSet;

import uk.ac.ox.cs.gsat.TGD;

/**
 * A FormulaFilter that simply returns the whole set S. Equivalent to not having
 * any indexing method.
 */
public class IdentityFormulaFilter<Q extends TGD> implements FormulaFilter<Q> {
    HashSet<Q> formulas = new HashSet<>();

    public Collection<Q> getAll() {
        return formulas;
    }

    public void addAll(Collection<Q> newFormulas) {
        formulas.addAll(newFormulas);
    }

    public void init(Collection<Q> formulas) {
        for (Q formula: formulas)
            add(formula);
    }

    public void add(Q newFormula) {
        formulas.add(newFormula);
    }

    public void removeAll(Collection<Q> newFormulas) {
        formulas.removeAll(newFormulas);
    }

    public void remove(Q newFormula) {
        formulas.remove(newFormula);
    }

    public Iterable<Q> getSubsumedCandidates(Q formula) {
        return formulas;
    }

    public Iterable<Q> getSubsumingCandidates(Q formula) {
        return formulas;
    }
}
