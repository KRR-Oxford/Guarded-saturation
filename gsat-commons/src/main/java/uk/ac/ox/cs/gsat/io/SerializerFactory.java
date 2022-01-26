package uk.ac.ox.cs.gsat.io;

import org.apache.commons.lang3.NotImplementedException;

import uk.ac.ox.cs.gsat.TGDFileFormat;
import uk.ac.ox.cs.gsat.api.io.Serializer;

public class SerializerFactory {

    protected final static SerializerFactory INSTANCE = new SerializerFactory();

    private SerializerFactory() {
    }

    public static SerializerFactory instance() {
        return INSTANCE;
    }
    
    public Serializer create(TGDFileFormat format) {
        switch (format) {
        case DLGP:
            return new DLGPSerializer();
        case OWL:
        default:
            String message = String.format("Unsupported serializable format: %s", format);
            throw new NotImplementedException(message);
        }
    }
}
