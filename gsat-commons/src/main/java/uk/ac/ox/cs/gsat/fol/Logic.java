package uk.ac.ox.cs.gsat.fol;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Conjunction;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.fol.Disjunction;
import uk.ac.ox.cs.pdq.fol.Formula;
import uk.ac.ox.cs.pdq.fol.FunctionTerm;
import uk.ac.ox.cs.pdq.fol.Implication;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * Helper functions for logic-based operations
 * 
 * @author Stefano
 */
public class Logic {

    static Map<ArgList, Formula> cache = new ConcurrentHashMap<>();

    private static class ArgList {
        public Formula formula;
        public Map<Term, Term> substitution;

        public ArgList(Formula  formula, Map<Term, Term> substitution) {
            this.formula = formula;
            this.substitution = substitution;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ArgList)) {
                return false;
            }

            ArgList argList = (ArgList) o;

            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            return this.formula.equals(argList.formula) && this.substitution.equals(argList.substitution);
        }

        @Override
        public int hashCode() {
            return formula.hashCode() + 7 * substitution.hashCode();
        }
    }
    
	public static boolean isFull(uk.ac.ox.cs.pdq.fol.TGD tgd) {

		return tgd.getExistential().length == 0;

	}
    public static Formula applySubstitution(Formula formula, Map<Term, Term> substitution) {
        return cache.computeIfAbsent(new ArgList(formula, substitution),args -> applySubstitutionFunc(args.formula, args.substitution));
    }
    
	/**
	 * From PDQ code, slightly modified
	 */
	static Formula applySubstitutionFunc(Formula formula, Map<Term, Term> substitution) {
		if (formula instanceof Conjunction) {
			Formula child1 = applySubstitutionFunc(((Conjunction) formula).getChildren()[0], substitution);
			Formula child2 = applySubstitutionFunc(((Conjunction) formula).getChildren()[1], substitution);
			return Conjunction.create(child1, child2);
		} else if (formula instanceof Disjunction) {
			Formula child1 = applySubstitutionFunc(((Disjunction) formula).getChildren()[0], substitution);
			Formula child2 = applySubstitutionFunc(((Disjunction) formula).getChildren()[1], substitution);
			return Disjunction.of(child1, child2);
		} else if (formula instanceof Implication) {
			Formula child1 = applySubstitutionFunc(((Implication) formula).getChildren()[0], substitution);
			Formula child2 = applySubstitutionFunc(((Implication) formula).getChildren()[1], substitution);
			return Implication.of(child1, child2);
		} else if (formula instanceof ConjunctiveQuery) {
			Atom[] atoms = ((ConjunctiveQuery) formula).getAtoms();
			Formula[] bodyAtoms = new Formula[atoms.length];
			for (int atomIndex = 0; atomIndex < atoms.length; ++atomIndex)
				bodyAtoms[atomIndex] = applySubstitutionFunc(atoms[atomIndex], substitution);
			return Conjunction.create(bodyAtoms);
		} else if (formula instanceof TGD) {
			Atom[] headAtoms = ((TGD) formula).getHeadAtoms();
			Set<Atom> headAtomsF = new HashSet<>();
			Atom[] bodyAtoms = ((TGD) formula).getBodyAtoms();
			Set<Atom> bodyAtomsF = new HashSet<>();
			for (int atomIndex = 0; atomIndex < headAtoms.length; ++atomIndex)
				headAtomsF.add((Atom) applySubstitutionFunc(headAtoms[atomIndex], substitution));
			for (int atomIndex = 0; atomIndex < bodyAtoms.length; ++atomIndex)
				bodyAtomsF.add((Atom) applySubstitutionFunc(bodyAtoms[atomIndex], substitution));
            if (formula instanceof SkGTGD) {
                if (formula instanceof OrderedSkGTGD) {
                    return new OrderedSkGTGD(bodyAtomsF, headAtomsF);
                } else {
                    return new SkGTGD(bodyAtomsF, headAtomsF);
                }
            } else if (formula instanceof GTGD)
                return new GTGD(bodyAtomsF, headAtomsF);
            else
                return new TGD(bodyAtomsF, headAtomsF);
        } else if (formula instanceof Atom) {
            Term[] aterms = ((Atom) formula).getTerms();
            Term[] nterms = applySubstitution(aterms, substitution);
            // before creating a new atom, we check if the substitution changes its terms
            if (aterms != nterms)
                return Atom.create(((Atom) formula).getPredicate(), nterms);
            else {
                return formula;
            }
        }
        throw new RuntimeException("Unsupported formula type: " + formula);
    }

    public static Term[] applySubstitution(Term[] terms, Map<Term, Term> substitution) {
        Term[] nterms = new Term[terms.length];
        boolean isSubstitutionApplied = false;
        for (int termIndex = 0; termIndex < nterms.length; ++termIndex) {
            Term term = terms[termIndex];

            Term substitute = substitution.get(term);
            // if the term substitution is a function term,
            // we need to also apply the substitution to the function term 
            if (substitute != null && substitute instanceof FunctionTerm) {
                isSubstitutionApplied = true;
                term = substitute;
            }
            // we handle the case of function term, using a recursive call
            if (term instanceof FunctionTerm) {
                Term[] oldfterms = ((FunctionTerm) term).getTerms();
                Term[] fterms = applySubstitution(((FunctionTerm) term).getTerms(), substitution);
                // if the substitution changes at least one term, we create a new function term
                if (fterms != oldfterms) {
                    isSubstitutionApplied = true;
                    nterms[termIndex] = FunctionTerm.create(((FunctionTerm) term).getFunction(), fterms);
                } else {
                    nterms[termIndex] = term;
                }
                continue;
            }

            // handle case where the term nor the substitute are function terms
            if (substitute != null) {
                isSubstitutionApplied = true;
                nterms[termIndex] = substitute;
            } else {
                nterms[termIndex] = term;
            }
        }

        if (isSubstitutionApplied) 
            return nterms;
        else 
            return terms;
    }

	static boolean containsAny(Atom atom, Variable[] eVariables) {
		for (Variable v : eVariables)
			for (Variable va : atom.getVariables())
				if (v.equals(va))
					return true;
		return false;
	}

	public static Map<Term, Term> getMGU(Atom s, Atom t) {

		return getMGU(s, t, new HashMap<Term, Term>());

	}

	// If we found substitutions a->b->c->...->y->z with z not substituted
	// by anything, then this function will update the map with substitutions a->z,
	// b->z, ..., y->z, and return z
	// It might make sense to use this in applySubstitution, instead of getMGU
	private static Term getSubstitute(Term a, Map<Term, Term> substitution) {
		if (substitution.containsKey(a)) {
			Term value = getSubstitute(substitution.get(a), substitution);
			substitution.put(a, value);
			return value;
		}
		return a;
	}

	// returns null if there is no MGU
	// otherwise a map containing the substitution
	public static Map<Term, Term> getMGU(Atom s, Atom t, Map<Term, Term> renaming) {

		if (!s.getPredicate().equals(t.getPredicate()))
			// throw new IllegalArgumentException("Cannot compute MGU of atoms with
			// different predicate names or arity: "+ s + " and "+ t);
			return null;

		Map<Term, Term> sigma = new HashMap<>(renaming);

        sigma = getMGU(s.getTerms(), t.getTerms(), sigma);

        if (sigma == null)
            return null;

		for (Entry<Term, Term> entry : sigma.entrySet())
			// we don't care about the actual value, we just want for everything to point to
			// the last value in the chain
			getSubstitute(entry.getKey(), sigma);

		return sigma;
	}

    // Warning: the mgu computation is optimized such that null is returned when a nested Skolem term
    // occurs, since they should not occur in our algorithms
    public static Map<Term, Term> getMGU(Term[] s, Term[] t, Map<Term, Term> sigma) {

		for (int i = 0; i < s.length; i++) {
            // first we get the representative of the term class
            // to which belongs the current term
            // according to the current unifier sigma
			Term s_term = getSubstitute(s[i], sigma);
            Term t_term = getSubstitute(t[i], sigma);


            // System.out.println("s: " + s[i] + "("+ s[i].isVariable() +") --> " + s_term);
            // System.out.println("t: " + t[i] + " --> " + t_term);
            // System.out.println("sigma: " + sigma);

            // // if the term already belong to the same class of term
            // there is nothing to do
			if (s_term.equals(t_term))
                continue;

            // if the representative are both skolem term
            if (s_term instanceof FunctionTerm && t_term instanceof FunctionTerm) {
                FunctionTerm s_sko = (FunctionTerm) s_term;
                FunctionTerm t_sko = (FunctionTerm) t_term;

                // if the function symbol are different, then there is a clash
                if (!s_sko.getFunction().equals(t_sko.getFunction()))
                    return null;

                // else we unify the terms in arguments and continue
                sigma = getMGU(s_sko.getTerms(), t_sko.getTerms(), sigma);
                // System.out.println("fsig: " + sigma);
                if (sigma != null)
                    continue;
                else
                    return null;
            }

            // if none of the terms are variables, there is a clash
            if (!s_term.isVariable() && !t_term.isVariable()) {
                return null;
            }

            // in case, we unify a variable with a skolem term
            // we check that the variable is not contain into one of the class
            // of the variables beloging to the skolem term.
            if (s_term instanceof FunctionTerm || t_term instanceof FunctionTerm) {
                FunctionTerm sko = (s_term instanceof FunctionTerm) ? (FunctionTerm) s_term : (FunctionTerm) t_term;
                Term var = (s_term.isVariable()) ? s_term : t_term;

                for (Variable v : sko.getVariables()) {
                    Term sub = getSubstitute(v, sigma);
                    // since the algorithm we consider do not derive any Skolem nesting
                    // we return null, as soon as a nesting occurs
                    if (sub instanceof FunctionTerm || sub.equals(var))
                        return null;
                }
            }

            if (s_term.isVariable())
                sigma.put(s_term, t_term);
            else 
                sigma.put(t_term, s_term);
		}

        return sigma;
    }

    public static Map<Term, Term> getVariableSubstitution(List<Atom> atoms1, List<Atom> atoms2) {

        Map<Term, Term> sigma = new HashMap<>();

        if (atoms1.size() != atoms2.size())
            return null;

        // assume they are all in the same order
        for (int i = 0; i < atoms1.size(); i++) {
            Atom atom1 = atoms1.get(i);
            Atom atom2 = atoms2.get(i);

            sigma = Logic.getMGU(atom1, atom2, sigma);

            if (sigma == null)
                return null;
        }

        return sigma;
    }

    public static GTGD applyMGU(GTGD tgd, Map<Term, Term> mgu) {

        return applyMGU(tgd.getBodySet(), tgd.getHeadSet(), mgu);

    }

    public static GTGD applyMGU(Set<Atom> bodyAtoms, Set<Atom> headAtoms, Map<Term, Term> mgu) {

        return new GTGD(applyMGU(bodyAtoms, mgu), applyMGU(headAtoms, mgu));

    }

    public static Set<Atom> applyMGU(Collection<Atom> atoms, Map<Term, Term> mgu) {
        Set<Atom> result = new HashSet<Atom>();

        for (Atom a : atoms)
            result.add((Atom) Logic.applySubstitution(a, mgu));

        return result;
    }
}
