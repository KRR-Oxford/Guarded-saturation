package uk.ac.ox.cs.gsat.filters;

import java.util.Collection;
import java.util.HashSet;

import uk.ac.ox.cs.gsat.GTGD;
import uk.ac.ox.cs.pdq.fol.Atom;

/**
 * A not very intelligent way to use headMap and bodyMap available in
 * PredicateFilter
 */
public class AnyPredicateFilter extends PredicateFilter {
    /**
     * Takes the first body predicate of {@code formula}, and returns the collection
     * in bodyMap indexed by that predicate
     */
    @Override
    public Iterable<GTGD> getSubsumedCandidates(GTGD formula) {
        Atom[] atoms = formula.getBodyAtoms();
        if (atoms.length == 0)
            return all;
        Collection<GTGD> answer = bodyMap.getOrDefault(atoms[0].getPredicate(), null);
        if (answer == null)
            return new HashSet<>();
        return answer;
    }

    /**
     * Takes the first head predicate of {@code formula}, and returns the collection
     * in headMap indexed by that predicate
     */
    @Override
    public Iterable<GTGD> getSubsumingCandidates(GTGD formula) {
        Atom[] atoms = formula.getHeadAtoms();
        if (atoms.length == 0)
            return all;
        Collection<GTGD> answer = headMap.getOrDefault(atoms[0].getPredicate(), null);
        if (answer == null)
            return new HashSet<>();
        return answer;
    }

}
