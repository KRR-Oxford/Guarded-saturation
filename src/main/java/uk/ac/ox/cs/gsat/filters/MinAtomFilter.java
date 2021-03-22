package uk.ac.ox.cs.gsat.filters;

import java.util.Collection;
import java.util.HashSet;

import uk.ac.ox.cs.gsat.TGDGSat;
import uk.ac.ox.cs.pdq.fol.Atom;

/**
 * An efficient usage of the bodyMap and headMap provided by AtomFilter.
 * 
 * Note: this is technically not an indexing method, as it may miss out on some
 * subsumptions
 */
public class MinAtomFilter extends AtomFilter {
    /**
     * Iterates over all body atoms of {@code formula}, considers all collections
     * indexed by these atoms in bodyMap, and returns the smallest of all of them
     */
    @Override
    public Iterable<TGDGSat> getSubsumedCandidates(TGDGSat formula) {
        Atom[] atoms = formula.getBodyAtoms();
        if (atoms.length == 0)
            return all;
        Collection<TGDGSat> answer = bodyMap.getOrDefault(atoms[0], null);
        if (answer == null)
            return new HashSet<TGDGSat>();
        for (int i = 1; i < atoms.length; i++) {
            Collection<TGDGSat> newAnswer = bodyMap.getOrDefault(atoms[i], null);
            if (newAnswer == null)
                return new HashSet<TGDGSat>();
            if (newAnswer.size() < answer.size())
                answer = newAnswer;
        }
        return answer;
    }

    /**
     * Iterates over all head atoms of {@code formula}, considers all collections
     * indexed by these atoms in headMap, and returns the smallest of all of them
     */
    @Override
    public Iterable<TGDGSat> getSubsumingCandidates(TGDGSat formula) {
        Atom[] atoms = formula.getHeadAtoms();
        if (atoms.length == 0)
            return all;
        Collection<TGDGSat> answer = headMap.getOrDefault(atoms[0], null);
        if (answer == null)
            return new HashSet<TGDGSat>();
        for (int i = 1; i < atoms.length; i++) {
            Collection<TGDGSat> newAnswer = headMap.getOrDefault(atoms[i], null);
            if (newAnswer == null)
                return new HashSet<TGDGSat>();
            if (newAnswer.size() < answer.size())
                answer = newAnswer;
        }
        return answer;
    }

}