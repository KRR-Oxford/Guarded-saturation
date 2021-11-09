package uk.ac.ox.cs.gsat.unification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.ox.cs.gsat.GTGD;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Constant;
import uk.ac.ox.cs.pdq.fol.Function;
import uk.ac.ox.cs.pdq.fol.FunctionTerm;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Term;

/**
 * Implementation of path unification index proposed in 
 * The path-indexing Method for Indexing Terms - Mark E. Stickel
 * specialised for our usage: 
 * the "terms" we consider are atoms, where the predicate can be considered as a function symbol,
 *    therefore all our paths will start with a predicate symbol
 */
public class AtomPathUnificationIndex<Q extends GTGD> implements UnificationIndex<Q> {

    private final Node<Q> root;

    public AtomPathUnificationIndex() {
        this.root = Node.createRoot();
    }

    @Override
    public Set<Q> get(Atom atom) {

        Predicate p = atom.getPredicate();
        Node<Q> pNode = this.root.lookup_s(p);
        if (pNode != null) {

            Term[] terms = atom.getTerms();
            return get(pNode, terms);
        } else {
            return new HashSet<>();
        }
    }

    // if the node represents the path P.s
    // where s is function or predicate symbol
    // terms are [t1, ..., tn]
    // it computes the GetUnifiables(P, s(t1, ..., tn))
    public Set<Q> get(Node<Q> node, Term[] terms) {

        boolean containsAtLeastOneFunctionTermOrConstant = false;
        for (Term t : terms) {
            if (t instanceof FunctionTerm || t instanceof Constant) {
                containsAtLeastOneFunctionTermOrConstant = true;
                break;
            }
        }

        // we distinguish two cases whether there is a function term
        // among the terms
        if (containsAtLeastOneFunctionTermOrConstant) {

            // we instantiate the candidate set with the first candidate
            // in order to compute the intersection
            // represents GetUnifiables(P, s(t1, ..., tn)) = intersection_i
            // GetUnifiables(P.s.i, ti)
            Set<Q> candidates = null;
            // the tCandidates are the candidates for unfications for the path
            // tCandidates represents GetUnifiables(P.s.i, ti)
            Set<Q> tCandidates = new HashSet<>();
            for (int i = 0; i < terms.length; i++) {
                Term t = terms[i];
                if (t.isVariable()) {
                    // in this case GetUnifiables(P.s.i, ti) = Terms
                    // so it will not change the intersection
                    continue;
                }

                Node<Q> iNode = node.lookup_i(i);
                
                if (t instanceof FunctionTerm) {
                    Function f = ((FunctionTerm) t).getFunction();

                    //GetUnifiables(P.s.i, f(u1, ...,uk)) = GetTerms(P.s.i, *) U
                    // \cap_j GetUnifiables(P.s.i.f.j, uj)

                    // add GetTerms(P.s.i, *)
                    tCandidates.addAll(iNode.getTGDs());

                    Node<Q> fNode = iNode.lookup_s(f);
                    if (fNode != null) {
                        // add \cap_j GetUnifiables(P.s.i.f.j, uj)
                        tCandidates.addAll(get(fNode, ((FunctionTerm)t).getTerms()));
                    }
                }  else if (t instanceof Constant) {
                    // GetUnifiables(P.s.i, c) = GetTerms(P.s.i, *) U GetTerms(P.s.i, c)

                    // add GetTerms(P.s.i, *)
                    tCandidates.addAll(iNode.getTGDs());

                    Node<Q> cNode = iNode.lookup_s(t);
                    if (cNode != null)
                        tCandidates.addAll(cNode.getTGDs());
                }

                if (candidates != null)
                    candidates.retainAll(tCandidates);
                else
                    candidates = new HashSet<>(tCandidates);

                tCandidates.clear();
            }
            return candidates;
        } else {
            // GetUnifiables(<>, p(x1, ..., xn)) = GetTerms(<>, *) U GetTerms(<>, p)
            // GetTerms(<>, *) is empty, since every terms start with a predicate symbol
            return node.getTGDs();
        }
    }

    @Override
    public void put(Atom atom, Q tgd) {

        Predicate p = atom.getPredicate();
        Node<Q> pNode = this.root.lookup_s(p);

        if (pNode == null) {
            pNode = new Node<>(p.getArity());
            this.root.put_s(p, pNode);
        }

        pNode.add(tgd);
        put(pNode, atom.getTerms(), tgd);
    }

