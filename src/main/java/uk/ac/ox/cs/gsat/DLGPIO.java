package uk.ac.ox.cs.gsat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.AtomSet;
import fr.lirmm.graphik.graal.api.core.ConjunctiveQuery;
import fr.lirmm.graphik.graal.api.core.Predicate;
import fr.lirmm.graphik.graal.api.core.Query;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.io.dlp.DlgpParser;
import fr.lirmm.graphik.util.stream.CloseableIterator;
import fr.lirmm.graphik.util.stream.IteratorException;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.UntypedConstant;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * DLGPIO
 */
public class DLGPIO implements ExecutionSteps {

    protected String path;
    protected boolean gSatOnly;
    private HashSet<Atom> atoms;
    protected HashSet<Rule> rules;
    private HashSet<Query> queries;

    public DLGPIO(String path, boolean gSatOnly) {
        this.path = path;
        this.gSatOnly = gSatOnly;
    }

    @Override
    public Collection<Dependency> getRules() throws Exception {

        atoms = new HashSet<>();
        rules = new HashSet<>();
        queries = new HashSet<>();

        DlgpParser parser = new DlgpParser(new File(path));

        while (parser.hasNext()) {
            Object o = parser.next();
            if (o instanceof Atom && !gSatOnly) {
                App.logger.fine("Atom: " + ((Atom) o));
                atoms.add((Atom) o);
            } else if (o instanceof Rule) {
                App.logger.fine("Rule: " + ((Rule) o));
                rules.add((Rule) o);
            } else if (o instanceof ConjunctiveQuery) {
                App.logger.fine("Conjunctive Query: " + ((Query) o));
                queries.add((Query) o);
            }
        }

        parser.close();

        System.out
                .println("# Rules: " + rules.size() + "; # Atoms: " + atoms.size() + "; # Queries: " + queries.size());

        return getPDQTGDsFromGraalRules(rules);

    }

    @Override
    public void writeData(String path) throws IOException {

        Collection<uk.ac.ox.cs.pdq.fol.Atom> pdqAtoms = getPDQAtomsFromGraalAtoms(atoms);
        System.out.println("# PDQ Atoms: " + pdqAtoms.size());

        IO.writeDatalogFacts(pdqAtoms, path);

    }

    @Override
    public Collection<uk.ac.ox.cs.pdq.fol.Atom> getQueries() {
        return getPDQAtomsFromGraalQueries(queries);
    }

    @Override
    public String getBaseOutputPath() {

        Path testName = Paths.get(path).getFileName();

        if (testName == null) {
            System.err.println("Path not correct. The system will now terminate.");
            System.exit(1);
        }

        return "test" + File.separator + "datalog" + File.separator + testName + File.separator;

    }

    public static Collection<Dependency> getPDQTGDsFromGraalRules(Collection<Rule> rules) throws IteratorException {

        Collection<Dependency> tgds = new LinkedList<>();

        for (Rule rule : rules) {
            Collection<uk.ac.ox.cs.pdq.fol.Atom> body = getPDQAtomsFromGraalAtomSet(rule.getBody());
            Collection<uk.ac.ox.cs.pdq.fol.Atom> head = getPDQAtomsFromGraalAtomSet(rule.getHead());
            if (!body.isEmpty() && !head.isEmpty())
                tgds.add(TGD.create(body.toArray(new uk.ac.ox.cs.pdq.fol.Atom[body.size()]),
                        head.toArray(new uk.ac.ox.cs.pdq.fol.Atom[head.size()])));
        }

        return tgds;

    }

    public static Collection<uk.ac.ox.cs.pdq.fol.Atom> getPDQAtomsFromGraalAtomSets(Collection<AtomSet> atomSets)
            throws IteratorException {

        Collection<uk.ac.ox.cs.pdq.fol.Atom> atoms = new LinkedList<>();

        for (AtomSet atomSet : atomSets)
            atoms.addAll(getPDQAtomsFromGraalAtomSet(atomSet));

        return atoms;

    }

    public static Collection<uk.ac.ox.cs.pdq.fol.Atom> getPDQAtomsFromGraalAtomSet(AtomSet atomSet)
            throws IteratorException {

        Collection<uk.ac.ox.cs.pdq.fol.Atom> atoms = new LinkedList<>();

        CloseableIterator<Atom> it = atomSet.iterator();

        while (it.hasNext()) {

            Atom next = it.next();

            uk.ac.ox.cs.pdq.fol.Atom pdqAtomFromGraalAtom = getPDQAtomFromGraalAtom(next.getPredicate(),
                    next.getTerms());

            if (pdqAtomFromGraalAtom != null)
                atoms.add(pdqAtomFromGraalAtom);

        }

        return atoms;

    }

    public static uk.ac.ox.cs.pdq.fol.Atom getPDQAtomFromGraalAtom(Predicate predicate, Collection<Term> terms) {

        uk.ac.ox.cs.pdq.fol.Predicate pdqPredicateFromGraalPredicate = getPDQPredicateFromGraalPredicate(predicate);

        if (pdqPredicateFromGraalPredicate == null)
            return null;

        return uk.ac.ox.cs.pdq.fol.Atom.create(pdqPredicateFromGraalPredicate, getPDQTermsFromGraalTerms(terms));

    }

    public static Collection<uk.ac.ox.cs.pdq.fol.Atom> getPDQAtomsFromGraalAtoms(Collection<Atom> atoms)
            throws IteratorException {

        Collection<uk.ac.ox.cs.pdq.fol.Atom> pdqAtoms = new LinkedList<>();

        for (Atom atom : atoms) {

            uk.ac.ox.cs.pdq.fol.Atom pdqAtomFromGraalAtom = getPDQAtomFromGraalAtom(atom.getPredicate(),
                    atom.getTerms());

            if (pdqAtomFromGraalAtom != null)
                pdqAtoms.add(pdqAtomFromGraalAtom);

        }

        return pdqAtoms;

    }

    public static uk.ac.ox.cs.pdq.fol.Predicate getPDQPredicateFromGraalPredicate(Predicate predicate) {

        if (predicate.equals(Predicate.TOP) || predicate.equals(Predicate.BOTTOM)
                || predicate.equals(Predicate.EQUALITY))
            return null;

        return uk.ac.ox.cs.pdq.fol.Predicate.create(predicate.getIdentifier().toString(), predicate.getArity());

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

    public static Collection<uk.ac.ox.cs.pdq.fol.Atom> getPDQAtomsFromGraalQueries(HashSet<Query> query) {
        // TODO
        return null;
    }

}