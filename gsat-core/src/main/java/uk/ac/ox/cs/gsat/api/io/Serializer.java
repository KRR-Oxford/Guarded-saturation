package uk.ac.ox.cs.gsat.api.io;

import java.io.IOException;
import java.util.Collection;

import uk.ac.ox.cs.gsat.TGD;
import uk.ac.ox.cs.pdq.fol.Atom;

public interface Serializer extends AutoCloseable {

    public void open() throws IOException;

    public void writeTGDs(Collection<? extends TGD> tgds) throws IOException;

    public void writeAtoms(Collection<Atom> atoms);
}
