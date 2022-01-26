package uk.ac.ox.cs.gsat.api.io;

import java.util.Collection;
import java.util.Set;

import uk.ac.ox.cs.gsat.fol.TGD;

public interface TGDFilter<T extends TGD> {

    public Set<T> filter(Collection<T> tgds);

    public boolean isKept(T tgd);
}
