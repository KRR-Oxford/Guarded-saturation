package uk.ac.ox.cs.gsat.fol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import uk.ac.ox.cs.gsat.App;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Function;
import uk.ac.ox.cs.pdq.fol.FunctionTerm;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * Unit tests for the Logic class
 * 
 * @author Stefano
 */
public class LogicTest {

	private static final Atom R_x1 = Atom.create(Predicate.create("R", 1), Variable.create("x1"));
	private static final Atom T_x1y1y2 = Atom.create(Predicate.create("T", 3), Variable.create("x1"),
			Variable.create("y1"), Variable.create("y2"));
	private static final Atom T_x1x2x3 = Atom.create(Predicate.create("T", 3), Variable.create("x1"),
			Variable.create("x2"), Variable.create("x3"));
	private static final Atom U_x1x2y = Atom.create(Predicate.create("U", 3), Variable.create("x1"),
			Variable.create("x2"), Variable.create("y"));
	private static final Atom U_x1x2x3 = Atom.create(Predicate.create("U", 3), Variable.create("x1"),
			Variable.create("x2"), Variable.create("x3"));
	private static final Atom P_x1 = Atom.create(Predicate.create("P", 1), Variable.create("x1"));
	private static final Atom V_x1x2 = Atom.create(Predicate.create("V", 2), Variable.create("x1"),
			Variable.create("x2"));
	private static final Atom S_x1 = Atom.create(Predicate.create("S", 1), Variable.create("x1"));
	private static final Atom M_x1 = Atom.create(Predicate.create("M", 1), Variable.create("x1"));
	private static final Atom R_x1x2 = Atom.create(Predicate.create("R", 2), Variable.create("x1"),
			Variable.create("x2"));
	private static final Atom S_x1x2y1y2 = Atom.create(Predicate.create("S", 4), Variable.create("x1"),
			Variable.create("x2"), Variable.create("y1"), Variable.create("y2"));
	private static final Atom T_x1x2y2 = Atom.create(Predicate.create("T", 3), Variable.create("x1"),
			Variable.create("x2"), Variable.create("y2"));
	private static final Atom S_x1x2x3x4 = Atom.create(Predicate.create("S", 4), Variable.create("x1"),
			Variable.create("x2"), Variable.create("x3"), Variable.create("x4"));
	private static final Atom U_x4 = Atom.create(Predicate.create("U", 1), Variable.create("x4"));
	private static final Atom T_z1z2z3 = Atom.create(Predicate.create("T", 3), Variable.create("z1"),
			Variable.create("z2"), Variable.create("z3"));
	private static final Atom U_z3 = Atom.create(Predicate.create("U", 1), Variable.create("z3"));
	private static final Atom P_z1 = Atom.create(Predicate.create("P", 1), Variable.create("z1"));
	private static final Atom B_x1x2 = Atom.create(Predicate.create("B", 2), Variable.create("x1"),
			Variable.create("x2"));
	private static final Atom H1_x1y1 = Atom.create(Predicate.create("H1", 2), Variable.create("x1"),
			Variable.create("y1"));
	private static final Atom H_x2 = Atom.create(Predicate.create("H2", 1), Variable.create("x2"));
	private static final Atom B_x2x1x3 = Atom.create(Predicate.create("B", 3), Variable.create("x2"),
			Variable.create("x1"), Variable.create("x3"));
	private static final Atom H1_x1z1y1y2 = Atom.create(Predicate.create("H1", 4), Variable.create("x1"),
			Variable.create("z1"), Variable.create("y1"), Variable.create("y2"));
	private static final Atom H2_y1y2 = Atom.create(Predicate.create("H2", 2), Variable.create("y1"),
			Variable.create("y2"));
	private static final Atom b_u1u2u3 = Atom.create(Predicate.create("b", 3), Variable.create("u1"),
			Variable.create("u2"), Variable.create("u3"));
	private static final Atom h1_u2e1e2e3 = Atom.create(Predicate.create("h1", 4), Variable.create("u2"),
			Variable.create("e1"), Variable.create("e2"), Variable.create("e3"));
	private static final Atom h2_e2e3 = Atom.create(Predicate.create("h2", 2), Variable.create("e2"),
			Variable.create("e3"));
	private static final Atom U_u1u2u3 = Atom.create(Predicate.create("U", 3), Variable.create("u1"),
			Variable.create("u2"), Variable.create("e1"));
	private static final Atom U_z1z2z3 = Atom.create(Predicate.create("U", 3), Variable.create("z1"),
			Variable.create("z2"), Variable.create("z3"));

