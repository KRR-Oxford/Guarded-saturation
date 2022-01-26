package uk.ac.ox.cs.gsat.kaon2;

import java.util.ArrayList;
import java.util.List;

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

public class RunKAON2FromClausesTest {

	public static void main(String[] args) throws Exception {
		TermFactory termFactory = new TermFactory(4);
		OWLSClauseManager owlClauseManager = new OWLSClauseManager();
        
		// Variables
		SVariable X = termFactory.getVariable(0);
		SVariable Y = termFactory.getVariable(1);
		// Terms
		int f = termFactory.getSymbolID(Sort.SORT_ABSTRACT_DOMAIN, 3, "f", 1);
		STerm f_X = termFactory.getFunctionalTerm(false, f, X);
		// Atoms
		STerm A_X = termFactory.getFunctionalTerm(Sort.SORT_PREDICATE, 1, "A", X);
		STerm B_Y = termFactory.getFunctionalTerm(Sort.SORT_PREDICATE, 1, "B", Y);
        STerm B_X = termFactory.getFunctionalTerm(Sort.SORT_PREDICATE, 1, "B", X);
        STerm B_f_X = termFactory.getFunctionalTerm(Sort.SORT_PREDICATE, 1, "B", f_X);
        STerm C_X = termFactory.getFunctionalTerm(Sort.SORT_PREDICATE, 1, "C", X);
		STerm R_X_Y = termFactory.getFunctionalTerm(Sort.SORT_PREDICATE, 1, "R", X, Y);
        STerm invR_Y_X = termFactory.getFunctionalTerm(Sort.SORT_PREDICATE, 1, "invR", Y, X);
		STerm R_X_f_X = termFactory.getFunctionalTerm(Sort.SORT_PREDICATE, 1, "R", X, f_X);
		// Clauses
		SClause A_X_to_R_X_f_X = owlClauseManager.createType3SClause(SClause.NO_PREMISES, literals(termFactory.getLiteral(true, R_X_f_X), termFactory.getLiteral(false, A_X)), false);
        SClause A_X_to_B_f_X = owlClauseManager.createType4SClause(SClause.NO_PREMISES, literals(termFactory.getLiteral(true, B_f_X), termFactory.getLiteral(false, A_X)), false);


        // why is this not working ?
        // try to obtain the same thing with the correct position for X and Y by introducing an inverse predicate and rule
        SClause R_X_Y_to_invR_Y_X = owlClauseManager.createType1SClause(SClause.NO_PREMISES, termFactory.getLiteral(false, R_X_Y), termFactory.getLiteral(true, invR_Y_X), false);
        SClause invR_Y_X_and_B_Y_to_C_X = owlClauseManager.createType7SClause(SClause.NO_PREMISES, literals(termFactory.getLiteral(true, C_X), termFactory.getLiteral(false, invR_Y_X), termFactory.getLiteral(false, B_Y)), invR_Y_X.getPrecedenceIndex(), literals(termFactory.getLiteral(false, B_Y)), literals(termFactory.getLiteral(true, C_X)),  1, false);
        run(termFactory, owlClauseManager, A_X_to_R_X_f_X, A_X_to_B_f_X, R_X_Y_to_invR_Y_X, invR_Y_X_and_B_Y_to_C_X);
        
        // probably wrong because the position of the X and Y do not correspond to the type 7 definition (do not work)
        // SClause R_X_Y_and_B_Y_to_C_X = owlClauseManager.createType7SClause(SClause.NO_PREMISES, literals(termFactory.getLiteral(true, C_X), termFactory.getLiteral(false, R_X_Y), termFactory.getLiteral(false, B_Y)), R_X_Y.getPrecedenceIndex(), literals(termFactory.getLiteral(true, C_X)), literals(termFactory.getLiteral(false, B_Y)), 1, false);

        // run(termFactory, owlClauseManager, A_X_to_R_X_f_X, A_X_to_B_f_X, R_X_Y_and_B_Y_to_C_X);


        // try to do a simplier variation with a correct position for X and Y to check the others clauses
        // SClause R_X_Y_and_B_X_to_C_X = owlClauseManager.createType7SClause(SClause.NO_PREMISES, literals(termFactory.getLiteral(true, C_X), termFactory.getLiteral(false, R_X_Y), termFactory.getLiteral(false, B_X)), R_X_Y.getPrecedenceIndex(), literals(termFactory.getLiteral(true, C_X), termFactory.getLiteral(false, B_X)), literals(), 1, false);
        // run(termFactory, owlClauseManager, A_X_to_R_X_f_X, A_X_to_B_f_X, R_X_Y_and_B_X_to_C_X);

	}


    protected static void run(TermFactory termFactory, OWLSClauseManager owlClauseManager, SClause... clauses) throws InterruptedException, EmptyClauseException {
        InterruptFlag interruptFlag = new InterruptFlag();
		boolean useContextualLiteralCutting = false;
		SelectionFunction selectionFunction = LargerPrecedenceSelectionFunction.INSTANCE;
		TheoremProverMonitor theoremProverMonitor = null;

        UnprocessedClauses unprocessedClauses = new SimpleUnprocessedClauses();
        System.out.println("==================\n");
        System.out.println("Input:");
        for (SClause clause : clauses) {
            System.out.println(clause);
            unprocessedClauses.addClause(clause);
        }
        System.out.println("");
        
        // Set up the rest of the theorem prover
		OWLWorkedOffClauses workedOffTBoxRBoxClauses = new OWLWorkedOffClauses(termFactory, owlClauseManager);
		TheoremProver theoremProver = new TheoremProver(interruptFlag, termFactory, OWLWorkedOffClauses.getOWLInferenceRules(false), OWLWorkedOffClauses.getOWLSimplificationRules(useContextualLiteralCutting), selectionFunction, unprocessedClauses, workedOffTBoxRBoxClauses, theoremProverMonitor);
		theoremProver.saturate();
		if (workedOffTBoxRBoxClauses.containsEmptyClause())
			throw new EmptyClauseException();
		ReductionToDatalog reduction = new ReductionToDatalog(interruptFlag, termFactory, false);
		reduction.addClauses(workedOffTBoxRBoxClauses);
		List<Rule> rules = reduction.getRules();

        System.out.println("Output: ");
		for (Rule rule : rules)
			System.out.println(rule.toString());

        System.out.println("\n==================\n");
    }
    
	protected static List<SLiteral> literals(SLiteral... literals) {
		 List<SLiteral> result = new ArrayList<SLiteral>();
		 for (SLiteral literal :  literals)
			 result.add(literal);
         OrientedSLiteralComparator.sortLiterals(literals);
         return result;
	}
}
