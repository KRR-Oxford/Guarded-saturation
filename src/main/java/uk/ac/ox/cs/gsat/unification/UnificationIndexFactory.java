package uk.ac.ox.cs.gsat.unification;

import java.util.Comparator;

import uk.ac.ox.cs.gsat.GTGD;

public class UnificationIndexFactory {

    private static final UnificationIndexFactory INSTANCE = new UnificationIndexFactory();

    private UnificationIndexFactory() {
    }

    public static UnificationIndexFactory getInstance() {
        return INSTANCE;
    }
    
    public <Q extends GTGD> UnificationIndex<Q> create(UnificationIndexType type) {
        return create(type, null);
    }

    public <Q extends GTGD> UnificationIndex<Q> create(UnificationIndexType type, Comparator<? super GTGD> comparator) {
        switch(type) {
        case PREDICATE_INDEX:
            return new PredicateUnificationIndex<Q>(comparator);
        case ATOM_PATH_INDEX:
            return new AtomPathUnificationIndex<Q>();
        default:
            String message = String.format("The type %s for unification index is not implemented", type);
            throw new IllegalStateException(message);
        }
    }

}