	@BeforeAll
	static void initAll() {
		Handler handlerObj = new ConsoleHandler();
		handlerObj.setLevel(Level.WARNING);
		App.logger.addHandler(handlerObj);
		App.logger.setLevel(Level.WARNING);
		App.logger.setUseParentHandlers(false);
	}

	@Test
	public void isFullTest() {

		// ∀ x2,x1,x3 B(x2,x1,x3) → ∃ z1,y1,y2 H1(x1,z1,y1,y2) & H2(y1,y2)
		TGD tgd = TGD.create(new Atom[] { B_x2x1x3 }, new Atom[] { H1_x1z1y1y2, H2_y1y2 });
		System.out.println("TGD: " + tgd);
		assertFalse(Logic.isFull(tgd), "This is a 'non-full' TGD");

		// ∀ u1,u2,u3 B(u1,u2,u3) → ∃ e1,e2,e3 H1(u2,e1,e2,e3) & H2(e2,e3)
		tgd = TGD.create(new Atom[] { b_u1u2u3 }, new Atom[] { h1_u2e1e2e3, h2_e2e3 });
		System.out.println("TGD: " + tgd);
		assertFalse(Logic.isFull(tgd), "This is a 'non-full' TGD");

		// ∀ x1,x2 B(x1,x2) → ∃ y1 H1(x1,y1) ∧ H2(x2)
		tgd = TGD.create(new Atom[] { B_x1x2 }, new Atom[] { H1_x1y1, H_x2 });
		System.out.println("TGD: " + tgd);
		assertFalse(Logic.isFull(tgd), "This is a 'non-full' TGD");

		// ∀ x1,x2 B(x1,x2) → ∃ y1 H1(x1,y1)
		tgd = TGD.create(new Atom[] { B_x1x2 }, new Atom[] { H1_x1y1 });
		System.out.println("TGD: " + tgd);
		assertFalse(Logic.isFull(tgd), "This is a 'non-full' TGD");

		// ∀ x1,x2 B(x1,x2) → H2(x2)
		tgd = TGD.create(new Atom[] { B_x1x2 }, new Atom[] { H_x2 });
		System.out.println("TGD: " + tgd);
		assertTrue(Logic.isFull(tgd), "This is a 'full' TGD");

		// ∀ x1 R(x1) → ∃ y1,y2 T(x1,y1,y2)
		TGD t1 = TGD.create(new Atom[] { R_x1 }, new Atom[] { T_x1y1y2 });
		// ∀ x1,x2,x3 T(x1,x2,x3) → ∃ y U(x1,x2,y)
		TGD t2 = TGD.create(new Atom[] { T_x1x2x3 }, new Atom[] { U_x1x2y });
		// ∀ x1,x2,x3 U(x1,x2,x3) → P(x1) ∧ V(x1,x2)
		TGD t3 = TGD.create(new Atom[] { U_x1x2x3 }, new Atom[] { P_x1, V_x1x2 });
		// ∀ x1,x2,x3 T(x1,x2,x3) ∧ V(x1,x2) ∧ S(x1) → M(x1)
		TGD t4 = TGD.create(new Atom[] { T_x1x2x3, V_x1x2, S_x1 }, new Atom[] { M_x1 });

		System.out.println("TGD: " + t1);
		assertFalse(Logic.isFull(t1), "This is a 'non-full' TGD");
		System.out.println("TGD: " + t2);
		assertFalse(Logic.isFull(t2), "This is a 'non-full' TGD");
		System.out.println("TGD: " + t3);
		assertTrue(Logic.isFull(t3), "This is a 'full' TGD");
		System.out.println("TGD: " + t4);
		assertTrue(Logic.isFull(t4), "This is a 'full' TGD");

		// ∀ x1,x2 R(x1,x2) → ∃ y1,y2 S(x1,x2,y1,y2) ∧ T(x1,x2,y2)
		t1 = TGD.create(new Atom[] { R_x1x2 }, new Atom[] { S_x1x2y1y2, T_x1x2y2 });
		// ∀ x1,x2,x3,x4 S(x1,x2,x3,x4) → U(x4)
		t2 = TGD.create(new Atom[] { S_x1x2x3x4 }, new Atom[] { U_x4 });
		// ∀ z1,z2,z3 T(z1,z2,z3) ∧ U(z3) → P(z1)
		t3 = TGD.create(new Atom[] { T_z1z2z3, U_z3 }, new Atom[] { P_z1 });
		System.out.println("TGD: " + t1);
		assertFalse(Logic.isFull(t1), "This is a 'non-full' TGD");
		System.out.println("TGD: " + t2);
		assertTrue(Logic.isFull(t2), "This is a 'full' TGD");
		System.out.println("TGD: " + t3);
		assertTrue(Logic.isFull(t3), "This is a 'full' TGD");

		// ∀ u1,u2,u3 U(u1,u2,u3) → ∃ z1,z2,z3 U(z1,z2,z3)
		tgd = TGD.create(new Atom[] { U_u1u2u3 }, new Atom[] { U_z1z2z3 });
		System.out.println("TGD: " + tgd);
		assertFalse(Logic.isFull(tgd), "This is a 'non-full' TGD");

	}

