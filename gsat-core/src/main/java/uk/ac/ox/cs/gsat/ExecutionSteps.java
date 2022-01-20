package uk.ac.ox.cs.gsat;

import java.io.IOException;
import java.util.Collection;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;

public interface ExecutionSteps {

    Collection<Dependency> getRules() throws Exception;

    void writeData(String path) throws IOException;

    Collection<Atom> getQueries();

    String getBaseOutputPath();

}