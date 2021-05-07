package uk.ac.ox.cs.gsat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter;

import fr.lirmm.graphik.graal.api.core.NegativeConstraint;
import fr.lirmm.graphik.graal.api.core.Predicate;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.io.owl.OWL2Parser;
import fr.lirmm.graphik.graal.io.owl.OWL2ParserException;
import fr.lirmm.graphik.util.Prefix;
import fr.lirmm.graphik.util.stream.IteratorException;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.TGD;

/**
 * OWLSanitiser
 */
public class OWLSanitiser {

    private static final Level level = Level.INFO;
    static final Logger logger = Logger.getLogger("OWL Sanitiser");
    // private static int annotations = 0;
    // private static int asymmetric = 0;
    private static int droppedAxioms = 0;

    public static void main(String[] args) throws Exception {
        Handler handlerObj = new ConsoleHandler();
        handlerObj.setLevel(level);
        logger.addHandler(handlerObj);
        logger.setLevel(level);
        logger.setUseParentHandlers(false);

        System.out.println("Starting the OWL Sanitiser...");

        try {

            if (args.length > 0)
                if (args[0].equals("guarded") || args[0].equals("guarded_kaon2"))
                    if (args.length == 3) {
                        sanitise(args[1], args[2], args[0].equals("guarded_kaon2"));
                    } else
                        printHelp("Wrong number of parameters for sanitise");
                else
                    printHelp("Wrong command (i.e. first argument)");
            else
                printHelp("No arguments provided");

        } catch (Throwable t) {
            System.err.println("Unknown error. The system will now terminate.");
            t.printStackTrace();
            logger.severe(t.getLocalizedMessage());
            System.exit(1);
        }

    }

    /**
     * Prints the help message of the program
     * 
     * @param message the error message
     */
    private static void printHelp(String message) {

        System.err.println();
        System.err.println(message);
        System.err.println();
        System.err.println("Note that only these commands are currently supported:");
        System.err.println("sanitise \t sanitise the input OWL file");
        System.err.println();
        System.err.println("if sanitise is specified the following arguments must be provided, in this strict order:");
        System.err.println("<PATH OF THE INPUT FILE> <PATH OF THE OUTPUT FILE>");
        System.err.println();

    }

    private static void sanitise(String input_file, String output_file, boolean simplifyForKaon2) {

        final long startTime = System.nanoTime();

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology;
        try {

            System.out.println("Loading the ontology from " + input_file);

            ontology = manager.loadOntologyFromOntologyDocument(new File(input_file));

            System.out.println("Sanitising ontology");

            List<OWLAxiom> simplifiedAxioms = new ArrayList<>();
            Set<OWLAxiom> supportedAxioms = new HashSet<>();
            for (OWLAxiom a : ontology.getAxioms()) {
                if (simplifyForKaon2 && isDescription(a) || isFunctionalAxiom(a))
                    continue;

                if (isFactRelated(a) || isTrivialAxiom(a)) {
                    if (!simplifyForKaon2)
                        supportedAxioms.add(a);
                } else {
                    simplifiedAxioms.addAll(simplifyAxiom(a, manager.getOWLDataFactory(), simplifyForKaon2));
                }
            }

            supportedAxioms.addAll(simplifiedAxioms.stream().filter(a -> !isFactRelated(a) && isSupported(a))
                    .collect(Collectors.toSet()));

            // // Storing in a different set, and then saving to file
            OWLDocumentFormat format = manager.getOntologyFormat(ontology);

            manager.createOntology(supportedAxioms).saveOntology(format, new FileOutputStream(output_file));

            System.out.println("\n---------------------------------------------------------");
            System.out.println("Initial axioms: " + ontology.getAxiomCount());
            System.out.println("Simplified axioms: " + simplifiedAxioms.size());
            System.out.println("Simplified axioms dropped: " + droppedAxioms);
            System.out.println("Final axioms: " + supportedAxioms.size());
            System.out.println("---------------------------------------------------------");


        } catch (OWLOntologyCreationException | OWLOntologyStorageException | FileNotFoundException e) {
            e.printStackTrace();
        }

        final long stopTime = System.nanoTime();

        long totalTime = stopTime - startTime;

        App.logger.info("Sanitiser total time : " + String.format(Locale.UK, "%.0f", totalTime / 1E6) + " ms = "
                + String.format(Locale.UK, "%.2f", totalTime / 1E9) + " s");

    }

