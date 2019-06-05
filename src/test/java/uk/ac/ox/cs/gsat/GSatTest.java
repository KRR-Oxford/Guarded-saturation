package uk.ac.ox.cs.gsat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Constant;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.UntypedConstant;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * Unit tests for the GSat class
 *
 * @author Stefano
 */
public class GSatTest {

	// Variables
	private static final Variable x1 = Variable.create("x1");
	private static final Variable x2 = Variable.create("x2");
	private static final Variable x3 = Variable.create("x3");
	private static final Variable x4 = Variable.create("x4");
	private static final Variable y1 = Variable.create("y1");
	private static final Variable y2 = Variable.create("y2");
	private static final Variable z1 = Variable.create("z1");
	private static final Variable z2 = Variable.create("z2");
	private static final Variable z3 = Variable.create("z3");

	// Atoms
	private static final Atom R_x1 = Atom.create(Predicate.create("R", 1), x1);
	private static final Atom T_x1y1y2 = Atom.create(Predicate.create("T", 3), x1, y1, y2);
	private static final Atom T_x1x2x3 = Atom.create(Predicate.create("T", 3), x1, x2, x3);
	private static final Atom U_x1x2y1 = Atom.create(Predicate.create("U", 3), x1, x2, y1);
	private static final Atom U_x1x2x3 = Atom.create(Predicate.create("U", 3), x1, x2, x3);
	private static final Atom P_x1 = Atom.create(Predicate.create("P", 1), x1);
	private static final Atom V_x1x2 = Atom.create(Predicate.create("V", 2), x1, x2);
	private static final Atom S_x1 = Atom.create(Predicate.create("S", 1), x1);
	private static final Atom M_x1 = Atom.create(Predicate.create("M", 1), x1);
	private static final Atom R_x1x2 = Atom.create(Predicate.create("R", 2), x1, x2);
	private static final Atom S_x1x2y1y2 = Atom.create(Predicate.create("S", 4), x1, x2, y1, y2);
	private static final Atom T_x1x2y2 = Atom.create(Predicate.create("T", 3), x1, x2, y2);
	private static final Atom S_x1x2x3x4 = Atom.create(Predicate.create("S", 4), x1, x2, x3, x4);
	private static final Atom U_x4 = Atom.create(Predicate.create("U", 1), x4);
	private static final Atom T_z1z2z3 = Atom.create(Predicate.create("T", 3), z1, z2, z3);
	private static final Atom U_z3 = Atom.create(Predicate.create("U", 1), z3);
	private static final Atom P_z1 = Atom.create(Predicate.create("P", 1), z1);
	private static final Atom B_x1x2 = Atom.create(Predicate.create("B", 2), x1, x2);
	private static final Atom H1_x1y1 = Atom.create(Predicate.create("H1", 2), x1, y1);
	private static final Atom H2_x2 = Atom.create(Predicate.create("H2", 1), x2);
	private static final Atom B_x2x1x3 = Atom.create(Predicate.create("B", 3), x2, x1, x3);
	private static final Atom H1_x1z1y1y2 = Atom.create(Predicate.create("H1", 4), x1, z1, y1, y2);
	private static final Atom H2_y1y2 = Atom.create(Predicate.create("H2", 2), y1, y2);
	private static final Atom B_u1u2u3 = Atom.create(Predicate.create("B", 3),
			Variable.create(GSat.getInstance().uVariable + "1"), Variable.create(GSat.getInstance().uVariable + "2"),
			Variable.create(GSat.getInstance().uVariable + "3"));
	private static final Atom H1_u2e1e2e3 = Atom.create(Predicate.create("H1", 4),
			Variable.create(GSat.getInstance().uVariable + "2"), Variable.create(GSat.getInstance().eVariable + "1"),
			Variable.create(GSat.getInstance().eVariable + "2"), Variable.create(GSat.getInstance().eVariable + "3"));
	private static final Atom H2_e2e3 = Atom.create(Predicate.create("H2", 2),
			Variable.create(GSat.getInstance().eVariable + "2"), Variable.create(GSat.getInstance().eVariable + "3"));

	@BeforeAll
	static void initAll() {
		Handler handlerObj = new ConsoleHandler();
		handlerObj.setLevel(Level.WARNING);
		App.logger.addHandler(handlerObj);
		App.logger.setLevel(Level.WARNING);
		App.logger.setUseParentHandlers(false);
	}

