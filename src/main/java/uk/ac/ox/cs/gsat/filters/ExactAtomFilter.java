package uk.ac.ox.cs.gsat.filters;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

import uk.ac.ox.cs.gsat.TGDGSat;
import uk.ac.ox.cs.pdq.fol.Atom;

public class ExactAtomFilter implements FormulaFilter {

    private class Node {
        boolean isBody = true;
        // maps hash of next clause to the next node in the trie
        TreeMap<Integer, Node> nextBody = new TreeMap<>();
        TreeMap<Integer, Node> nextHead = new TreeMap<>();
        // Formulas that end up at this node
        HashSet<TGDGSat> formulas = new HashSet<>();
    }

    Node root = new Node();

    private class SubsumedCandidatesIterable implements Iterable<TGDGSat> {
        private class SubsumedCandidatesIterator implements Iterator<TGDGSat> {
            private class IntNodePair {
                int index;
                Node node;

                IntNodePair(int index, Node node) {
                    this.index = index;
                    this.node = node;
                }
            }

            private Stack<IntNodePair> traversing = new Stack<>();
            private Iterator<TGDGSat> next = null;

            private int[] bodyHashes, headHashes;

            // the incoming hashes array should be sorted
            public SubsumedCandidatesIterator(int[] bodyHashes, int[] headHashes) {
                this.bodyHashes = bodyHashes;
                this.headHashes = headHashes;
                if (bodyHashes.length != 0)
                    traversing.push(new IntNodePair(0, root));
            }

            @Override
            public boolean hasNext() {
                // System.out.println("starting subsumed hasNext");
                if (next != null && next.hasNext())
                    return true;
                while (!traversing.empty() && (next == null || !next.hasNext())) {
                    // [1, 2], [3] -> [1], [3, 4]
                    // System.out.println(" traversing");
                    IntNodePair top = traversing.pop();
                    Node topNode = top.node;
                    int topIndex = top.index;
                    if (topNode.isBody) {
                        // if element appears in bodyHashes, it should appear in nextBody and nextHead
                        if (topIndex == bodyHashes.length) {
                            // all elements still in the body should be pushed
                            for (Map.Entry<Integer, Node> nodeInt : topNode.nextBody.entrySet()) {
                                traversing.push(new IntNodePair(topIndex, nodeInt.getValue()));
                            }
                            // all elements in the nextHead that are in hashes should be pushed
                            for (int i = 0; i < headHashes.length; i++) {
                                if (topNode.nextHead.containsKey(headHashes[i])) {
                                    traversing.push(new IntNodePair(i + 1, topNode.nextHead.get(headHashes[i])));
                                }
                            }
                        } else {// not correct I think
                            for (Map.Entry<Integer, Node> nodeInt : topNode.nextBody.entrySet()) {
                                if (nodeInt.getKey() > bodyHashes[topIndex])
                                    break;
                                if (nodeInt.getKey() == bodyHashes[topIndex])
                                    traversing.push(new IntNodePair(topIndex + 1, nodeInt.getValue()));
                                else
                                    traversing.push(new IntNodePair(topIndex, nodeInt.getValue()));
                            }
                        }
                    }

                    else {
                        // if element appears in nextBody, it should also appear in hashes
                        next = topNode.formulas.iterator();
                        for (int i = topIndex; i < headHashes.length; i++) {
                            if (topNode.nextHead.containsKey(headHashes[i]))
                                traversing.push(new IntNodePair(i + 1, topNode.nextHead.get(headHashes[i])));
                        }
                    }
                }
                // System.out.println("ending subsumed hasNext");
                return next != null && next.hasNext();
            }

            @Override
            public TGDGSat next() {
                // System.out.println("starting subsumed next" + hasNext());
                // hasNext();
                TGDGSat answer = next.next();
                // System.out.println("ending subsuming next");
                return answer;
            }
        }

        private final TGDGSat formula;

        public SubsumedCandidatesIterable(TGDGSat formula) {
            this.formula = formula;
        }

        @Override
        public Iterator<TGDGSat> iterator() {
            return new SubsumedCandidatesIterator(formula.bodyHashes, formula.headHashes);
        }

    }

