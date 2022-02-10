package uk.ac.ox.cs.gsat.kaon2;

import java.util.Collection;

import org.semanticweb.kaon2.saturation.InferenceRule;
import org.semanticweb.kaon2.saturation.SClause;
import org.semanticweb.kaon2.saturation.SLiteral;
import org.semanticweb.kaon2.saturation.SelectionFunction.SelectedLiterals;
import org.semanticweb.kaon2.saturation.SimplificationRule;
import org.semanticweb.kaon2.saturation.TheoremProver;
import org.semanticweb.kaon2.saturation.TheoremProverMonitor;

import uk.ac.ox.cs.gsat.api.SaturationStatColumns;
import uk.ac.ox.cs.gsat.statistics.StatisticsCollector;

public class KAON2Statistics implements TheoremProverMonitor {

    private StatisticsCollector<SaturationStatColumns> statisticsCollector;
    private String processName;

    public KAON2Statistics(String processName, StatisticsCollector<SaturationStatColumns> statisticsCollector) {
        this.processName = processName;
        this.statisticsCollector = statisticsCollector;
    }

    @Override
    public void backwardSubsumedClauses(TheoremProver arg0, SClause arg1, Collection<SClause> arg2) {
        statisticsCollector.incr(processName, SaturationStatColumns.SUBSUMED);
    }

    @Override
    public void clauseWorkedOff(TheoremProver prover, SClause givenClause) {
    }

    @Override
    public void givenClauseChosen(TheoremProver prover,SClause givenClause) {
        for (int pos = 0; pos < givenClause.getNumberOfLiterals(); pos++) {
            SLiteral literal = givenClause.getLiteral(0);
            if (literal.isPositive()
                    && (literal.getFirstTerm().getDepth() > 0 || literal.getSecondTerm().getDepth() > 0)) {
                statisticsCollector.incr(processName, SaturationStatColumns.NEW_NFTGD_NB);
                return;
            }
        }
        statisticsCollector.incr(processName, SaturationStatColumns.NEW_FTGD_NB);
    }

    @Override
    public void givenClauseSimplified(TheoremProver arg0, SClause arg1, SClause arg2, SimplificationRule arg3) {
        // forward subsumed clause
        statisticsCollector.incr(processName, SaturationStatColumns.SUBSUMED);
    }

    @Override
    public void processConclusion(TheoremProver arg0, SClause arg1, SClause arg2, boolean arg3, InferenceRule arg4) {
    }

    @Override
    public void selectedLiteralsChosen(TheoremProver arg0, SClause arg1, SelectedLiterals arg2) {
    }

    @Override
    public void theoremProvingFinished(TheoremProver arg0) {
    }

    @Override
    public void theoremProvingStarted(TheoremProver arg0) {
    }

    

    
    // public void print() {
    //     System.out.println("Derived full/non full TGDs: " + derivedFullTGDNumber + " , " + derivedNonFullTGDNumber);
    //     System.out.println("Subsumed elements : " + subsumedClausesNumber);
    // }

    // protected void println(SClause clause, TermFactory termFactory) {
    //     print(clause, termFactory);
    //     m_out.println();
    // }

    // protected void print(SClause clause, TermFactory termFactory) {
    //     print(m_out, clause, termFactory);
    // }

    // public static void print(PrintWriter out, SClause clause, TermFactory termFactory) {
    //     StringBuffer buffer = new StringBuffer();
    //     toString(buffer, namespace, clause, termFactory);
    //     out.print(buffer.toString());
    // }

    // public static void toString(StringBuffer buffer, Namespaces namespaces, SClause clause, TermFactory termFactory) {
    //     if (clause.getNumberOfLiterals() == 0)
    //         buffer.append("[ ]");
    //     else {
    //         for (int i = 0; i < clause.getNumberOfLiterals(); i++) {
    //             if (i != 0)
    //                 buffer.append(" v ");
    //             toString(buffer, namespaces, clause.getLiteral(i), termFactory);
    //         }
    //     }
    // }

    // public static void toString(StringBuffer buffer, Namespaces namespaces, SLiteral literal, TermFactory termFactory) {
    //     if (literal.isEqualityEncodedPredicate()) {
    //         if (!literal.isPositive())
    //             buffer.append('!');
    //         toString(buffer, namespaces, literal.getFirstTerm(), termFactory, false);
    //     } else {
    //         toString(buffer, namespaces, literal.getFirstTerm(), termFactory, false);
    //         if (literal.isPositive())
    //             buffer.append(" == ");
    //         else
    //             buffer.append(" != ");
    //         toString(buffer, namespaces, literal.getSecondTerm(), termFactory, false);
    //     }
    // }

    // public static void toString(StringBuffer buffer, Namespaces namespaces, STerm term, TermFactory termFactory,
    //         boolean atSubstitutionPosition) {
    //     if (term instanceof SVariable) {
    //         int variableID = ((SVariable) term).getVariableID();
    //         if (variableID == 0)
    //             buffer.append('X');
    //         else if (variableID == 1)
    //             buffer.append('Y');
    //         else {
    //             buffer.append('Y');
    //             buffer.append(variableID);
    //         }
    //     } else {
    //         if (!atSubstitutionPosition && term.isSubstitutionPosition())
    //             buffer.append('[');
    //         String symbol = termFactory.getSymbolForID(term.getPrecedenceIndex()).toString();
    //         String encodedSymbol = namespaces.abbreviateAsNamespace(symbol);
    //         buffer.append(encodedSymbol);
    //         if (term.getArity() > 0) {
    //             buffer.append('(');
    //             for (int i = 0; i < term.getArity(); i++) {
    //                 if (i != 0)
    //                     buffer.append(',');
    //                 toString(buffer, namespaces, term.getSubTerm(i), termFactory, term.isSubstitutionPosition());
    //             }
    //             buffer.append(')');
    //         }
    //         if (!atSubstitutionPosition && term.isSubstitutionPosition())
    //             buffer.append(']');
    //     }
    // }
}
