package uk.ac.ox.cs.gsat.mat;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

import tech.oxfordsemantic.jrdfox.logic.datalog.BodyFormula;
import tech.oxfordsemantic.jrdfox.logic.datalog.Rule;
import tech.oxfordsemantic.jrdfox.logic.datalog.TupleTableAtom;
import tech.oxfordsemantic.jrdfox.logic.expression.IRI;
import tech.oxfordsemantic.jrdfox.logic.expression.Term;
import tech.oxfordsemantic.jrdfox.logic.expression.Variable;
import tech.oxfordsemantic.jrdfox.logic.sparql.pattern.TriplePattern;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.TGD;

public class RDFoxFactory {

    /**
     * translate a full TDG into a set of datalog rules:
     * one rule for each TGD head atom.
     */
    public static List<Rule> createDatalogRule(TGD fullTgd) {
        List<Rule> result = new ArrayList<>();

        Atom[] bodyAtoms = fullTgd.getBodyAtoms();
        for (Atom headAtom : fullTgd.getHeadAtoms()) {
            result.add(fullTDGAsDatalog(headAtom, bodyAtoms));
        }

        return result;
    }

    /**
     * create a datalog rule from PDQ atoms
     */
    protected static Rule fullTDGAsDatalog(Atom headAtom, Atom[] bodyAtoms) {
        List<BodyFormula> body = new ArrayList<>();
        for (Atom batom : bodyAtoms) {
            body.add(pdqAtomAsRDFoxAtom(batom));
        }

        return Rule.create(pdqAtomAsRDFoxAtom(headAtom), body);
    }


    /**
     * translate a binary or unary atom from PDQ as a atom in RDFox
     */
    protected static TupleTableAtom pdqAtomAsRDFoxAtom(Atom atom) {

        Predicate predicate = atom.getPredicate();
        if (predicate.getArity() == 1) {
            return TupleTableAtom.rdf(pdqTermAsRDFoxTerm(atom.getTerm(0)), IRI.RDF_TYPE, predicateAsIRI(predicate));
        } else if (predicate.getArity() == 2) {
            return TupleTableAtom.rdf(pdqTermAsRDFoxTerm(atom.getTerm(0)), predicateAsIRI(predicate), pdqTermAsRDFoxTerm(atom.getTerm(1)));
        } else {
            String message = String.format("The atom %s is neither unary nor binary", atom);
            throw new IllegalStateException(message);
        }
    }

    /**
     * translate a binary or unary atom from PDQ as a triple in RDFox
     */
    protected static TriplePattern pdqAtomAsRDFoxTriple(Atom atom) {

        Predicate predicate = atom.getPredicate();
        if (predicate.getArity() == 1) {
            return TriplePattern.create(pdqTermAsRDFoxTerm(atom.getTerm(0)), IRI.RDF_TYPE, predicateAsIRI(predicate));
        } else if (predicate.getArity() == 2) {
            return TriplePattern.create(pdqTermAsRDFoxTerm(atom.getTerm(0)), predicateAsIRI(predicate), pdqTermAsRDFoxTerm(atom.getTerm(1)));
        } else {
            String message = String.format("The atom %s is neither unary nor binary", atom);
            throw new IllegalStateException(message);
        }
    }
    
    protected static IRI predicateAsIRI(Predicate predicate) {
        return IRI.create(predicate.getName());
    }

    protected static Term pdqTermAsRDFoxTerm(uk.ac.ox.cs.pdq.fol.Term term) {
        if (term.isVariable()) {
            return Variable.create(((uk.ac.ox.cs.pdq.fol.Variable) term).getSymbol());
        } else if (term.isUntypedConstant()) {
            return IRI.create(term.toString());
        } else {
            String message = String.format("The term %s seems to be neither a variable nor a constant, so it can not be translated as a RDFox object", term);
            throw new IllegalStateException(message);
        }
    }
}
