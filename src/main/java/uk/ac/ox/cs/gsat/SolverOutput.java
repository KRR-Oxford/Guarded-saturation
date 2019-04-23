package uk.ac.ox.cs.gsat;

/**
 * From EmbASP code
 */
public class SolverOutput {

    /** Variable in which results are stored */
    protected String output;
    /** The errors thrown by the solver */
    protected String errors;

    public SolverOutput() {
        output = "";
        errors = "";
    }

    public SolverOutput(final String initial_output) {
        output = initial_output;
        errors = "";
    }

    public SolverOutput(final String out, final String err) {
        output = out;
        errors = err;
    }

    public int getNumberOfLinesOutput() {
        return output.split("\r\n|\r|\n").length;
    }

    public String getErrors() {
        return errors;
    }

    public String getOutput() {
        return output;
    }

    public void setErrors(final String err) {
        errors = err;
    }

    public void setOutput(final String output) {
        this.output = output;
    }

    @Override
    public String toString() {
        return "Output:\n" + output + "\n" + errors;
    }

}