	@Test
	public void getMGUAtomAtom() {

		System.out.println("Testing getMGU(Atom, Atom)");

		Variable x1 = Variable.create("x1");
		Variable x2 = Variable.create("x2");
		Variable y = Variable.create("y");
		Variable z1 = Variable.create("z1");
		Variable z2 = Variable.create("z2");

		Atom Rx = Atom.create(Predicate.create("R", 3), x1, x2, y);
		Atom Rz = Atom.create(Predicate.create("R", 3), z1, z1, z2);

		Map<Term, Term> expected = new HashMap<>();
		expected.put(z1, x2);
		expected.put(z2, y);
		expected.put(x1, x2);

		assertEquals(expected, Logic.getMGU(Rz, Rx));

		Variable z3 = Variable.create("z3");

		Rx = Atom.create(Predicate.create("R", 4), x1, x1, x2, y);
		Rz = Atom.create(Predicate.create("R", 4), z1, z2, z1, z3);

		expected = new HashMap<>();
		expected.put(z1, x2);
		expected.put(z2, x2);
		expected.put(z3, y);
		expected.put(x1, x2);

		assertEquals(expected, Logic.getMGU(Rz, Rx));

	}

	// @Test
	// public void getMGUAtomsAtoms() {

	// System.out.println("Testing getMGU(Atoms, Atoms)");

	// Variable x1 = Variable.create("x1");
	// Variable x2 = Variable.create("x2");
	// Variable y = Variable.create("y");
	// Variable z1 = Variable.create("z1");
	// Variable z2 = Variable.create("z2");

	// Atom Rx = Atom.create(Predicate.create("R", 3), x1, x2, y);
	// Atom Rz = Atom.create(Predicate.create("R", 3), z1, z1, z2);

	// Map<Term, Term> expected = new HashMap<>();
	// expected.put(z1, x2);
	// expected.put(z2, y);
	// expected.put(x1, x2);

	// assertEquals(expected, Logic.getMGU(Arrays.asList(Rz), Arrays.asList(Rx)));

	// Variable z3 = Variable.create("z3");

	// Rx = Atom.create(Predicate.create("R", 4), x1, x1, x2, y);
	// Rz = Atom.create(Predicate.create("R", 4), z1, z2, z1, z3);

	// expected = new HashMap<>();
	// expected.put(z1, x2);
	// expected.put(z2, x2);
	// expected.put(z3, y);
	// expected.put(x1, x2);

	// assertEquals(expected, Logic.getMGU(Rz, Rx));

	// }

