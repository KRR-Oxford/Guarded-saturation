package uk.ac.ox.cs.gsat.filters;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.satalg.SaturationConfig;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Function;
import uk.ac.ox.cs.pdq.fol.FunctionTerm;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Term;

/**
 * Implements a tree based index that filters out subsumption candidates based
 * on sets of predicates.
 */
public class TreePredicateFilter<Q extends TGD> implements FormulaFilter<Q> {

    protected final SaturationConfig config;

    public TreePredicateFilter(SaturationConfig config) {
        this.config = config;
    }

    private class Node {
        boolean isBody = true;
        // maps hash of next clause to the next node in the trie
        TreeMap<Integer, Node> nextBody = new TreeMap<>();
        TreeMap<Integer, Node> nextHead = new TreeMap<>();
        // Formulas that end up at this node
        HashSet<Q> formulas = new HashSet<>();
    }

    Node root = new Node();
    
    private class SubsumedCandidatesIterable implements Iterable<Q> {
        private class SubsumedCandidatesIterator implements Iterator<Q> {
            private class IntNodePair {
                int index;
                Node node;

                IntNodePair(int index, Node node) {
                    this.index = index;
                    this.node = node;
                }
            }

            private Stack<IntNodePair> traversing = new Stack<>();
            private Iterator<Q> next = null;

            private int[] bodyHashes, headHashes;

            public SubsumedCandidatesIterator(int[] bodyHashes, int[] headHashes) {
                this.bodyHashes = bodyHashes;
                this.headHashes = headHashes;
                if (bodyHashes.length != 0)
                    traversing.push(new IntNodePair(0, root));
            }

            @Override
            public boolean hasNext() {
                if (next != null && next.hasNext())
                    return true;
                while (!traversing.empty() && (next == null || !next.hasNext())) {
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
                            }
                            // all elements in the nextHead that are in hashes should be pushed
                            for (int i = 0; i < headHashes.length; i++) {
                                if (topNode.nextHead.containsKey(headHashes[i])) {
                                    traversing.push(new IntNodePair(i + 1, topNode.nextHead.get(headHashes[i])));
                                }
                            }
                        } else {
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
                return next != null && next.hasNext();
            }

            @Override
            public Q next() {
                Q answer = next.next();
                return answer;
            }
        }

        private final Q formula;

        public SubsumedCandidatesIterable(Q formula) {
            this.formula = formula;
        }

        @Override
        public Iterator<Q> iterator() {
            return new SubsumedCandidatesIterator(formula.getBodyHashes(), formula.getHeadHashes());
        }

    }

    private class SubsumingCandidatesIterable implements Iterable<Q> {
        private class SubsumingCandidatesIterator implements Iterator<Q> {
            private class IntNodePair {
                int index;
                Node node;

                IntNodePair(int index, Node node) {
                    this.index = index;
                    this.node = node;
                }
            }

            private Stack<IntNodePair> traversing = new Stack<>();
            private Iterator<Q> next = null;

            private int[] bodyHashes, headHashes;

            public SubsumingCandidatesIterator(int[] bodyHashes, int[] headHashes) {
                this.bodyHashes = bodyHashes;
                this.headHashes = headHashes;
                traversing.push(new IntNodePair(0, root));
            }

            @Override
            public boolean hasNext() {
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
                return next != null && next.hasNext();
            }

            @Override
            public Q next() {
                Q answer = next.next();
                return answer;
            }
        }

        private final Q formula;

        public SubsumingCandidatesIterable(Q formula) {
            this.formula = formula;
        }

        @Override
        public Iterator<Q> iterator() {
            return new SubsumingCandidatesIterator(formula.getBodyHashes(), formula.getHeadHashes());
        }

    }

    private class AllIterable implements Iterable<Q> {
        private class AllIterator implements Iterator<Q> {
            private Stack<Node> traversing = new Stack<>();
            private Iterator<Q> next = null;

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
            public Q next() {
                hasNext();
                Q answer = next.next();
                return answer;
            }
        }

        @Override
        public Iterator<Q> iterator() {
            return new AllIterator();
        }

    }

    public Collection<Q> getAll() {
        HashSet<Q> answer = new HashSet<>();
        (new AllIterable()).forEach(answer::add);
        return answer;
    }

