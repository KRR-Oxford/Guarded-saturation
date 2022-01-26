package uk.ac.ox.cs.gsat;

import java.util.Collection;

import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.mat.SolverOutput;

public class ExecutionOutput {

    protected Collection<? extends TGD> fullTGDSaturation;

    protected SolverOutput solverOutput;

    public ExecutionOutput(Collection<TGD> fullTGDSaturation, SolverOutput solverOutput) {
        setFullTGDSaturation(fullTGDSaturation);
        setSolverOutput(solverOutput);
    }

    @Override
    public String toString() {
        return "ExecutionOutput {\nGuarded Saturation:\n" + fullTGDSaturation + "\nSolverOutput:\n" + solverOutput
                + "\n}\n";
    }

    public Collection<? extends TGD> getFullTGDSaturation() {
        return fullTGDSaturation;
    }

    public void setFullTGDSaturation(Collection<? extends TGD> fullTGDSaturation) {
        this.fullTGDSaturation = fullTGDSaturation;
    }

    public SolverOutput getSolverOutput() {
        return solverOutput;
    }

    public void setSolverOutput(SolverOutput solverOutput) {
        this.solverOutput = solverOutput;
    }

}
