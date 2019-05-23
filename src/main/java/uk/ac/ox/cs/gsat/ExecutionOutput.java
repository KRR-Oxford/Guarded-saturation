package uk.ac.ox.cs.gsat;

import java.util.Collection;

public class ExecutionOutput {

    protected Collection<TGDGSat> guardedSaturation;

    protected SolverOutput solverOutput;

    public ExecutionOutput(Collection<TGDGSat> guardedSaturation, SolverOutput solverOutput) {
        setGuardedSaturation(guardedSaturation);
        setSolverOutput(solverOutput);
    }

    @Override
    public String toString() {
        return "ExecutionOutput {\nGuarded Saturation:\n" + guardedSaturation + "\nSolverOutput:\n" + solverOutput
                + "\n}\n";
    }

    public Collection<TGDGSat> getGuardedSaturation() {
        return guardedSaturation;
    }

    public void setGuardedSaturation(Collection<TGDGSat> guardedSaturation) {
        this.guardedSaturation = guardedSaturation;
    }

    public SolverOutput getSolverOutput() {
        return solverOutput;
    }

    public void setSolverOutput(SolverOutput solverOutput) {
        this.solverOutput = solverOutput;
    }

}