    public void init(Collection<Q> formulas) {

        // // initialize the predicate hashes based the ordering
        // for (Predicate predicate : keptPredicates) {
        //     System.out.println(predicate+": "+predicateCount.get(predicate));
        //     headAtomIndeces.put(predicate, atomCounter);
        //     atomCounter++;
        // }
        // compute the length of formulas, ie, the number of predicate in average
        float length = 0;
        Set<Predicate> predicates = new HashSet<>();

        for (Q formula: formulas) {
            for (Atom atom : formula.getAtoms()) {
                Predicate predicate = atom.getPredicate();
                predicates.add(predicate);
            }
            length += predicates.size();
            predicates.clear();
        }

        length = length / formulas.size();
        
        // select the kept predicates used for the index
        int predicateMax = config.getMaxPredicate();

        int bagNumber = (predicateMax > 0)
                ? Math.max(Double.valueOf(Math.pow(formulas.size() / predicateMax, 1 / length)).intValue(), 1)
                : 0;

        System.out.println("length of the formulas: " + length);
        System.out.println("number of bags: " + bagNumber);


        init(formulas, true, bagNumber);
        init(formulas, false, bagNumber);
        for (Q formula: formulas)
            add(formula);

        // printIndex("index-init.dot");
    }

    private void init(Collection<Q> formulas, boolean isBody, int bagNumber) {
        //gather the formulas by predicates
        Map<Predicate, Set<Q>> predicateFormulas = new HashMap<>();
        Map<Function, Set<Q>> functionsFormulas = new HashMap<>();

        for (Q formula: formulas) {
            Atom[] atoms = (isBody) ? formula.getBodyAtoms() : formula.getHeadAtoms();
            for (Atom atom : atoms) {
                Predicate predicate = atom.getPredicate();
                if (!predicateFormulas.containsKey(predicate))
                    predicateFormulas.put(predicate, new HashSet<>());

                predicateFormulas.get(predicate).add(formula);

                for (Term t: atom.getTerms()) {
                    // System.out.println(t);
                    if (t instanceof FunctionTerm) {
                        Function f = ((FunctionTerm) t).getFunction();
                        if (functionsFormulas.containsKey(f)) {
                            functionsFormulas.get(f).add(formula);
                        } else {
                            HashSet<Q> set = new HashSet<>();
                            functionsFormulas.put(f, set);
                            set.add(formula);
                        }
                    }
                }
            }
        }
        
        List<List<Predicate>> predicatesBags = computeBags(predicateFormulas, bagNumber);
        
        //        List<Predicate> keptPredicates = predicates.subList(0, Math.min(predicateMax, predicates.size()));
        
        // initialize the predicate hashes based the ordering
        for (List<Predicate> bag : predicatesBags) {
            for (Predicate predicate : bag) {
                if (isBody)
                    bodyAtomIndeces.put(predicate, atomCounter);
                else {
                    headAtomIndeces.put(predicate, atomCounter);
                }
            }
            atomCounter++;
        }

        if (!isBody) {
            List<List<Function>> functionsBags = computeBags(functionsFormulas, bagNumber);
            // System.out.println(functionsBags.size());
            for (List<Function> bag : functionsBags) {
                // System.out.println(bag);
                for (Function f : bag) 
                    functionIndeces.put(f, atomCounter);
                atomCounter++;
            }
        }

    }

    
    public <K> List<List<K>> computeBags(Map<K, Set<Q>> predicateFormulas, int bagNb) {

        // sort the keys by the descending frequency
        List<K> keys = new LinkedList<>(predicateFormulas.keySet());

        // in the case the bagNumber is lower than zero,
        // we consider the bags contains at most one element
        final int bagNumber;
        if (bagNb <= 0)
            bagNumber = keys.size();
        else
            bagNumber = bagNb;

        if (bagNb == 0)
            return new ArrayList<>();

        class KComp implements Comparator<K> {
            public int compare(K a, K b) {
                return predicateFormulas.get(b).size() - predicateFormulas.get(a).size();
            }
        }

        keys.sort(new KComp());

        List<List<K>> bags = new ArrayList<>();
        // List<Integer> bagSizes = new ArrayList<>();
        List<Set<Q>> bagFormulas = new ArrayList<>();
        class BagComp implements Comparator<Integer> {
            public int compare(Integer a, Integer b) {
                return bagFormulas.get(a).size() - bagFormulas.get(b).size();
            }
        }
        PriorityQueue<Integer> sortedBags = new PriorityQueue<>(bagNumber, new BagComp());

        // initialisation of bags
        for (int n = 0; n < bagNumber; n++) {
            List<K> bag = new ArrayList<>();
            bags.add(bag);
            // bagSizes.add(0);
            bagFormulas.add(new HashSet<>());
        }
        for (int n = 0; n < bagNumber; n++) {
            sortedBags.add(n);
        }

        for (K key : keys) {
            // add to the smallest bags
            Integer index = sortedBags.poll();
            List<K> bag = bags.get(index);
            // Integer size = bagSizes.get(index);

            // update
            bag.add(key);
            // Integer newSize = size + keyFormulas.get(key).size();
            // bagSizes.set(index, newSize);
            bagFormulas.get(index).addAll(predicateFormulas.get(key));
            sortedBags.add(index);
        }

        return bags;
    }
    
