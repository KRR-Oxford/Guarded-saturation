package uk.ac.ox.cs.gsat.unification;

import java.util.Set;

import uk.ac.ox.cs.gsat.GTGD;
import uk.ac.ox.cs.pdq.fol.Atom;

public interface UnificationIndex<Q extends GTGD> {

    public Set<Q> get(Atom atom);

    public void put(Atom atom, Q tgd);

    public void remove(Atom atom, Q tgd);

}
