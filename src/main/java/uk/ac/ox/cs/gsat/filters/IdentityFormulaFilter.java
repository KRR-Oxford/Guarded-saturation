package uk.ac.ox.cs.gsat.filters;

import java.util.Collection;
import java.util.HashSet;

import uk.ac.ox.cs.gsat.TGDGSat;

public class IdentityFormulaFilter implements FormulaFilter {
    HashSet<TGDGSat> formulas = new HashSet<>();

    public Collection<TGDGSat> getAll() {
        return formulas;
    }

    public void addAll(Collection<TGDGSat> newFormulas) {
        formulas.addAll(newFormulas);
    }

    public void add(TGDGSat newFormula) {
        formulas.add(newFormula);
    }

    public void removeAll(Collection<TGDGSat> newFormulas) {
        formulas.removeAll(newFormulas);
    }

    public void remove(TGDGSat newFormula) {
        formulas.remove(newFormula);
    }

    public Iterable<TGDGSat> getSubsumedCandidates(TGDGSat formula) {
        return formulas;
    }

    public Iterable<TGDGSat> getSubsumingCandidates(TGDGSat formula) {
        return formulas;
    }
}