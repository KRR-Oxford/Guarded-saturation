package uk.ac.ox.cs.gsat;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.LogicalSymbols;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.TypedConstant;
import uk.ac.ox.cs.pdq.fol.UntypedConstant;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * TGDGSat
 */
public class TGDGSat extends TGD {

    private static final long serialVersionUID = 1L;

    /**
     * Bottom/False We assume that LogicalSymbols.BOTTOM does not appear as a
     * predicate symbol in the input.
     */
    public final static Atom Bottom = Atom.create(Predicate.create(LogicalSymbols.BOTTOM.toString(), 0));

    private final Set<Atom> bodySet;
    private final Set<Atom> headSet;

    private final Atom guard;

    private TGDGSat(Atom[] body, Atom[] head) {

        super(body, head);

        bodySet = Set.of(body);
        headSet = Set.of(head);

        guard = computeGuard();

    }

    // protected TGDGSat(TGD tgd) {

    // this(tgd.getBodyAtoms(), tgd.getHeadAtoms());

    // }

    public TGDGSat(Set<Atom> body, Set<Atom> head) {

        this(body.toArray(new Atom[body.size()]), head.toArray(new Atom[head.size()]));

    }

    public Set<Atom> getBodySet() {
        return bodySet;
    }

    public Set<Atom> getHeadSet() {
        return headSet;
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof TGDGSat))
            return false;

        TGDGSat tgd2 = (TGDGSat) obj;

        return this.getBodySet().equals(tgd2.getBodySet()) && this.getHeadSet().equals(tgd2.getHeadSet());

    }

    @Override
    public int hashCode() {

        return Objects.hash(this.getBodySet(), this.getHeadSet());

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

    public Variable[] getUniversal() {
        return getTopLevelQuantifiedVariables();
    }

    public Atom getGuard() {
        return guard;
    }

}