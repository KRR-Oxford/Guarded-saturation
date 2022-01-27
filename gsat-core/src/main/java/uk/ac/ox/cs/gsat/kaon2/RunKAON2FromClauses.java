package uk.ac.ox.cs.gsat.kaon2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.semanticweb.kaon2.api.logic.Rule;
import org.semanticweb.kaon2.datalog.EmptyClauseException;
import org.semanticweb.kaon2.reasoner.ReductionToDatalog;
import org.semanticweb.kaon2.saturation.OrientedSLiteralComparator;
import org.semanticweb.kaon2.saturation.SClause;
import org.semanticweb.kaon2.saturation.SLiteral;
import org.semanticweb.kaon2.saturation.STerm;
import org.semanticweb.kaon2.saturation.SVariable;
import org.semanticweb.kaon2.saturation.SelectionFunction;
import org.semanticweb.kaon2.saturation.SimpleUnprocessedClauses;
import org.semanticweb.kaon2.saturation.Sort;
import org.semanticweb.kaon2.saturation.TermFactory;
import org.semanticweb.kaon2.saturation.TheoremProver;
import org.semanticweb.kaon2.saturation.TheoremProverMonitor;
import org.semanticweb.kaon2.saturation.UnprocessedClauses;
import org.semanticweb.kaon2.saturation.owl.LargerPrecedenceSelectionFunction;
import org.semanticweb.kaon2.saturation.owl.OWLSClauseManager;
import org.semanticweb.kaon2.saturation.owl.OWLWorkedOffClauses;
import org.semanticweb.kaon2.util.InterruptFlag;

import uk.ac.ox.cs.gsat.fol.SkGTGD;
import uk.ac.ox.cs.gsat.fol.TGDFactory;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.FunctionTerm;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

public class RunKAON2FromClauses {

    private static int SkolemFunctionIndex = 0;
    
	public static void main(String[] args) throws Exception {

        // Terms
        Variable x = Variable.create("x");
        Variable y = Variable.create("y");

        // Atoms
        Atom A_x = Atom.create(Predicate.create("A", 1), x);
        Atom B_x = Atom.create(Predicate.create("B", 1), x);
        Atom B_y = Atom.create(Predicate.create("B", 1), y);
        Atom R_x_y = Atom.create(Predicate.create("R", 2), x, y);
        Atom invR_y_x = Atom.create(Predicate.create("R'", 2), y, x);
        Atom C_x = Atom.create(Predicate.create("C", 1), x);
        
        // a TGD input
        TGDFactory<SkGTGD> factory = TGDFactory.getSkGTGDInstance(true);
        List<SkGTGD> tgds = new ArrayList<>();
        tgds.addAll(factory.computeSingleHeadSkolemizedOnFrontierVariable(SkGTGD.create(Set.of( A_x ), Set.of( R_x_y, B_y ))));
        tgds.addAll(factory.computeSingleHeadSkolemizedOnFrontierVariable(SkGTGD.create(Set.of( R_x_y, B_y ), Set.of( C_x))));
        // tgds.addAll(factory.computeSingleHeadSkolemizedOnFrontierVariable(new SkGTGD(Set.of( R_x_y ), Set.of(B_y))));
        // tgds.addAll(factory.computeSingleHeadSkolemizedOnFrontierVariable(new SkGTGD(Set.of( invR_y_x, B_y ), Set.of( C_x))));
        // tgds.addAll(factory.computeSingleHeadSkolemizedOnFrontierVariable(new SkGTGD(Set.of( R_x_y ), Set.of( invR_y_x ))));
        
		InterruptFlag interruptFlag = new InterruptFlag();
		boolean useContextualLiteralCutting = false;
		SelectionFunction selectionFunction = LargerPrecedenceSelectionFunction.INSTANCE;
		TheoremProverMonitor theoremProverMonitor = null;
		TermFactory termFactory = new TermFactory(4);
		OWLSClauseManager owlClauseManager = new OWLSClauseManager();
		UnprocessedClauses unprocessedClauses = new SimpleUnprocessedClauses();
		// Variables
		SVariable X = termFactory.getVariable(0);
		SVariable Y = termFactory.getVariable(1);
		// Terms
		int f = termFactory.getSymbolID(Sort.SORT_ABSTRACT_DOMAIN, 3, "f", 1);
		STerm f_X = termFactory.getFunctionalTerm(false, f, X);
		// Atoms
		STerm A_X = termFactory.getFunctionalTerm(Sort.SORT_PREDICATE, 0, "A", X);
		STerm B_X = termFactory.getFunctionalTerm(Sort.SORT_PREDICATE, 0, "B", X);
		STerm R_X_Y = termFactory.getFunctionalTerm(Sort.SORT_PREDICATE, 0, "R", X, Y);
		STerm R_X_f_X = termFactory.getFunctionalTerm(Sort.SORT_PREDICATE, 0, "R", X, f_X);
		// Clauses
		// SClause A_X_to_R_X_f_X_old = owlClauseManager.createType3SClause(SClause.NO_PREMISES, literals(termFactory.getLiteral(true, R_X_f_X), termFactory.getLiteral(false, A_X)), false);
        // System.out.println(A_X_to_R_X_f_X_old);
        // SClause A_X_to_R_X_f_X = sclauseFromTGD(termFactory, owlClauseManager, tgds.get(0));
        // System.out.println(A_X_to_R_X_f_X);

		// // SClause R_X_Y_to_B_X_old = owlClauseManager.createType7SClause(SClause.NO_PREMISES, literals(termFactory.getLiteral(true, B_X), termFactory.getLiteral(false, R_X_Y)), R_X_Y.getPrecedenceIndex(), literals(termFactory.getLiteral(true, B_X)), literals(), 1, false);
        // // System.out.println(R_X_Y_to_B_X_old);
        // SClause R_X_Y_to_B_Y = sclauseFromTGD(termFactory, owlClauseManager, tgds.get(1));
        // System.out.println(R_X_Y_to_B_Y);

        // SClause R_X_Y_and_B_Y_to_C_X = sclauseFromTGD(termFactory, owlClauseManager, tgds.get(2));
        // System.out.println(R_X_Y_and_B_Y_to_C_X);

        // SClause R_X_Y_and_invR_X_Y = sclauseFromTGD(termFactory, owlClauseManager, tgds.get(3));
        // System.out.println(R_X_Y_and_invR_X_Y);

        for (SkGTGD tgd : tgds) {
            SClause clause = sclauseFromTGD(termFactory, owlClauseManager, tgd);
            System.out.println(tgd);
            System.out.println(clause);
            unprocessedClauses.addClause(clause);
        }

        // unprocessedClauses.addClause(A_X_to_R_X_f_X);
        // unprocessedClauses.addClause(R_X_Y_to_B_Y);
        // unprocessedClauses.addClause(R_X_Y_and_B_Y_to_C_X);
        // unprocessedClauses.addClause(R_X_Y_and_invR_X_Y);


        // Set up the rest of the theorem prover
		OWLWorkedOffClauses workedOffTBoxRBoxClauses = new OWLWorkedOffClauses(termFactory, owlClauseManager);
		TheoremProver theoremProver = new TheoremProver(interruptFlag, termFactory, OWLWorkedOffClauses.getOWLInferenceRules(false), OWLWorkedOffClauses.getOWLSimplificationRules(useContextualLiteralCutting), selectionFunction, unprocessedClauses, workedOffTBoxRBoxClauses, theoremProverMonitor);
		theoremProver.saturate();
		if (workedOffTBoxRBoxClauses.containsEmptyClause())
			throw new EmptyClauseException();
		ReductionToDatalog reduction = new ReductionToDatalog(interruptFlag, termFactory, false);
		reduction.addClauses(workedOffTBoxRBoxClauses);
        System.out.println("running ...");
		List<Rule> rules = reduction.getRules();
		for (Rule rule : rules)
			System.out.println(rule.toString());
	}
	
