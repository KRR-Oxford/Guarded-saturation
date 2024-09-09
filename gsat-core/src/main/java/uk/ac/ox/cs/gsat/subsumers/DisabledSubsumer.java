package uk.ac.ox.cs.gsat.subsumers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import uk.ac.ox.cs.gsat.fol.TGD;

public class DisabledSubsumer<Q extends TGD> implements Subsumer<Q> {

    private final Collection<Q> tgds;

    public DisabledSubsumer() {
        this.tgds = new HashSet<>();
    }
    
    @Override
    public Collection<Q> subsumesAny(Q tgd) {
        return new ArrayList<Q>();
    }

    @Override
    public boolean subsumed(Q tgd) {
        return this.tgds.contains(tgd);
    }

    @Override
    public void add(Q tgd) {
        this.tgds.add(tgd);
    }

    @Override
    public Collection<Q> getAll() {
        return tgds;
    }

}
