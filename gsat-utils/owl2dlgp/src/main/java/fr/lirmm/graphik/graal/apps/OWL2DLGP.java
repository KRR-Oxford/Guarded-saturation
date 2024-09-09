/* Graal v0.7.4
 * Copyright (c) 2014-2015 Inria Sophia Antipolis - Méditerranée / LIRMM (Université de Montpellier & CNRS)
 * All rights reserved.
 * This file is part of Graal <https://graphik-team.github.io/graal/>.
 *
 * Author(s): Clément SIPIETER
 *            Mélanie KÖNIG
 *            Swan ROCHER
 *            Jean-François BAGET
 *            Michel LECLÈRE
 *            Marie-Laure MUGNIER
 */
/**
* 
*/
package fr.lirmm.graphik.graal.apps;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.AtomSet;
import fr.lirmm.graphik.graal.api.core.Predicate;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.core.Variable;
import fr.lirmm.graphik.graal.core.DefaultNegativeConstraint;
import fr.lirmm.graphik.graal.core.atomset.LinkedListAtomSet;
import fr.lirmm.graphik.graal.core.factory.DefaultAtomFactory;
import fr.lirmm.graphik.graal.core.term.DefaultTermFactory;
import fr.lirmm.graphik.graal.io.dlp.Directive;
import fr.lirmm.graphik.graal.io.dlp.Directive.Type;
import fr.lirmm.graphik.graal.io.dlp.DlgpWriter;
import fr.lirmm.graphik.graal.io.owl.OWL2Parser;
import fr.lirmm.graphik.graal.io.owl.OWL2ParserException;
import fr.lirmm.graphik.util.Apps;
import fr.lirmm.graphik.util.DefaultURI;
import fr.lirmm.graphik.util.Prefix;
import fr.lirmm.graphik.util.stream.CloseableIterator;
import fr.lirmm.graphik.util.stream.CloseableIteratorWithoutException;

/**
 * @author Clément Sipieter (INRIA) {@literal <clement@6pi.fr>}
 *
 */
public class OWL2DLGP {

    private static Predicate THING = new Predicate(new DefaultURI("http://www.w3.org/2002/07/owl#Thing"), 1);
    private static Atom NOTHING = DefaultAtomFactory.instance().create(
            new Predicate(new DefaultURI("http://www.w3.org/2002/07/owl#Nothing"), 1),
            DefaultTermFactory.instance().createVariable("X"));
    private static int rulesCount = 0;
    private static List<Rule> guardedRules = new ArrayList<>();
    private static List<Rule> equalityRules = new ArrayList<>();
    private static List<Rule> negativeRules = new ArrayList<>();
    private static List<Rule> unguardedRules = new ArrayList<>();
    private static List<Rule> existentialUnguardedRules = new ArrayList<>();
    private static List<Rule> existentialGuardedRules = new ArrayList<>();
    private static List<Atom> facts = new ArrayList<>();
    private static List<Object> otherDependencies = new ArrayList<>();

    @Parameter(names = { "-h", "--help" }, description = "Print this message", help = true)
    private boolean help;

    @Parameter(names = { "--version" }, description = "Print version information")
    private boolean version = false;

    @Parameter(names = { "-f", "--file" }, description = "OWL input file")
    private String inputFile = "";

    @Parameter(names = { "-o", "--output" }, description = "The output file")
    private String outputFile = "";

    @Parameter(names = { "-b", "--base" }, description = "specify dlgp base directive")
    private String base = "";

    @Parameter(names = { "-d", "--debug" }, description = "enable debug mode", hidden = true)
    private Boolean debugMode = false;

    @Parameter(names = { "-G", "--guarded-only" }, description = "remove unguarded rules from the output")
    private static Boolean guardedOnly = false;



    public static void main(String args[]) throws OWLOntologyCreationException, IOException, OWL2ParserException {

        DlgpWriter writer;
        OWL2DLGP options = new OWL2DLGP();
        JCommander commander = new JCommander(options, args);
        commander.setProgramName("java -jar owl2dlgp-*.jar");

        if (options.help) {
            commander.usage();
            System.exit(0);
        }

        if (options.version) {
            Apps.printVersion("owl2dlgp");
            System.exit(0);
        }

        if (options.debugMode) {
            ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(OWL2Parser.class)).setLevel(Level.DEBUG);
        }

        OWL2Parser parser;
        if (options.inputFile.isEmpty()) {
            parser = new OWL2Parser(System.in);
        } else {
            parser = new OWL2Parser(new File(options.inputFile));
        }

