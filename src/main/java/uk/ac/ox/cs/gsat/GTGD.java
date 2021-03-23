package uk.ac.ox.cs.gsat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * GTGD
 */
public class GTGD extends TGD {

    private static final long serialVersionUID = 1L;

	private final Atom guard;
    
    public GTGD(Set<Atom> body, Set<Atom> head) {
        super(body, head);

        this.guard = computeGuard();
    }

    public Atom computeGuard() {

        List<Variable> universalList = Arrays.asList(getUniversal());

        Atom currentGuard = null;
        for (Atom atom : getBodySet())
            if (Arrays.asList(atom.getTerms()).containsAll(universalList))
                if (currentGuard == null || atom.getPredicate().getArity() < currentGuard.getPredicate().getArity())
                    currentGuard = atom;
                else if (atom.getPredicate().getArity() == currentGuard.getPredicate().getArity()
                        && atom.getPredicate().getName().compareTo(currentGuard.getPredicate().getName()) < 0)
                    currentGuard = atom;

        if (currentGuard == null)
            throw new IllegalArgumentException("TGDGSat must be guarded!");

        return currentGuard;

    }

    public Atom getGuard() {
        return guard;
    }
}

