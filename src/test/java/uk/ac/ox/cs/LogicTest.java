package uk.ac.ox.cs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import uk.ac.ox.cs.gsat.Logic;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.TGD;
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

	@Test
	public void applySubstitutionTest() {
		// TODO
	}

	@Test
	public void containsAnyTest() {
		// TODO
	}

	@Test
	public void isFullTest() {

		// ∀ x2,x1,x3 B(x2,x1,x3) → ∃ z1,y1,y2 H1(x1,z1,y1,y2) & H2(y1,y2)
		TGD tgd = TGD.create(new Atom[] { B_x2x1x3 }, new Atom[] { H1_x1z1y1y2, H2_y1y2 });
		System.out.println("TGD: " + tgd);
		assertFalse("This is a 'non-full' TGD", Logic.isFull(tgd));

		// ∀ u1,u2,u3 B(u1,u2,u3) → ∃ e1,e2,e3 H1(u2,e1,e2,e3) & H2(e2,e3)
		tgd = TGD.create(new Atom[] { b_u1u2u3 }, new Atom[] { h1_u2e1e2e3, h2_e2e3 });
		System.out.println("TGD: " + tgd);
		assertFalse("This is a 'non-full' TGD", Logic.isFull(tgd));

		// ∀ x1,x2 B(x1,x2) → ∃ y1 H1(x1,y1) ∧ H2(x2)
		tgd = TGD.create(new Atom[] { B_x1x2 }, new Atom[] { H1_x1y1, H_x2 });
		System.out.println("TGD: " + tgd);
		assertFalse("This is a 'non-full' TGD", Logic.isFull(tgd));

		// ∀ x1,x2 B(x1,x2) → ∃ y1 H1(x1,y1)
		tgd = TGD.create(new Atom[] { B_x1x2 }, new Atom[] { H1_x1y1 });
		System.out.println("TGD: " + tgd);
		assertFalse("This is a 'non-full' TGD", Logic.isFull(tgd));

		// ∀ x1,x2 B(x1,x2) → H2(x2)
		tgd = TGD.create(new Atom[] { B_x1x2 }, new Atom[] { H_x2 });
		System.out.println("TGD: " + tgd);
		assertTrue("This is a 'full' TGD", Logic.isFull(tgd));

		// ∀ x1 R(x1) → ∃ y1,y2 T(x1,y1,y2)
		TGD t1 = TGD.create(new Atom[] { R_x1 }, new Atom[] { T_x1y1y2 });
		// ∀ x1,x2,x3 T(x1,x2,x3) → ∃ y U(x1,x2,y)
		TGD t2 = TGD.create(new Atom[] { T_x1x2x3 }, new Atom[] { U_x1x2y });
		// ∀ x1,x2,x3 U(x1,x2,x3) → P(x1) ∧ V(x1,x2)
		TGD t3 = TGD.create(new Atom[] { U_x1x2x3 }, new Atom[] { P_x1, V_x1x2 });
		// ∀ x1,x2,x3 T(x1,x2,x3) ∧ V(x1,x2) ∧ S(x1) → M(x1)
		TGD t4 = TGD.create(new Atom[] { T_x1x2x3, V_x1x2, S_x1 }, new Atom[] { M_x1 });

		System.out.println("TGD: " + t1);
		assertFalse("This is a 'non-full' TGD", Logic.isFull(t1));
		System.out.println("TGD: " + t2);
		assertFalse("This is a 'non-full' TGD", Logic.isFull(t2));
		System.out.println("TGD: " + t3);
		assertTrue("This is a 'full' TGD", Logic.isFull(t3));
		System.out.println("TGD: " + t4);
		assertTrue("This is a 'full' TGD", Logic.isFull(t4));

		// ∀ x1,x2 R(x1,x2) → ∃ y1,y2 S(x1,x2,y1,y2) ∧ T(x1,x2,y2)
		t1 = TGD.create(new Atom[] { R_x1x2 }, new Atom[] { S_x1x2y1y2, T_x1x2y2 });
		// ∀ x1,x2,x3,x4 S(x1,x2,x3,x4) → U(x4)
		t2 = TGD.create(new Atom[] { S_x1x2x3x4 }, new Atom[] { U_x4 });
		// ∀ z1,z2,z3 T(z1,z2,z3) ∧ U(z3) → P(z1)
		t3 = TGD.create(new Atom[] { T_z1z2z3, U_z3 }, new Atom[] { P_z1 });
		System.out.println("TGD: " + t1);
		assertFalse("This is a 'non-full' TGD", Logic.isFull(t1));
		System.out.println("TGD: " + t2);
		assertTrue("This is a 'full' TGD", Logic.isFull(t2));
		System.out.println("TGD: " + t3);
		assertTrue("This is a 'full' TGD", Logic.isFull(t3));

		// ∀ u1,u2,u3 U(u1,u2,u3) → ∃ z1,z2,z3 U(z1,z2,z3)
		tgd = TGD.create(new Atom[] { U_u1u2u3 }, new Atom[] { U_z1z2z3 });
		System.out.println("TGD: " + tgd);
		assertFalse("This is a 'non-full' TGD", Logic.isFull(tgd));

	}

}