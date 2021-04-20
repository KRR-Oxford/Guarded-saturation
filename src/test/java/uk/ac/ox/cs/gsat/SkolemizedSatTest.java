package uk.ac.ox.cs.gsat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Variable;

public class SkolemizedSatTest {

    private static final Variable x1 = Variable.create("x1");
    private static final Variable x2 = Variable.create("x2");
    private static final Variable x3 = Variable.create("x3");
    private static final Variable x4 = Variable.create("x4");
    private static final Variable y1 = Variable.create("y1");
    private static final Variable y2 = Variable.create("y2");
    private static final Variable z1 = Variable.create("z1");
    private static final Variable z2 = Variable.create("z2");
    private static final Variable z3 = Variable.create("z3");

    private static final TGDFactory<TGD> FACTORY = TGDFactory.getTGDInstance();

    @BeforeAll
    static void initAll() {
        Handler handlerObj = new ConsoleHandler();
        handlerObj.setLevel(Level.WARNING);
        App.logger.addHandler(handlerObj);
        App.logger.setLevel(Level.WARNING);
        App.logger.setUseParentHandlers(false);
    }

    @Test
    public void simpleTest() {

        SkolemizedSat sksat = SkolemizedSat.getInstance();
        // Variables

        // A(x1) -> ∃ x2. R(x1, x2)
        // R(x1, x2) -> U(x2)
        // R(x1, x2), U(x2) -> P(x1)
        Atom Ax1 = Atom.create(Predicate.create("A", 1), x1);
        Atom Rx1x2 = Atom.create(Predicate.create("R", 2), x1, x2);
        Atom Ux2 = Atom.create(Predicate.create("U", 1), x2);
        Atom Px1 = Atom.create(Predicate.create("P", 1), x1);
        GTGD nonFull = new GTGD(Set.of(Ax1), Set.of(Rx1x2));
        GTGD full = new GTGD(Set.of(Rx1x2), Set.of(Ux2));
        GTGD full1 = new GTGD(Set.of(Rx1x2, Ux2), Set.of(Px1));


        HashSet<TGD> expected = new HashSet<TGD>();
        expected.add(EvolveBasedSat.FACTORY.computeVNF(full, sksat.eVariable, sksat.uVariable));
        expected.add(EvolveBasedSat.FACTORY.computeVNF(full1, sksat.eVariable, sksat.uVariable));
        expected.add(EvolveBasedSat.FACTORY.computeVNF(new GTGD(Set.of(Ax1), Set.of(Px1)), sksat.eVariable, sksat.uVariable));

        Collection<Dependency> input = new ArrayList<>();
        input.add(nonFull);
        input.add(full);
        input.add(full1);

        Collection<GTGD> result = sksat.run(input);

        assertEquals(result, expected);
    }

}
