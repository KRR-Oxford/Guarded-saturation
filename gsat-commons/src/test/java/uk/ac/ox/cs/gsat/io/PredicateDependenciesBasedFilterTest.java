package uk.ac.ox.cs.gsat.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.Set;

import org.junit.jupiter.api.Test;

import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Variable;

public class PredicateDependenciesBasedFilterTest {

    private static final Variable x1 = Variable.create("x1");
    private static final Atom R_x1 = Atom.create(Predicate.create("R", 1), x1);
    private static final Atom S_x1 = Atom.create(Predicate.create("S", 1), x1);
    private static final Atom T_x1 = Atom.create(Predicate.create("T", 1), x1);
    private static final Atom U_x1 = Atom.create(Predicate.create("U", 1), x1);

    @Test
    public void basicTest () {

        TGD R_to_S = new TGD(Set.of(R_x1), Set.of(S_x1));
        TGD S_to_T = new TGD(Set.of(S_x1), Set.of(T_x1));
        TGD R_to_U = new TGD(Set.of(R_x1), Set.of(U_x1));

        Set<Predicate> wantedPredicate = Set.of(Predicate.create("T", 1));
        PredicateDependenciesBasedFilter<TGD> filter = new PredicateDependenciesBasedFilter<TGD>(wantedPredicate);

        Collection<TGD> tgds = Set.of(R_to_S, S_to_T, R_to_U);
        Collection<TGD> filterTGDs = filter.filter(tgds);

        Collection<TGD> expected = Set.of(R_to_S, S_to_T);
        
        assertEquals(expected, filterTGDs);
    }
}
