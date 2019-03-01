import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

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
 * AppTest
 */
public class AppTest {

	@Test
	public void runGSatTest() {

		// (forall[x1](R(x1) --> (exists[y1,y2]T(x1,y1,y2))))
		// (forall[x1,x2,x3](T(x1,x2,x3) --> (exists[y]U(x1,x2,y))))
		// (forall[x1,x2,x3](U(x1,x2,x3) --> (P(x1) & V(x1,x2))))
		// (forall[x1,x2,x3]((T(x1,x2,x3) & (V(x1,x2) & S(x1))) --> M(x1)))
		TGD t1 = TGD.create(new Atom[] { Atom.create(Predicate.create("R", 1), Variable.create("x1")) },
				new Atom[] { Atom.create(Predicate.create("T", 3), Variable.create("x1"), Variable.create("y1"),
						Variable.create("y2")) });
		TGD t2 = TGD.create(
				new Atom[] { Atom.create(Predicate.create("T", 3), Variable.create("x1"), Variable.create("x2"),
						Variable.create("x3")) },
				new Atom[] { Atom.create(Predicate.create("U", 3), Variable.create("x1"), Variable.create("x2"),
						Variable.create("y")) });
		TGD t3 = TGD.create(
				new Atom[] { Atom.create(Predicate.create("U", 3), Variable.create("x1"), Variable.create("x2"),
						Variable.create("x3")) },
				new Atom[] { Atom.create(Predicate.create("P", 1), Variable.create("x1")),
						Atom.create(Predicate.create("V", 2), Variable.create("x1"), Variable.create("x2")) });
		TGD t4 = TGD.create(
				new Atom[] {
						Atom.create(Predicate.create("T", 3), Variable.create("x1"), Variable.create("x2"),
								Variable.create("x3")),
						Atom.create(Predicate.create("V", 2), Variable.create("x1"), Variable.create("x2")),
						Atom.create(Predicate.create("S", 1), Variable.create("x1")) },
				new Atom[] { Atom.create(Predicate.create("M", 1), Variable.create("x1")), });

		Collection<TGD> allTGDs = new LinkedList<>();
		allTGDs.add(t1);
		allTGDs.add(t2);
		allTGDs.add(t3);
		allTGDs.add(t4);
		System.out.println("Initial rules:");
		allTGDs.forEach(System.out::println);

		Collection<TGD> guardedSaturation = App
				.runGSat(allTGDs.toArray(new TGD[allTGDs.size()]));

		System.out.println("Guarded saturation:");
		guardedSaturation.forEach(System.out::println);
		// (forall[u1,u2,u3](T(u1,u2,u3) --> (P(u1) & V(u1,u2))))
		// (forall[u1]((R(u1) & S(u1)) --> P(u1)))
		// (forall[u1,u2,u3]((T(u1,u2,u3) & (V(u1,u2) & S(u1))) --> M(u1)))
		// (forall[u1](R(u1) --> P(u1)))
		// (forall[u1,u2,u3](U(u1,u2,u3) --> (P(u1) & V(u1,u2))))
		// (forall[u1]((R(u1) & S(u1)) --> M(u1)))

		assertEquals(6, guardedSaturation.size());

		// TGD tgdExpected = TGD.create(bodyE, headE);
		// assertTrue(guardedSaturation.stream().anyMatch(tgdExpected));

		// (forall[x1,x2](R(x1,x2) --> (exists[y1,y2](S(x1,x2,y1,y2) & T(x1,x2,y2)))))
		// (forall[x1,x2,x3,x4](S(x1,x2,x3,x4) --> U(x4)))
		// (forall[z1,z2,z3]((T(z1,z2,z3) & U(z3)) --> P(z1)))
		t1 = TGD.create(
				new Atom[] { Atom.create(Predicate.create("R", 2), Variable.create("x1"), Variable.create("x2")) },
				new Atom[] {
						Atom.create(Predicate.create("S", 4), Variable.create("x1"), Variable.create("x2"),
								Variable.create("y1"), Variable.create("y2")),
						Atom.create(Predicate.create("T", 3), Variable.create("x1"), Variable.create("x2"),
								Variable.create("y2")) });
		t2 = TGD.create(
				new Atom[] { Atom.create(Predicate.create("S", 4), Variable.create("x1"), Variable.create("x2"),
						Variable.create("x3"), Variable.create("x4")) },
				new Atom[] { Atom.create(Predicate.create("U", 1), Variable.create("x4")) });
		t3 = TGD.create(
				new Atom[] { Atom.create(Predicate.create("T", 3), Variable.create("z1"), Variable.create("z2"),
						Variable.create("z3")), Atom.create(Predicate.create("U", 1), Variable.create("z3")) },
				new Atom[] { Atom.create(Predicate.create("P", 1), Variable.create("z1")), });

		allTGDs = new LinkedList<>();
		allTGDs.add(t1);
		allTGDs.add(t2);
		allTGDs.add(t3);
		System.out.println("Initial rules:");
		allTGDs.forEach(System.out::println);

		guardedSaturation = App.runGSat(allTGDs.toArray(new TGD[allTGDs.size()]));

		System.out.println("Guarded saturation:");
		guardedSaturation.forEach(System.out::println);
		// (forall[u1,u2,u3]((T(u1,u2,u3) & U(u3)) --> P(u1)))
		// (forall[u1,u2,u3,u4](S(u1,u2,u3,u4) --> U(u4)))
		// (forall[u1,u2](R(u1,u2) --> P(u1)))

		assertEquals(3, guardedSaturation.size());

	}