    private class SubsumingCandidatesIterable implements Iterable<TGDGSat> {
        private class SubsumingCandidatesIterator implements Iterator<TGDGSat> {
            private class IntNodePair {
                int index;
                Node node;

                IntNodePair(int index, Node node) {
                    this.index = index;
                    this.node = node;
                }
            }

            private Stack<IntNodePair> traversing = new Stack<>();
            private Iterator<TGDGSat> next = null;

            private int[] bodyHashes, headHashes;

            // the incoming hashes array should be sorted
            public SubsumingCandidatesIterator(int[] bodyHashes, int[] headHashes) {
                this.bodyHashes = bodyHashes;
                this.headHashes = headHashes;
                traversing.push(new IntNodePair(0, root));
            }

            @Override
            public boolean hasNext() {
                // System.out.println("start subsuming hasNext");
                if (next != null && next.hasNext())
                    return true;
                while (!traversing.empty() && (next == null || !next.hasNext())) {
                    IntNodePair top = traversing.pop();
                    Node topNode = top.node;
                    int topIndex = top.index;
                    if (topNode.isBody) {
                        // [1], [2]
                        // [1, 2], [3] -> [1], [3, 4]
                        // if element appears in nextBody, it should appear in bodyHashes
                        // if above true, this is clearly incorrect
                        for (int i = topIndex; i < bodyHashes.length; i++) {
                            if (topNode.nextBody.containsKey(bodyHashes[i]))
                                traversing.push(new IntNodePair(i + 1, topNode.nextBody.get(bodyHashes[i])));
                        }
                        // push all the transitions to nextHead
                        for (Map.Entry<Integer, Node> nodeInt : topNode.nextHead.entrySet()) {
                            traversing.push(new IntNodePair(0, nodeInt.getValue()));
                            if (nodeInt.getKey() > headHashes[0])
                                break;
                            if (nodeInt.getKey() == headHashes[0])
                                traversing.push(new IntNodePair(1, nodeInt.getValue()));
                            else
                                traversing.push(new IntNodePair(0, nodeInt.getValue()));
                        }
                    }
                    // if element appears in hashes, it should appear in nextHead
                    else {
                        if (topIndex == headHashes.length) {
                            next = topNode.formulas.iterator();
                            // add all elements in this subtree
                            for (Map.Entry<Integer, Node> nodeInt : topNode.nextHead.entrySet()) {
                                traversing.push(new IntNodePair(topIndex, nodeInt.getValue()));
                            }
                        } else {
                            // add elements that are <= topIndex
                            for (Map.Entry<Integer, Node> nodeInt : topNode.nextHead.entrySet()) {
                                if (nodeInt.getKey() > headHashes[topIndex])
                                    break;
                                if (nodeInt.getKey() == headHashes[topIndex])
                                    traversing.push(new IntNodePair(topIndex + 1, nodeInt.getValue()));
                                else
                                    traversing.push(new IntNodePair(topIndex, nodeInt.getValue()));

                            }
                        }
                    }
                }
                // System.out.println("end subsuming hasNext");
                return next != null && next.hasNext();
            }

            @Override
            public TGDGSat next() {
                // System.out.println("starting subsuming next" + hasNext());
                // hasNext();
                TGDGSat answer = next.next();
                // System.out.println("finishing subsuming next");
                return answer;
            }
        }

        private final TGDGSat formula;

        public SubsumingCandidatesIterable(TGDGSat formula) {
            this.formula = formula;
        }

        @Override
        public Iterator<TGDGSat> iterator() {
            return new SubsumingCandidatesIterator(formula.bodyHashes, formula.headHashes);
        }

    }

    private class AllIterable implements Iterable<TGDGSat> {
        private class AllIterator implements Iterator<TGDGSat> {
            private Stack<Node> traversing = new Stack<>();
            private Iterator<TGDGSat> next = null;

            // the incoming hashes array should be sorted
            public AllIterator() {
                traversing.push(root);
            }

            @Override
            public boolean hasNext() {
                if (next != null && next.hasNext())
                    return true;
                while (!traversing.empty() && (next == null || !next.hasNext())) {
                    Node top = traversing.pop();
                    next = top.formulas.iterator();
                    for (Node node : top.nextBody.values()) {
                        traversing.push(node);
                    }
                    for (Node node : top.nextHead.values()) {
                        traversing.push(node);
                    }
                }
                return next != null && next.hasNext();
            }