	@Test
	public void runGSatTest() {
		System.out.println("runGSat tests");
		GSat gsat = GSat.getInstance();
		// Example 1
		// ∀ x1 R(x1) → ∃ y1,y2 T(x1,y1,y2)
		// ∀ x1,x2,x3 T(x1,x2,x3) → ∃ y1 U(x1,x2,y1)
		// ∀ x1,x2,x3 U(x1,x2,x3) → P(x1) ∧ V(x1,x2)
		// ∀ x1,x2,x3 T(x1,x2,x3) ∧ V(x1,x2) ∧ S(x1) → M(x1)
		TGD t1 = TGD.create(new Atom[] { R_x1 }, new Atom[] { T_x1y1y2 });
		TGD t2 = TGD.create(new Atom[] { T_x1x2x3 }, new Atom[] { U_x1x2y1 });
		TGD t3 = TGD.create(new Atom[] { U_x1x2x3 }, new Atom[] { P_x1, V_x1x2 });
		TGD t4 = TGD.create(new Atom[] { T_x1x2x3, V_x1x2, S_x1 }, new Atom[] { M_x1 });

		Collection<TGD> initial = new HashSet<>();
		initial.add(t1);
		initial.add(t2);
		initial.add(t3);
		initial.add(t4);

		Collection<TGDGSat> result = gsat.runGSat(initial.toArray(new TGD[initial.size()]));
		// ∀ u1,u2,u3 T(u1,u2,u3) → P(u1) ∧ V(u1,u2)
		// ∀ u1 R(u1) ∧ S(u1) → P(u1)
		// ∀ u1,u2,u3 T(u1,u2,u3) ∧ V(u1,u2) ∧ S(u1) → M(u1)
		// ∀ u1 R(u1) → P(u1)
		// ∀ u1,u2,u3 U(u1,u2,u3) → P(u1) ∧ V(u1,u2)
		// ∀ u1 R(u1) ∧ S(u1) → M(u1)
		checkRunGSatTest(initial, 6, result);

		// Example 2
		// ∀ x1,x2 R(x1,x2) → ∃ y1,y2 S(x1,x2,y1,y2) ∧ T(x1,x2,y2)
		// ∀ x1,x2,x3,x4 S(x1,x2,x3,x4) → U(x4)
		// ∀ z1,z2,z3 T(z1,z2,z3) ∧ U(z3) → P(z1)
		t1 = TGD.create(new Atom[] { R_x1x2 }, new Atom[] { S_x1x2y1y2, T_x1x2y2 });
		t2 = TGD.create(new Atom[] { S_x1x2x3x4 }, new Atom[] { U_x4 });
		t3 = TGD.create(new Atom[] { T_z1z2z3, U_z3 }, new Atom[] { P_z1 });

		initial = new HashSet<>();
		initial.add(t1);
		initial.add(t2);
		initial.add(t3);

		result = gsat.runGSat(initial.toArray(new TGD[initial.size()]));

		// ∀ u1,u2,u3 T(u1,u2,u3) ∧ U(u3) → P(u1)
		// ∀ u1,u2,u3,u4 S(u1,u2,u3,u4) → U(u4)
		// ∀ u1,u2 R(u1,u2) → P(u1)
		checkRunGSatTest(initial, 3, result);
	}

	private void checkRunGSatTest(Collection<TGD> initial, int expected, Collection<TGDGSat> result) {
		System.out.println("Initial TGDs:  " + initial);
		System.out.println("Created rules: " + result);

		assertEquals(expected, result.size());
	}

