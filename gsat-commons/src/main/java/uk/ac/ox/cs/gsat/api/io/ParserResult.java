package uk.ac.ox.cs.gsat.api.io;

import java.util.Set;

import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Atom;

/**
 * Parser result represent the output of a {@link Parser}
 */
public interface ParserResult {

    public Set<TGD> getTGDs() throws Exception;

    public Set<Atom> getAtoms() throws Exception;

    public Set<Atom> getConjunctiveQueries() throws Exception;
}
