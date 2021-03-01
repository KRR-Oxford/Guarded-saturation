package uk.ac.ox.cs.gsat.filters;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import uk.ac.ox.cs.gsat.TGDGSat;
import uk.ac.ox.cs.pdq.fol.Atom;

public abstract class AtomFilter implements FormulaFilter {
    protected Map<Atom, Collection<TGDGSat>> bodyMap = new HashMap<>(), headMap = new HashMap<>();
    protected Collection<TGDGSat> all = new HashSet<>();

    public Collection<TGDGSat> getAll() {
        return all;
    }

    public void add(TGDGSat formula) {
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

    public void remove(TGDGSat formula) {
        all.remove(formula);
        for (Atom atom : formula.getBodyAtoms()) {
            bodyMap.get(atom).remove(formula);
        }
        for (Atom atom : formula.getHeadAtoms()) {
            headMap.get(atom).remove(formula);
        }
    }
}