	@Test
	public void HNFTest() {
		System.out.println("HNF tests");
		GSat gsat = GSat.getInstance();

		// 1. Split non-full and full parts
		// ∀ x1,x2 B(x1,x2) → ∃ y1 H1(x1,y1) ∧ H2(x2)
		Atom[] body = { B_x1x2 };
		TGD tgd = TGD.create(body, new Atom[] { H1_x1y1, H2_x2 });
		Collection<TGD> result = gsat.HNF(tgd);

		// ∀ x1,x2 B(x1,x2) → ∃ y1 H1(x1,y1)
		TGD tgdExpected1 = TGD.create(body, new Atom[] { H1_x1y1 });
		// ∀ x1,x2 B(x1,x2) → H2(x2)
		TGD tgdExpected2 = TGD.create(body, new Atom[] { H2_x2 });
		Collection<TGD> expected = new HashSet<>();
		expected.add(tgdExpected1);
		expected.add(tgdExpected2);

		checkHNFTest(tgd, expected, result);

		// 2. Remove head atoms that occur in the body: create no new rule
		// ∀ x1,x2 B(x1,x2) & H2(x2) → H2(x2) & B(x1,x2)
		body = new Atom[] { B_x1x2, H2_x2 };
		tgd = TGD.create(body, new Atom[] { H2_x2, B_x1x2 });
		result = gsat.HNF(tgd);
		expected = new HashSet<>();
		checkHNFTest(tgd, expected, result);

		// 3. Remove head atoms that occur in the body: only create non-full rule
		// ∀ x1,x2 B(x1,x2) & H2(x2) → \exists y. H1(x1,y1) & H2(x2) & B(x1,x2)
		tgd = TGD.create(body, new Atom[] { H1_x1y1, H2_x2, B_x1x2 });
		result = gsat.HNF(tgd);
		expected = new HashSet<>();
		expected.add(TGD.create(body, new Atom[] { H1_x1y1 }));
		checkHNFTest(tgd, expected, result);

		// 4. Rule containing bottom in the head. Remove all head atoms.
		// ∀ x1,x2 B(x1,x2) → ∃ y1 H1(x1,y1) ∧ H2(x2) & ⊥
		tgd = TGD.create(body, new Atom[] { H1_x1y1, H2_x2, TGDGSat.Bottom });
		result = gsat.HNF(tgd);
		expected = new HashSet<>();
		expected.add(TGD.create(body, new Atom[] { TGDGSat.Bottom }));
		checkHNFTest(tgd, expected, result);
	}

	private void checkHNFTest(TGD tgd, Collection<TGD> expected, Collection<TGD> result) {
		System.out.println("Original TGD: " + tgd);
		System.out.println("Created rules: " + result);
		System.out.println("Expected rules: " + expected);

		assertEquals(expected, result);
	}

	@Test
	public void VNFsTest() {

		// ∀ x2,x1,x3 B(x2,x1,x3) → ∃ z1,y1,y2 H1(x1,z1,y1,y2) & H2(y1,y2)
		TGD tgd = TGD.create(new Atom[] { B_x2x1x3 }, new Atom[] { H1_x1z1y1y2, H2_y1y2 });
		System.out.println("Original TGD: " + tgd);

		Collection<TGDGSat> tgdsVNFs = GSat.getInstance().VNFs(Arrays.asList(tgd));
		System.out.println("TGDs in VNFs: " + tgdsVNFs);

		// ∀ u1,u2,u3 B(u1,u2,u3) → ∃ e1,e2,e3 H1(u2,e1,e2,e3) & H2(e2,e3)
		TGDGSat tgdExpected = new TGDGSat(TGD.create(new Atom[] { B_u1u2u3 }, new Atom[] { H1_u2e1e2e3, H2_e2e3 }));

		assertEquals(1, tgdsVNFs.size());
		assertTrue(tgdsVNFs.contains(tgdExpected), "Expecting: " + tgdExpected + ", got: " + tgdsVNFs);

	}

	@Test
	public void VNFTest() {

		// ∀ x2,x1,x3 B(x2,x1,x3) → ∃ z1,y1,y2 H1(x1,z1,y1,y2) & H2(y1,y2)
		TGD tgd = TGD.create(new Atom[] { B_x2x1x3 }, new Atom[] { H1_x1z1y1y2, H2_y1y2 });
		System.out.println("Original TGD: " + tgd);

		TGD tgdVNF = GSat.getInstance().VNF(tgd);
		System.out.println("TGD in VNF: " + tgdVNF);

		// ∀ u1,u2,u3 B(u1,u2,u3) → ∃ e1,e2,e3 H1(u2,e1,e2,e3) & H2(e2,e3)
		TGD tgdExpected = new TGDGSat(TGD.create(new Atom[] { B_u1u2u3 }, new Atom[] { H1_u2e1e2e3, H2_e2e3 }));

		assertEquals(tgdExpected, tgdVNF);

	}

	// @Test
	// public void evolveTest() {
	// }