    private static Collection<? extends OWLAxiom> simplifyAxiom(OWLAxiom a, OWLDataFactory df,
            boolean simplifyForKaon2) {
        Collection<OWLAxiom> result = new ArrayList<>();

        if (a.getAxiomType() == AxiomType.EQUIVALENT_CLASSES) {
            Set<OWLClassExpression> classes = ((OWLEquivalentClassesAxiom) a).getClassExpressions();

            for (OWLClassExpression c1 : classes) {
                for (OWLClassExpression c2 : classes) {
                    if (c1 != c2) {
                        result.addAll(simplifyAxiom(df.getOWLSubClassOfAxiom(c1, c2), df, simplifyForKaon2));
                    }
                }
            }
        } else if (a.getAxiomType() == AxiomType.EQUIVALENT_DATA_PROPERTIES) {
            Set<OWLDataPropertyExpression> properties = ((OWLEquivalentDataPropertiesAxiom) a).getProperties();

            for (OWLDataPropertyExpression p1 : properties) {
                for (OWLDataPropertyExpression p2 : properties) {
                    if (p1 != p2) {
                        result.addAll(simplifyAxiom(df.getOWLSubDataPropertyOfAxiom(p1, p2), df, simplifyForKaon2));
                    }
                }
            }
        } else if (a.getAxiomType() == AxiomType.EQUIVALENT_OBJECT_PROPERTIES) {
            Set<OWLObjectPropertyExpression> properties = ((OWLEquivalentObjectPropertiesAxiom) a).getProperties();

            for (OWLObjectPropertyExpression p1 : properties) {
                for (OWLObjectPropertyExpression p2 : properties) {
                    if (p1 != p2) {
                        result.addAll(simplifyAxiom(df.getOWLSubObjectPropertyOfAxiom(p1, p2), df, simplifyForKaon2));
                    }
                }
            }
        } else if (a.getAxiomType() == AxiomType.SUBCLASS_OF) {
            RemoveUnionFromSubclassVisitor bodyVisitor = new RemoveUnionFromSubclassVisitor(df, simplifyForKaon2);
            SimplifySuperclassVisitor headVisitor = new SimplifySuperclassVisitor(df, simplifyForKaon2);

            ((OWLSubClassOfAxiom) a).getSuperClass().accept(headVisitor);
            ((OWLSubClassOfAxiom) a).getSubClass().accept(bodyVisitor);

            OWLClassExpression head = headVisitor.getSimplication();
            for (OWLClassExpression body : bodyVisitor.getSimplications())
                result.add(df.getOWLSubClassOfAxiom(body, head));

        } else {
            result.add(a);
        }

        return result;
    }

    private static boolean isDescription(OWLAxiom a) {
        return a.getAxiomType() == AxiomType.DISJOINT_CLASSES;
    }

    private static boolean isTrivialAxiom(OWLAxiom a) {
        return (a.getAxiomType() == AxiomType.SUBCLASS_OF && ((OWLSubClassOfAxiom) a).getSuperClass().isOWLThing())
                || (a.getAxiomType() == AxiomType.SUBCLASS_OF && ((OWLSubClassOfAxiom) a).getSubClass().isOWLNothing());
    }

    private static boolean isFactRelated(OWLAxiom a) {
        return a.getAxiomType() == AxiomType.CLASS_ASSERTION || a.getAxiomType() == AxiomType.DATA_PROPERTY_ASSERTION
                || a.getAxiomType() == AxiomType.OBJECT_PROPERTY_ASSERTION
                || a.getAxiomType() == AxiomType.ANNOTATION_ASSERTION || a.getAxiomType() == AxiomType.DECLARATION
                || a.getAxiomType() == AxiomType.DIFFERENT_INDIVIDUALS
                || (a.getAxiomType() == AxiomType.SUBCLASS_OF && (((OWLSubClassOfAxiom) a).getSubClass()
                        .getClassExpressionType() == ClassExpressionType.OBJECT_ONE_OF));
    }

