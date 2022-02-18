package uk.ac.ox.cs.gsat.api.io;

import java.util.Collection;

import uk.ac.ox.cs.gsat.fol.TGD;

/**
 * A TGD processor parses TGD files (see {@link Parser}) and apply a set of transformations to them such as filtering (see {@link TGDTransformation}).
 */
public interface TGDProcessor {

    /**
     * parse and process the TGDs stored in a file
     */
    public Collection<TGD> getProcessedTGDs(String path) throws Exception;

}