	// @Test
	// public void evolveRenameTest() {
	// // ∀ x2,x1,x3 B(x2,x1,x3) → ∃ z1,y1,y2 H1(x1,z1,y1,y2) & H2(y1,y2)
	// final TGD tgd = TGD.create(new Atom[] { B_x2x1x3 }, new Atom[] { H1_x1z1y1y2,
	// H2_y1y2 });
	// System.out.println("Original TGD: " + tgd);

	// assertThrows(IllegalArgumentException.class, () ->
	// GSat.getInstance().evolveRename(tgd),
	// "Expected evolveRename to throw IllegalArgumentException, but it didn't");

	// // ∀ u1,u2,u3 B(u1,u2,u3) → H1(u2,u1)
	// TGD tgd2 = new TGDGSat(TGD.create(new Atom[] { B_u1u2u3 }, new Atom[] {
	// H1_u2u1 }));
	// System.out.println("Original TGD: " + tgd2);

	// TGD tgdsEvolveRename = GSat.getInstance().evolveRename(tgd2);
	// System.out.println("TGDs in evolveRename: " + tgdsEvolveRename);

	// // ∀ z1,z2,z3 B(z1,z2,z3) → ∃ z1,z2 H1(z2,z1)
	// TGD tgdExpected = TGD.create(new Atom[] { B_z1z2z3 }, new Atom[] { H1_z2z1
	// });

	// assertEquals(tgdExpected, tgdsEvolveRename);

	// }

	// @Test
	// public void getMGUTest() {

	// Collection<Variable> existentials = new HashSet<>();
	// existentials.add(Variable.create(GSat.getInstance().eVariable + "1"));

	// Map<Term, Term> mgu = GSat.getInstance().getMGU(new Atom[] { U_u1u2u3 }, new
	// Atom[] { U_z1z2z3 },
	// Arrays.asList(U_z1z2z3), existentials);

	// System.out.println(mgu);

	// assertNotNull(mgu);

	// }

	@Test
	public void equalsTGDs() {

		Atom OWL1 = Atom.create(Predicate.create("http://www.daml.org/2001/03/daml+oil#Nothing", 1),
				Variable.create(GSat.getInstance().uVariable + "1"));
		Atom OWL2 = Atom.create(Predicate.create("http://www.w3.org/2000/01/rdf-schema#Resource", 1),
				Variable.create(GSat.getInstance().eVariable + "1"));
		Atom OWL3 = Atom.create(Predicate.create("true", 1), Variable.create(GSat.getInstance().eVariable + "1"));
		TGDGSat tgd1 = new TGDGSat(TGD.create(new Atom[] { OWL1 }, new Atom[] { OWL2, OWL3 }));

		TGDGSat tgd2 = new TGDGSat(TGD.create(new Atom[] { OWL1 }, new Atom[] { OWL2, OWL3 }));

		assertEquals(tgd1, tgd2);
		assertTrue(tgd1.equals(tgd2));

		GSat gsat = GSat.getInstance();
		Variable x1 = Variable.create(gsat.uVariable + "1");
		Variable y1 = Variable.create(gsat.eVariable + "1");

		Atom Px1 = Atom.create(Predicate.create("P", 1), x1);
		Atom Rx1y1 = Atom.create(Predicate.create("R", 2), x1, y1);
		Atom Sy1 = Atom.create(Predicate.create("S", 1), y1);
		Atom Sx1 = Atom.create(Predicate.create("S", 1), x1);

		Collection<TGDGSat> expected1 = new HashSet<>();
		Atom Ty1 = Atom.create(Predicate.create("T", 1), y1);
		expected1.add(new TGDGSat(new Atom[] { Px1, Sx1 }, new Atom[] { Rx1y1, Sy1, Ty1 }));
		Collection<TGDGSat> expected2 = new HashSet<>();
		expected2.add(new TGDGSat(new Atom[] { Px1, Sx1 }, new Atom[] { Sy1, Ty1, Rx1y1 }));

		// System.out.println(expected1);
		// System.out.println(expected2);
		// assertEquals(expected1, expected2);

		assertEquals(new TGDGSat(new Atom[] { Px1, Sx1 }, new Atom[] { Rx1y1, Sy1, Ty1 }),
				new TGDGSat(new Atom[] { Px1, Sx1 }, new Atom[] { Sy1, Ty1, Rx1y1 }));

	}