    private static boolean isFunctionalAxiom(OWLAxiom a) {
        return a.getAxiomType() == AxiomType.FUNCTIONAL_DATA_PROPERTY
                || a.getAxiomType() == AxiomType.FUNCTIONAL_OBJECT_PROPERTY
                || a.getAxiomType() == AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY;
    }

    private static boolean isSupported(OWLAxiom a) {

        // ugly and very inefficient, but it could work
        try {

            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.createOntology(Set.of(a));

            OutputStream output = new OutputStream() {
                private StringBuilder string = new StringBuilder();

                @Override
                public void write(int b) throws IOException {
                    this.string.append((char) b);
                }

                public String toString() {
                    return this.string.toString();
                }
            };
            ontology.saveOntology(new OWLXMLDocumentFormat(), output);

            OWL2Parser parser = new OWL2Parser(output.toString());

            Set<Rule> rules = new HashSet<>();

            while (parser.hasNext()) {
                Object o = parser.next();
                if (o instanceof Rule) {
                    App.logger.fine("Rule: " + ((Rule) o));
                    rules.add((Rule) o);
                } else if (o instanceof NegativeConstraint) {
                    rules.add((NegativeConstraint) o);
                } else if (!(o instanceof Prefix)) {
                    parser.close();
                    System.out.println(isFactRelated(a));
                    dropAxiom("Parsed something that is not a rule nor a negative constraint: " + o + "; it is a `"
                            + o.getClass().getSimpleName() + "`", a, rules, null);
                    throw new IllegalStateException();
                }
            }

            parser.close();

            // filter top body
            rules = rules.stream().filter(r -> !r.getBody().iterator().next().getPredicate().equals(Predicate.TOP))
                    .collect(Collectors.toSet());

            if (rules.isEmpty()) {
                return dropAxiom("OWL axiom, which is not translated by Graal parser", a, null, null);
            }
            Collection<Dependency> pdqtgDsFromGraalRules = DLGPIO.getPDQTGDsFromGraalRules(rules, new HashMap<>());

            // a rule have been discarded during the translation
            if (pdqtgDsFromGraalRules.size() != rules.size()) {
                dropAxiom("The number of rules identified by Graal is different from the number of the converted TDGs",
                        a, rules, pdqtgDsFromGraalRules);
                throw new IllegalStateException();
            }

            int guardedTGDsCount = 0;
            for (Dependency dependency : pdqtgDsFromGraalRules)
                if (dependency instanceof TGD && ((TGD) dependency).isGuarded()) {
                    guardedTGDsCount++;
                }

            // all the TGD are not guarded
            if (guardedTGDsCount == 0)
                return dropAxiom("All the dependency of the axiom are either no TGD or not guarded", a, rules,
                        pdqtgDsFromGraalRules);
            else if (guardedTGDsCount != pdqtgDsFromGraalRules.size()) {
                dropAxiom("Found a dependency that is not a TGD or that is not guarded: ", a, rules,
                        pdqtgDsFromGraalRules);
                throw new IllegalStateException();
            }

            return true;

        } catch (OWL2ParserException | IteratorException | OWLOntologyStorageException
                | OWLOntologyCreationException e) {
            e.printStackTrace();
            dropAxiom("Exception thrown!", a, null, null);
            throw new IllegalStateException(e);
        }

    }

    private static boolean dropAxiom(String message, OWLAxiom a, Set<Rule> rules,
            Collection<Dependency> pdqtgDsFromGraalRules) {

        System.out.println("\n---------------------------------------------------------");
        System.out.println("Message: '" + message + "'\n\nDropped axiom: " + a
                + (rules != null ? "\n\n" + rules.size() + " rules: " + rules : "")
                + (pdqtgDsFromGraalRules != null ? "\n\nTGDs: " + pdqtgDsFromGraalRules : ""));
        System.out.println("\n---------------------------------------------------------");

        droppedAxioms++;
        return false;
    }

