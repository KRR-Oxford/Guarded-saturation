package uk.ac.ox.cs.gsat.api.io;

import java.io.IOException;
import java.util.Collection;

import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Atom;

/**
 * Serialize set of {@link TGD} and {@link Atom} to a file.
 */
public interface Serializer extends AutoCloseable {

    public void open(String filePath) throws IOException;

    public void writeTGDs(Collection<? extends TGD> tgds) throws IOException;

    public void writeAtoms(Collection<Atom> atoms) throws IOException;
}
