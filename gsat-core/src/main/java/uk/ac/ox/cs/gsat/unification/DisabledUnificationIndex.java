package uk.ac.ox.cs.gsat.unification;

import java.util.HashSet;
import java.util.Set;

import uk.ac.ox.cs.gsat.GTGD;
import uk.ac.ox.cs.pdq.fol.Atom;

public class DisabledUnificationIndex<Q extends GTGD> implements UnificationIndex<Q> {

    private final Set<Q> tgds;

    public DisabledUnificationIndex() {
        this.tgds = new HashSet<>();
    }

    @Override
    public Set<Q> get(Atom atom) {
        return tgds;
    }

    @Override
    public void put(Atom atom, Q tgd) {
        this.tgds.add(tgd);
    }

    @Override
    public void remove(Atom atom, Q tgd) {
        this.tgds.remove(tgd);
    }
}
