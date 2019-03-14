import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Test;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.UntypedConstant;
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

	private static final Atom IMP_xy = Atom.create(Predicate.create("IMP", 2), Variable.create("x"),
			Variable.create("y"));
	private static final Atom DIP_yz = Atom.create(Predicate.create("DIP", 2), Variable.create("y"),
			Variable.create("z"));
	private static final Atom DIP_xy = Atom.create(Predicate.create("DIP", 2), Variable.create("x"),
			Variable.create("y"));
	private static final Atom IMP_yw = Atom.create(Predicate.create("IMP", 2), Variable.create("y"),
			Variable.create("w"));
	private static final Atom Q1_x = Atom.create(Predicate.create("Q1", 1), Variable.create("x"));
	private static final Atom r1_xy = Atom.create(Predicate.create("r1", 2), Variable.create("x"),
			Variable.create("y"));
	private static final Atom r2_y = Atom.create(Predicate.create("r2", 1), Variable.create("y"));
	private static final Atom r1_zx = Atom.create(Predicate.create("r1", 2), Variable.create("z"),
			Variable.create("x"));
	private static final Atom r2_x = Atom.create(Predicate.create("r2", 1), Variable.create("x"));
	private static final Atom Parent_x = Atom.create(Predicate.create("Parent", 1), Variable.create("x"));
	private static final Atom anon2_x = Atom.create(Predicate.create("anon2", 1), Variable.create("x"));
	private static final Atom Person_x = Atom.create(Predicate.create("Person", 1), Variable.create("x"));
	private static final Atom anon1_x = Atom.create(Predicate.create("anon1", 1), Variable.create("x"));
	private static final Atom hasSon_xy = Atom.create(Predicate.create("hasSon", 2), Variable.create("x"),
			Variable.create("y"));
	private static final Atom Person_y = Atom.create(Predicate.create("Person", 1), Variable.create("y"));

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
		// ∀ x1,x2,x3 T(x1,x2,x3) → ∃ y U(x1,x2,y)
		// ∀ x1,x2,x3 U(x1,x2,x3) → P(x1) ∧ V(x1,x2)
		// ∀ x1,x2,x3 T(x1,x2,x3) ∧ V(x1,x2) ∧ S(x1) → M(x1)
		TGD t1 = TGD.create(new Atom[] { R_x1 }, new Atom[] { T_x1y1y2 });
		TGD t2 = TGD.create(new Atom[] { T_x1x2x3 }, new Atom[] { U_x1x2y });
		TGD t3 = TGD.create(new Atom[] { U_x1x2x3 }, new Atom[] { P_x1, V_x1x2 });
		TGD t4 = TGD.create(new Atom[] { T_x1x2x3, V_x1x2, S_x1 }, new Atom[] { M_x1 });

		System.out.println("TGD: " + t1);
		assertFalse("This is a 'non-full' TGD", Logic.isFull(t1));
		System.out.println("TGD: " + t2);
		assertFalse("This is a 'non-full' TGD", Logic.isFull(t2));
		System.out.println("TGD: " + t3);
		assertTrue("This is a 'full' TGD", Logic.isFull(t3));
		System.out.println("TGD: " + t4);
		assertTrue("This is a 'full' TGD", Logic.isFull(t4));

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

	@Test
	public void fromIRISPM_guardedCarlo() {
		// guardedCarlo.dtg

		// DIP(?y, ?z) :- IMP(?x, ?y).
		// IMP(?y, ?w) :- DIP(?x, ?y).

		// Q1(?x) :- DIP (?x, ?y).

		// DIP('a', 'b').

		// ?- Q1(?x).

		Collection<TGD> allTGDs = new LinkedList<>();
		allTGDs.add(TGD.create(new Atom[] { IMP_xy }, new Atom[] { DIP_yz }));
		allTGDs.add(TGD.create(new Atom[] { DIP_xy }, new Atom[] { IMP_yw }));
		allTGDs.add(TGD.create(new Atom[] { DIP_xy }, new Atom[] { Q1_x }));

		Collection<Atom> allFacts = new LinkedList<>();
		allFacts.add(Atom.create(Predicate.create("DIP", 2), UntypedConstant.create("a"), UntypedConstant.create("b")));

		Collection<ConjunctiveQuery> allQueries = new LinkedList<>();
		allQueries.add(ConjunctiveQuery.create(new Variable[] { Variable.create("x") }, new Atom[] { Q1_x }));

		fromIRISPM(allTGDs, allFacts, allQueries, 2);

	}

	@Test
	public void fromIRISPM_guardedExample() {
		// guardedExample.dtg

		// r1(?z, ?x) :- r1(?x, ?y), r2(?y).
		// r2(?x) :- r1(?x, ?y).

		// r1('a', 'b').
		// r2('b').

		// ?- r1(?x, ?y).
		// ?- r2(?x).

		Collection<TGD> allTGDs = new LinkedList<>();
		allTGDs.add(TGD.create(new Atom[] { r1_xy, r2_y }, new Atom[] { r1_zx }));
		allTGDs.add(TGD.create(new Atom[] { r1_xy }, new Atom[] { r2_x }));

		Collection<Atom> allFacts = new LinkedList<>();
		allFacts.add(Atom.create(Predicate.create("r1", 2), UntypedConstant.create("a"), UntypedConstant.create("b")));
		allFacts.add(Atom.create(Predicate.create("r2", 1), UntypedConstant.create("b")));

		Collection<ConjunctiveQuery> allQueries = new LinkedList<>();
		allQueries.add(ConjunctiveQuery.create(new Variable[] { Variable.create("x"), Variable.create("y") },
				new Atom[] { r1_xy }));
		allQueries.add(ConjunctiveQuery.create(new Variable[] { Variable.create("x") }, new Atom[] { r2_x }));

		fromIRISPM(allTGDs, allFacts, allQueries, 1);

	}

	@Test
	public void fromIRISPM_guarded_owl_nostorage() {
		// guarded_owl_nostorage.dtg

		// /// First Level Datalog Query or Program///
		// ?- Parent(?x), hasSon(?x, 'Giorgio').
		// ?- hasSon(?x, ?y).
		// ?- Parent(?x).
		// ?- Person(?x).

		// /// ABox ///
		// Person('Ermanna').
		// Parent('Lucia').
		// hasSon('Ermanna', 'Giorgio').
		// hasSon('Katia', 'Manuela').

		// /// TBox ///
		// anon2 (?x) :- Parent(?x).
		// Parent(?x) :- anon2(?x).

		// Person(?x) :- anon2(?x).
		// anon1(?x) :- anon2(?x).
		// anon2(?x) :- anon1(?x), Person(?x).

		// hasSon(?x, ?y) :- anon1(?x).
		// Person(?y) :- hasSon(?x, ?y), anon1(?x).
		// anon1(?x) :- hasSon(?x, ?y), Person(?y).

		// // Domain and Range //
		// Person(?x) :- hasSon(?x, ?y).
		// Person(?y) :- hasSon(?x, ?y).

		Collection<TGD> allTGDs = new LinkedList<>();
		allTGDs.add(TGD.create(new Atom[] { Parent_x }, new Atom[] { anon2_x }));
		allTGDs.add(TGD.create(new Atom[] { anon2_x }, new Atom[] { Parent_x }));
		allTGDs.add(TGD.create(new Atom[] { anon2_x }, new Atom[] { Person_x }));
		allTGDs.add(TGD.create(new Atom[] { anon2_x }, new Atom[] { anon1_x }));
		allTGDs.add(TGD.create(new Atom[] { anon1_x, Person_x }, new Atom[] { anon2_x }));
		allTGDs.add(TGD.create(new Atom[] { anon1_x }, new Atom[] { hasSon_xy }));
		allTGDs.add(TGD.create(new Atom[] { hasSon_xy, anon1_x }, new Atom[] { Person_x }));
		allTGDs.add(TGD.create(new Atom[] { hasSon_xy, Person_x }, new Atom[] { anon1_x }));
		allTGDs.add(TGD.create(new Atom[] { hasSon_xy }, new Atom[] { Person_x }));
		allTGDs.add(TGD.create(new Atom[] { hasSon_xy }, new Atom[] { Person_y }));

		Collection<Atom> allFacts = new LinkedList<>();
		allFacts.add(Atom.create(Predicate.create("Person", 1), UntypedConstant.create("Ermanna")));
		allFacts.add(Atom.create(Predicate.create("Parent", 1), UntypedConstant.create("Lucia")));
		allFacts.add(Atom.create(Predicate.create("hasSon", 2), UntypedConstant.create("Ermanna"),
				UntypedConstant.create("Giorgio")));
		allFacts.add(Atom.create(Predicate.create("hasSon", 2), UntypedConstant.create("Katia"),
				UntypedConstant.create("Manuela")));

		Collection<ConjunctiveQuery> allQueries = new LinkedList<>();
		allQueries.add(ConjunctiveQuery.create(new Variable[] { Variable.create("x") }, new Atom[] { Parent_x,
				Atom.create(Predicate.create("hasSon", 2), Variable.create("x"), UntypedConstant.create("Giorgio")) }));
		allQueries.add(ConjunctiveQuery.create(new Variable[] { Variable.create("x"), Variable.create("y") },
				new Atom[] { hasSon_xy }));
		allQueries.add(ConjunctiveQuery.create(new Variable[] { Variable.create("x") }, new Atom[] { Parent_x }));
		allQueries.add(ConjunctiveQuery.create(new Variable[] { Variable.create("x") }, new Atom[] { Person_x }));

		fromIRISPM(allTGDs, allFacts, allQueries, 12);

	}

	private void fromIRISPM(Collection<TGD> allTGDs, Collection<Atom> allFacts, Collection<ConjunctiveQuery> allQueries,
			int guardedSaturationSize) {
		System.out.println("Initial rules:");
		allTGDs.forEach(System.out::println);
		Collection<TGD> guardedSaturation = GSat.runGSat(allTGDs.toArray(new TGD[allTGDs.size()]));

		System.out.println("Guarded saturation:");
		guardedSaturation.forEach(System.out::println);

		assertEquals(guardedSaturationSize, guardedSaturation.size());

		System.out.println("Initial data:");
		allFacts.forEach(System.out::println);

		System.out.println("Initial queries:");
		allQueries.forEach(System.out::println);

		String baseOutputPath = "test" + File.separator + "UnitTests" + File.separator + "IRISPM" + File.separator;
		new File(baseOutputPath).mkdirs();
		try {
			IO.writeDatalogRules(guardedSaturation, baseOutputPath + "rules.rul");
			IO.writeDatalogFacts(allFacts, baseOutputPath + "facts.data");
			for (ConjunctiveQuery q : allQueries) {
				IO.writeDatalogQueries(Arrays.asList(q), baseOutputPath + "query.rul");
				SolverOutput output = Logic.invokeSolver(
						"executables" + File.separator + "idlv_1.1.2_windows_x86-64.exe", "--query",
						Arrays.asList(baseOutputPath + "rules.rul", baseOutputPath + "facts.data",
								baseOutputPath + "query.rul"));
				System.out.println(output);
			}
		} catch (IOException | InterruptedException e) {
			fail(e.getLocalizedMessage());
		}

	}

}
