import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Test;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * Unit tests for the GSat class
 * 
 * @author Stefano
 */
public class GSatTest {

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

	@Test
	public void runGSatTest() {

		/**
		 * Example 1
		 */
		// ∀ x1 R(x1) → ∃ y1,y2 T(x1,y1,y2)
		// ∀ x1,x2,x3 T(x1,x2,x3) → ∃ y U(x1,x2,y)
		// ∀ x1,x2,x3 U(x1,x2,x3) → P(x1) ∧ V(x1,x2)
		// ∀ x1,x2,x3 T(x1,x2,x3) ∧ V(x1,x2) ∧ S(x1) → M(x1)

		TGD t1 = TGD.create(new Atom[] { R_x1 }, new Atom[] { T_x1y1y2 });
		TGD t2 = TGD.create(new Atom[] { T_x1x2x3 }, new Atom[] { U_x1x2y });
		TGD t3 = TGD.create(new Atom[] { U_x1x2x3 }, new Atom[] { P_x1, V_x1x2 });
		TGD t4 = TGD.create(new Atom[] { T_x1x2x3, V_x1x2, S_x1 }, new Atom[] { M_x1 });

		Collection<TGD> allTGDs = new LinkedList<>();
		allTGDs.add(t1);
		allTGDs.add(t2);
		allTGDs.add(t3);
		allTGDs.add(t4);
		System.out.println("Initial rules:");
		allTGDs.forEach(System.out::println);

		Collection<TGD> guardedSaturation = GSat.runGSat(allTGDs.toArray(new TGD[allTGDs.size()]));

		System.out.println("Guarded saturation:");
		guardedSaturation.forEach(System.out::println);
		// ∀ u1,u2,u3 T(u1,u2,u3) → P(u1) ∧ V(u1,u2)
		// ∀ u1 R(u1) ∧ S(u1) → P(u1)
		// ∀ u1,u2,u3 T(u1,u2,u3) ∧ V(u1,u2) ∧ S(u1) → M(u1)
		// ∀ u1 R(u1) → P(u1)
		// ∀ u1,u2,u3 U(u1,u2,u3) → P(u1) ∧ V(u1,u2)
		// ∀ u1 R(u1) ∧ S(u1) → M(u1)

		assertEquals(6, guardedSaturation.size());

		// TGD tgdExpected = TGD.create(bodyE, headE);
		// assertTrue(guardedSaturation.stream().anyMatch(tgdExpected));

		/**
		 * Example 2
		 */
		// ∀ x1,x2 R(x1,x2) → ∃ y1,y2 S(x1,x2,y1,y2) ∧ T(x1,x2,y2)
		// ∀ x1,x2,x3,x4 S(x1,x2,x3,x4) → U(x4)
		// ∀ z1,z2,z3 T(z1,z2,z3) ∧ U(z3) → P(z1)

		t1 = TGD.create(new Atom[] { R_x1x2 }, new Atom[] { S_x1x2y1y2, T_x1x2y2 });
		t2 = TGD.create(new Atom[] { S_x1x2x3x4 }, new Atom[] { U_x4 });
		t3 = TGD.create(new Atom[] { T_z1z2z3, U_z3 }, new Atom[] { P_z1 });

		allTGDs = new LinkedList<>();
		allTGDs.add(t1);
		allTGDs.add(t2);
		allTGDs.add(t3);
		System.out.println("Initial rules:");
		allTGDs.forEach(System.out::println);

		guardedSaturation = GSat.runGSat(allTGDs.toArray(new TGD[allTGDs.size()]));

		System.out.println("Guarded saturation:");
		guardedSaturation.forEach(System.out::println);
		// ∀ u1,u2,u3 T(u1,u2,u3) ∧ U(u3) → P(u1)
		// ∀ u1,u2,u3,u4 S(u1,u2,u3,u4) → U(u4)
		// ∀ u1,u2 R(u1,u2) → P(u1)

		assertEquals(3, guardedSaturation.size());

	}

