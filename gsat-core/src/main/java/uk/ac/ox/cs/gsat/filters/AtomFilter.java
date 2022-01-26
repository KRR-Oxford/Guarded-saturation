package uk.ac.ox.cs.gsat.filters;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Atom;

/**
 * Extension of the FormulaFilter class that for each atom contains a collection
 * of the formulas in S with that atom either in the head or in the body
 * (including the same variable names).
 */
public abstract class AtomFilter<Q extends TGD> implements FormulaFilter<Q> {
    protected Map<Atom, Collection<Q>> bodyMap = new HashMap<>(), headMap = new HashMap<>();
    protected Collection<Q> all = new HashSet<>();

    public Collection<Q> getAll() {
        return all;
    }

    public void init(Collection<Q> formulas) {
        for (Q formula: formulas)
            add(formula);
    }
    
    public void add(Q formula) {
        all.add(formula);
        for (Atom atom : formula.getBodyAtoms()) {
            if (!bodyMap.containsKey(atom)) {
                bodyMap.put(atom, new HashSet<>());
            }
            bodyMap.get(atom).add(formula);
        }
        for (Atom atom : formula.getHeadAtoms()) {
            if (!headMap.containsKey(atom)) {
                headMap.put(atom, new HashSet<>());
            }
            headMap.get(atom).add(formula);
        }
    }

    public void remove(Q formula) {
        all.remove(formula);
        for (Atom atom : formula.getBodyAtoms()) {
            bodyMap.get(atom).remove(formula);
        }
        for (Atom atom : formula.getHeadAtoms()) {
            headMap.get(atom).remove(formula);
        }
    }
}
