package uk.ac.ox.cs.gsat.filters;

import java.util.Collection;
import java.util.HashSet;

import uk.ac.ox.cs.gsat.TGDGSat;
import uk.ac.ox.cs.pdq.fol.Atom;

/**
 * An efficient usage of the bodyMap and headMap provided by PredicateFilter
 */
public class MinPredicateFilter extends PredicateFilter {

    /**
     * Iterates over all body predicates of {@code formula}, considers all
     * collections indexed by these predicates in bodyMap, and returns the smallest
     * of all of them
     */
    @Override
    public Iterable<TGDGSat> getSubsumedCandidates(TGDGSat formula) {
        Atom[] atoms = formula.getBodyAtoms();
        if (atoms.length == 0)
            return all;
        Collection<TGDGSat> answer = bodyMap.getOrDefault(atoms[0].getPredicate(), null);
        if (answer == null)
            return new HashSet<TGDGSat>();
        for (int i = 1; i < atoms.length; i++) {
            Collection<TGDGSat> newAnswer = bodyMap.getOrDefault(atoms[i].getPredicate(), null);
            if (newAnswer == null)
                return new HashSet<TGDGSat>();
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
    public Iterable<TGDGSat> getSubsumingCandidates(TGDGSat formula) {
        Atom[] atoms = formula.getHeadAtoms();
        if (atoms.length == 0)
            return all;
        Collection<TGDGSat> answer = headMap.getOrDefault(atoms[0].getPredicate(), null);
        if (answer == null)
            return new HashSet<TGDGSat>();
        for (int i = 1; i < atoms.length; i++) {
            Collection<TGDGSat> newAnswer = headMap.getOrDefault(atoms[i].getPredicate(), null);
            if (newAnswer == null)
                return new HashSet<TGDGSat>();
            if (newAnswer.size() < answer.size())
                answer = newAnswer;
        }
        return answer;
    }

}