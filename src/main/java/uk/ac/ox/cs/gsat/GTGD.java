package uk.ac.ox.cs.gsat;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
            throw new IllegalArgumentException("GTGD must be guarded!");

        return currentGuard;

    }

    public Atom getGuard() {
        return guard;
    }

	@Override
	public GTGD computeVNF(String eVariable, String uVariable) {
		return fromTGD(super.computeVNF(eVariable, uVariable));
	}

	@Override
	public Collection<GTGD> computeHNF() {
		return fromTGD(super.computeHNF());
	}
	
    public static GTGD fromTGD(TGD tgd) {
        return new GTGD(tgd.getBodySet(), tgd.getHeadSet());
    }

    public static Collection<GTGD> fromTGD(Collection<? extends TGD> tgds) {
        return tgds.stream().map(tgd -> fromTGD(tgd)).collect(Collectors.toSet());
    }
}