	@Test
	public void evolveNewTest() {

		System.out.println("Evolve (new) tests");
		GSat gsat = GSat.getInstance();
		Variable x1 = Variable.create(gsat.uVariable + "1");
		Variable x2 = Variable.create(gsat.uVariable + "2");
		Variable x3 = Variable.create(gsat.uVariable + "3");
		Variable y1 = Variable.create(gsat.eVariable + "1");
		Variable y2 = Variable.create(gsat.eVariable + "2");
		Constant c1 = UntypedConstant.create("c1");
		Constant c2 = UntypedConstant.create("c2");
		Constant c3 = UntypedConstant.create("c3");

		// 1. evolve a single new rule
		// P(x) -> ∃ y. R(x,y) & S(y)
		// R(x1,x2) & S(x1) -> T(x2)
		Atom Px1 = Atom.create(Predicate.create("P", 1), x1);
		Atom Rx1y1 = Atom.create(Predicate.create("R", 2), x1, y1);
		Atom Sy1 = Atom.create(Predicate.create("S", 1), y1);
		TGD nonFull = TGD.create(new Atom[] { Px1 }, new Atom[] { Rx1y1, Sy1 });
		Atom Rx1x2 = Atom.create(Predicate.create("R", 2), x1, x2);
		Atom Sx1 = Atom.create(Predicate.create("S", 1), x1);
		Atom Tx2 = Atom.create(Predicate.create("T", 1), x2);
		TGD full = TGD.create(new Atom[] { Rx1x2, Sx1 }, new Atom[] { Tx2 });
		Collection<TGDGSat> evolved = gsat.evolveNew(nonFull, full);
		Collection<TGDGSat> expected = new HashSet<>();
		Atom Ty1 = Atom.create(Predicate.create("T", 1), y1);
		expected.add(new TGDGSat(new Atom[] { Px1, Sx1 }, new Atom[] { Rx1y1, Sy1, Ty1 }));
		checkEvolveNewTest(nonFull, full, expected, evolved);

		// 2. evolve multiple rules
		// P(x1) -> ∃ y. R(x1,y1) & R(y1,x1)
		// R(x1,x2) -> S(x1)
		Atom Ry1x1 = Atom.create(Predicate.create("R", 2), y1, x1);
		nonFull = TGD.create(new Atom[] { Px1 }, new Atom[] { Rx1y1, Ry1x1 });
		full = TGD.create(new Atom[] { Rx1x2 }, new Atom[] { Sx1 });
		evolved = gsat.evolveNew(nonFull, full);
		expected = new HashSet<>();
		expected.add(new TGDGSat(nonFull));
		expected.add(new TGDGSat(new Atom[] { Px1 }, new Atom[] { Sx1 }));
		expected.add(new TGDGSat(new Atom[] { Px1 }, new Atom[] { Rx1y1, Ry1x1, Sy1 }));
		checkEvolveNewTest(nonFull, full, expected, evolved);

		// 3. do not evolve for T(x2) will contain an existential and no matching head
		// atom
		// P(x1) -> ∃ y. R(x1,y1)
		// R(x1,x2) & T(x2) -> S(x1)
		nonFull = TGD.create(new Atom[] { Px1 }, new Atom[] { Rx1y1 });
		full = TGD.create(new Atom[] { Rx1x2, Tx2 }, new Atom[] { Sx1 });
		evolved = gsat.evolveNew(nonFull, full);
		expected = new HashSet<>();
		checkEvolveNewTest(nonFull, full, expected, evolved);

		// 4. evolve two head atoms with matching predicates, but only one unifies
		// P(x1) -> ∃ y. R(x1,y1) & R(y1,y1)
		// R(x1,x1) -> S(x1)
		Atom Ry1y1 = Atom.create(Predicate.create("R", 2), y1, y1);
		nonFull = TGD.create(new Atom[] { Px1 }, new Atom[] { Rx1y1, Ry1y1 });
		Atom Rx1x1 = Atom.create(Predicate.create("R", 2), x1, x1);
		full = TGD.create(new Atom[] { Rx1x1 }, new Atom[] { Sx1 });
		evolved = gsat.evolveNew(nonFull, full);
		expected = new HashSet<>();
		expected.add(new TGDGSat(new Atom[] { Px1 }, new Atom[] { Rx1y1, Ry1y1, Sy1 }));
		checkEvolveNewTest(nonFull, full, expected, evolved);

		// 5. non-guard T(x2) only matches in case guard is matched with R(x1,y1)
		// P(x1) -> ∃ y. R(x1,y1) & R(x1,y2) & T(y1)
		// R(x1,x2) & T(x2) -> S(x1)
		Atom Rx1y2 = Atom.create(Predicate.create("R", 2), x1, y2);
		nonFull = TGD.create(new Atom[] { Px1 }, new Atom[] { Rx1y1, Rx1y2, Ty1 });
		full = TGD.create(new Atom[] { Rx1x2, Tx2 }, new Atom[] { Sx1 });
		evolved = gsat.evolveNew(nonFull, full);
		expected = new HashSet<>();
		expected.add(new TGDGSat(nonFull));
		Atom Ty2 = Atom.create(Predicate.create("T", 1), y2);
		TGD nonFull2 = TGD.create(new Atom[] { Px1 }, new Atom[] { Rx1y1, Rx1y2, Ty2 }); // to avoid errors
		expected.add(new TGDGSat(nonFull2));
		expected.add(new TGDGSat(TGD.create(new Atom[] { Px1 }, new Atom[] { Sx1 })));
		checkEvolveNewTest(nonFull, full, expected, evolved);

		// 6. Rename universal variables in non-full rule
		// R(x1,x2) -> ∃ y. U(x1,y1,x2)
		// U(x1,x2,x1) -> T(x2)
		Atom Ux1y1x2 = Atom.create(Predicate.create("U", 3), x1, y1, x2);
		nonFull = TGD.create(new Atom[] { Rx1x2 }, new Atom[] { Ux1y1x2 });
		Atom Ux1x2x1 = Atom.create(Predicate.create("U", 3), x1, x2, x1);
		full = TGD.create(new Atom[] { Ux1x2x1 }, new Atom[] { Tx2 });
		evolved = gsat.evolveNew(nonFull, full);
		expected = new HashSet<>();
		Atom Ux1y1x1 = Atom.create(Predicate.create("U", 3), x1, y1, x1);
		expected.add(new TGDGSat(TGD.create(new Atom[] { Rx1x1 }, new Atom[] { Ux1y1x1, Ty1 })));
		checkEvolveNewTest(nonFull, full, expected, evolved);

		// 7. Rename a variable when unifying the third atoms when it was already
		// renamed while unifying the second atoms
		// U(x1,x2,x3) -> \exists y. U(x1,y1,x3) & R (x2,y1) & T(y1,x2)
		// U(x1,x2,x3) & R(x1,x2) & T(x2,x3) -> S(x1)
		Atom Ux1x2x3 = Atom.create(Predicate.create("U", 3), x1, x2, x3);
		Atom Ux1y1x3 = Atom.create(Predicate.create("U", 3), x1, y1, x3);
		Atom Rx2y1 = Atom.create(Predicate.create("R", 2), x2, y1);
		Atom Ty1x2 = Atom.create(Predicate.create("T", 2), y1, x2);
		nonFull = TGD.create(new Atom[] { Ux1x2x3 }, new Atom[] { Ux1y1x3, Rx2y1, Ty1x2 });
		Atom Tx2x3 = Atom.create(Predicate.create("T", 2), x2, x3);
		full = TGD.create(new Atom[] { Ux1x2x3, Rx1x2, Tx2x3 }, new Atom[] { Sx1 });
		evolved = gsat.evolveNew(nonFull, full);
		expected = new HashSet<>();
		Atom Ux1x1x1 = Atom.create(Predicate.create("U", 3), x1, x1, x1);
		Atom Ty1x1 = Atom.create(Predicate.create("T", 2), y1, x1);
		expected.add(new TGDGSat(TGD.create(new Atom[] { Ux1x1x1 }, new Atom[] { Ux1y1x1, Rx1y1, Ty1x1 })));
		expected.add(new TGDGSat(TGD.create(new Atom[] { Ux1x1x1 }, new Atom[] { Sx1 })));
		checkEvolveNewTest(nonFull, full, expected, evolved);

		// 8. Remove atoms from the head if they are occuring in the body
		// S(x1) -> \exists y. R(x1,y1)
		// R(x1,x2) -> S(x1)
		nonFull = TGD.create(new Atom[] { Sx1 }, new Atom[] { Rx1y1, });
		full = TGD.create(new Atom[] { Rx1x2 }, new Atom[] { Sx1 });
		evolved = gsat.evolveNew(nonFull, full);
		expected = new HashSet<>();
		expected.add(new TGDGSat(nonFull));
		checkEvolveNewTest(nonFull, full, expected, evolved);

		// 9: Evolve with constants
		// R(x1,c1) → ∃ y1,y2 U(c2,x1,y1) ∧ R(x1,y1)
		// U(x1,c3,x2) → P(x1)
		Atom Rx1c1 = Atom.create(Predicate.create("R", 2), x1, c1);
		Atom Uc2x1y1 = Atom.create(Predicate.create("U", 3), c2, x1, y1);
		nonFull = TGD.create(new Atom[] { Rx1c1 }, new Atom[] { Uc2x1y1, Rx1y1 });
		Atom Ux1c3x2 = Atom.create(Predicate.create("U", 3), x1, c3, x2);
		full = TGD.create(new Atom[] { Ux1c3x2 }, new Atom[] { Px1 });
		evolved = gsat.evolveNew(nonFull, full);
		expected = new HashSet<>();
		Atom Rc3c1 = Atom.create(Predicate.create("R", 2), c3, c1);
		Atom Rc3y1 = Atom.create(Predicate.create("R", 2), c3, y1);
		Atom Uc2c3y1 = Atom.create(Predicate.create("U", 3), c2, c3, y1);
		expected.add(new TGDGSat(TGD.create(new Atom[] { Rc3c1 }, new Atom[] { Uc2c3y1, Rc3y1 })));
		Atom Pc2 = Atom.create(Predicate.create("P", 1), c2);
		expected.add(new TGDGSat(TGD.create(new Atom[] { Rc3c1 }, new Atom[] { Pc2 })));
		checkEvolveNewTest(nonFull, full, expected, evolved);

		// 10: Evolve no rule for c1 cannot be unified with y1
		// R(x1,c1) → ∃ y1,y2 R(c3,y1)
		// R(x1,c1) → P(x1)
		nonFull = TGD.create(new Atom[] { Rx1c1 }, new Atom[] { Rc3y1 });
		full = TGD.create(new Atom[] { Rx1c1 }, new Atom[] { Px1 });
		evolved = gsat.evolveNew(nonFull, full);
		expected = new HashSet<>();
		checkEvolveNewTest(nonFull, full, expected, evolved);

		// 11: Evolve no rule for c1 cannot be unified with c3
		// R(x1,c1) → ∃ y1,y2 R(c3,y1)
		// R(c1,x1) → P(x1)
		nonFull = TGD.create(new Atom[] { Rx1c1 }, new Atom[] { Rc3y1 });
		Atom Rc1x1 = Atom.create(Predicate.create("R", 2), c1, x1);
		full = TGD.create(new Atom[] { Rc1x1 }, new Atom[] { Px1 });
		evolved = gsat.evolveNew(nonFull, full);
		expected = new HashSet<>();
		checkEvolveNewTest(nonFull, full, expected, evolved);

		// 12: Evolve no rule for x3 will be mapped to y1, hence R(c1,x3) needs to be
		// unified with R(c3,y1), but c1 and c3 cannot be unified.
		// R(x1,c1) → ∃ y1 U(x1,x2,y1) & R(c3,y1)
		// U(x1,x2,x3) & R(c1,x3) → P(x1)
		Atom Uc1x2y1 = Atom.create(Predicate.create("U", 3), c1, x2, y1);
		nonFull = TGD.create(new Atom[] { Rx1c1 }, new Atom[] { Uc1x2y1, Rc3y1 });
		Atom Rc1x3 = Atom.create(Predicate.create("R", 2), c1, x3);
		full = TGD.create(new Atom[] { Ux1x2x3, Rc1x3 }, new Atom[] { Px1 });
		evolved = gsat.evolveNew(nonFull, full);
		expected = new HashSet<>();
		checkEvolveNewTest(nonFull, full, expected, evolved);

		// 13: Evolve no rule for x3 will be mapped to y1, hence R(x1,x3) & T(x1,x3)
		// need to be unified with R(c3,y1) & T(c1,y1),
		// but x1 cannot be mapped to c3 and c1.
		// S(x1) → ∃ y1 U(x1,x2,y1) & R(c3,y1) & T(c1,y1)
		// U(x1,x2,x3) & R(x1,x3) & T(x1,x3) → P(x1)
		Atom Ux1x2y1 = Atom.create(Predicate.create("U", 3), x1, x2, y1);
		Atom Tc1y1 = Atom.create(Predicate.create("T", 2), c1, y1);
		nonFull = TGD.create(new Atom[] { Sx1 }, new Atom[] { Ux1x2y1, Rc3y1, Tc1y1 });
		Atom Rx1x3 = Atom.create(Predicate.create("R", 2), x1, x3);
		Atom Tx1x3 = Atom.create(Predicate.create("T", 2), x1, x3);
		full = TGD.create(new Atom[] { Ux1x2x3, Rx1x3, Tx1x3 }, new Atom[] { Px1 });
		evolved = gsat.evolveNew(nonFull, full);
		expected = new HashSet<>();
		checkEvolveNewTest(nonFull, full, expected, evolved);

		// 14: Derive with a rule containing bottom. Remove all head atoms in the
		// resulting rule.
		// S(x1) -> \exists y. R(x1,y1)
		// R(x1,x2) -> P(x1) & ⊥
		nonFull = TGD.create(new Atom[] { Sx1 }, new Atom[] { Rx1y1, });
		full = TGD.create(new Atom[] { Rx1x2 }, new Atom[] { Px1, TGDGSat.Bottom });
		evolved = gsat.evolveNew(nonFull, full);
		expected = new HashSet<>();
		expected.add(new TGDGSat(TGD.create(new Atom[] { Sx1 }, new Atom[] { TGDGSat.Bottom })));
		checkEvolveNewTest(nonFull, full, expected, evolved);
	}

