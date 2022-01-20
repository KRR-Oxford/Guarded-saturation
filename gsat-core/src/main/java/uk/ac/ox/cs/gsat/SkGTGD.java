package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.FunctionTerm;
import uk.ac.ox.cs.pdq.fol.Term;

public class SkGTGD extends GTGD {


    protected final boolean isNonFull;
    protected final boolean isFunctional;
    protected final Atom[] functionalBodyAtoms;

	public SkGTGD(Set<Atom> body, Set<Atom> head) {
        super(body, head);

        this.functionalBodyAtoms = getFunctionalAtoms(this.getBodyAtoms());
        boolean isHeadFunctional = getFunctionalAtoms(this.getHeadAtoms()).length > 0;
        this.isNonFull = !(this.functionalBodyAtoms.length > 0) && isHeadFunctional;
        this.isFunctional = this.functionalBodyAtoms.length > 0 || isHeadFunctional;
    }

	public boolean isNonFull() {
        return isNonFull;
    }

    public boolean isFunctional() {
        return isFunctional;
    }

    public Atom[] getFunctionalBodyAtoms() {
		return functionalBodyAtoms;
	}

    private static Atom[] getFunctionalAtoms(Atom[] atoms) {
        if (atoms.length == 1) {
            if (isFunctional(atoms[0].getTerms()))
                return atoms;
            else
                return new Atom[0];
        }

        List<Atom> functionalAtoms = new ArrayList<>();
        for (Atom atom : atoms)
            if (isFunctional(atom.getTerms()))
                functionalAtoms.add(atom);

        return functionalAtoms.toArray(new Atom[0]);
    }

    private static boolean isFunctional(Term[] terms) {
        
        for (Term term : terms)
            if (term instanceof FunctionTerm)
                return true;

        return false;
    }
}
