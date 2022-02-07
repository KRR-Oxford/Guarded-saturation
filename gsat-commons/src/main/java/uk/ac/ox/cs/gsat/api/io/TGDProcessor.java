package uk.ac.ox.cs.gsat.api.io;

import java.util.Collection;

import uk.ac.ox.cs.gsat.fol.TGD;

/**
 * A TGD processor parses TGD files and apply a set of transformations to them such as filtering.
 */
public interface TGDProcessor {

    public Collection<TGD> getProcessedTGDs(String path) throws Exception;

}