	private void checkEvolveNewTest(TGD nonFull, TGD full, Collection<TGDGSat> expected, Collection<TGDGSat> result) {
		System.out.println("Non-Full rule: " + nonFull);
		System.out.println("Full rule:     " + full);
		System.out.println("Evolved rules:  " + result);
		System.out.println("Expected rules: " + expected);

		// if (expected.size() != result.size())
		// fail("checkEvolveNewTest used in the wrong way...");
		//
		// Iterator<TGDGSat> iteratorE = expected.iterator();
		// while (iteratorE.hasNext()) {
		// TGDGSat nextE = iteratorE.next();
		// Iterator<TGDGSat> iteratorR = result.iterator();
		// boolean found = false;
		// while (iteratorR.hasNext())
		// if (nextE.equals(iteratorR.next()))
		// found = true;
		// assertTrue(found, "Evolved wrong rules: " + nextE);
		// }
		Iterator<TGDGSat> iteratorR = result.iterator();
		while (iteratorR.hasNext()) {
			TGDGSat nextR = iteratorR.next();
			Iterator<TGDGSat> iteratorE = expected.iterator();
			boolean found = false;
			while (iteratorE.hasNext())
				if (nextR.equals(iteratorE.next()))
					found = true;
			assertTrue(found, "Evolved wrong rules: " + nextR);
		}

	}

