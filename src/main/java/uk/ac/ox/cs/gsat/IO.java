package uk.ac.ox.cs.gsat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.semanticweb.kaon2.api.logic.Literal;
import org.semanticweb.kaon2.api.logic.Rule;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.fol.Constant;
import uk.ac.ox.cs.pdq.fol.Formula;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.UntypedConstant;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * Helper functions for I/O operations
 * 
 * @author Stefano
 */
public class IO {

    public static void writeDatalogRules(Collection<? extends TGD> guardedSaturation, String path) throws IOException {

        Collection<String> datalogRules = new LinkedList<>();
        guardedSaturation.forEach((tgd) -> datalogRules.addAll(getDatalogRules(tgd)));

        Files.write(Paths.get(path), datalogRules, StandardCharsets.UTF_8);

    }

    public static Collection<? extends String> getDatalogRules(TGD tgd) {

        if (!Logic.isFull(tgd))
            throw new IllegalArgumentException("TGD not full while writing to Datalog");

        StringBuilder body = new StringBuilder();
        String to_append = ":-";
        for (Atom atom : tgd.getBodyAtoms()) {
            body.append(to_append);
            if (to_append.equals(":-"))
                to_append = ",";
            App.logger.fine("Atom:" + renameVariablesAndConstantsDatalog(atom));
            body.append(renameVariablesAndConstantsDatalog(atom).toString());
        }
        body.append(".");

        String bodyString = body.toString();

        Collection<String> rules = new LinkedList<>();

        if (tgd.getHeadAtoms().length == 1 && tgd.getHeadAtoms()[0].equals(TGDGSat.Bottom))
            rules.add(bodyString); // Negative Constraint
        else
            // if multiple atoms in the head, we have to return multiple rules
            for (Atom atom : tgd.getHeadAtoms()) {
                App.logger.fine("Atom:" + renameVariablesAndConstantsDatalog(atom));
                rules.add(renameVariablesAndConstantsDatalog(atom).toString() + bodyString);
            }

        return rules;

    }

    public static Collection<? extends String> getDatalogRules(Rule rule) {

        Map<Term, Term> substitution = new HashMap<>();
        int counter = 1;
        for (org.semanticweb.kaon2.api.logic.Variable variable : rule.getBoundVariables()) {
            substitution.put(Variable.create(variable.getVariableName()),
                    Variable.create(GSat.getInstance().uVariable + counter++));

        }

        StringBuilder body = new StringBuilder();
        String to_append = ":-";
        for (Literal literal : rule.getBodyLiterals()) {
            body.append(to_append);
            if (to_append.equals(":-"))
                to_append = ",";
            Atom atom = getPDQAtomFromKAON2Literal(literal);
            Atom renameVariablesAndConstantsDatalog = renameVariablesAndConstantsDatalog(
                    (Atom) Logic.applySubstitution(atom, substitution));
            App.logger.fine("Atom:" + renameVariablesAndConstantsDatalog);
            body.append(renameVariablesAndConstantsDatalog.toString());
        }
        body.append(".");

        String bodyString = body.toString();

        Collection<String> rules = new LinkedList<>();

        if (rule.getHeadLiterals().length == 0)
            rules.add(bodyString); // Negative Constraint
        else
            // if multiple atoms in the head, we have to return multiple rules
            for (Literal literal : rule.getHeadLiterals()) {
                Atom atom = getPDQAtomFromKAON2Literal(literal);
                Atom renameVariablesAndConstantsDatalog = renameVariablesAndConstantsDatalog(
                        (Atom) Logic.applySubstitution(atom, substitution));
                App.logger.fine("Atom:" + renameVariablesAndConstantsDatalog);
                rules.add(renameVariablesAndConstantsDatalog.toString() + bodyString);
            }

        return rules;

    }

    private static Atom getPDQAtomFromKAON2Literal(Literal literal) {
        org.semanticweb.kaon2.api.logic.Term[] arguments = literal.getArguments();
        Term[] nterms = new Term[arguments.length];
        for (int termIndex = 0; termIndex < arguments.length; ++termIndex) {
            if (arguments[termIndex] instanceof org.semanticweb.kaon2.api.logic.Variable)
                nterms[termIndex] = Variable
                        .create(((org.semanticweb.kaon2.api.logic.Variable) (arguments[termIndex])).getVariableName());
            else if (arguments[termIndex] instanceof org.semanticweb.kaon2.api.logic.Constant)
                nterms[termIndex] = UntypedConstant.create(
                        ((org.semanticweb.kaon2.api.logic.Constant) (arguments[termIndex])).getValue().toString());
        }

        Atom atom = Atom.create(Predicate.create(literal.getPredicate().toString(), literal.getArity()), nterms);
        return atom;
    }

