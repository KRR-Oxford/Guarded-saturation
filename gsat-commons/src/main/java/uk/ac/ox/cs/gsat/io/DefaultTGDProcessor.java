package uk.ac.ox.cs.gsat.io;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import uk.ac.ox.cs.gsat.TGDFileFormat;
import uk.ac.ox.cs.gsat.api.io.Parser;
import uk.ac.ox.cs.gsat.api.io.TGDTransformation;
import uk.ac.ox.cs.gsat.api.io.TGDProcessor;
import uk.ac.ox.cs.gsat.fol.TGD;

public class DefaultTGDProcessor implements TGDProcessor {

    protected final List<TGDTransformation<TGD>> transformations;
    private final boolean includeNegativeConstraints;
    private final boolean skipFacts;

    public DefaultTGDProcessor(List<TGDTransformation<TGD>> transformations, boolean skipFacts, boolean includeNegativeConstraints) {
        this.transformations = transformations;
        this.skipFacts = skipFacts;
        this.includeNegativeConstraints = includeNegativeConstraints;
    }

    @Override
    public Collection<TGD> getProcessedTGDs(String path) throws Exception {

        // create the parser for the input file
        TGDFileFormat inputFormat = TGDFileFormat.getFormatFromPath(path);

        if (inputFormat == null) {
            String message = String.format(
                                           "The input file format should be of one of %s and should use one of these extensions %s",
                                           Arrays.asList(TGDFileFormat.values()), TGDFileFormat.getExtensions());
            throw new IllegalArgumentException(message);
        }
        Parser parser = ParserFactory.instance().create(inputFormat, skipFacts, includeNegativeConstraints);

        // parse the file 
        Collection<TGD> result = parser.parse(path).getTGDs();

        // apply the transformations
        for(TGDTransformation<TGD> transformation : transformations) {
            result = transformation.apply(result);
        }

        return result;
    }

}
