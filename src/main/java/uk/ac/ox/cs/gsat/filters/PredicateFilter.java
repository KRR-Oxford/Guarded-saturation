package uk.ac.ox.cs.gsat.filters;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import uk.ac.ox.cs.gsat.GTGD;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Predicate;

/**
 * Extension of the FormulaFilter class that for each atom contains a collection
 * of the formulas in S with that predicate either in the head or in the body.
 */
public abstract class PredicateFilter implements FormulaFilter {
    protected Map<Predicate, Collection<GTGD>> bodyMap = new HashMap<>(), headMap = new HashMap<>();
    protected Collection<GTGD> all = new HashSet<>();

    public Collection<GTGD> getAll() {
        return all;
    }

    public void add(GTGD formula) {
        all.add(formula);
        for (Atom atom : formula.getBodyAtoms()) {
            if (!bodyMap.containsKey(atom.getPredicate())) {
                bodyMap.put(atom.getPredicate(), new HashSet<>());
            }
            bodyMap.get(atom.getPredicate()).add(formula);
        }
        for (Atom atom : formula.getHeadAtoms()) {
            if (!headMap.containsKey(atom.getPredicate())) {
                headMap.put(atom.getPredicate(), new HashSet<>());
            }
            headMap.get(atom.getPredicate()).add(formula);
        }
    }

    public void remove(GTGD formula) {
        all.remove(formula);
        for (Atom atom : formula.getBodyAtoms()) {
            bodyMap.get(atom.getPredicate()).remove(formula);
        }
        for (Atom atom : formula.getHeadAtoms()) {
            headMap.get(atom.getPredicate()).remove(formula);
        }
    }
}