    private static class RemoveUnionFromSubclassVisitor extends OWLClassExpressionVisitorAdapter {

        private final LinkedList<Set<OWLClassExpression>> simplifiedClasses = new LinkedList<>();
        private final OWLDataFactory df;
        private final boolean simplifyForKaon2;

        public RemoveUnionFromSubclassVisitor(OWLDataFactory df, boolean simplifyForKaon2) {
            this.df = df;
            this.simplifyForKaon2 = simplifyForKaon2;
        }

        public Set<OWLClassExpression> getSimplications() {
            return simplifiedClasses.pop();
        }

        @Override
        protected void handleDefault(OWLClassExpression c) {
            this.simplifiedClasses.push(Set.of(c));
        }

        @Override
        public void visit(OWLObjectIntersectionOf ce) {
            List<List<OWLClassExpression>> simplifiedOperandsList = computeSimplifiedOperands(ce);

            Set<OWLClassExpression> result = new HashSet<>();
            for (List<OWLClassExpression> ops : getProduct(simplifiedOperandsList))
                result.add(df.getOWLObjectIntersectionOf(new HashSet<>(ops)));

            simplifiedClasses.push(result);
        }

        private List<List<OWLClassExpression>> computeSimplifiedOperands(OWLNaryBooleanClassExpression ce) {
            List<List<OWLClassExpression>> simplifiedOperandsList = new ArrayList<>();
            for (OWLClassExpression c : ce.getOperands()) {
                c.accept(this);
                simplifiedOperandsList.add(new ArrayList<>(this.getSimplications()));
            }

            return simplifiedOperandsList;
        }

        private List<List<OWLClassExpression>> getProduct(List<List<OWLClassExpression>> simplifiedOperandsList) {
            List<List<OWLClassExpression>> resultLists = new ArrayList<>();
            if (simplifiedOperandsList.size() == 0) {
                resultLists.add(new ArrayList<OWLClassExpression>());
                return resultLists;
            } else {
                List<OWLClassExpression> firstList = simplifiedOperandsList.get(0);
                List<List<OWLClassExpression>> remainingLists = getProduct(
                        simplifiedOperandsList.subList(1, simplifiedOperandsList.size()));
                for (OWLClassExpression condition : firstList) {
                    for (List<OWLClassExpression> remainingList : remainingLists) {
                        ArrayList<OWLClassExpression> resultList = new ArrayList<OWLClassExpression>(
                                1 + remainingList.size());
                        resultList.add(condition);
                        resultList.addAll(remainingList);
                        resultLists.add(resultList);
                    }
                }
            }
            return resultLists;
        }

        @Override
        public void visit(OWLObjectAllValuesFrom ce) {
            OWLClassExpression f = ce.getFiller();
            f.accept(this);

            Set<OWLClassExpression> result = new HashSet<>();
            for (OWLClassExpression sf : this.getSimplications())
                result.add(df.getOWLObjectAllValuesFrom(ce.getProperty(), sf));
            simplifiedClasses.push(result);
        }

        @Override
        public void visit(OWLObjectSomeValuesFrom ce) {
            OWLClassExpression f = ce.getFiller();
            f.accept(this);

            Set<OWLClassExpression> result = new HashSet<>();
            for (OWLClassExpression sf : this.getSimplications())
                result.add(df.getOWLObjectSomeValuesFrom(ce.getProperty(), sf));
            simplifiedClasses.push(result);
        }

        @Override
        public void visit(OWLObjectUnionOf ce) {
            List<List<OWLClassExpression>> simplifiedOperandsList = computeSimplifiedOperands(ce);

            Set<OWLClassExpression> result = new HashSet<>();
            for (List<OWLClassExpression> l : simplifiedOperandsList)
                for (OWLClassExpression c : l)
                    result.add(c);
            simplifiedClasses.push(result);
        }

        @Override
        public void visit(OWLObjectOneOf ce) {
            if (simplifyForKaon2)
                simplifiedClasses.push(Set.of(df.getOWLThing()));
            else
                simplifiedClasses.push(Set.of(ce));
        }
    }