    // recurve function to add tgd to a node corresponding to the path P.i.ti where
    // ti is in terms at the index i
    private void put(Node<Q> node, Term[] terms, Q tgd) {

        for (int i = 0; i < terms.length; i++) {
            Term t = terms[i];
            Node<Q> iNode = node.lookup_i(i);
            if (t instanceof FunctionTerm) {
                Function f = ((FunctionTerm) t).getFunction();
                Node<Q> fNode = iNode.lookup_s(f);
                if (fNode == null) {
                    fNode = new Node<>(f.getArity());
                    // we recursively add tgd using the subterms
                    put(fNode, ((FunctionTerm) t).getTerms(), tgd);
                    iNode.put_s(f, fNode);
                }
                // we add tgd to the set corresponding to GetTerms(P.i, f)
                fNode.add(tgd);
            } else if (t instanceof Constant) {
                Constant c = (Constant) t;
                Node<Q> cNode = iNode.lookup_s(c);
                if (cNode == null) {
                    cNode = Node.createLeaf();
                    iNode.put_s(c, cNode);
                }
                // we add tgd to the set corresponding to GetTerms(P.i, c)
                cNode.add(tgd);
            } else {
                // we add tgd to the set corresponding to GetTerms(P.i, *)
                iNode.add(tgd);
            }
        }

    }

    @Override
    public void remove(Atom atom, Q tgd) {
        Predicate p = atom.getPredicate();
        Node<Q> pNode = this.root.lookup_s(p);

        if (pNode == null)
            return;

        pNode.remove(tgd);

        if (pNode.isEmpty()) {
            this.root.remove(p, pNode);
        } else {
            Term[] terms = atom.getTerms();
            remove(pNode, terms, tgd);
        }
    }

    public void remove(Node<Q> node, Term[] terms, Q tgd) {
        for (int i = 0; i < terms.length; i++) {
            Term t = terms[i];
            Node<Q> iNode = node.lookup_i(i);
            if (t instanceof FunctionTerm) {
                Function f = ((FunctionTerm) t).getFunction();
                Node<Q> fNode = iNode.lookup_s(f);
                if (fNode != null) {
                    fNode.remove(tgd);
                    remove(fNode, ((FunctionTerm) t).getTerms(), tgd);
                    if (fNode.isEmpty())
                        iNode.remove(f, fNode);
                }
            } else if (t instanceof Constant) {
                Constant c = (Constant) t;
                Node<Q> cNode = iNode.lookup_s(c);
                cNode.remove(tgd);
                if (cNode.isEmpty())
                    iNode.remove(c, cNode);
            } else {
                iNode.remove(tgd);
            }
        }
    }

    static class Node<P extends GTGD> {

        // integer lookup operator
        private final List<Node<P>> ilp;
        // predicate symbols lookup operator
        private final Map<Object, Node<P>> slp;

        // tgds selected by the path of the node
        private final Set<P> tgds;

        public static <T extends GTGD> Node<T> createLeaf() {
            return new Node<T>(null, null);
        }

        public static <T extends GTGD> Node<T> createRoot() {
            return new Node<T>(null, new HashMap<>());
        }

        private Node(List<Node<P>> ilp, Map<Object, Node<P>> pslp) {
            this.ilp = ilp;
            this.slp = pslp;

            this.tgds = new HashSet<>();
        }

        Node(int arity) {
            this(new ArrayList<Node<P>>(arity), null);

            for (int i = 0; i < arity; i++)
                this.ilp.add(new Node<>(null, new HashMap<>()));
        }

        void put_s(Object p, Node<P> n) {
            this.slp.put(p, n);
        }

        Node<P> lookup_s(Object p) {
            return this.slp.get(p);
        }

        public Node<P> lookup_i(int i) {
            return this.ilp.get(i);
        }

        public Set<P> getTGDs() {
            return this.tgds;
        }

        void add(P tgd) {
            this.tgds.add(tgd);
        }

        void remove(P tgd) {
            this.tgds.remove(tgd);
        }

        public void remove(Object s, Node<P> pNode) {
            this.slp.remove(s, pNode);
        }

        public boolean isEmpty() {
            return this.tgds.isEmpty();
        }

    }
}
