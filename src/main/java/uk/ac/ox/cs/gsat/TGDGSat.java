package uk.ac.ox.cs.gsat;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.TypedConstant;
import uk.ac.ox.cs.pdq.fol.UntypedConstant;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * TGDGSat
 */
public class TGDGSat extends TGD implements Comparable<TGDGSat> {

    private static final long serialVersionUID = 1L;

    protected TGDGSat(Atom[] body, Atom[] head) {

        super(body, head);

    }

    public TGDGSat(TGD tgd) {

        super(tgd.getBodyAtoms(), tgd.getHeadAtoms());

    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof TGDGSat))
            return false;

        TGDGSat tgd2 = (TGDGSat) obj;

        var body1 = Set.of(this.getBodyAtoms());
        var body2 = Set.of(tgd2.getBodyAtoms());

        var head1 = Set.of(this.getHeadAtoms());
        var head2 = Set.of(tgd2.getHeadAtoms());

        return body1.equals(body2) && head1.equals(head2);

    }

    @Override
    public int compareTo(TGDGSat o) {

        if (this.equals(o))
            return 0;

        return this.toString().compareTo(o.toString()); // Lexicographic order FIXME

    }

    @Override
    public int hashCode() {

        return this.toString().hashCode();

    }

    public Collection<String> getAllTermSymbols() {

        // Collection<String> result = getTypedConstants().stream().map((constant) -> )
        // .collect(Collectors.toList());
        Collection<String> result = new LinkedList<>();

        for (Term term : getTerms())
            if (term.isVariable())
                result.add(((Variable) term).getSymbol());
            else if (term.isUntypedConstant())
                result.add(((UntypedConstant) term).getSymbol());
            else if (term instanceof TypedConstant && ((TypedConstant) term).getValue() instanceof String)
                result.add((String) ((TypedConstant) term).getValue());
            else
                throw new IllegalArgumentException("Term type not supported: " + term + " : " + term.getClass());

        return result;

    }

    public Atom getGuard() {

        List<Variable> universalList = Arrays.asList(getUniversal());

        for (Atom atom : getBodyAtoms())
            if (Arrays.asList(atom.getTerms()).containsAll(universalList))
                return atom;

        throw new IllegalArgumentException("The TGD must be guarded to find get a Guard");

    }

}