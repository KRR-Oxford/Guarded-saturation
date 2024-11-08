package uk.ac.ox.cs.gsat.kaon2;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.semanticweb.kaon2.api.logic.Literal;
import org.semanticweb.kaon2.api.logic.Rule;

import uk.ac.ox.cs.gsat.Log;
import uk.ac.ox.cs.gsat.fol.Logic;
import uk.ac.ox.cs.gsat.io.DatalogSerializer;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.UntypedConstant;
import uk.ac.ox.cs.pdq.fol.Variable;


/**
 * This class is not used anymore, it seems to translate KAON2 rule to Datalog string.
 * Maybe useful to implement {@link KAON2Convertor}
 */
public class Kaon2IO {

    private static final String VARIABLE_PREFIX = "KAON2";

    public static Collection<? extends String> getDatalogRules(Rule rule) {

            
        Map<Term, Term> substitution = new HashMap<>();
        int counter = 1;
        for (org.semanticweb.kaon2.api.logic.Variable variable : rule.getBoundVariables()) {
            substitution.put(Variable.create(variable.getVariableName()),
                    Variable.create(VARIABLE_PREFIX + counter++));

        }

        StringBuilder body = new StringBuilder();
        String to_append = ":-";
        for (int pos = 0; pos < rule.getBodyLength(); pos++) {
            Literal literal = rule.getBodyLiteral(pos);
            body.append(to_append);
            if (to_append.equals(":-"))
                to_append = ",";
            Atom atom = getPDQAtomFromKAON2Literal(literal);
            Atom renameVariablesAndConstantsDatalog = DatalogSerializer.renameVariablesAndConstantsDatalog(
                    (Atom) Logic.applySubstitution(atom, substitution));
            Log.GLOBAL.fine("Atom:" + renameVariablesAndConstantsDatalog);
            body.append(renameVariablesAndConstantsDatalog.toString());
        }
        body.append(".");

        String bodyString = body.toString();

        Collection<String> rules = new LinkedList<>();

        if (rule.getHeadLength() == 0)
            rules.add(bodyString); // Negative Constraint
        else
            // if multiple atoms in the head, we have to return multiple rules
            for (int pos = 0; pos < rule.getHeadLength(); pos++) {
                Literal literal = rule.getHeadLiteral(pos);
                Atom atom = getPDQAtomFromKAON2Literal(literal);
                Atom renameVariablesAndConstantsDatalog = DatalogSerializer.renameVariablesAndConstantsDatalog(
                        (Atom) Logic.applySubstitution(atom, substitution));
                Log.GLOBAL.fine("Atom:" + renameVariablesAndConstantsDatalog);
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

}
