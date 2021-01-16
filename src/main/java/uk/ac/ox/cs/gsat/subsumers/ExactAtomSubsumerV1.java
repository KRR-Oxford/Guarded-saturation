package uk.ac.ox.cs.gsat.subsumers;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

import uk.ac.ox.cs.gsat.TGDGSat;
import uk.ac.ox.cs.pdq.fol.Atom;

public class ExactAtomSubsumerV1 implements Subsumer {
    private class Node {
        boolean isBody = true;
        // maps hash of next clause to the next node in the trie
        TreeMap<Integer, Node> nextBody = new TreeMap<>();
        TreeMap<Integer, Node> nextHead = new TreeMap<>();
        // Formulas that end up at this node
        TGDGSat currentFormula = null;
    }

    Node root = new Node();

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
        // this should be already sorted
        return hashes.stream().mapToInt(Integer::intValue).toArray();
    }

    private void checkHashes(TGDGSat formula) {
        if (formula.bodyHashes == null)
            formula.bodyHashes = computeHashes(formula.getBodyAtoms(), bodyAtomIndeces);
        if (formula.headHashes == null)
            formula.headHashes = computeHashes(formula.getHeadAtoms(), headAtomIndeces);
    }

    private class IntNodePair {
        int index;
        Node node;

        IntNodePair(int index, Node node) {
            this.index = index;
            this.node = node;
        }
    }

    @Override
    public Collection<TGDGSat> subsumesAny(TGDGSat formula) {
        HashSet<TGDGSat> answer = new HashSet<>();
        Stack<IntNodePair> traversing = new Stack<>();
        int[] bodyHashes = formula.bodyHashes;
        int[] headHashes = formula.headHashes;
        if (bodyHashes.length != 0)
            traversing.push(new IntNodePair(0, root));
        while (!traversing.empty()) {
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
                if (topNode.currentFormula != null)
                    answer.add(topNode.currentFormula);
                for (int i = topIndex; i < headHashes.length; i++) {
                    if (topNode.nextHead.containsKey(headHashes[i]))
                        traversing.push(new IntNodePair(i + 1, topNode.nextHead.get(headHashes[i])));
                }
                topNode.currentFormula = null;
            }
        }
        return answer;
    }

    @Override
    public boolean subsumed(TGDGSat formula) {
        checkHashes(formula);
        int[] bodyHashes = formula.bodyHashes;
        int[] headHashes = formula.headHashes;
        Stack<IntNodePair> traversing = new Stack<>();
        traversing.push(new IntNodePair(0, root));
        while (!traversing.empty()) {
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
                    // possibly just return true might work here, depending on how we delete TODO
                    if (topNode.currentFormula != null)
                        return true;
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
        return false;
    }

    @Override
    public void add(TGDGSat formula) {
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
        current.currentFormula = formula;
    }

    @Override
    public Collection<TGDGSat> getAll() {
        Stack<Node> traversing = new Stack<>();
        HashSet<TGDGSat> answer = new HashSet<>();
        traversing.push(root);
        while (!traversing.empty()) {
            Node top = traversing.pop();
            if (top.currentFormula != null)
                answer.add(top.currentFormula);
            for (Node node : top.nextBody.values()) {
                traversing.push(node);
            }
            for (Node node : top.nextHead.values()) {
                traversing.push(node);
            }
        }
        return answer;
    }

}