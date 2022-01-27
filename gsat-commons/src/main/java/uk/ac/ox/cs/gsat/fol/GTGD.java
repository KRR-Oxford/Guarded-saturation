package uk.ac.ox.cs.gsat.fol;

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

    protected GTGD(Set<Atom> body, Set<Atom> head) {
        super(body, head);

        this.guard = computeGuard();
    }

    protected GTGD(Atom[] body, Atom[] head) {
        super(body, head);

        this.guard = computeGuard();
    }

    public Atom computeGuard() {

        List<Variable> universalList = Arrays.asList(getUniversal());

        Atom currentGuard = null;
        for (Atom atom : getBodySet())
            if (Arrays.asList(atom.getVariables()).containsAll(universalList))
                if (currentGuard == null || atom.getPredicate().getArity() < currentGuard.getPredicate().getArity())
                    currentGuard = atom;
                else if (atom.getPredicate().getArity() == currentGuard.getPredicate().getArity()
                        && atom.getPredicate().getName().compareTo(currentGuard.getPredicate().getName()) < 0)
                    currentGuard = atom;

        if (currentGuard == null)
            throw new IllegalArgumentException("GTGD must be guarded! But found " + this);

        return currentGuard;

    }

    public Atom getGuard() {
        return guard;
    }

    public static GTGD create(Set<Atom> body, Set<Atom> head) {
        return Cache.gtgd.retrieve(new GTGD(body, head));
    }

    public static GTGD create(Atom[] body, Atom[] head) {
        return Cache.gtgd.retrieve(new GTGD(body, head));
    }
}