	@Test
	public void MGUTest() {

		System.out.println("MGU tests:");

		// ****** MGU tests: *******
		// 1.
		Variable x1 = Variable.create("x1");
		Variable x2 = Variable.create("x2");
		Variable y1 = Variable.create("y1");
		Variable z1 = Variable.create("z1");
		Variable z2 = Variable.create("z2");
		Variable z3 = Variable.create("z3");
		Atom Rx = Atom.create(Predicate.create("R", 3), x1, x2, y1);
		Atom Rz = Atom.create(Predicate.create("R", 3), z1, z1, z2);
		Map<Term, Term> expected = new HashMap<>();
		expected.put(z1, x2);
		expected.put(z2, y1);
		expected.put(x1, x2);
		checkMGUTest(expected, Logic.getMGU(Rz, Rx));

		// 2.
		Rx = Atom.create(Predicate.create("R", 4), x1, x1, x2, y1);
		Rz = Atom.create(Predicate.create("R", 4), z1, z2, z1, z3);
		expected = new HashMap<>();
		expected.put(z1, x2);
		expected.put(z2, x2);
		expected.put(z3, y1);
		expected.put(x1, x2);
		checkMGUTest(expected, Logic.getMGU(Rz, Rx));

		// 3. non-matching predicate
		Rx = Atom.create(Predicate.create("R", 1), y1);
		Rz = Atom.create(Predicate.create("S", 1), z1);
		checkMGUTest(null, Logic.getMGU(Rz, Rx));

		// 4.
		Rx = Atom.create(Predicate.create("R", 4), x1, y1, y1, x2);
		Rz = Atom.create(Predicate.create("R", 4), z1, z2, z2, z1);
		expected = new HashMap<>();
		expected.put(z1, x2);
		expected.put(z2, y1);
		expected.put(x1, x2);
		checkMGUTest(expected, Logic.getMGU(Rz, Rx));

	}
    @Test
    public void MGUSkolemTest() {

		Variable x1 = Variable.create("x1");
		Variable y1 = Variable.create("y1");
		Variable z1 = Variable.create("z1");
		Variable z2 = Variable.create("z2");

        FunctionTerm fx1 = FunctionTerm.create(new Function("f", 1), x1);
        FunctionTerm fy1 = FunctionTerm.create(new Function("f", 1), y1);
        FunctionTerm gy1 = FunctionTerm.create(new Function("g", 1), y1);

        // 1.
        Atom A1 = Atom.create(Predicate.create("R", 2), fy1, y1);
        Atom B1 = Atom.create(Predicate.create("R", 2), x1, x1);
        checkMGUTest(null, Logic.getMGU(A1, B1));

        // 2.
        Atom A2 = Atom.create(Predicate.create("R", 2), fy1, y1);
        Atom B2 = Atom.create(Predicate.create("R", 2), fx1, x1);
        Map<Term, Term> expected2 = new HashMap<>();
        expected2.put(y1, x1);
        checkMGUTest(expected2, Logic.getMGU(A2, B2));

        // 3.
        Atom A3 = Atom.create(Predicate.create("R", 2), fx1, fy1);
        Atom B3 = Atom.create(Predicate.create("R", 2), z1, z1);
        Map<Term, Term> expected3 = new HashMap<>();
        expected3.put(z1, fx1);
        expected3.put(y1, x1);
        checkMGUTest(expected3, Logic.getMGU(A3, B3));

        // 4.
        Atom A4 = Atom.create(Predicate.create("R", 3), y1, x1, y1);
        Atom B4 = Atom.create(Predicate.create("R", 3), fx1, z1, z1);
        Map<Term, Term> expected4 = null;
        checkMGUTest(expected4, Logic.getMGU(A4, B4));

        // 5.
        Atom A5 = Atom.create(Predicate.create("R", 1), gy1);
        Atom B5 = Atom.create(Predicate.create("R", 1), fx1);
        Map<Term, Term> expected5 = null;
        checkMGUTest(expected5, Logic.getMGU(A5, B5));

        // 6.
        Atom A6 = Atom.create(Predicate.create("R", 3), fx1, fy1, z1);
        Atom B6 = Atom.create(Predicate.create("R", 3), z1, z2, z2);
        Map<Term, Term> expected6 = new HashMap<>();
        expected6.put(z1, fx1);
        expected6.put(z2, fy1);
        expected6.put(x1, y1);
        checkMGUTest(expected6, Logic.getMGU(A6, B6));

        // 7.
        Atom A7 = Atom.create(Predicate.create("R", 2), gy1, y1);
        Atom B7 = Atom.create(Predicate.create("R", 2), x1, fx1);
        Map<Term, Term> expected7 = null;
        checkMGUTest(expected7, Logic.getMGU(A7, B7));
    }

	private void checkMGUTest(Map<Term, Term> expected, Map<Term, Term> result) {
		System.out.print("Returned MGU: ");
		System.out.println(result);
		System.out.print("Expected MGU: ");
		System.out.println(expected + "\n");
		assertEquals(expected, result, "Computed wrong MGU.");
	}

}
