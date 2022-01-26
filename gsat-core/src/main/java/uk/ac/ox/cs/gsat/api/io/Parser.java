package uk.ac.ox.cs.gsat.api.io;

import java.util.Set;

import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.fol.Dependency;

public interface Parser {

    public Set<Dependency> getDependencies();

    public Set<TGD> getTGDs();

    public Set<Atom> getAtoms();

    public Set<ConjunctiveQuery> getConjunctiveQueries();

}
