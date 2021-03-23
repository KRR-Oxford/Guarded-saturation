package uk.ac.ox.cs.gsat.filters;

import java.util.Collection;
import java.util.HashSet;

import uk.ac.ox.cs.gsat.GTGD;

/**
 * A FormulaFilter that simply returns the whole set S. Equivalent to not having
 * any indexing method.
 */
public class IdentityFormulaFilter implements FormulaFilter {
    HashSet<GTGD> formulas = new HashSet<>();

    public Collection<GTGD> getAll() {
        return formulas;
    }

    public void addAll(Collection<GTGD> newFormulas) {
        formulas.addAll(newFormulas);
    }

    public void add(GTGD newFormula) {
        formulas.add(newFormula);
    }

    public void removeAll(Collection<GTGD> newFormulas) {
        formulas.removeAll(newFormulas);
    }

    public void remove(GTGD newFormula) {
        formulas.remove(newFormula);
    }

    public Iterable<GTGD> getSubsumedCandidates(GTGD formula) {
        return formulas;
    }

    public Iterable<GTGD> getSubsumingCandidates(GTGD formula) {
        return formulas;
    }
}