	// @Test
	// public void MGUTest() {

	// System.out.println("MGU tests:");

	// // ****** MGU tests: *******
	// GSat gsat = GSat.getInstance();
	// Variable x1 = Variable.create(gsat.uVariable + "1");
	// Variable y1 = Variable.create(gsat.eVariable + "1");
	// Variable y2 = Variable.create(gsat.eVariable + "2");
	// Variable z1 = Variable.create(gsat.zVariable + "1");

	// // 3.
	// Atom Rx = Atom.create(Predicate.create("R", 2), x1, y1);
	// Atom Rz = Atom.create(Predicate.create("R", 2), z1, z1);
	// // expect null since x1 is mapped to y1, conflicting with the evc
	// checkMGUTest(null, GSat.getInstance().getGuardMGU(Rz, Rx));

	// // 4.
	// Rx = Atom.create(Predicate.create("R", 2), y1, y2);
	// Rz = Atom.create(Predicate.create("R", 2), z1, z1);
	// // expect null since z1 can't be mapped to both, y1 and y2 (y1 and y2 need to
	// // stay constant)
	// checkMGUTest(null, GSat.getInstance().getGuardMGU(Rz, Rx));

	// }

	// private void checkMGUTest(Map<Term, Term> expected, Map<Term, Term> result) {
	// System.out.print("Returned MGU: ");
	// System.out.println(result);
	// System.out.print("Expected MGU: ");
	// System.out.println(expected + "\n");
	// assertEquals(expected, result, "Computed wrong MGU.");
	// }

}
