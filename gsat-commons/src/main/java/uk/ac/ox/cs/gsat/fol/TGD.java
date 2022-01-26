package uk.ac.ox.cs.gsat.fol;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.FunctionTerm;
import uk.ac.ox.cs.pdq.fol.LogicalSymbols;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.TypedConstant;
import uk.ac.ox.cs.pdq.fol.UntypedConstant;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * TGD
 */
public class TGD extends uk.ac.ox.cs.pdq.fol.TGD {

	private static final long serialVersionUID = 1L;

	/**
	 * Bottom/False We assume that LogicalSymbols.BOTTOM does not appear as a
	 * predicate symbol in the input.
	 */
	public final static Atom Bottom = Atom.create(Predicate.create(LogicalSymbols.BOTTOM.toString(), 0));

	private final Set<Atom> bodySet;
	private final Set<Atom> headSet;
	private int[] bodyHashes = null, headHashes = null;

	public TGD(Atom[] body, Atom[] head) {

		super(body, head);

		bodySet = Set.of(body);
		headSet = Set.of(head);

	}

	public TGD(Collection<Atom> body, Collection<Atom> head) {
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

		if (!(obj instanceof TGD))
			return false;

		TGD tgd2 = (TGD) obj;

		return this.getBodySet().equals(tgd2.getBodySet()) && this.getHeadSet().equals(tgd2.getHeadSet());

	}

	@Override
	public int hashCode() {

		return Objects.hash(this.getBodySet(), this.getHeadSet());

	}

	public Collection<String> getAllTermSymbols() {

		Collection<String> result = new LinkedList<>();

		for (Term term : getTerms())
			if (term.isVariable())
				result.add(((Variable) term).getSymbol());
			else if (term.isUntypedConstant())
				result.add(((UntypedConstant) term).getSymbol());
			else if (term instanceof TypedConstant && ((TypedConstant) term).getValue() instanceof String)
				result.add((String) ((TypedConstant) term).getValue());
            else if (term instanceof FunctionTerm)
                for (Term t : ((FunctionTerm) term).getTerms())
                    result.add(((Variable) t).getSymbol());
			else
				throw new IllegalArgumentException("Term type not supported: " + term + " : " + term.getClass());

		return result;

	}

	public Variable[] getUniversal() {
		return getTopLevelQuantifiedVariables();
	}

	public void setBodyHashes(int[] newHashes) {
		bodyHashes = newHashes;
	}

	public int[] getBodyHashes() {
		return bodyHashes;
	}

	public void setHeadHashes(int[] newHashes) {
		headHashes = newHashes;
	}

	public int[] getHeadHashes() {
		return headHashes;
	}

	public int getWidth() {
		int bwidth = this.getBody().getFreeVariables().length;
		int hwidth = this.getHead().getFreeVariables().length + this.getHead().getBoundVariables().length;

		return Math.max(bwidth, hwidth);
	}

}
