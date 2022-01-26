package uk.ac.ox.cs.gsat.io;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.AtomSet;
import fr.lirmm.graphik.graal.api.core.NegativeConstraint;
import fr.lirmm.graphik.graal.api.core.Predicate;
import fr.lirmm.graphik.graal.api.core.Query;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.util.stream.CloseableIterator;
import fr.lirmm.graphik.util.stream.IteratorException;
import uk.ac.ox.cs.gsat.fol.GTGD;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.pdq.fol.UntypedConstant;
import uk.ac.ox.cs.pdq.fol.Variable;

public class GraalConvertor {

    public static Set<TGD> getPDQTGDsFromGraalRules(Collection<Rule> rules, Map<String, String> predicateRenaming) throws IteratorException {

        Set<TGD> tgds = new HashSet<>();

        for (Rule rule : rules) {
            Collection<uk.ac.ox.cs.pdq.fol.Atom> body = getPDQAtomsFromGraalAtomSet(rule.getBody(), predicateRenaming);
            Collection<uk.ac.ox.cs.pdq.fol.Atom> head = getPDQAtomsFromGraalAtomSet(rule.getHead(), predicateRenaming);

            if (!body.isEmpty())
                if (rule instanceof NegativeConstraint)
                    tgds.add(new TGD(body, List.of(GTGD.Bottom)));
                else if (!head.isEmpty())
                    tgds.add(new TGD(body, head));
        }

        return tgds;

    }

    public static Collection<uk.ac.ox.cs.pdq.fol.Atom> getPDQAtomsFromGraalAtomSets(Collection<AtomSet> atomSets, Map<String, String> predicateRenaming)
            throws IteratorException {

        Collection<uk.ac.ox.cs.pdq.fol.Atom> atoms = new LinkedList<>();

        for (AtomSet atomSet : atomSets)
            atoms.addAll(getPDQAtomsFromGraalAtomSet(atomSet, predicateRenaming));

        return atoms;

    }

    public static Collection<uk.ac.ox.cs.pdq.fol.Atom> getPDQAtomsFromGraalAtomSet(AtomSet atomSet, Map<String, String> predicateRenaming)
            throws IteratorException {

        Collection<uk.ac.ox.cs.pdq.fol.Atom> atoms = new LinkedList<>();

        CloseableIterator<Atom> it = atomSet.iterator();

        while (it.hasNext()) {

            Atom next = it.next();

            uk.ac.ox.cs.pdq.fol.Atom pdqAtomFromGraalAtom = getPDQAtomFromGraalAtom(next.getPredicate(),
                                                                                    next.getTerms(), predicateRenaming);

            if (pdqAtomFromGraalAtom != null)
                atoms.add(pdqAtomFromGraalAtom);

        }

        return atoms;

    }

    public static uk.ac.ox.cs.pdq.fol.Atom getPDQAtomFromGraalAtom(Predicate predicate, Collection<Term> terms, Map<String, String> predicateRenaming) {

        uk.ac.ox.cs.pdq.fol.Predicate pdqPredicateFromGraalPredicate = getPDQPredicateFromGraalPredicate(predicate, predicateRenaming);

        if (pdqPredicateFromGraalPredicate == null)
            return null;

        return uk.ac.ox.cs.pdq.fol.Atom.create(pdqPredicateFromGraalPredicate, getPDQTermsFromGraalTerms(terms));

    }

    public static Collection<uk.ac.ox.cs.pdq.fol.Atom> getPDQAtomsFromGraalAtoms(Collection<Atom> atoms, Map<String, String> predicateRenaming)
            throws IteratorException {

        Collection<uk.ac.ox.cs.pdq.fol.Atom> pdqAtoms = new LinkedList<>();

        for (Atom atom : atoms) {

            uk.ac.ox.cs.pdq.fol.Atom pdqAtomFromGraalAtom = getPDQAtomFromGraalAtom(atom.getPredicate(),
                                                                                    atom.getTerms(), predicateRenaming);

            if (pdqAtomFromGraalAtom != null)
                pdqAtoms.add(pdqAtomFromGraalAtom);

        }

        return pdqAtoms;

    }

    public static uk.ac.ox.cs.pdq.fol.Predicate getPDQPredicateFromGraalPredicate(Predicate predicate, Map<String, String> predicateRenaming) {

        if (predicate.equals(Predicate.TOP) || predicate.equals(Predicate.BOTTOM)
                || predicate.equals(Predicate.EQUALITY))
            return null;

        String name = predicate.getIdentifier().toString();

        for (String replacement : predicateRenaming.keySet())
            name = name.replaceFirst(predicateRenaming.get(replacement), replacement);

        return uk.ac.ox.cs.pdq.fol.Predicate.create(name, predicate.getArity());

    }

    public static uk.ac.ox.cs.pdq.fol.Term[] getPDQTermsFromGraalTerms(Collection<Term> terms) {

        Collection<uk.ac.ox.cs.pdq.fol.Term> PDQterms = new LinkedList<>();

        for (Term term : terms) {

            String term_label = term.toString();
            if (term_label != null && term_label.length() > 2 && term_label.substring(0, 2).equals("_:"))
                PDQterms.add(UntypedConstant.create(term_label));
            else if (term.isConstant() || term.isLiteral())
                PDQterms.add(UntypedConstant.create(term_label));
            else if (term.isVariable())
                PDQterms.add(Variable.create(term_label));
            else
                throw new IllegalArgumentException("Unknown Term " + term + " of " + term.getClass());

        }

        return PDQterms.toArray(new uk.ac.ox.cs.pdq.fol.Term[PDQterms.size()]);

    }

    public static Collection<uk.ac.ox.cs.pdq.fol.Atom> getPDQAtomsFromGraalQueries(HashSet<Query> queries) {
        // TODO
        return null;
    }

}
