package uk.ac.ox.cs.gsat.unification;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import uk.ac.ox.cs.gsat.GTGD;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Predicate;

public class PredicateUnificationIndex<Q extends GTGD> implements UnificationIndex<Q> {

    private final HashMap<Predicate, Set<Q>> map;
	private final Comparator<? super GTGD> comparator;

    public PredicateUnificationIndex(Comparator<? super GTGD> comparator) {
        this.map = new HashMap<Predicate, Set<Q>>();
        this.comparator = comparator;
    }
    
	@Override
	public Set<Q> get(Atom atom) {
		Set<Q> set = this.map.get(atom.getPredicate());
        if (set != null)
            return set;
        else
            return new HashSet<>();
	}

	@Override
	public void put(Atom atom, Q tgd) {
        if (this.comparator != null) {
            this.map.computeIfAbsent(atom.getPredicate(), k -> new TreeSet<Q>(comparator)).add(tgd);
        } else {
            this.map.computeIfAbsent(atom.getPredicate(), k -> new HashSet<Q>()).add(tgd);
        }
	}

	@Override
	public void remove(Atom atom, Q tgd) {
        Set<Q> set = this.get(atom);
        if (set != null) {
            set.remove(tgd);
            if (set.isEmpty())
                this.map.remove(atom.getPredicate());
        }
	}
    
}