	@Test
	public void HNFTest() {

		// ∀ x1,x2 B(x1,x2) → ∃ y1 H1(x1,y1) ∧ H2(x2)
		TGD tgd = TGD.create(new Atom[] { B_x1x2 }, new Atom[] { H1_x1y1, H_x2 });
		System.out.println("Original TGD: " + tgd);

		Collection<TGD> tgdsHNF = GSat.HNF(tgd);
		System.out.println("TGDs in HNF:");
		tgdsHNF.forEach(System.out::println);

		// ∀ x1,x2 B(x1,x2) → ∃ y1 H1(x1,y1)
		// ∀ x1,x2 B(x1,x2) → H2(x2)
		Atom[] bodyE = { B_x1x2 };
		Atom[] headE1 = { H1_x1y1 };
		Atom[] headE2 = { H_x2 };
		TGD tgdExpected1 = TGD.create(bodyE, headE1);
		TGD tgdExpected2 = TGD.create(bodyE, headE2);
		Collection<TGD> tgdsExpected = new LinkedList<>();
		tgdsExpected.add(tgdExpected1);
		tgdsExpected.add(tgdExpected2);

		assertEquals(tgdsExpected, tgdsHNF);

	}

	@Test
	public void VNFsTest() {

		// ∀ x2,x1,x3 B(x2,x1,x3) → ∃ z1,y1,y2 H1(x1,z1,y1,y2) & H2(y1,y2)
		TGD tgd = TGD.create(new Atom[] { B_x2x1x3 }, new Atom[] { H1_x1z1y1y2, H2_y1y2 });
		System.out.println("Original TGD: " + tgd);

		Collection<TGD> tgdsVNFs = GSat.VNFs(Arrays.asList(tgd));
		System.out.println("TGDs in VNFs: " + tgdsVNFs);

		// ∀ u1,u2,u3 B(u1,u2,u3) → ∃ e1,e2,e3 H1(u2,e1,e2,e3) & H2(e2,e3)
		TGD tgdExpected = TGD.create(new Atom[] { b_u1u2u3 }, new Atom[] { h1_u2e1e2e3, h2_e2e3 });

		assertEquals(1, tgdsVNFs.size());
		assertTrue(tgdsVNFs.contains(tgdExpected));

	}

	@Test
	public void VNFTest() {

		// ∀ x2,x1,x3 B(x2,x1,x3) → ∃ z1,y1,y2 H1(x1,z1,y1,y2) & H2(y1,y2)
		TGD tgd = TGD.create(new Atom[] { B_x2x1x3 }, new Atom[] { H1_x1z1y1y2, H2_y1y2 });
		System.out.println("Original TGD: " + tgd);

		TGD tgdVNF = GSat.VNF(tgd);
		System.out.println("TGD in VNF: " + tgdVNF);

		// ∀ u1,u2,u3 B(u1,u2,u3) → ∃ e1,e2,e3 H1(u2,e1,e2,e3) & H2(e2,e3)
		TGD tgdExpected = TGD.create(new Atom[] { b_u1u2u3 }, new Atom[] { h1_u2e1e2e3, h2_e2e3 });

		assertEquals(tgdExpected, tgdVNF);

	}

	@Test
	public void evolveTest() {
		// TODO
	}

	@Test
	public void evolveRenameTest() {
		// TODO
	}

	@Test
	public void getJoinAtomsTest() {
		// TODO
	}

	@Test
	public void getEvolveRuleTest() {
		// TODO
	}

	@Test
	public void getMGUTest() {
		Collection<Variable> existentials = new LinkedList<>();
		existentials.add(Variable.create("e1"));

		Map<Term, Term> mgu = GSat.getMGU(new Atom[] { U_u1u2u3 }, new Atom[] { U_z1z2z3 }, Arrays.asList(U_z1z2z3),
				existentials);

		System.out.println(mgu);

		assertNotNull(mgu);

	}

	@Test
	public void applyMGU() {
		// TODO
	}

}