        if (options.outputFile.isEmpty()) {
            writer = new DlgpWriter(System.out);
        } else {
            writer = new DlgpWriter(new File(options.outputFile));
        }

        if (!options.base.isEmpty()) {
            writer.write(new Directive(Type.BASE, options.base));
        }

        // MAIN
        Object o;
        while (parser.hasNext()) {
            o = parser.next();

            if (o instanceof Rule) {
                Rule r = (Rule) o;
                processRule(r);
            }

            if (o instanceof Prefix)
                writer.write(o);

            if (!(o instanceof Prefix)) {
                writer.write(new Directive(Directive.Type.TOP, THING));
                writer.write(new DefaultNegativeConstraint(new LinkedListAtomSet(NOTHING)));
                break;
            }
        }

        while (parser.hasNext()) {
            o = parser.next();

            if (o instanceof Rule) {
                Rule r = (Rule) o;
                processRule(r);
            } else if (!guardedOnly && (o instanceof Atom)) {
                facts.add((Atom) o);
            } else if (!guardedOnly && (o instanceof AtomSet)) {
                AtomSet s = (AtomSet) o;
                CloseableIterator<Atom> it = s.iterator();
                while (it.hasNext())
                    facts.add(it.next());
            } else if (!guardedOnly)
                otherDependencies.add(o);
        }

        writer.write("\n");

        writeRulesPart(writer, facts, "facts");
        writeRulesPart(writer, otherDependencies, "other dependencies");
        writeRulesPart(writer, equalityRules, "rules with equality");
        writeRulesPart(writer, negativeRules, "rules with negation");
        writeRulesPart(writer, guardedRules, "full guarded rules");
        writeRulesPart(writer, unguardedRules, "full unguarded rules");
        writeRulesPart(writer, existentialGuardedRules, "existential guarded rules");
        writeRulesPart(writer, existentialUnguardedRules, "existential unguarded rules");

        writer.writeComment("Facts: " + facts.size());
        writer.writeComment("Other dependencies: " + otherDependencies.size());
        writer.writeComment("Rules with equality: " + equalityRules.size());
        writer.writeComment("Rules with negation: " + negativeRules.size());
        writer.writeComment("Full guarded rules: " + guardedRules.size());
        writer.writeComment("Full unguarded rules: " + unguardedRules.size());
        writer.writeComment("Existential guarded rules: " + existentialGuardedRules.size());
        writer.writeComment("Existential unguarded rules: " + existentialUnguardedRules.size());
        writer.writeComment("Rules: " + rulesCount);

        writer.close();

    }

    private static void writeRulesPart(DlgpWriter writer, List<? extends Object> dependencies, String title) throws IOException {
        if (dependencies.isEmpty())
            return;

        writer.writeComment("---------- " + title + " ----------");

        for (Object r : dependencies)
            writer.write(r);

        writer.writeComment("------------------------------------");
        writer.write("\n");

    }

    private static void processRule(Rule r) {
        boolean isGuarded = isGuardedRule(r);
        rulesCount++;

        if (containsEqualityAtom(r)) {
            if (!guardedOnly)
                equalityRules.add(r);
            return;
        }

        if (isNegativeRules(r)) {
            if (!guardedOnly || isGuarded)
                negativeRules.add(r);
            return;
        }

        if (r.getExistentials().size() > 0) {
            if (isGuarded) {
                existentialGuardedRules.add(r);
                return;
            }
            if (!guardedOnly)
                existentialUnguardedRules.add(r);
            return;
        }

        if (isGuarded)
            guardedRules.add(r);
        else if (!guardedOnly)
            unguardedRules.add(r);
    }

    private static boolean isNegativeRules(Rule r) {
        CloseableIteratorWithoutException<Atom> headIt = r.getHead().iterator();

        while (headIt.hasNext()) {
            Atom a = headIt.next();
            if (a.getPredicate().equals(Predicate.BOTTOM))
                return true;
        }

        return false;
    }

    private static boolean containsEqualityAtom(Rule r) {
        CloseableIteratorWithoutException<Atom> headIt = r.getHead().iterator();

        while (headIt.hasNext()) {
            Atom a = headIt.next();
            if (a.getPredicate().equals(Predicate.EQUALITY))
                return true;
        }

        return false;
    }

    private static boolean isGuardedRule(Rule r) {

        CloseableIteratorWithoutException<Atom> bodyIt = r.getBody().iterator();
        Set<Variable> variables = r.getBody().getVariables();
        while (bodyIt.hasNext()) {
            Atom a = bodyIt.next();
            if (a.getVariables().containsAll(variables)) {
                return true;
            }
        }
        return false;
    }
}
