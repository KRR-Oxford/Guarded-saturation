import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.Collection;
import java.util.LinkedList;

import org.junit.Test;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * AppTest
 */
public class AppTest {

    @Test
    public void VNFTest() {
        Atom[] body = { Atom.create(Predicate.create("B", 3), Variable.create("x2"), Variable.create("x1"),
                Variable.create("x3")) };
        Atom[] head = {
                Atom.create(Predicate.create("H1", 4), Variable.create("x1"), Variable.create("z1"),
                        Variable.create("y1"), Variable.create("y2")),
                Atom.create(Predicate.create("H2", 2), Variable.create("y1"), Variable.create("y2")) };
        Dependency dependency = Dependency.create(body, head);
        System.out.println("Original dependency: " + dependency);

        Dependency dependencyVNF = App.VNF(dependency);
        System.out.println("Dependency in VNF: " + dependencyVNF);

        Atom[] bodyE = { Atom.create(Predicate.create("B", 3), Variable.create("u1"), Variable.create("u2"),
                Variable.create("u3")) };
        Atom[] headE = {
                Atom.create(Predicate.create("H1", 4), Variable.create("u2"), Variable.create("e1"),
                        Variable.create("e2"), Variable.create("e3")),
                Atom.create(Predicate.create("H2", 2), Variable.create("e2"), Variable.create("e3")) };
        Dependency dependencyExpected = Dependency.create(bodyE, headE);

        assertEquals(dependencyExpected, dependencyVNF);
    }

    @Test
    public void HNFTest() {
        Atom[] body = { Atom.create(Predicate.create("B", 2), Variable.create("x1"), Variable.create("x2")) };
        Atom[] head = { Atom.create(Predicate.create("H1", 2), Variable.create("x1"), Variable.create("y1")),
                Atom.create(Predicate.create("H2", 1), Variable.create("x2")) };
        Dependency dependency = Dependency.create(body, head);
        System.out.println("Original dependency: " + dependency);

        Collection<Dependency> dependenciesHNF = App.HNF(dependency);
        System.out.println("Dependencies in HNF:");
        dependenciesHNF.forEach(System.out::println);

        Atom[] bodyE = { Atom.create(Predicate.create("B", 2), Variable.create("x1"), Variable.create("x2")) };
        Atom[] headE1 = { Atom.create(Predicate.create("H1", 2), Variable.create("x1"), Variable.create("y1")) };
        Atom[] headE2 = { Atom.create(Predicate.create("H2", 1), Variable.create("x2")) };
        Dependency dependencyExpected1 = Dependency.create(bodyE, headE1);
        Dependency dependencyExpected2 = Dependency.create(bodyE, headE2);
        Collection<Dependency> dependenciesExpected = new LinkedList<>();
        dependenciesExpected.add(dependencyExpected1);
        dependenciesExpected.add(dependencyExpected2);

        assertEquals(dependenciesExpected, dependenciesHNF);
    }

    @Test
    public void runGSatTest() {

        Dependency t1 = Dependency.create(new Atom[] { Atom.create(Predicate.create("R", 1), Variable.create("x1")) },
                new Atom[] { Atom.create(Predicate.create("T", 3), Variable.create("x1"), Variable.create("y1"),
                        Variable.create("y2")) });
        Dependency t2 = Dependency.create(
                new Atom[] { Atom.create(Predicate.create("T", 3), Variable.create("x1"), Variable.create("x2"),
                        Variable.create("x3")) },
                new Atom[] { Atom.create(Predicate.create("U", 3), Variable.create("x1"), Variable.create("x2"),
                        Variable.create("y")) });
        Dependency t3 = Dependency.create(
                new Atom[] { Atom.create(Predicate.create("U", 3), Variable.create("x1"), Variable.create("x2"),
                        Variable.create("x3")) },
                new Atom[] { Atom.create(Predicate.create("P", 1), Variable.create("x1")),
                        Atom.create(Predicate.create("V", 2), Variable.create("x1"), Variable.create("x2")) });
        Dependency t4 = Dependency.create(
                new Atom[] {
                        Atom.create(Predicate.create("T", 3), Variable.create("x1"), Variable.create("x2"),
                                Variable.create("x3")),
                        Atom.create(Predicate.create("V", 2), Variable.create("x1"), Variable.create("x2")),
                        Atom.create(Predicate.create("S", 1), Variable.create("x1")) },
                new Atom[] { Atom.create(Predicate.create("M", 1), Variable.create("x1")), });

        Collection<Dependency> allDependencies = new LinkedList<>();
        allDependencies.add(t1);
        allDependencies.add(t2);
        allDependencies.add(t3);
        allDependencies.add(t4);
        System.out.println("Initial rules:");
        allDependencies.forEach(System.out::println);

        Collection<Dependency> guardedSaturation = App
                .runGSat(allDependencies.toArray(new Dependency[allDependencies.size()]));

		assertEquals(5, guardedSaturation.size());
		
        // Dependency dependencyExpected = Dependency.create(bodyE, headE);
        // assertTrue(guardedSaturation.stream().anyMatch(dependencyExpected));
    }

