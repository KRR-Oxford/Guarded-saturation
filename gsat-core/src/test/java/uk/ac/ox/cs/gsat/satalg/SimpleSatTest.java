package uk.ac.ox.cs.gsat.satalg;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import uk.ac.ox.cs.gsat.Log;
import uk.ac.ox.cs.gsat.fol.GTGD;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.fol.TGDFactory;
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

    private static final TGDFactory<TGD> FACTORY = TGDFactory.getTGDInstance(true);

    @BeforeAll
    static void initAll() {
        Handler handlerObj = new ConsoleHandler();
        handlerObj.setLevel(Level.WARNING);
        Log.GLOBAL.addHandler(handlerObj);
        Log.GLOBAL.setLevel(Level.WARNING);
        Log.GLOBAL.setUseParentHandlers(false);
    }

	@Test
	public void simpleTest() {

		SimpleSat sgsat = new SimpleSat(new SaturationAlgorithmConfiguration());
		// Variables

		// A(x1) -> ∃ x2. R(x1, x2)
		// R(x1, x2) -> U(x2)
		// R(x1, x2), U(x2) -> P(x1)
		Atom Ax1 = Atom.create(Predicate.create("A", 1), x1);
		Atom Rx1x2 = Atom.create(Predicate.create("R", 2), x1, x2);
		Atom Ux2 = Atom.create(Predicate.create("U", 1), x2);
		Atom Px1 = Atom.create(Predicate.create("P", 1), x1);
		GTGD nonFull = GTGD.create(Set.of(Ax1), Set.of(Rx1x2));
		GTGD full = GTGD.create(Set.of(Rx1x2), Set.of(Ux2));
		GTGD full1 = GTGD.create(Set.of(Rx1x2, Ux2), Set.of(Px1));
        

        TGD expected = TGD.create(Set.of(Ax1), Set.of(Px1));
        GTGD expected1 = GTGD.create(Set.of(Rx1x2), Set.of(Px1));

		Collection<Dependency> input = new ArrayList<>();
		input.add(nonFull);
		input.add(full);
		input.add(full1);

		Collection<TGD> result = sgsat.run(input);
        
        assertTrue(result.contains(FACTORY.computeVNF(full, sgsat.eVariable, sgsat.uVariable)));
        // full1 is subsumed by expected1
        assertFalse(result.contains(FACTORY.computeVNF(full1, sgsat.eVariable, sgsat.uVariable)));
        assertTrue(result.contains(FACTORY.computeVNF(expected, sgsat.eVariable, sgsat.uVariable)));
        assertTrue(result.contains(FACTORY.computeVNF(expected1, sgsat.eVariable, sgsat.uVariable)));
        checkWidth(input, result);
	}

	@Test
	public void thesisTest() {

		SimpleSat sgsat = new SimpleSat(new SaturationAlgorithmConfiguration());
		// Variables

		// R(x1, x2) -> ∃ y1, y2. S(x1, x2, y1, y2) & T(x1, x2, y2)
		// S(x1,x2,x3,x4) -> U(x4)
		// T(z1, z2, z3), U(z3) -> P(z1)

		Atom Rx1x2 = Atom.create(Predicate.create("R", 2), x1, x2);
		Atom Sx1x2y1y2 = Atom.create(Predicate.create("S", 4), x1, x2, y1, y2);
		Atom Tx1x2y2 = Atom.create(Predicate.create("T", 3), x1, x2, y2);
		GTGD nonFull = GTGD.create(Set.of(Rx1x2), Set.of(Sx1x2y1y2, Tx1x2y2));
		Atom Sx1x2x3x4 = Atom.create(Predicate.create("S", 4), x1, x2, x3, x4);
		Atom Ux4 = Atom.create(Predicate.create("U", 1), x4);
		GTGD full = GTGD.create(Set.of(Sx1x2x3x4), Set.of(Ux4));
		Atom Tz1z2z3 = Atom.create(Predicate.create("T", 3), z1, z2, z3);
		Atom Uz3 = Atom.create(Predicate.create("U", 1), z3);
		Atom Pz1 = Atom.create(Predicate.create("P", 1), z1);
		GTGD full1 = GTGD.create(Set.of(Tz1z2z3, Uz3), Set.of(Pz1));

        Atom Px1 = Atom.create(Predicate.create("P", 1), x1);
        TGD expected = TGD.create(Set.of(Rx1x2), Set.of(Px1));
        
		Collection<Dependency> input = new ArrayList<>();
		input.add(nonFull);
		input.add(full);
		input.add(full1);

		Collection<TGD> result = sgsat.run(input);

        assertTrue(result.contains(FACTORY.computeVNF(full, sgsat.eVariable, sgsat.uVariable)));
        assertTrue(result.contains(FACTORY.computeVNF(full1, sgsat.eVariable, sgsat.uVariable)));
        assertTrue(result.contains(FACTORY.computeVNF(expected, sgsat.eVariable, sgsat.uVariable)));
        checkWidth(input, result);
	}


    private int getWidth(Collection<? extends Dependency> tgds) {
        int width = 0;

        for(Dependency tgd : tgds)
            if (tgd instanceof TGD)
                width = Math.max(((TGD) tgd).getWidth(), width);

        return width;
    }

    private void checkWidth(Collection<Dependency> input, Collection<TGD> output) {
        assertTrue(getWidth(output)<= getWidth(input));
    }
}