	@Test
	public void HNFTest() {
		// ∀ x1,x2 B(x1,x2) → ∃ y1 H1(x1,y1) ∧ H2(x2)
		// (forall[x1,x2](B(x1,x2) --> (exists[y1](H1(x1,y1) & H2(x2)))))
		Atom[] body = { Atom.create(Predicate.create("B", 2), Variable.create("x1"), Variable.create("x2")) };
		Atom[] head = { Atom.create(Predicate.create("H1", 2), Variable.create("x1"), Variable.create("y1")),
				Atom.create(Predicate.create("H2", 1), Variable.create("x2")) };
		TGD tgd = TGD.create(body, head);
		System.out.println("Original TGD: " + tgd);

		Collection<TGD> tgdsHNF = App.HNF(tgd);
		System.out.println("TGDs in HNF:");
		tgdsHNF.forEach(System.out::println);

		// (forall[x1,x2](B(x1,x2) --> (exists[y1]H1(x1,y1))))
		// (forall[x1,x2](B(x1,x2) --> H2(x2)))
		Atom[] bodyE = { Atom.create(Predicate.create("B", 2), Variable.create("x1"), Variable.create("x2")) };
		Atom[] headE1 = { Atom.create(Predicate.create("H1", 2), Variable.create("x1"), Variable.create("y1")) };
		Atom[] headE2 = { Atom.create(Predicate.create("H2", 1), Variable.create("x2")) };
		TGD tgdExpected1 = TGD.create(bodyE, headE1);
		TGD tgdExpected2 = TGD.create(bodyE, headE2);
		Collection<TGD> tgdsExpected = new LinkedList<>();
		tgdsExpected.add(tgdExpected1);
		tgdsExpected.add(tgdExpected2);

		assertEquals(tgdsExpected, tgdsHNF);
	}

	@Test
	public void VNFTest() {
		// (forall[x2,x1,x3](B(x2,x1,x3) --> (exists[z1,y1,y2](H1(x1,z1,y1,y2) & H2(y1,y2)))))
		Atom[] body = { Atom.create(Predicate.create("B", 3), Variable.create("x2"), Variable.create("x1"),
				Variable.create("x3")) };
		Atom[] head = {
				Atom.create(Predicate.create("H1", 4), Variable.create("x1"), Variable.create("z1"),
						Variable.create("y1"), Variable.create("y2")),
				Atom.create(Predicate.create("H2", 2), Variable.create("y1"), Variable.create("y2")) };
		TGD tgd = TGD.create(body, head);
		System.out.println("Original TGD: " + tgd);

		TGD tgdVNF = App.VNF(tgd);
		System.out.println("TGD in VNF: " + tgdVNF);

		// (forall[u1,u2,u3](B(u1,u2,u3) --> (exists[e1,e2,e3](H1(u2,e1,e2,e3) & H2(e2,e3)))))
		Atom[] bodyE = { Atom.create(Predicate.create("B", 3), Variable.create("u1"), Variable.create("u2"),
				Variable.create("u3")) };
		Atom[] headE = {
				Atom.create(Predicate.create("H1", 4), Variable.create("u2"), Variable.create("e1"),
						Variable.create("e2"), Variable.create("e3")),
				Atom.create(Predicate.create("H2", 2), Variable.create("e2"), Variable.create("e3")) };
		TGD tgdExpected = TGD.create(bodyE, headE);

		assertEquals(tgdExpected, tgdVNF);
	}