    @Test
    public void isFullTest() {
        Dependency dependency = Dependency.create(
                new Atom[] { Atom.create(Predicate.create("B", 3), Variable.create("x2"), Variable.create("x1"),
                        Variable.create("x3")) },
                new Atom[] {
                        Atom.create(Predicate.create("H1", 4), Variable.create("x1"), Variable.create("z1"),
                                Variable.create("y1"), Variable.create("y2")),
                        Atom.create(Predicate.create("H2", 2), Variable.create("y1"), Variable.create("y2")) });
        System.out.println("Dependency: " + dependency);

        assertFalse("This is a 'non-full' TGD", App.isFull(dependency));

        dependency = Dependency.create(
                new Atom[] { Atom.create(Predicate.create("B", 3), Variable.create("u1"), Variable.create("u2"),
                        Variable.create("u3")) },
                new Atom[] {
                        Atom.create(Predicate.create("H1", 4), Variable.create("u2"), Variable.create("e1"),
                                Variable.create("e2"), Variable.create("e3")),
                        Atom.create(Predicate.create("H2", 2), Variable.create("e2"), Variable.create("e3")) });
        System.out.println("Dependency: " + dependency);

        assertFalse("This is a 'non-full' TGD", App.isFull(dependency));

        dependency = Dependency.create(
                new Atom[] { Atom.create(Predicate.create("B", 2), Variable.create("x1"), Variable.create("x2")) },
                new Atom[] { Atom.create(Predicate.create("H1", 2), Variable.create("x1"), Variable.create("y1")),
                        Atom.create(Predicate.create("H2", 1), Variable.create("x2")) });
        System.out.println("Dependency: " + dependency);

        assertFalse("This is a 'non-full' TGD", App.isFull(dependency));

        dependency = Dependency.create(
                new Atom[] { Atom.create(Predicate.create("B", 2), Variable.create("x1"), Variable.create("x2")) },
                new Atom[] { Atom.create(Predicate.create("H1", 2), Variable.create("x1"), Variable.create("y1")) });
        System.out.println("Dependency: " + dependency);

        assertFalse("This is a 'non-full' TGD", App.isFull(dependency));

        dependency = Dependency.create(
                new Atom[] { Atom.create(Predicate.create("B", 2), Variable.create("x1"), Variable.create("x2")) },
                new Atom[] { Atom.create(Predicate.create("H2", 1), Variable.create("x2")) });
        System.out.println("Dependency: " + dependency);

        assertTrue("This is a 'full' TGD", App.isFull(dependency));

        Dependency t1 = Dependency.create(new Atom[] { Atom.create(Predicate.create("R", 1), Variable.create("x1")) },
                new Atom[] { Atom.create(Predicate.create("T", 3), Variable.create("x1"), Variable.create("y1"),
                        Variable.create("y2")) });
        Dependency t2 = Dependency.create(
                new Atom[] { Atom.create(Predicate.create("T", 3), Variable.create("x1"), Variable.create("x2"),
                        Variable.create("x3")) },
                new Atom[] { Atom.create(Predicate.create("U", 3), Variable.create("x1"), Variable.create("x2"),
                        Variable.create("y")) });
        Dependency t3 = Dependency.create(
                new Atom[] { Atom.create(Predicate.create("U", 3), Variable.create("x1"), Variable.create("x2"),
                        Variable.create("x3")) },
                new Atom[] { Atom.create(Predicate.create("P", 1), Variable.create("x1")),
                        Atom.create(Predicate.create("V", 2), Variable.create("x1"), Variable.create("x2")) });
        Dependency t4 = Dependency.create(
                new Atom[] {
                        Atom.create(Predicate.create("T", 3), Variable.create("x1"), Variable.create("x2"),
                                Variable.create("x3")),
                        Atom.create(Predicate.create("V", 2), Variable.create("x1"), Variable.create("x2")),
                        Atom.create(Predicate.create("S", 1), Variable.create("x1")) },
                new Atom[] { Atom.create(Predicate.create("M", 1), Variable.create("x1")), });

        System.out.println("Dependency: " + t1);
        assertFalse("This is a 'non-full' TGD", App.isFull(t1));
        System.out.println("Dependency: " + t2);
        assertFalse("This is a 'non-full' TGD", App.isFull(t2));
        System.out.println("Dependency: " + t3);
        assertTrue("This is a 'full' TGD", App.isFull(t3));
        System.out.println("Dependency: " + t4);
        assertTrue("This is a 'full' TGD", App.isFull(t4));
    }

    @Test
    public void evolveTest() {
        // TODO
    }

}
