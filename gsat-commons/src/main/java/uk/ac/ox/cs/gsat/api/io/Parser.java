package uk.ac.ox.cs.gsat.api.io;

import java.util.Set;

import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;

public interface Parser {

    public void parse(String file, boolean skipFacts, boolean includeNegativeConstraints) throws Exception;

    public Set<TGD> getTGDs() throws Exception;

    public Set<Atom> getAtoms();

    public Set<ConjunctiveQuery> getConjunctiveQueries();

}
