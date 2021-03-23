package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Variable;

public class SimpleSatTest {

    private static final Variable x1 = Variable.create("x1");
    private static final Variable x2 = Variable.create("x2");
    private static final Variable x3 = Variable.create("x3");
    private static final Variable x4 = Variable.create("x4");
    private static final Variable y1 = Variable.create("y1");
    private static final Variable y2 = Variable.create("y2");
    private static final Variable z1 = Variable.create("z1");
    private static final Variable z2 = Variable.create("z2");
    private static final Variable z3 = Variable.create("z3");

    @BeforeAll
    static void initAll() {
        Handler handlerObj = new ConsoleHandler();
        handlerObj.setLevel(Level.WARNING);
        App.logger.addHandler(handlerObj);
        App.logger.setLevel(Level.WARNING);
        App.logger.setUseParentHandlers(false);
    }

        @Test
    public void defaultTest() {

        SimpleSat sgsat = SimpleSat.getInstance();
        // Variables

        // R(x1, x2) -> ∃ y1, y2. S(x1, x2, y1, y2) & T(x1, x2, y2)
        // S(x1,x2,x3,x4) -> U(x4)
        // T(z1, z2, z3) -> P(z1)

        Atom Rx1x2 = Atom.create(Predicate.create("R", 2), x1, x2);
        Atom Sx1x2y1y2 = Atom.create(Predicate.create("S", 4), x1, x2, y1, y2);
        Atom Tx1x2y2 = Atom.create(Predicate.create("T", 3), x1, x2, y2);
        GTGD nonFull = new GTGD(Set.of(Rx1x2), Set.of(Sx1x2y1y2, Tx1x2y2));
        Atom Sx1x2x3x4 = Atom.create(Predicate.create("S", 4), x1, x2,x3,x4);
        Atom Ux4 = Atom.create(Predicate.create("U", 1), x4);
        GTGD full = new GTGD(Set.of(Sx1x2x3x4), Set.of(Ux4));
        Atom Tz1z2z3 = Atom.create(Predicate.create("T", 3), z1, z2, z3);
        Atom Uz3 = Atom.create(Predicate.create("U", 1), z3);
        Atom Pz1 = Atom.create(Predicate.create("P", 1), z1);
        GTGD full1 = new GTGD(Set.of(Tz1z2z3, Uz3), Set.of(Pz1));

        Collection<Dependency> input = new ArrayList<>();
        input.add(nonFull);
        input.add(full);
        input.add(full1);

        Collection<TGD> result = sgsat.run(input);

        for(TGD tgd : result)
            System.out.println(tgd);

        // Collection<TGDGSat> expected = new HashSet<>();
        // Atom Ty1 = Atom.create(Predicate.create("T", 1), y1);
        // expected.add(new TGDGSat(Set.of(Px1, Sx1), Set.of(Rx1y1, Sy1, Ty1)));

        // checkEvolveNewTest(nonFull, full, expected, evolved);
    }
}
