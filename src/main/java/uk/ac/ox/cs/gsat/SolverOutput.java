package uk.ac.ox.cs.gsat;

/**
 * From EmbASP code
 */
public class SolverOutput implements Cloneable {
    /** Variable in which results are stored */
    protected String output;
    /** The errors thrown by the solver */
    protected String errors;

    public SolverOutput() {
        output = new String();
    }

    public SolverOutput(final String initial_output) {
        output = initial_output;
    }

    public SolverOutput(final String out, final String err) {
        output = out;
        errors = err;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public String getErrors() {
        return errors;
    }

    public String getOutput() {
        return output;
    }

    protected void parse() {
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