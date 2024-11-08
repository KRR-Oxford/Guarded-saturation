package uk.ac.ox.cs.gsat.filters;

import java.util.Collection;
import java.util.HashSet;

import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Atom;

/**
 * A not very intelligent way to use headMap and bodyMap available in
 * PredicateFilter
 */
public class AnyPredicateFilter<Q extends TGD> extends PredicateFilter<Q> {
    /**
     * Takes the first body predicate of {@code formula}, and returns the collection
     * in bodyMap indexed by that predicate
     */
    @Override
    public Iterable<Q> getSubsumedCandidates(Q formula) {
        Atom[] atoms = formula.getBodyAtoms();
        if (atoms.length == 0)
            return all;
        Collection<Q> answer = bodyMap.getOrDefault(atoms[0].getPredicate(), null);
        if (answer == null)
            return new HashSet<>();
        return answer;
    }

    /**
     * Takes the first head predicate of {@code formula}, and returns the collection
     * in headMap indexed by that predicate
     */
    @Override
    public Iterable<Q> getSubsumingCandidates(Q formula) {
        Atom[] atoms = formula.getHeadAtoms();
        if (atoms.length == 0)
            return all;
        Collection<Q> answer = headMap.getOrDefault(atoms[0].getPredicate(), null);
        if (answer == null)
            return new HashSet<>();
        return answer;
    }

}
