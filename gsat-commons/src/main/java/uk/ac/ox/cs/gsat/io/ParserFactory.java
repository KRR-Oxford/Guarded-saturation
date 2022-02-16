package uk.ac.ox.cs.gsat.io;

import org.apache.commons.lang3.NotImplementedException;

import uk.ac.ox.cs.gsat.TGDFileFormat;
import uk.ac.ox.cs.gsat.api.io.Parser;

public class ParserFactory {

    protected final static ParserFactory INSTANCE = new ParserFactory();

    private ParserFactory() {
    }

    public static ParserFactory instance() {
        return INSTANCE;
    }
    
    public Parser create(TGDFileFormat format, boolean skipFacts, boolean includeNegativeConstraints) {
        switch (format) {
        case DLGP:
            return new DLGPParser(skipFacts, includeNegativeConstraints);
        case OWL:
            return new OWLParser(skipFacts, includeNegativeConstraints);
        default:
            String message = String.format("Unsupported format to parse: %s", format);
            throw new NotImplementedException(message);
        }
    }
}
