package uk.ac.ox.cs.gsat;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) throws Exception {

        if (args.length > 1) {

            String input = FileSystems.getDefault().getPath(args[0]).normalize().toFile().getCanonicalPath()
                    .toString();
            String output = FileSystems.getDefault().getPath(args[1]).normalize().toFile().getCanonicalPath()
                    .toString();

            DLGPIO io = new DLGPIO(input, true);

            List<TGD> rules = new ArrayList<>();

            for(Dependency tgd: io.getRules())
                rules.add((TGD) tgd);

            createOutput(rules, output);
            
        } else {
            System.out.println("first input is the path to the DLGP file");
            System.out.println("second input is the ouput file");
        }
    }

    private static void createOutput(List<TGD> rules, String output) throws IOException {
        Map<TGD, List<TGD>> adjacencyMap = createAdjacencyMap(rules);
        Map<TGD, Integer> counters = new HashMap<>();
        for (int counter = 0; counter<rules.size(); counter++)
            counters.put(rules.get(counter), counter);

        Writer writer = new FileWriter(output);

        outputRules(rules, counters, writer);
        writer.append("\n");
        outputAdjacency(adjacencyMap, counters, writer);
        writer.append("\n");
        outputRulesProperties(rules, counters, writer);

        writer.close();
    }

    private static void outputRulesProperties(List<TGD> rules, Map<TGD, Integer> counters, Writer writer) throws IOException {
        writer.append("== RULE PROPERTIES ===\n");
        writer.append("+------+------+\n");
        for (TGD tgd : rules) {
            int tgdCounter = counters.get(tgd);
            boolean isFullBool = tgd.getExistential().length == 0;
            String isFull = (isFullBool) ? "X" : "-";
            // System.out.println(tgd);
            // System.out.println("body: " + Arrays.toString(tgd.getBody().getFreeVariables()));
            // System.out.println("head: " + Arrays.toString(tgd.getHead().getFreeVariables()));
            String containsProjection = (isFullBool && !Set.of(tgd.getHead().getFreeVariables()).containsAll(Set.of(tgd.getBody().getFreeVariables()))) ? "X" : "-";
            writer.append("|  " + isFull + "   |  " + containsProjection + "   |   _R" + tgdCounter +"\n");
        }
        writer.append("+------+------+\n");
        writer.append("|  rr  | proj |\n");
        writer.append("+------+------+\n");

    }

    private static void outputAdjacency(Map<TGD, List<TGD>> adjacencyMap, Map<TGD, Integer> counters, Writer writer) throws IOException {
        writer.append("======== Predicate Graph =========\n");
        for (TGD tgd : adjacencyMap.keySet()) {
            int tgdCounter = counters.get(tgd);
            for (TGD child: adjacencyMap.get(tgd)) {
                int childCounter = counters.get(child);
                writer.append("_R" + tgdCounter + " --> " + "_R" + childCounter + "\n");
            }
        }
    }

    private static void outputRules(List<TGD> rules, Map<TGD, Integer> counters, Writer writer) throws IOException {
        writer.append("======== RULE SET =========\n");
        for(TGD tgd: rules) {
            int counter = counters.get(tgd);
            writer.append("[_R" + counter + "] " + Arrays.toString(tgd.getHeadAtoms()) + " :- " + Arrays.toString(tgd.getBodyAtoms()) + "\n");
        }
    }

    // each row represents the left input, each column the right one
    public static Map<TGD,List<TGD>> createAdjacencyMap(List<TGD> rules) {
        Map<TGD,List<TGD>> adjacencyMap = new HashMap<>();
        List<Set<Predicate>> bodyPredicateBags = createPredicateBag(rules, false);
        List<Set<Predicate>> headPredicateBags = createPredicateBag(rules, true);
        
        for (int i = 0; i < rules.size(); i++) {
            List<TGD> children = new ArrayList<>();
            adjacencyMap.put(rules.get(i), children);
            Set<Predicate> headPredicates = headPredicateBags.get(i);
            for(int j = 0; j < rules.size(); j++) { 
                Set<Predicate> bodyPredicates = bodyPredicateBags.get(j);
                if (!Collections.disjoint(headPredicates, bodyPredicates))
                    children.add(rules.get(j));
            }
        }

        return adjacencyMap;
    }

    public static List<Set<Predicate>> createPredicateBag(List<TGD> rules, boolean forHead) {
        List<Set<Predicate>> bags = new ArrayList<>();

        for (TGD tgd : rules) {
            Set<Predicate> bag = new HashSet<>();
            bags.add(bag);
            Atom[] atoms = (forHead) ? tgd.getHeadAtoms() : new Atom[] { computeGuard(tgd) };
            for (Atom atom : atoms) {
                bag.add(atom.getPredicate());
            }
        }
        return bags;
    }

    public static Atom computeGuard(TGD tgd) {

        List<Variable> universalList = Arrays.asList(tgd.getTopLevelQuantifiedVariables());

        Atom currentGuard = null;
        for (Atom atom : tgd.getBodyAtoms())
            if (Arrays.asList(atom.getVariables()).containsAll(universalList))
                if (currentGuard == null || atom.getPredicate().getArity() < currentGuard.getPredicate().getArity())
                    currentGuard = atom;
                else if (atom.getPredicate().getArity() == currentGuard.getPredicate().getArity()
                        && atom.getPredicate().getName().compareTo(currentGuard.getPredicate().getName()) < 0)
                    currentGuard = atom;

        if (currentGuard == null)
            throw new IllegalArgumentException("GTGD must be guarded! But found " + tgd);

        return currentGuard;

    }


}