	protected static List<SLiteral> literals(SLiteral... literals) {
        List<SLiteral> result = new ArrayList<SLiteral>();
        for (SLiteral literal :  literals)
            result.add(literal);
        OrientedSLiteralComparator.sortLiterals(literals);
        return result;
	}

    protected static SClause sclauseFromTGD(TermFactory termFactory, OWLSClauseManager owlClauseManager, SkGTGD tgd) {
        Atom[] body = tgd.getBodyAtoms();
        Atom head = tgd.getHeadAtoms()[0];
        Predicate headPredicate = head.getPredicate();
        List<List<Atom>> aritySplittedBody = splitByArity(body);
        List<Atom> unaryBodyAtoms = aritySplittedBody.get(0);
        List<Atom> binaryBodyAtoms = aritySplittedBody.get(1);
        
        if (headPredicate.getArity() == 1) {
            // type 4 or 6, 7
            if (head.getTerm(0) instanceof FunctionTerm) {
                // type 4
                SLiteral[] literals = new SLiteral[body.length + 1];
                literals[0] = termFactory.getLiteral(true, stermFromAtom(termFactory, head));
                int pos = 1;
                for (Atom atom : body) {
                    literals[pos] = termFactory.getLiteral(false, stermFromAtom(termFactory, atom));
                    pos++;
                }

                SClause clause = owlClauseManager.createType4SClause(SClause.NO_PREMISES, literals(literals), false);
                return clause;
            } else {
                // type 6 or 7
                if (binaryBodyAtoms.isEmpty()) {
                    // type 6
                    throw new RuntimeException("not implemented");
                } else {
                    if (binaryBodyAtoms.size() == 1) {
                        // type 7
                        // TODO: check the variable ordering ...
                        Variable X = (Variable) binaryBodyAtoms.get(0).getTerms()[0];
                        Variable Y = (Variable) binaryBodyAtoms.get(0).getTerms()[1];
                        SLiteral[] literals = new SLiteral[body.length + 1];

                        literals[0] = termFactory.getLiteral(true, stermFromAtom(termFactory, head));
                        boolean isHeadInX = head.getTerm(0).equals(X);
                        int pos = 1;
                        for (Atom atom : unaryBodyAtoms) {
                            literals[pos] = termFactory.getLiteral(false, stermFromAtom(termFactory, atom));
                            pos++;
                        }
                        STerm R_X_Y = stermFromAtom(termFactory, binaryBodyAtoms.get(0));
                        literals[pos] = termFactory.getLiteral(false, R_X_Y);

                        SLiteral[] literalsInX = Arrays.copyOfRange(literals, (isHeadInX) ? 0 : 1,
                                1 + unaryBodyAtoms.size());
                        SLiteral[] literalsInY = (isHeadInX) ? new SLiteral[0] : new SLiteral[] { literals[0] };

                        System.out.println("literalsInX " + Arrays.toString(literalsInX));
                        System.out.println("literalsInY " + Arrays.toString(literalsInY));
                        
                        return owlClauseManager.createType7SClause(SClause.NO_PREMISES, literals(literals), R_X_Y.getPrecedenceIndex(), literals(literalsInX), literals(literalsInY), 1, false);
                    } else {
                        throw new RuntimeException("not implemented case with several binary atoms in the body");
                    }
                }
            }
        } else {
            if (binaryBodyAtoms.isEmpty()) {
                // type 3
                SLiteral[] literals = new SLiteral[body.length + 1];
                literals[0] = termFactory.getLiteral(true, stermFromAtom(termFactory, head));
                int pos = 1;
                for (Atom atom : body) {
                    literals[pos] = termFactory.getLiteral(false, stermFromAtom(termFactory, atom));
                    pos++;
                }

                return owlClauseManager.createType3SClause(SClause.NO_PREMISES, literals(literals), false);
            } else {
                // type 1 or 2
                if (binaryBodyAtoms.size() > 1)
                    throw new IllegalStateException("type 1 or 2 sould only have one atom in their body");
                if (head.getTerm(0).equals(binaryBodyAtoms.get(0).getTerm(0))) {
                    // type 2
                    throw new RuntimeException("not implement: type 2");
                } else {
                    // type 1
                    SLiteral negativeLiteral = termFactory.getLiteral(false, stermFromAtom(termFactory, binaryBodyAtoms.get(0)));
                    SLiteral positiveLiteral = termFactory.getLiteral(true, stermFromAtom(termFactory, head));
                    return owlClauseManager.createType1SClause(SClause.NO_PREMISES, negativeLiteral, positiveLiteral, false);
                }
            }
        }
    }

