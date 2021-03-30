package uk.ac.ox.cs.gsat;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import uk.ac.ox.cs.pdq.fol.Atom;
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

	private TGD(Atom[] body, Atom[] head) {

		super(body, head);

		bodySet = Set.of(body);
		headSet = Set.of(head);

	}

	public TGD(Set<Atom> body, Set<Atom> head) {

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

	/**
	 *
	 * Returns the Variable Normal Form (VNF) 
	 *
	 * @param eVariable existential variable prefix
	 * @param uVariable universal variable prefix
	 * @return Variable Normal Form of tgd
	 */
	public TGD computeVNF(String eVariable, String uVariable) {

		Variable[] uVariables = this.getUniversal();
		Variable[] eVariables = this.getExistential();

		Map<Term, Term> substitution = new HashMap<>();
		int counter = 1;
		for (Variable v : uVariables)
			substitution.put(v, Variable.create(uVariable + counter++));
		counter = 1;
		for (Variable v : eVariables)
			substitution.put(v, Variable.create(eVariable + counter++));

		App.logger.fine("VNF substitution:\n" + substitution);

		TGD applySubstitution = (TGD) Logic.applySubstitution(this, substitution);
		App.logger.fine("VNF: " + this + "===>>>" + applySubstitution);
		return applySubstitution;

	}

	/**
	 *
	 * Returns the Head Normal Form (HNF) 
	 *
	 * @return Head Normal Form of tgd
	 */
	public Collection<? extends TGD> computeHNF() {

		Variable[] eVariables = this.getExistential();

		Set<Atom> eHead = new HashSet<>();
		Set<Atom> fHead = new HashSet<>();

		Set<Atom> bodyAtoms = this.getBodySet();
		for (Atom a : this.getHeadAtoms())
			if (a.equals(TGD.Bottom))
				// remove all head atoms since ⊥ & S ≡ ⊥ for any conjunction S
				return Set.of(new TGD(bodyAtoms, Set.of(TGD.Bottom)));
			else if (Logic.containsAny(a, eVariables))
				eHead.add(a);
			else if (!bodyAtoms.contains(a))
				// Do not add atoms that already appear in the body.
				// This is only needed for fHead since we have no existentials in the body
				fHead.add(a);

		if (this.getHeadAtoms().length == eHead.size() || this.getHeadAtoms().length == fHead.size())
			return Set.of(this);

		Collection<TGD> result = new HashSet<>();
		if (!eHead.isEmpty())
			result.add(new TGD(bodyAtoms, eHead));
		if (!fHead.isEmpty())
			result.add(new TGD(bodyAtoms, fHead));
		return result;
	}

}