    public <K> List<List<K>> computeBagsSlow(Map<K,Set<Q>> keyFormulas, int predicateMax) {
        // sort the keys by the descending frequency
        List<K> keys = new LinkedList<>(keyFormulas.keySet());

        class KComp implements Comparator<K> {
            public int compare(K a, K b) {
                return keyFormulas.get(b).size() - keyFormulas.get(a).size();
            }
        }

        keys.sort(new KComp());

        for (K k : keys)
            System.out.println(k + " " + keyFormulas.get(k).size());

        List<List<K>> bags = new ArrayList<>();
        while (!keys.isEmpty()) {
            // Set<Q> bagFormulas = new HashSet<>();
            int formulasCount = 0;
            List<K> bag = new ArrayList<>();
            while(formulasCount < predicateMax && !keys.isEmpty()) {
                // System.out.println(weight + " " + predicates.size() + " " + bag.size());
                int pos = 0;
                Iterator<K> keyIterator = keys.iterator();
                while (keyIterator.hasNext()) {
                    pos++;
                    K key = keyIterator.next();
                    // Set<Q> newbagFormulas = bagFormulas;
                    // if (keyFormulas.get(predicate).size() + bagFormulas.size() > predicateMax) {
                    //         newbagFormulas = new HashSet<>(bagFormulas);
                    // }
                    // newbagFormulas.addAll(keyFormulas.get(predicate));
                    if (pos == keys.size() || keyFormulas.get(key).size() + formulasCount  < predicateMax) {
                        bag.add(key);
                        formulasCount+= keyFormulas.get(key).size();
                        break;
                    }
                }
                keyIterator.remove();
            }
            // System.out.println("bag size " + bag.size()+ "  " + bagFormulas.size());
            bags.add(bag);
        }
        return bags;
    }
    
