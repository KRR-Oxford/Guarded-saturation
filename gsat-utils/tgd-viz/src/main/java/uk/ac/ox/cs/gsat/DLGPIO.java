package uk.ac.ox.cs.gsat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.AtomSet;
import fr.lirmm.graphik.graal.api.core.ConjunctiveQuery;
import fr.lirmm.graphik.graal.api.core.NegativeConstraint;
import fr.lirmm.graphik.graal.api.core.Predicate;
import fr.lirmm.graphik.graal.api.core.Query;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.api.io.ParseException;
import fr.lirmm.graphik.graal.api.io.Parser;
import fr.lirmm.graphik.graal.io.dlp.DlgpParser;
import fr.lirmm.graphik.util.Prefix;
import fr.lirmm.graphik.util.stream.CloseableIterator;
import fr.lirmm.graphik.util.stream.IteratorException;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.UntypedConstant;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * DLGPIO
 */
public class DLGPIO {

    protected static final boolean NEGATIVE_CONSTRAINT = false;

    protected String path;
    protected boolean saturationOnly;
    protected HashSet<Atom> atoms;
    protected HashSet<AtomSet> atomSets;
    protected HashSet<Rule> rules;
    protected HashSet<Query> queries;
	protected Map<String, String> prefixes = new HashMap<>();

    public DLGPIO(String path, boolean saturationOnly) {
        this.path = path;
        this.saturationOnly = saturationOnly;
    }

    public Collection<Dependency> getRules() throws Exception {

        parseInput(new DlgpParser(new File(path)));
        return getPDQTGDsFromGraalRules(rules, this.prefixes);

    }

    protected void parseInput(Parser<Object> parser) throws ParseException {

        prefixes = new HashMap<>();
        atoms = new HashSet<>();
        atomSets = new HashSet<>();
        rules = new HashSet<>();
        queries = new HashSet<>();

        while (parser.hasNext()) {
            Object o = parser.next();

            if (o instanceof Prefix) {
                Prefix p = (Prefix) o;
                prefixes.put(p.getPrefixName(), p.getPrefix());
            }
            
            if (o instanceof Atom && !saturationOnly) {
                // App.logger.fine("Atom: " + ((Atom) o));
                atoms.add((Atom) o);
            } else if (o instanceof AtomSet && !saturationOnly) {
                // App.logger.fine("Atom Set: " + (AtomSet) o);
                atomSets.add((AtomSet) o);
            } else if (o instanceof Rule) {
                // App.logger.fine("Rule: " + ((Rule) o));
                if (!((Rule) o).getHead().iterator().next().getPredicate().equals(Predicate.BOTTOM) || NEGATIVE_CONSTRAINT)
                    rules.add((Rule) o);
            } else if (o instanceof ConjunctiveQuery) {
                // App.logger.fine("Conjunctive Query: " + ((Query) o));
                queries.add((Query) o);
            } else if (o instanceof NegativeConstraint) {
                // App.logger.fine("Negative Constraint: " + ((NegativeConstraint) o));
                if (NEGATIVE_CONSTRAINT)
                    rules.add((NegativeConstraint) o);
            }
        }

        parser.close();

        System.out.println("# Rules: " + rules.size() + "; # Atoms: " + atoms.size() + "; # AtomSets: "
                + atomSets.size() + "; # Queries: " + queries.size() + "; # Constraints: "
                + rules.stream().filter(r -> r instanceof NegativeConstraint).count());

    }


    public Collection<uk.ac.ox.cs.pdq.fol.Atom> getQueries() {
        return getPDQAtomsFromGraalQueries(queries);
    }


    public String getBaseOutputPath() {

        Path testName = Paths.get(path).getFileName();

        if (testName == null) {
            System.err.println("Path not correct. Using the default value 'dlgp'");
            return "dlgp";
        }

        return testName.toString();

    }

    public static Collection<Dependency> getPDQTGDsFromGraalRules(Collection<Rule> rules, Map<String, String> predicateRenaming) throws IteratorException {

        Collection<Dependency> tgds = new LinkedList<>();

        for (Rule rule : rules) {
            Collection<uk.ac.ox.cs.pdq.fol.Atom> body = getPDQAtomsFromGraalAtomSet(rule.getBody(), predicateRenaming);
            Collection<uk.ac.ox.cs.pdq.fol.Atom> head = getPDQAtomsFromGraalAtomSet(rule.getHead(), predicateRenaming);

            if (!body.isEmpty())
                if (rule instanceof NegativeConstraint)
                    tgds.add(TGD.create(body.toArray(new uk.ac.ox.cs.pdq.fol.Atom[body.size()]),
                            new uk.ac.ox.cs.pdq.fol.Atom[] { }));
                else if (!head.isEmpty())
                    tgds.add(TGD.create(body.toArray(new uk.ac.ox.cs.pdq.fol.Atom[body.size()]),
                            head.toArray(new uk.ac.ox.cs.pdq.fol.Atom[head.size()])));
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
