package uk.ac.ox.cs.gsat.unification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.ox.cs.gsat.GTGD;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Function;
import uk.ac.ox.cs.pdq.fol.FunctionTerm;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Term;

/**
 * Implementation of path unification index proposed in 
 * The path-indexing Method for Indexing Terms - Mark E. Stickel
 * specialised for our usage: 
 * 1. the "terms" we consider are atoms, where the predicate can be considered as a function symbol,
 *    therefore all our paths will with a predicate symbol
 * 2. we build a restricted index (over approximation) that considers constant symbols as variables,
 *    and stop the paths after the first function encounters i.e. the paths are the following forms 
 *    < p.i.[f|*] > where p is a predicate, i is position in p, f is a function and * represents any variable
 */
public class AtomPathUnificationIndex<Q extends GTGD> implements UnificationIndex<Q> {

    private final Node<Q> root;

    public AtomPathUnificationIndex() {
        this.root = Node.createRoot();
    }

	@Override
	public Set<Q> get(Atom atom) {
        Predicate p = atom.getPredicate();
        Node<Q> pNode = this.root.lookup(p);
        if (pNode != null) {

            Term[] terms = atom.getTerms();
            boolean containsAtLeastOneFunctionTerm = false;
            for (Term t : terms) {
                if (t instanceof FunctionTerm) {
                    containsAtLeastOneFunctionTerm = true;
                    break;
                }
            }
            
            // we distinguish two cases whether there is a function term
            // among the terms
            if (containsAtLeastOneFunctionTerm) {

                // we instantiate the candidate set with the first candidate
                // in order to compute the intersection
                // candicates represents GetUnifiables(<>, atom) = intersection_i GetUnifiables(<p,i>, t)
                // p is the predicate and i the position of the term i in atom
                Set<Q> candidates = null;
                // the tCandidates are the candidates for unfications for the path
                // tCandidates represents GetUnifiables(<p,i>, t)
                Set<Q> tCandidates = new HashSet<>();
                int i = 0;
                for (Term t : terms) {
                    Node<Q> iNode = pNode.lookup(i);
                    tCandidates.addAll(iNode.getTGDs());

                    if (t instanceof FunctionTerm) {
                        Function f = ((FunctionTerm) t).getFunction();
                        Node<Q> fNode = iNode.lookup(f);
                        if (fNode != null) {
                            tCandidates.addAll(fNode.getTGDs());
                        }
                    }

                    if (candidates != null)
                        candidates.retainAll(tCandidates);
                    else
                        candidates = new HashSet<>(tCandidates);

                    // System.out.println("tcandidates " + tCandidates);
                    tCandidates.clear();
                    i++;
                }
                // System.out.println("root " + this.root);
                // System.out.println("get " + atom + ":  " + candidates);
                // System.out.println("=============");
                return candidates;
            } else {
                // System.out.println("root " + this.root);
                // System.out.println("get " + atom + ":  " + pNode.getTGDs());
                // System.out.println("=============");
                return pNode.getTGDs();
            }

        } else {
            return new HashSet<>();
        }
    }

    @Override
    public void put(Atom atom, Q tgd) {
        // System.out.println("root " + this.root);
        // System.out.println("put " + atom + " || " + tgd);
        // System.out.println("=============");
        Predicate p = atom.getPredicate();
        Node<Q> pNode = this.root.lookup(p);

        if (pNode == null) {
            pNode = new Node<>(p);
            this.root.put(p, pNode);
        }

        pNode.add(tgd);
        
        int i = 0;
        for (Term t : atom.getTerms()) {
            Node<Q> iNode = pNode.lookup(i);
            if (t instanceof FunctionTerm) {
                Function f = ((FunctionTerm) t).getFunction();
                Node<Q> fNode = iNode.lookup(f);
                if (fNode == null) {
                    fNode = Node.createLeaf();
                    // we add tgd to the set corresponding to GetTerms(<p,i,f>, *)
                    // here the arity of f is not taken into account
                    iNode.put(f, fNode);
                }
                fNode.add(tgd);
            } else {
                // we add tgd to the set corresponding to GetTerms(<p,i>, *)
                iNode.add(tgd);
            }
            i++;
        }
    }

    @Override
    public void remove(Atom atom, Q tgd) {
        Predicate p = atom.getPredicate();
        Node<Q> pNode = this.root.lookup(p);

        if (pNode == null)
            return;

        pNode.remove(tgd);

        if (pNode.isEmpty()) {
            this.root.remove(p, pNode);
        } else {
            int i = 0;
            for (Term t : atom.getTerms()) {
                Node<Q> iNode = pNode.lookup(i);
                if (t instanceof FunctionTerm) {
                    Function f = ((FunctionTerm) t).getFunction();
                    Node<Q> fNode = iNode.lookup(f);
                    if (fNode != null) {
                        fNode.remove(tgd);
                        if (fNode.isEmpty())
                            iNode.remove(f, fNode);
                    }
                } else {
                    iNode.remove(tgd);
                }
                i++;
            }
        }
    }

    static class Node<P extends GTGD> {

        // integer lookup operator
        private final List<Node<P>> ilp;
        // predicate symbols lookup operator
        private final Map<Predicate, Node<P>> pslp;
        // function symbols lookup operator
        private final Map<Function, Node<P>> fslp;

        // tgds selected by the path of the node
        private final Set<P> tgds;

        public static <T extends GTGD> Node<T> createLeaf() {
            return new Node<T>(null, null, null);
        }

		public static <T extends GTGD> Node<T> createRoot() {
            return new Node<T>(null, new HashMap<>(), null);
        }

        Node(Predicate p) {
            this(new ArrayList<Node<P>>(p.getArity()), null, null);

            for (int i = 0; i < p.getArity(); i++)
                this.ilp.add(new Node<>(null, null, new HashMap<>()));
        }

        private Node(List<Node<P>> ilp, Map<Predicate, Node<P>> pslp, Map<Function, Node<P>> fslp) {
            this.ilp = ilp;
            this.pslp = pslp;
            this.fslp = fslp;

            this.tgds = new HashSet<>();
        }

        void put (Function f, Node<P> n) {
            this.fslp.put(f,n);
        }

        void put (Predicate p, Node<P> n) {
            this.pslp.put(p,n);
        }

        Node<P> lookup(Function f) {
            return this.fslp.get(f);
        }

        Node<P> lookup(Predicate p) {
            return this.pslp.get(p);
        }

        public Node<P> lookup(int i) {
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

        public void remove(Function f, Node<P> fNode) {
            this.fslp.remove(f, fNode);
		}

        public void remove(Predicate p, Node<P> pNode) {
            this.pslp.remove(p, pNode);
		}

		public boolean isEmpty() {
			return this.tgds.isEmpty();
		}

    }
}