    private static class SimplifySuperclassVisitor extends OWLClassExpressionVisitorAdapter {

        private final LinkedList<OWLClassExpression> simplifiedClasses = new LinkedList<>();
        private final OWLDataFactory df;
        private final boolean simplifyForKaon2;

        public SimplifySuperclassVisitor(OWLDataFactory df, boolean simplifyForKaon2) {
            this.df = df;
            this.simplifyForKaon2 = simplifyForKaon2;
        }

        public OWLClassExpression getSimplication() {
            return simplifiedClasses.pop();
        }

        @Override
        protected void handleDefault(OWLClassExpression c) {
            this.simplifiedClasses.push(c);
        }

        @Override
        public void visit(OWLObjectExactCardinality ce) {
            OWLPropertyExpression p = ce.getProperty();

            if (ce.getCardinality() == 0) {
                simplifiedClasses.push(ce);
                return;
            }

            OWLClassExpression f = ce.getFiller();
            f.accept(this);
            simplifiedClasses
                    .push(df.getOWLObjectSomeValuesFrom((OWLObjectPropertyExpression) p, this.getSimplication()));
        }

        @Override
        public void visit(OWLDataExactCardinality ce) {
            if (ce.getCardinality() == 0) {
                simplifiedClasses.push(ce);
                return;
            }

            OWLDataRange f = ce.getFiller();
            simplifiedClasses.push(df.getOWLDataSomeValuesFrom((OWLDataPropertyExpression) ce.getProperty(), f));
        }

        @Override
        public void visit(OWLObjectComplementOf ce) {
            simplifiedClasses.push(df.getOWLThing());
        }

        @Override
        public void visit(OWLObjectMaxCardinality ce) {
            if (ce.getCardinality() > 0) {
                simplifiedClasses.push(df.getOWLThing());
            } else {
                simplifiedClasses.push(df.getOWLNothing());
            }
        }

        @Override
        public void visit(OWLDataMaxCardinality ce) {
            if (ce.getCardinality() > 0) {
                simplifiedClasses.push(df.getOWLThing());
            } else {
                simplifiedClasses.push(df.getOWLNothing());
            }
        }

        @Override
        public void visit(OWLObjectIntersectionOf ce) {
            HashSet<OWLClassExpression> simplifiedOperands = new HashSet<>();
            for (OWLClassExpression c : ce.getOperands()) {
                c.accept(this);
                simplifiedOperands.add(this.getSimplication());
            }

            simplifiedClasses.push(df.getOWLObjectIntersectionOf(simplifiedOperands));
        }

        @Override
        public void visit(OWLObjectAllValuesFrom ce) {
            OWLClassExpression f = ce.getFiller();
            f.accept(this);
            simplifiedClasses.push(df.getOWLObjectAllValuesFrom(ce.getProperty(), this.getSimplication()));
        }

        @Override
        public void visit(OWLObjectSomeValuesFrom ce) {
            OWLClassExpression f = ce.getFiller();
            f.accept(this);

            simplifiedClasses.push(df.getOWLObjectSomeValuesFrom(ce.getProperty(), this.getSimplication()));
        }

        @Override
        public void visit(OWLObjectMinCardinality ce) {
            OWLClassExpression f = ce.getFiller();
            f.accept(this);
            if (ce.getCardinality() > 0)
                simplifiedClasses.push(df.getOWLObjectSomeValuesFrom(ce.getProperty(), this.getSimplication()));
            else
                simplifiedClasses.push(df.getOWLThing());
        }

        @Override
        public void visit(OWLObjectHasValue ce) {
            if (simplifyForKaon2)
                simplifiedClasses.push(df.getOWLThing());
            else
                simplifiedClasses.push(ce);
        }

        @Override
        public void visit(OWLDataHasValue ce) {
            if (simplifyForKaon2)
                simplifiedClasses.push(df.getOWLThing());
            else
                simplifiedClasses.push(ce);
        }

        @Override
        public void visit(OWLObjectOneOf ce) {
            if (simplifyForKaon2)
                simplifiedClasses.push(df.getOWLThing());
            else
                simplifiedClasses.push(ce);
        }

    }
}
