package uk.ac.ox.cs.gsat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import fr.lirmm.graphik.graal.api.core.NegativeConstraint;
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
                if (args[0].equals("sanitise"))
                    if (args.length == 3) {
                        sanitise(args[1], args[2]);
                    } else
                        printHelp("Wrong number of parameters for sanitise");
                else
                    printHelp("Wrong command (i.e. first argument)");
            else
                printHelp("No arguments provided");

        } catch (Throwable t) {
            System.err.println("Unknown error. The system will now terminate.");
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

    private static void sanitise(String input_file, String output_file) {

        final long startTime = System.nanoTime();

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology;
        try {

            System.out.println("Loading the ontology from " + input_file);

            ontology = manager.loadOntologyFromOntologyDocument(new File(input_file));
            // System.out.println(ontology);

            System.out.println("Sanitising ontology");

            // // Old-style Java
            // Collection<RemoveAxiom> axiomsToRemove = new LinkedList<>();
            //
            // for (OWLLogicalAxiom logicalAxiom : ontology.getLogicalAxioms())
            // if (isForbidden(logicalAxiom))
            // axiomsToRemove.add(new RemoveAxiom(ontology, logicalAxiom));
            //
            // axiomsToRemove.forEach(ra -> manager.applyChange(ra));

            // // A bit better
            // Collection<RemoveAxiom> axiomsToRemove = ontology.logicalAxioms().filter((a
            // -> isForbidden(a))).map(a -> new RemoveAxiom(ontology,
            // a)).collect(Collectors.toList());
            // axiomsToRemove.forEach(ra -> manager.applyChange(ra));

            // // Without temporary collection
            // ontology.logicalAxioms().filter(a -> isForbidden(a)).map(a -> new
            // RemoveAxiom(ontology, a)).forEach(ra -> manager.applyChange(ra));

            // // Without temporary variables
            // ontology.logicalAxioms().filter(a -> isForbidden(a)).forEach(a ->
            // ontology.remove(a));
            // // I need to use this, because Graal uses the old version of OWLAPI (4) and
            // we cannot load both in the pom file!
            // ontology.getAxioms().stream().filter(a -> isForbidden(a)).map(a -> new
            // RemoveAxiom(ontology, a))
            // .forEach(ra -> manager.applyChange(ra));

            // // Using the specific function of AxiomType, but with a new temporary
            // ontology
            // OWLOntology output_ontology = manager.createOntology(
            // AxiomType.getAxiomsWithoutTypes(ontology.getAxioms(),
            // AxiomType.FUNCTIONAL_OBJECT_PROPERTY, AxiomType.SUB_OBJECT_PROPERTY,
            // AxiomType.TRANSITIVE_OBJECT_PROPERTY, AxiomType.HAS_KEY,
            // AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY,
            // AxiomType.REFLEXIVE_OBJECT_PROPERTY, AxiomType.IRREFLEXIVE_OBJECT_PROPERTY));

            // // All together
            // manager.createOntology(AxiomType.getAxiomsWithoutTypes(ontology.getAxioms(),
            // AxiomType.FUNCTIONAL_OBJECT_PROPERTY, AxiomType.SUB_OBJECT_PROPERTY,
            // AxiomType.TRANSITIVE_OBJECT_PROPERTY, AxiomType.HAS_KEY,
            // AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY,
            // AxiomType.REFLEXIVE_OBJECT_PROPERTY, AxiomType.IRREFLEXIVE_OBJECT_PROPERTY))
            // .saveOntology(new OWLXMLDocumentFormat(), new FileOutputStream(output_file));

            // Storing in a different set, and then saving to file
            manager.createOntology(
                    ontology.getAxioms().stream().filter(a -> isSupported(a)).collect(Collectors.toSet()))
                    .saveOntology(new RDFXMLDocumentFormat(), new FileOutputStream(output_file));

            // System.out.println("Saving the ontology to " + output_file);

            // ontology.saveOntology(new OWLXMLDocumentFormat(), new
            // FileOutputStream(output_file));
            // I have to use this because KAON2 does not like OWL/XML
            // ontology.saveOntology(new RDFXMLDocumentFormat(), new
            // FileOutputStream(output_file));
            // output_ontology.saveOntology(new OWLXMLDocumentFormat(), new
            // FileOutputStream(output_file));

            System.out.println("Axioms dropped: " + droppedAxioms + "/" + ontology.getAxiomCount());

        } catch (OWLOntologyCreationException | OWLOntologyStorageException | FileNotFoundException e) {
            e.printStackTrace();
        }

        final long stopTime = System.nanoTime();

        long totalTime = stopTime - startTime;

        App.logger.info("Sanitiser total time : " + String.format(Locale.UK, "%.0f", totalTime / 1E6) + " ms = "
                + String.format(Locale.UK, "%.2f", totalTime / 1E9) + " s");

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
            // System.out.println(output.toString());
            OWL2Parser parser = new OWL2Parser(output.toString());
            // System.out.println(parser.hasNext());

            if (!parser.hasNext()) { // useless part of the OWL file
                parser.close();
                return dropAxiom("Useless part of the OWL file, nothing to parse", a, null, null);
            }

            HashSet<Rule> rules = new HashSet<>();

            while (parser.hasNext()) {
                Object o = parser.next();
                if (o instanceof Rule) {
                    App.logger.fine("Rule: " + ((Rule) o));
                    rules.add((Rule) o);
                } else if (o instanceof NegativeConstraint) {
                    rules.add((NegativeConstraint) o);
                } else if (!(o instanceof Prefix)) {
                    // System.out.println(o.getClass());
                    parser.close();
                    return dropAxiom("Parsed something that is not a rule nor a negative constraint: " + o
                            + "; it is a `" + o.getClass().getSimpleName() + "`", a, rules, null);
                }
            }

            parser.close();
            // System.out.println(rules);

            if (rules.isEmpty()) // useless part of the OWL file
                return dropAxiom("Useless part of the OWL file, no rules", a, null, null);

            Collection<Dependency> pdqtgDsFromGraalRules = DLGPIO.getPDQTGDsFromGraalRules(rules, new HashMap<>());

            if (pdqtgDsFromGraalRules.size() != rules.size()) // there is something that we discard
                return dropAxiom(
                        "The number of rules identified by Graal is different from the number of the converted TDGs", a,
                        rules, pdqtgDsFromGraalRules);

            for (Dependency dependency : pdqtgDsFromGraalRules)
                if (!(dependency instanceof TGD) || !(((TGD) dependency).isGuarded()))
                    return dropAxiom("Found a dependency that is not a TGD or that is not guarded: " + dependency, a,
                            rules, pdqtgDsFromGraalRules);

            return true;

        } catch (OWL2ParserException | IteratorException | OWLOntologyStorageException
                | OWLOntologyCreationException e) {
            e.printStackTrace();
            return dropAxiom("Exception thrown!", a, null, null);
        }

    }

    private static boolean dropAxiom(String message, OWLAxiom a, HashSet<Rule> rules,
            Collection<Dependency> pdqtgDsFromGraalRules) {
        System.out
                .println("Message: '" + message + "'\nDropped axiom: " + a + (rules != null ? "\nrules: " + rules : "")
                        + (pdqtgDsFromGraalRules != null ? "\nTGDs: " + pdqtgDsFromGraalRules : ""));
        droppedAxioms++;
        return false;
    }

    // private static boolean isForbidden(OWLAxiom a) {

    // // ugly and very inefficient, but it could work
    // try {

    // OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    // OWLOntology ontology = manager.createOntology(Set.of(a));

    // OutputStream output = new OutputStream() {
    // private StringBuilder string = new StringBuilder();

    // @Override
    // public void write(int b) throws IOException {
    // this.string.append((char) b);
    // }

    // public String toString() {
    // return this.string.toString();
    // }
    // };
    // ontology.saveOntology(new OWLXMLDocumentFormat(), output);
    // // System.out.println(output.toString());
    // OWL2Parser parser = new OWL2Parser(output.toString());
    // // System.out.println(parser.hasNext());

    // if (!parser.hasNext()) { // useless part of the OWL file
    // parser.close();
    // return true;
    // }

    // HashSet<Rule> rules = new HashSet<>();

    // while (parser.hasNext()) {
    // Object o = parser.next();
    // if (o instanceof Rule) {
    // App.logger.fine("Rule: " + ((Rule) o));
    // rules.add((Rule) o);
    // } else if (o instanceof Prefix) {
    // ;
    // } else {
    // // System.out.println(o.getClass());
    // parser.close();
    // return true;
    // }
    // }

    // parser.close();
    // // System.out.println(rules);

    // Collection<Dependency> pdqtgDsFromGraalRules =
    // DLGPIO.getPDQTGDsFromGraalRules(rules);

    // if (pdqtgDsFromGraalRules.size() != rules.size()) { // there is something
    // that we discard
    // return true;
    // }

    // for (Dependency dependency : pdqtgDsFromGraalRules)
    // if (dependency instanceof TGD && ((TGD) dependency).isGuarded())
    // ;
    // else
    // return true;

    // return false;

    // } catch (OWL2ParserException | IteratorException |
    // OWLOntologyStorageException
    // | OWLOntologyCreationException e) {
    // e.printStackTrace();
    // return true;
    // }

    // // System.out.println("ERROR!!!");

    // // if (a.isOfType(AxiomType.ANNOTATION_ASSERTION,
    // // AxiomType.ANNOTATION_PROPERTY_DOMAIN,
    // // AxiomType.ANNOTATION_PROPERTY_RANGE)) {
    // // annotations++;
    // // return true;
    // // }
    // // if (a.isOfType(AxiomType.ASYMMETRIC_OBJECT_PROPERTY)) {
    // // asymmetric++;
    // // return true;
    // // }
    // // if (a.isOfType(AxiomType.CLASS_ASSERTION)) {
    // // //
    // // }
    // // if (a.isOfType(AxiomType.FUNCTIONAL_OBJECT_PROPERTY,
    // // AxiomType.TRANSITIVE_OBJECT_PROPERTY, AxiomType.HAS_KEY,
    // // AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY,
    // // AxiomType.REFLEXIVE_OBJECT_PROPERTY,
    // // AxiomType.IRREFLEXIVE_OBJECT_PROPERTY))
    // // return true;

    // // return false;

    // }

}