    protected static List<List<Atom>> splitByArity(Atom[] atoms) {
        List<List<Atom>> result = new ArrayList<>();
        List<Atom> unaryAtoms = new ArrayList<>();
        List<Atom> binaryAtoms = new ArrayList<>();
        result.add(unaryAtoms);
        result.add(binaryAtoms);
        
        for (Atom atom : atoms) {
            if (atom.getPredicate().getArity() == 1)
                unaryAtoms.add(atom);
            else if (atom.getPredicate().getArity() == 2)
                binaryAtoms.add(atom);
            else
                throw new IllegalStateException("predicates should unary or binary");
        }
        return result;    
    }

    // for unary atoms only
    protected static List<Atom> getAtomsContaining(List<Atom> atoms, Term term) {
        List<Atom> result = new ArrayList<>();

        for (Atom atom : atoms)
            if (atom.getTerm(0).equals(term))
                result.add(atom);

        return result;
    }
    
    protected static int getMaximalArity(Atom[] atoms) {
        int result = 0;
        for(Atom atom : atoms)
            result = Math.max(result, atom.getPredicate().getArity());
        return result;
    }
    
    protected static STerm stermFromAtom(TermFactory termFactory, Atom atom) {
        if (atom.getPredicate().getArity() == 1)
            return termFactory.getFunctionalTerm(Sort.SORT_PREDICATE, 0, atom.getPredicate(), stermFromTerm(termFactory, atom.getTerm(0)));
        else if (atom.getPredicate().getArity() == 2) {
            return termFactory.getFunctionalTerm(Sort.SORT_PREDICATE, 0, atom.getPredicate(), stermFromTerm(termFactory, atom.getTerm(0)), stermFromTerm(termFactory, atom.getTerm(1)));
        } else {
            throw new IllegalStateException();
        }
    }

    protected static STerm stermFromTerm(TermFactory termFactory, Term term) {
        if (term.isVariable())
            return termFactory.getVariable(term.hashCode());
        else if (term instanceof FunctionTerm) {
            FunctionTerm ft = (FunctionTerm) term;
            if (ft.getTerms().length == 1) {
                int f = termFactory.getSymbolID(Sort.SORT_ABSTRACT_DOMAIN, 3, ft.getFunction(), 1);
                STerm f_X = termFactory.getFunctionalTerm(false, f, stermFromTerm(termFactory, ft.getTerms()[0]));
                return f_X;
            } else
                throw new IllegalStateException("function terms should be unary");

        } else
            throw new IllegalStateException("constant are not supported in TGDs");
    }
}
