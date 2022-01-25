package uk.ac.ox.cs.gsat.io;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.lang3.NotImplementedException;

import fr.lirmm.graphik.graal.io.dlp.DlgpWriter;
import uk.ac.ox.cs.gsat.TGD;
import uk.ac.ox.cs.gsat.api.io.Serializer;
import uk.ac.ox.cs.pdq.fol.Atom;

public class DLGPSerializer implements Serializer {

    protected final String filePath;
    protected DlgpWriter writer;

    public DLGPSerializer(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void writeTGDs(Collection<TGD> tgds) throws IOException {
        for (TGD tgd : tgds) 
            this.writer.write(GraalFactory.createRule(tgd));
    }

    @Override
    public void writeAtoms(Collection<Atom> atoms) {
        throw new NotImplementedException("");
    }

    @Override
    public void close() throws Exception {
        this.writer.close();
    }

    @Override
    public void open() throws IOException {
        this.writer = new DlgpWriter(filePath);
    }
}