    public static Atom renameVariablesAndConstantsDatalog(Atom atom) {
        // App.logger.info(atom);
        // App.logger.info(atom.getTypedAndUntypedConstants());

        Map<Term, Term> substitution = new HashMap<>();
        for (Variable v : atom.getVariables())
            substitution.put(v, Variable.create(v.getSymbol().toUpperCase()));
        for (Constant c : atom.getTypedAndUntypedConstants())
            if (!c.toString().startsWith("\"")) // we assume that it is a quoted string
                substitution.put(c, UntypedConstant.create('"' + c.toString() + '"'));

        // App.logger.info(substitution);

        Atom newAtom = (Atom) Logic.applySubstitution(atom, substitution);

        String name = newAtom.getPredicate().getName();
        if (name != null) {
            if (name.length() > 6
                    && (name.substring(0, 7).equals("http://") || name.substring(0, 7).equals("file://"))) {
                // URL in angle bracket
                App.logger.info("URL as predicate name. Adding angle brackets." + name);
                name = '<' + name + '>';
            } else if (name.length() > 0 && name.substring(0, 1).matches("[A-Z]")) { // First char to Lower Case
                App.logger.info("Predicate starting with an upper-case letter. Transforming it to lower-case.");
                name = name.substring(0, 1).toLowerCase() + name.substring(1);
            }
            // // URL in angle bracket
            // if (name.length() > 6 && name.substring(0, 7).equals("http://")) {
            // App.logger.info("URL as predicate name. Adding angle brackets.");
            // // return Atom.create(Predicate.create('<' + name + '>',
            // predicate.getArity()),
            // // newAtom.getTerms());
            // return Atom.create(
            // Predicate.create(Base64.getUrlEncoder().withoutPadding().encodeToString(name.getBytes()),
            // predicate.getArity()),
            // newAtom.getTerms());
            // }
            // // remove unwanted chars
            // if (!name.matches("[a-z]([A-Z_])+")) {
            // App.logger.warn("Not a valid predicate name. " + name);
            // return Atom.create(Predicate.create(name.replaceAll("", ""),
            // predicate.getArity()),
            // newAtom.getTerms());
            // }
            // // remove unwanted chars // FIXME
            // if (!name.matches("[a-z]([A-Z_])+")) {
            // App.logger.warning("Not a valid predicate name. " + name);
            // // return Atom.create(Predicate.create(name.replaceAll("", ""),
            // // predicate.getArity()),
            // // newAtom.getTerms());
            // }
            newAtom = Atom.create(Predicate.create(name, newAtom.getPredicate().getArity()), newAtom.getTerms());
        }
        return newAtom;

    }

    public static void writeDatalogFacts(Collection<Atom> facts, String path) throws IOException {

        Collection<String> datalogFacts = new LinkedList<>();

        for (Atom atom : facts)
            datalogFacts.add(renameVariablesAndConstantsDatalog(atom).toString() + '.');

        Files.write(Paths.get(path), datalogFacts, StandardCharsets.UTF_8);

    }

    public static void writeDatalogQueries(Collection<ConjunctiveQuery> queries, String path) throws IOException {

        Collection<String> datalogQueries = new LinkedList<>();

        for (ConjunctiveQuery query : queries)
            // System.out.println(query);
            datalogQueries.add(getDatalogQuery(query));

        Files.write(Paths.get(path), datalogQueries, StandardCharsets.UTF_8);

    }

    public static String getDatalogQuery(ConjunctiveQuery query) {

        StringBuilder querySB = new StringBuilder();
        String to_append = "";
        for (Formula f : query.getChildren()) {
            if (!(f instanceof Atom)) {
                App.logger.warning("We only accept atomic queries");
                return "";
            }
            querySB.append(to_append);
            if (to_append.equals(""))
                to_append = ",";
            querySB.append(renameVariablesAndConstantsDatalog((Atom) f).toString());
        }
        querySB.append(" ?");

        return querySB.toString();

    }

    public static void writeDatalogQuery(Atom query, String path) throws IOException {

        Collection<String> datalogQueries = new LinkedList<>();

        datalogQueries.add(renameVariablesAndConstantsDatalog((Atom) query).toString() + " ?");

        Files.write(Paths.get(path), datalogQueries, StandardCharsets.UTF_8);

    }

    public static void writeSolverOutput(SolverOutput solverOutput, String path) throws IOException {

        if (!Configuration.isSolverOutputToFile())
            return;

        Files.write(Paths.get(path), Arrays.asList(solverOutput.getOutput(), solverOutput.getErrors()),
                StandardCharsets.UTF_8);

    }

}