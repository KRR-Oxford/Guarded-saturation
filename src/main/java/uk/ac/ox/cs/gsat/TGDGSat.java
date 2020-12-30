package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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

    private class Trie {
        // this is ugly, need to clean up
        public HashMap<Predicate, Trie> predicates = new HashMap<>();
        public HashMap<Term, Trie> nextConsts = new HashMap<>();
        public HashMap<Integer, Trie> nextVars = new HashMap<>();

        public void add(Atom atom) {
            Predicate pred = atom.getPredicate();
            if (!predicates.containsKey(pred)) {
                predicates.put(pred, new Trie());
            }
            predicates.get(pred).addFromIndex(atom, new HashMap<Term, Integer>(), 0);
        }

        public void addFromIndex(Atom atom, HashMap<Term, Integer> termIndex, int index) {
            if (index == atom.getNumberOfTerms()) {
                return;
            }
            Term term = atom.getTerm(index);
            if (term.isUntypedConstant()) {
                if (!nextConsts.containsKey(term))
                    nextConsts.put(term, new Trie());

                nextConsts.get(term).addFromIndex(atom, termIndex, index + 1);
            } else {
                int previousIndex = termIndex.containsKey(term) ? termIndex.get(term) : index;
                termIndex.put(term, index);
                if (!nextVars.containsKey(previousIndex))
                    nextVars.put(previousIndex, new Trie());
                nextVars.get(previousIndex).addFromIndex(atom, termIndex, index + 1);
            }
        }
    }

    private final Set<Atom> bodySet;
    private final Set<Atom> headSet;
    private Trie headTrie;
    private Trie bodyTrie;
    private Set<Predicate> headPredicates;
    private Set<Predicate> bodyPredicates;

    private final Atom guard;

    public boolean bodySubsumes(Atom atom) {
        return subsumes(bodyTrie, atom);
    }

    public boolean headSubsumes(Atom atom) {
        return subsumes(headTrie, atom);
    }

    private boolean subsumes(Trie trie, Atom atom) {
        Predicate pred = atom.getPredicate();
        if (!trie.predicates.containsKey(pred))
            return false;
        return subsumesFromIndex(trie.predicates.get(pred), atom, new HashMap<>(), 0);
    }

    private boolean subsumesFromIndex(Trie trie, Atom atom, HashMap<Term, List<Integer>> termIndex, int index) {
        // we reached the end
        if (index == atom.getNumberOfTerms())
            return true;
        Term term = atom.getTerm(index);
        if (!termIndex.containsKey(term)) {
            termIndex.put(term, new ArrayList<Integer>());
        }
        List<Integer> termPositions = termIndex.get(term);

        // try a variable that hasn't appeared so far first
        if (trie.nextVars.containsKey(index) && subsumesFromIndex(trie.nextVars.get(index), atom, termIndex, index + 1))
            return true;
        for (Integer v : termPositions) {
            if (trie.nextVars.containsKey(v) && subsumesFromIndex(trie.nextVars.get(v), atom, termIndex, index + 1))
                return true;
        }
        termPositions.add(index);
        // If this is a constant additionally check if we can subsume with an identical
        // constant
        if (term.isUntypedConstant()) {
            // check if this exact constant is subsumed
            if (trie.nextConsts.containsKey(term)
                    && subsumesFromIndex(trie.nextConsts.get(term), atom, termIndex, index + 1))
                return true;
        }
        return false;
    }

    private TGDGSat(Atom[] body, Atom[] head) {

        super(body, head);

        bodySet = Set.of(body);
        headSet = Set.of(head);

        guard = computeGuard();

        bodyTrie = new Trie();
        headTrie = new Trie();
        bodyPredicates = new HashSet<>();
        headPredicates = new HashSet<>();
        // // compute predicates and build tries
        for (Atom atom : body) {
            bodyPredicates.add(atom.getPredicate());
            bodyTrie.add(atom);
        }
        for (Atom atom : head) {
            headPredicates.add(atom.getPredicate());
            headTrie.add(atom);
        }

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
        Collection<String> result = new LinkedList<>(); // why is this a list? TODO

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

    // This should definitely be possible to optimise
    // Though it is likely not important, as does not change complexity
    public Atom computeGuard() {

        List<Variable> universalList = Arrays.asList(getUniversal());

        Atom currentGuard = null;
        for (Atom atom : getBodySet()) // is this not sorted? I guess TODO, it isn't
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