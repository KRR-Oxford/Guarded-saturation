package uk.ac.ox.cs.gsat.subsumers;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

import uk.ac.ox.cs.gsat.TGD;
import uk.ac.ox.cs.pdq.fol.Atom;

/**
 * Identifies tgd a as subsumed by tgd b if b.head is contained in a.head, and
 * a.body is contained in b.body (without any unification).
 * 
 * Similar to the SimpleSubsumer class with a good index, but the indexing is
 * built in.
 */
public class ExactAtomSubsumer<Q extends TGD> implements Subsumer<Q> {
    private class Node {
        boolean isBody = true;
        // maps hash of next clause to the next node in the trie
        TreeMap<Integer, Node> nextBody = new TreeMap<>();
        TreeMap<Integer, Node> nextHead = new TreeMap<>();
        // Formulas that end up at this node
        Q currentFormula = null;
    }

    Node root = new Node();

    private long numSubsumed = 0;
    private int atomCounter = 0;
    private HashMap<Atom, Integer> bodyAtomIndeces = new HashMap<>(), headAtomIndeces = new HashMap<>();

    private int[] computeHashes(Atom[] atoms, HashMap<Atom, Integer> atomIndeces) {
        TreeSet<Integer> hashes = new TreeSet<Integer>();
        for (Atom atom : atoms) {
            if (!atomIndeces.containsKey(atom)) {
                atomIndeces.put(atom, atomCounter);
                atomCounter++;
            }
            hashes.add(atomIndeces.get(atom));
        }
        return hashes.stream().mapToInt(Integer::intValue).toArray();
    }

    private void checkHashes(Q formula) {
        if (formula.getBodyHashes() == null)
            formula.setBodyHashes(computeHashes(formula.getBodyAtoms(), bodyAtomIndeces));
        if (formula.getHeadHashes() == null)
            formula.setHeadHashes(computeHashes(formula.getHeadAtoms(), headAtomIndeces));
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
    public Collection<Q> subsumesAny(Q formula) {
        HashSet<Q> answer = new HashSet<>();
        Stack<IntNodePair> traversing = new Stack<>();
        // TODO: implement this in a less hacky manner
        Stack<IntNodePair> reversedTraversal = new Stack<>();
        int[] bodyHashes = formula.getBodyHashes();
        int[] headHashes = formula.getHeadHashes();
        if (bodyHashes.length != 0)
            traversing.push(new IntNodePair(0, root));
        while (!traversing.empty()) {
            // [1, 2], [3] -> [1], [3, 4]
            IntNodePair top = traversing.pop();
            Node topNode = top.node;
            int topIndex = top.index;
            if (topNode.isBody) {
                // if element appears in bodyHashes, it should appear in nextBody and nextHead
                if (topIndex == bodyHashes.length) {
                    // all elements still in the body should be pushed
                    for (Map.Entry<Integer, Node> nodeInt : topNode.nextBody.entrySet()) {
                        traversing.push(new IntNodePair(topIndex, nodeInt.getValue()));
                        reversedTraversal.push(new IntNodePair(nodeInt.getKey() * 2, topNode));
                    }
                    // all elements in the nextHead that are in hashes should be pushed
                    for (int i = 0; i < headHashes.length; i++) {
                        if (topNode.nextHead.containsKey(headHashes[i])) {
                            traversing.push(new IntNodePair(i + 1, topNode.nextHead.get(headHashes[i])));
                            reversedTraversal.push(new IntNodePair(headHashes[i] * 2 + 1, topNode));
                        }
                    }
                } else {
                    for (Map.Entry<Integer, Node> nodeInt : topNode.nextBody.entrySet()) {
                        if (nodeInt.getKey() > bodyHashes[topIndex])
                            break;
                        if (nodeInt.getKey() == bodyHashes[topIndex]) {
                            traversing.push(new IntNodePair(topIndex + 1, nodeInt.getValue()));
                            reversedTraversal.push(new IntNodePair(nodeInt.getKey() * 2, topNode));
                        } else {
                            traversing.push(new IntNodePair(topIndex, nodeInt.getValue()));
                            reversedTraversal.push(new IntNodePair(nodeInt.getKey() * 2, topNode));
                        }
                    }
                }
            }

            else {
                // if element appears in nextBody, it should also appear in hashes
                if (topNode.currentFormula != null)
                    answer.add(topNode.currentFormula);
                for (int i = topIndex; i < headHashes.length; i++) {
                    if (topNode.nextHead.containsKey(headHashes[i])) {
                        traversing.push(new IntNodePair(i + 1, topNode.nextHead.get(headHashes[i])));
                        reversedTraversal.push(new IntNodePair(headHashes[i] * 2 + 1, topNode));
                    }
                }
                topNode.currentFormula = null;
            }
        }
        // discard deleted nodes
        while (!reversedTraversal.empty()) {
            IntNodePair top = reversedTraversal.pop();
            Node topNode = top.node;
            int index = top.index / 2;
            if (top.index % 2 == 0) {
                Node node = topNode.nextBody.get(index);
                if (node.nextBody.isEmpty() && node.nextHead.isEmpty() && node.currentFormula == null) {
                    topNode.nextBody.remove(index);
                }
            } else {
                Node node = topNode.nextHead.get(index);
                if (node.nextBody.isEmpty() && node.nextHead.isEmpty() && node.currentFormula == null) {
                    topNode.nextHead.remove(index);
                }
            }
        }
        numSubsumed += answer.size();
        return answer;
    }

    @Override
    public boolean subsumed(Q formula) {
        checkHashes(formula);
        int[] bodyHashes = formula.getBodyHashes();
        int[] headHashes = formula.getHeadHashes();
        Stack<IntNodePair> traversing = new Stack<>();
        traversing.push(new IntNodePair(0, root));
        while (!traversing.empty()) {
            IntNodePair top = traversing.pop();
            Node topNode = top.node;
            int topIndex = top.index;
            if (topNode.isBody) {
                // [1, 2], [3] -> [1], [3, 4]
                // if element appears in nextBody, it should appear in bodyHashes
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
                    numSubsumed += 1;
                    return true;
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
    public void add(Q formula) {
        checkHashes(formula);
        if (formula.getHeadAtoms().length == 0)
            return;
        Node current = root;
        for (int hash : formula.getBodyHashes()) {
            if (!current.nextBody.containsKey(hash)) {
                current.nextBody.put(hash, new Node());
            }
            current = current.nextBody.get(hash);
        }
        for (int hash : formula.getHeadHashes()) {
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
    public Collection<Q> getAll() {
        Stack<Node> traversing = new Stack<>();
        HashSet<Q> answer = new HashSet<>();
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

    public long getNumberSubsumed() {
        return numSubsumed;
    }

}
