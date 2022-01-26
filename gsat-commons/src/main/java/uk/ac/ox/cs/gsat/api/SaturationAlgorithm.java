package uk.ac.ox.cs.gsat.api;

import java.util.Collection;

import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Dependency;

public interface SaturationAlgorithm {

    public Collection<? extends TGD> run(Collection<? extends Dependency> tgds);
}