    public void printIndex(String path) {
        File file = new File(path);
        file.delete();
        try {
			file.createNewFile();
            FileWriter writer = new FileWriter(file);
            writer.append("digraph index {");
            printIndexRec(writer, root);
            writer.append("}");
                    writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
    }

    private void printIndexRec(FileWriter writer, Node current) throws IOException {
        String formulasString = "";

        for (Q tgd : current.formulas) {
            formulasString += tgd.toString().replaceAll("(^|\\s)([^\\s])+[#/-]", "").replace("&", ",") + "\n";
        }
        //        formulasString = ""+current.formulas.size();
        writer.append(
                      String.format("%s [label=\"%s\", %s color=\"%s\"];\n", current.hashCode(), formulasString , current.isBody ? "" : "shape=box,", current.isBody ? "blue" : "red"));
        for (Integer hash : current.nextBody.keySet()) {
            Node child = current.nextBody.get(hash);
            writer.append(String.format("%s -> %s[label=\"%s\"];\n", current.hashCode(), child.hashCode(), hash));
            printIndexRec(writer, child);
        }

        for (Integer hash : current.nextHead.keySet()) {
            Node child = current.nextHead.get(hash);
            writer.append(String.format("%s -> %s[label=\"%s\"];\n", current.hashCode(), child.hashCode(), hash));
            printIndexRec(writer, child);
        }

    }
    
    public void add(Q formula) {
        checkHashes(formula);

        if (formula.getHeadAtoms().length == 0)
            return;
        // System.out.println(formula);
        // System.out.println(Arrays.toString(formula.getBodyHashes()));
        // System.out.println(Arrays.toString(formula.getHeadHashes()));

        Node current = root;
        for (int hash : formula.getBodyHashes()) {
            if (!current.nextBody.containsKey(hash)) {
                current.nextBody.put(hash, new Node());
            }
            current = current.nextBody.get(hash);
            // System.out.println("size " + current.formulas.size());
            // System.out.println("isbody " + current.isBody);
        }
        for (int hash : formula.getHeadHashes()) {
            if (!current.nextHead.containsKey(hash)) {
                Node newNode = new Node();
                newNode.isBody = false;
                current.nextHead.put(hash, newNode);
            }
            current = current.nextHead.get(hash);
            // System.out.println("size " + current.formulas.size());
            // System.out.println("isbody " + current.isBody);
        }
        current.formulas.add(formula);
    }

    private class IntNodePair {
        int index;
        Node node;

        IntNodePair(int index, Node node) {
            this.index = index;
            this.node = node;
        }
    }

    public void remove(Q formula) {
        checkHashes(formula);
        Stack<IntNodePair> reversedTraversal = new Stack<>();
        Node current = root;
        for (int hash : formula.getBodyHashes()) {
            reversedTraversal.add(new IntNodePair(hash * 2, current));
            if (!current.nextBody.containsKey(hash)) {
                return;
            }
            current = current.nextBody.get(hash);
        }
        for (int hash : formula.getHeadHashes()) {
            reversedTraversal.add(new IntNodePair(hash * 2 + 1, current));
            if (!current.nextHead.containsKey(hash)) {
                return;
            }
            current = current.nextHead.get(hash);
        }
        current.formulas.remove(formula);
        while (!reversedTraversal.empty()) {
            IntNodePair top = reversedTraversal.pop();
            Node topNode = top.node;
            int index = top.index / 2;
            if (top.index % 2 == 0) {
                Node node = topNode.nextBody.get(index);
                if (node.nextBody.isEmpty() && node.nextHead.isEmpty() && node.formulas.isEmpty()) {
                    topNode.nextBody.remove(index);
                } else
                    return;
            } else {
                Node node = topNode.nextHead.get(index);
                if (node.nextBody.isEmpty() && node.nextHead.isEmpty() && node.formulas.isEmpty()) {
                    topNode.nextHead.remove(index);
                } else
                    return;
            }
        }
    }

    private int atomCounter = 1;
    private HashMap<Predicate, Integer> bodyAtomIndeces = new HashMap<>(), headAtomIndeces = new HashMap<>();
    private HashMap<Function, Integer> functionIndeces = new HashMap<>();

    private int[] computeHashes(Atom[] atoms, HashMap<Predicate, Integer> atomIndeces, boolean isBody) {
        TreeSet<Integer> hashes = new TreeSet<Integer>();
        for (Atom atom : atoms) {
            // if (!isBody && !atomIndeces.containsKey(atom.getPredicate())) {
            //     atomIndeces.put(atom.getPredicate(), atomCounter);
            //     atomCounter++;
            // }

            if (atomIndeces.containsKey(atom.getPredicate()))
                hashes.add(atomIndeces.get(atom.getPredicate()));

            if (!isBody) {
                for (Term t : atom.getTerms()) {
                    if (t instanceof FunctionTerm) {
                        if (functionIndeces.containsKey(((FunctionTerm) t).getFunction()))
                            hashes.add(functionIndeces.get(((FunctionTerm) t).getFunction()));
                    }
                }
            }
        }
        if (hashes.isEmpty())
            if (atomIndeces == headAtomIndeces)
                hashes.add(0);

        return hashes.stream().mapToInt(Integer::intValue).toArray();
    }

    private void checkHashes(Q formula) {
        if (formula.getBodyHashes() == null)
            formula.setBodyHashes(computeHashes(formula.getBodyAtoms(), bodyAtomIndeces, true));
        if (formula.getHeadHashes() == null)
            formula.setHeadHashes(computeHashes(formula.getHeadAtoms(), headAtomIndeces, false));
    }

    public Iterable<Q> getSubsumedCandidates(Q formula) {
        checkHashes(formula);
        return new SubsumedCandidatesIterable(formula);
    }

    public Iterable<Q> getSubsumingCandidates(Q formula) {
        checkHashes(formula);
        return new SubsumingCandidatesIterable(formula);
    }
}
