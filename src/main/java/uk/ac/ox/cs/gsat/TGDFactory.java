package uk.ac.ox.cs.gsat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

public class TGDFactory<Q extends TGD> {

    private static final TGDFactory<TGD> TGDINSTANCE = new TGDFactory<TGD>(new TGDConstructor());
    private static final TGDFactory<GTGD> GTGDINSTANCE = new TGDFactory<GTGD>(new GTGDConstructor());
	private Constructor<Q> constructor;

    private TGDFactory(Constructor<Q> constructor) {
        this.constructor = constructor;
    }

    public static TGDFactory<TGD> getTGDInstance() {
        return TGDINSTANCE;
    }

    public static TGDFactory<GTGD> getGTGDInstance() {
        return GTGDINSTANCE;
    }

    /**
     *
     * Returns the Variable Normal Form (VNF)
     *
     * @param eVariable existential variable prefix
     * @param uVariable universal variable prefix
     * @param tgd
     * @return Variable Normal Form of tgd
     */
    public Q computeVNF(Q tgd, String eVariable, String uVariable) {
        if (Configuration.isSortedVNF()) {
            return computeVNFAfterSortingByPredicates(tgd, eVariable, uVariable);
        } else {
            return computeVNFWithoutSorting(tgd, eVariable, uVariable);
        }
    }

    /**
     *
     * Returns the Variable Normal Form (VNF)
     * renaming the variables without ordering them
     *
     * @param tgd
     * @param eVariable existential variable prefix
     * @param uVariable universal variable prefix
     * @return Variable Normal Form of tgd
     */
    Q computeVNFWithoutSorting(Q tgd, String eVariable, String uVariable) {

        Variable[] uVariables = tgd.getUniversal();
        Variable[] eVariables = tgd.getExistential();

        Map<Term, Term> substitution = new HashMap<>();
        int counter = 1;
        for (Variable v : uVariables)
            substitution.put(v, Variable.create(uVariable + counter++));

        counter = 1;
        for (Variable v : eVariables)
            substitution.put(v, Variable.create(eVariable + counter++));

        App.logger.fine("VNF substitution:\n" + substitution);

        Q applySubstitution = (Q) Logic.applySubstitution(tgd, substitution);
        App.logger.fine("VNF: " + tgd + "===>>>" + applySubstitution);
        return applySubstitution;

    }

    /**
     *
     * Returns  Variable Normal Form (VNF)
     * renaming the variables by sorting them
     * according the predicate name and the position of the atoms they belong
     * starting by considering only head atoms and then body atoms
     *
     * @param tgd
     * @param eVariable existential variable prefix
     * @param uVariable universal variable prefix
     * @return Variable Normal Form of tgd
     */
    Q computeVNFAfterSortingByPredicates(Q tgd, String eVariable, String uVariable) {

        Set<Variable> eVariables = Set.of(tgd.getExistential());

        Map<Term, Term> substitution = new HashMap<>();
        int ecounter = 1;
        int ucounter = 1;
        List<Atom> headAtoms = Arrays.asList(tgd.getHeadAtoms());
        headAtoms.sort(new AtomComparator());
        for(Atom a : headAtoms) {
            for (Variable v : a.getVariables()) {
                if (eVariables.contains(v) && !substitution.containsKey(v))
                    substitution.put(v, Variable.create(eVariable + ecounter++));
                else if (!substitution.containsKey(v))
                    substitution.put(v, Variable.create(uVariable + ucounter++));
            }
        }

        List<Atom> bodyAtoms = Arrays.asList(tgd.getBodyAtoms());
        bodyAtoms.sort(new AtomComparator());
        for(Atom a : bodyAtoms) {
            for (Variable v : a.getVariables()) {
                if (!substitution.containsKey(v))
                    substitution.put(v, Variable.create(uVariable + ucounter++));
            }
        }

        App.logger.fine("VNF substitution:\n" + substitution);

        Q applySubstitution = (Q) Logic.applySubstitution(tgd, substitution);
        App.logger.fine("VNF: " + tgd + "===>>>" + applySubstitution);
        return applySubstitution;

    }

    public class AtomComparator implements Comparator<Atom> {
        @Override
        public int compare(Atom x, Atom y) {
            return x.getPredicate().getName().compareTo(y.getPredicate().getName());
        }
    }

    /**
     *
     * Returns the Head Normal Form (HNF)
     * @param tgd
     *
     * @return Head Normal Form of tgd
     */
    public Collection<Q> computeHNF(Q tgd) {

        Variable[] eVariables = tgd.getExistential();

        Set<Atom> eHead = new HashSet<>();
        Set<Atom> fHead = new HashSet<>();

        Set<Atom> bodyAtoms = tgd.getBodySet();
        for (Atom a : tgd.getHeadAtoms())
            if (a.equals(TGD.Bottom))
                // remove all head atoms since ⊥ & S ≡ ⊥ for any conjunction S
                return Set.of(constructor.create(bodyAtoms, Set.of(TGD.Bottom)));
            else if (Logic.containsAny(a, eVariables))
                eHead.add(a);
            else if (!bodyAtoms.contains(a))
                // Do not add atoms that already appear in the body.
                // This is only needed for fHead since we have no existentials in the body
                fHead.add(a);

        if (tgd.getHeadAtoms().length == eHead.size() || tgd.getHeadAtoms().length == fHead.size())
            return Set.of(tgd);

        Collection<Q> result = new HashSet<>();
        if (!eHead.isEmpty())
            result.add(constructor.create(bodyAtoms, eHead));
        if (!fHead.isEmpty())
            result.add(constructor.create(bodyAtoms, fHead));
        return result;
    }

    private static interface Constructor<T extends TGD> {
        T create(Set<Atom> bodyAtoms, Set<Atom> headAtoms);
    }

    private static class TGDConstructor implements Constructor<TGD> {

		@Override
		public TGD create(Set<Atom> bodyAtoms, Set<Atom> headAtoms) {
			return new TGD(bodyAtoms, headAtoms);
		}
    }

    private static class GTGDConstructor implements Constructor<GTGD> {

		@Override
		public GTGD create(Set<Atom> bodyAtoms, Set<Atom> headAtoms) {
			return new GTGD(bodyAtoms, headAtoms);
		}
    }
}