            @Override
            public TGDGSat next() {
                hasNext();
                TGDGSat answer = next.next();
                return answer;
            }
        }

        @Override
        public Iterator<TGDGSat> iterator() {
            return new AllIterator();
        }

    }

    public Collection<TGDGSat> getAll() {
        // System.out.println("getting all");
        HashSet<TGDGSat> answer = new HashSet<>();
        (new AllIterable()).forEach(answer::add);
        // System.out.println("end getting all");
        return answer;
    }

    public void add(TGDGSat formula) {
        // StackTraceElement[] stackTraceElements =
        // Thread.currentThread().getStackTrace();
        // System.out.println(stackTraceElements[2].getClassName() + " " +
        // stackTraceElements[2].getMethodName() + " "
        // + stackTraceElements[1].getLineNumber());
        // System.out.println("adding " + formula);
        checkHashes(formula);
        if (formula.getHeadAtoms().length == 0)
            return;
        Node current = root;
        for (int hash : formula.bodyHashes) {
            if (!current.nextBody.containsKey(hash)) {
                current.nextBody.put(hash, new Node());
            }
            current = current.nextBody.get(hash);
        }
        for (int hash : formula.headHashes) {
            if (!current.nextHead.containsKey(hash)) {
                Node newNode = new Node();
                newNode.isBody = false;
                current.nextHead.put(hash, newNode);
            }
            current = current.nextHead.get(hash);
        }
        current.formulas.add(formula);
        // System.out.println("end adding");
    }

    // this can still be improved, as I am not deleting empty nodes
    public void remove(TGDGSat formula) {
        // System.out.println("removing");
        // StackTraceElement[] stackTraceElements =
        // Thread.currentThread().getStackTrace();
        // System.out.println(stackTraceElements[2].getClassName() + " " +
        // stackTraceElements[2].getMethodName() + " "
        // + stackTraceElements[1].getLineNumber());
        checkHashes(formula);
        Node current = root;
        for (int hash : formula.bodyHashes) {
            if (!current.nextBody.containsKey(hash)) {
                // System.out.println("element not found");
                return;
            }
            current = current.nextBody.get(hash);
        }
        for (int hash : formula.headHashes) {
            if (!current.nextHead.containsKey(hash)) {
                // System.out.println("element not found");
                return;
            }
            current = current.nextHead.get(hash);
        }
        current.formulas.remove(formula);
        // System.out.println("ending remove");
    }

    private int atomCounter = 0;
    private HashMap<Atom, Integer> bodyAtomIndeces = new HashMap<>(), headAtomIndeces = new HashMap<>();

    private int[] computeHashes(Atom[] atoms, HashMap<Atom, Integer> atomIndeces) {
        // System.out.println("computing hashes");
        TreeSet<Integer> hashes = new TreeSet<Integer>();
        for (Atom atom : atoms) {
            if (!atomIndeces.containsKey(atom)) {
                atomIndeces.put(atom, atomCounter);
                atomCounter++;
            }
            hashes.add(atomIndeces.get(atom));
        }
        // System.out.println("end computing hashes");
        // this should be already sorted
        return hashes.stream().mapToInt(Integer::intValue).toArray();
    }

    private void checkHashes(TGDGSat formula) {
        // System.out.println("checking hashes");
        if (formula.bodyHashes == null)
            formula.bodyHashes = computeHashes(formula.getBodyAtoms(), bodyAtomIndeces);
        if (formula.headHashes == null)
            formula.headHashes = computeHashes(formula.getHeadAtoms(), headAtomIndeces);
        // System.out.println("end checking hashes");
    }

    public Iterable<TGDGSat> getSubsumedCandidates(TGDGSat formula) {
        // System.out.println("getting subsumed candidates");
        checkHashes(formula);
        return new SubsumedCandidatesIterable(formula);
    }

    public Iterable<TGDGSat> getSubsumingCandidates(TGDGSat formula) {
        // System.out.println("getting subsuming candidates");
        checkHashes(formula);
        return new SubsumingCandidatesIterable(formula);
    }
}