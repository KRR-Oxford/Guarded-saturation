package uk.ac.ox.cs.gsat.filters;

import java.util.Collection;
import java.util.HashSet;

import uk.ac.ox.cs.gsat.TGD;
import uk.ac.ox.cs.pdq.fol.Atom;

/**
 * An efficient usage of the bodyMap and headMap provided by PredicateFilter
 */
public class MinPredicateFilter<Q extends TGD> extends PredicateFilter<Q> {

    /**
     * Iterates over all body predicates of {@code formula}, considers all
     * collections indexed by these predicates in bodyMap, and returns the smallest
     * of all of them
     */
    @Override
    public Iterable<Q> getSubsumedCandidates(Q formula) {
        Atom[] atoms = formula.getBodyAtoms();
        if (atoms.length == 0)
            return all;
        Collection<Q> answer = bodyMap.getOrDefault(atoms[0].getPredicate(), null);
        if (answer == null)
            return new HashSet<Q>();
        for (int i = 1; i < atoms.length; i++) {
            Collection<Q> newAnswer = bodyMap.getOrDefault(atoms[i].getPredicate(), null);
            if (newAnswer == null)
                return new HashSet<Q>();
            if (newAnswer.size() < answer.size())
                answer = newAnswer;
        }
        return answer;
    }

    /**
     * Iterates over all head predicates of {@code formula}, considers all
     * collections indexed by these predicates in headMap, and returns the smallest
     * of all of them.
     */
    @Override
    public Iterable<Q> getSubsumingCandidates(Q formula) {
        Atom[] atoms = formula.getHeadAtoms();
        if (atoms.length == 0)
            return all;
        Collection<Q> answer = headMap.getOrDefault(atoms[0].getPredicate(), null);
        if (answer == null)
            return new HashSet<Q>();
        for (int i = 1; i < atoms.length; i++) {
            Collection<Q> newAnswer = headMap.getOrDefault(atoms[i].getPredicate(), null);
            if (newAnswer == null)
                return new HashSet<Q>();
            if (newAnswer.size() < answer.size())
                answer = newAnswer;
        }
        return answer;
    }

}
