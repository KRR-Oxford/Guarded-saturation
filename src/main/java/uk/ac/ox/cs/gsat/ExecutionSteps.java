package uk.ac.ox.cs.gsat;

import java.io.IOException;
import java.util.Collection;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;

public interface ExecutionSteps {

    public Collection<Dependency> getRules() throws Exception;

    public void writeData(String path) throws IOException;

    public Collection<Atom> getQueries();

    public String getBaseOutputPath();

}