	@Test
	public void isFullTest() {
		// (forall[x2,x1,x3](B(x2,x1,x3) --> (exists[z1,y1,y2](H1(x1,z1,y1,y2) & H2(y1,y2)))))
		TGD tgd = TGD.create(
				new Atom[] { Atom.create(Predicate.create("B", 3), Variable.create("x2"), Variable.create("x1"),
						Variable.create("x3")) },
				new Atom[] {
						Atom.create(Predicate.create("H1", 4), Variable.create("x1"), Variable.create("z1"),
								Variable.create("y1"), Variable.create("y2")),
						Atom.create(Predicate.create("H2", 2), Variable.create("y1"), Variable.create("y2")) });
		System.out.println("TGD: " + tgd);

		assertFalse("This is a 'non-full' TGD", App.isFull(tgd));

		// (forall[u1,u2,u3](B(u1,u2,u3) --> (exists[e1,e2,e3](H1(u2,e1,e2,e3) & H2(e2,e3)))))
		tgd = TGD.create(
				new Atom[] { Atom.create(Predicate.create("B", 3), Variable.create("u1"), Variable.create("u2"),
						Variable.create("u3")) },
				new Atom[] {
						Atom.create(Predicate.create("H1", 4), Variable.create("u2"), Variable.create("e1"),
								Variable.create("e2"), Variable.create("e3")),
						Atom.create(Predicate.create("H2", 2), Variable.create("e2"), Variable.create("e3")) });
		System.out.println("TGD: " + tgd);

		assertFalse("This is a 'non-full' TGD", App.isFull(tgd));

		// (forall[x1,x2](B(x1,x2) --> (exists[y1](H1(x1,y1) & H2(x2)))))
		tgd = TGD.create(
				new Atom[] { Atom.create(Predicate.create("B", 2), Variable.create("x1"), Variable.create("x2")) },
				new Atom[] { Atom.create(Predicate.create("H1", 2), Variable.create("x1"), Variable.create("y1")),
						Atom.create(Predicate.create("H2", 1), Variable.create("x2")) });
		System.out.println("TGD: " + tgd);

		assertFalse("This is a 'non-full' TGD", App.isFull(tgd));

		// (forall[x1,x2](B(x1,x2) --> (exists[y1]H1(x1,y1))))
		tgd = TGD.create(
				new Atom[] { Atom.create(Predicate.create("B", 2), Variable.create("x1"), Variable.create("x2")) },
				new Atom[] { Atom.create(Predicate.create("H1", 2), Variable.create("x1"), Variable.create("y1")) });
		System.out.println("TGD: " + tgd);

		assertFalse("This is a 'non-full' TGD", App.isFull(tgd));

		// (forall[x1,x2](B(x1,x2) --> H2(x2)))
		tgd = TGD.create(
				new Atom[] { Atom.create(Predicate.create("B", 2), Variable.create("x1"), Variable.create("x2")) },
				new Atom[] { Atom.create(Predicate.create("H2", 1), Variable.create("x2")) });
		System.out.println("TGD: " + tgd);

		assertTrue("This is a 'full' TGD", App.isFull(tgd));

		// (forall[x1](R(x1) --> (exists[y1,y2]T(x1,y1,y2))))
		// (forall[x1,x2,x3](T(x1,x2,x3) --> (exists[y]U(x1,x2,y))))
		// (forall[x1,x2,x3](U(x1,x2,x3) --> (P(x1) & V(x1,x2))))
		// (forall[x1,x2,x3]((T(x1,x2,x3) & (V(x1,x2) & S(x1))) --> M(x1)))
		TGD t1 = TGD.create(new Atom[] { Atom.create(Predicate.create("R", 1), Variable.create("x1")) },
				new Atom[] { Atom.create(Predicate.create("T", 3), Variable.create("x1"), Variable.create("y1"),
						Variable.create("y2")) });
		TGD t2 = TGD.create(
				new Atom[] { Atom.create(Predicate.create("T", 3), Variable.create("x1"), Variable.create("x2"),
						Variable.create("x3")) },
				new Atom[] { Atom.create(Predicate.create("U", 3), Variable.create("x1"), Variable.create("x2"),
						Variable.create("y")) });
		TGD t3 = TGD.create(
				new Atom[] { Atom.create(Predicate.create("U", 3), Variable.create("x1"), Variable.create("x2"),
						Variable.create("x3")) },
				new Atom[] { Atom.create(Predicate.create("P", 1), Variable.create("x1")),
						Atom.create(Predicate.create("V", 2), Variable.create("x1"), Variable.create("x2")) });
		TGD t4 = TGD.create(
				new Atom[] {
						Atom.create(Predicate.create("T", 3), Variable.create("x1"), Variable.create("x2"),
								Variable.create("x3")),
						Atom.create(Predicate.create("V", 2), Variable.create("x1"), Variable.create("x2")),
						Atom.create(Predicate.create("S", 1), Variable.create("x1")) },
				new Atom[] { Atom.create(Predicate.create("M", 1), Variable.create("x1")), });

		System.out.println("TGD: " + t1);
		assertFalse("This is a 'non-full' TGD", App.isFull(t1));
		System.out.println("TGD: " + t2);
		assertFalse("This is a 'non-full' TGD", App.isFull(t2));
		System.out.println("TGD: " + t3);
		assertTrue("This is a 'full' TGD", App.isFull(t3));
		System.out.println("TGD: " + t4);
		assertTrue("This is a 'full' TGD", App.isFull(t4));
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

		Map<Term, Term> mgu = App.getMGU(
				new Atom[] { Atom.create(Predicate.create("U", 3), Variable.create("u1"), Variable.create("u2"),
						Variable.create("e1")) },
				new Atom[] { Atom.create(Predicate.create("U", 3), Variable.create("z1"), Variable.create("z2"),
						Variable.create("z3")) },
				Arrays.asList(Atom.create(Predicate.create("U", 3), Variable.create("z1"), Variable.create("z2"),
						Variable.create("z3"))),
				existentials);

		System.out.println(mgu);

		assertNotNull(mgu);

	}

	@Test
	public void applyMGU() {
		// TODO
	}

}
