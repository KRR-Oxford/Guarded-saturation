package uk.ac.ox.cs.gsat.api.io;

import java.util.Collection;
import java.util.Set;

import uk.ac.ox.cs.gsat.fol.TGD;

public interface TGDTransformation<T extends TGD> {

    public Set<T> apply(Collection<T> tgds);

}
