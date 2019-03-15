import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import org.junit.Test;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.UntypedConstant;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * Unit tests for the GSat class from IRIS+- files
 * 
 * @author Stefano
 */
public class IRISPMTest {

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

	private void fromIRISPM(Collection<TGD> allTGDs, Collection<Atom> allFacts, Collection<ConjunctiveQuery> allQueries,
			int guardedSaturationSize, int[] queryLenghts) {
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
			int count = 0;
			assertEquals(allQueries.size(), queryLenghts.length);
			for (ConjunctiveQuery q : allQueries) {

				IO.writeDatalogQueries(Arrays.asList(q), baseOutputPath + "query.rul");
				SolverOutput output = Logic.invokeSolver(
						"executables" + File.separator + "idlv_1.1.2_windows_x86-64.exe", "--query",
						Arrays.asList(baseOutputPath + "rules.rul", baseOutputPath + "facts.data",
								baseOutputPath + "query.rul"));
				System.out.println(output);

				int expectedLines = queryLenghts[count++];
				if (expectedLines != -1) { // -1 disable the check
					assertEquals(expectedLines, countLines(output.getOutput()));
					assertEquals(0, output.getErrors().length());
				}

			}
		} catch (IOException | InterruptedException e) {
			fail(e.getLocalizedMessage());
		}

	}

	private static int countLines(String str) {
		String[] lines = str.split("\r\n|\r|\n");
		return lines.length;
	}

	@Test
	public void guardedCarlo() {
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

		fromIRISPM(allTGDs, allFacts, allQueries, 2, new int[] { 1 });

	}

	@Test
	public void guardedExample() {
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

		fromIRISPM(allTGDs, allFacts, allQueries, 1, new int[] { 1, 2 });

	}

	@Test
	public void guarded_owl_nostorage() {
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

		fromIRISPM(allTGDs, allFacts, allQueries, 12, new int[] { -1, 2, 3, 5 });

	}

}
