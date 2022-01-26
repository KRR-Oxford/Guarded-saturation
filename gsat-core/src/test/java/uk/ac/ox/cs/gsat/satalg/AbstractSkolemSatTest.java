package uk.ac.ox.cs.gsat.satalg;

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

import uk.ac.ox.cs.gsat.App;
import uk.ac.ox.cs.gsat.fol.SkGTGD;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Variable;

public abstract class AbstractSkolemSatTest<Q extends SkGTGD> {

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
    private static final Atom Ax1 = Atom.create(Predicate.create("A", 1), x1);
    private static final Atom Rx1x2 = Atom.create(Predicate.create("R", 2), x1, x2);
    private static final Atom Ux2 = Atom.create(Predicate.create("U", 1), x2);
    private static final Atom Px1 = Atom.create(Predicate.create("P", 1), x1);
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
	private static final Atom H1_x1x2y1y2 = Atom.create(Predicate.create("H1", 4), x1, x2, y1, y2);


    @BeforeAll
    static void initAll() {
        Handler handlerObj = new ConsoleHandler();
        handlerObj.setLevel(Level.WARNING);
        App.logger.addHandler(handlerObj);
        App.logger.setLevel(Level.WARNING);
        App.logger.setUseParentHandlers(false);
    }

    private AbstractSaturation<Q> sksat;

    public AbstractSkolemSatTest(AbstractSaturation<Q> sksat) {
        this.sksat = sksat;
    }

    
    @Test
    public void simpleTest() {

        /**
         * input TGDs :
         * - A(x1) -> ∃ x2 R(x1, x2) 
         * - R(x1, x2) -> U(x2) 
         * - R(x1, x2), U(x2) -> P(x1)
         */
        Q nonFull = sksat.getFactory().create(Set.of(Ax1), Set.of(Rx1x2));
        Q full = sksat.getFactory().create(Set.of(Rx1x2), Set.of(Ux2));
        Q full1 = sksat.getFactory().create(Set.of(Rx1x2, Ux2), Set.of(Px1));

        Collection<Dependency> input = new ArrayList<>();
        input.add(nonFull);
        input.add(full);
        input.add(full1);

        /**
         * expected output TGDs
         * - the VNF of the input full TGDs
         * - the VNF of A(x1) -> P(x1)
         */
        HashSet<TGD> expected = new HashSet<TGD>();
        expected.add(sksat.getFactory().computeVNF(full, sksat.eVariable, sksat.uVariable));
        expected.add(sksat.getFactory().computeVNF(full1, sksat.eVariable, sksat.uVariable));
        expected.add(sksat.getFactory().computeVNF(sksat.getFactory().create(Set.of(Ax1), Set.of(Px1)), sksat.eVariable,
                sksat.uVariable));

        Collection<Q> result = sksat.run(input);

        assertEquals(expected, result);
    }

    @Test
    public void twoAtomsUnifiedTest() {

        /**
         * input TGDs :
         * - A(x1) -> ∃ x2 R(x1, x2) , U(x2)
         * - R(x1, x2), U(x2) -> P(x1)
         */
        Q nonFull = sksat.getFactory().create(Set.of(Ax1), Set.of(Rx1x2, Ux2));
        Q full1 = sksat.getFactory().create(Set.of(Rx1x2, Ux2), Set.of(Px1));

        Collection<Dependency> input = new ArrayList<>();
        input.add(nonFull);
        input.add(full1);

        /**
         * expected output TGDs
         * - the VNF of the input full TGDs
         * - the VNF of A(x1) -> P(x1)
         */
        HashSet<TGD> expected = new HashSet<TGD>();
        expected.add(sksat.getFactory().computeVNF(full1, sksat.eVariable, sksat.uVariable));
        expected.add(sksat.getFactory().computeVNF(sksat.getFactory().create(Set.of(Ax1), Set.of(Px1)), sksat.eVariable,
                sksat.uVariable));

        Collection<Q> result = sksat.run(input);

        assertTrue(result.containsAll(expected));
    }

    /**
     * Inspired by the example 1 of GSatTest by transforming 
     * the input TGDs into single head TGDs
     */    
    @Test
    public void example1() {
    
        /** input TGDs:
         * - ∀ x1 R(x1) → ∃ y1,y2 T(x1,y1,y2)
         * - ∀ x1,x2,x3 T(x1,x2,x3) → ∃ y1 U(x1,x2,y1)
         * - ∀ x1,x2,x3 U(x1,x2,x3) → P(x1)
         * - ∀ x1,x2,x3 U(x1,x2,x3) → V(x1,x2)
         * - ∀ x1,x2,x3 T(x1,x2,x3) ∧ V(x1,x2) ∧ S(x1) → M(x1)
         */
		Q t1 = sksat.getFactory().create(Set.of( R_x1 ), Set.of( T_x1y1y2 ));
		Q t2 = sksat.getFactory().create(Set.of( T_x1x2x3 ), Set.of( U_x1x2y1 ));
		Q t3 = sksat.getFactory().create(Set.of( U_x1x2x3 ), Set.of( P_x1 ));
        Q t3bis = sksat.getFactory().create(Set.of( U_x1x2x3 ), Set.of( V_x1x2 ));
		Q t4 = sksat.getFactory().create(Set.of( T_x1x2x3, V_x1x2, S_x1 ), Set.of( M_x1 ));

		Collection<Dependency> initial = new HashSet<>();
		initial.add(t1);
		initial.add(t2);
		initial.add(t3);
        initial.add(t3bis);
		initial.add(t4);

        Collection<Q> result = sksat.run(initial);

        /** expected output TGDs
         * ∀ u1,u2,u3 U(u1,u2,u3) → P(u1)
         * ∀ u1,u2,u3 U(u1,u2,u3) → V(u1,u2)
         * ∀ u1,u2,u3 T(u1,u2,u3) ∧ V(u1,u2) ∧ S(u1) → M(u1)
         * ∀ u1,u2,u3 T(u1,u2,u3) → P(u1)
         * ∀ u1,u2,u3 T(u1,u2,u3) → V(u1,u2)
         * ∀ u1 R(u1) → P(u1)
         * ∀ u1 R(u1) ∧ S(u1) → M(u1)
         */

        HashSet<TGD> expected = new HashSet<TGD>();
        expected.add(sksat.getFactory().computeVNF(t3, sksat.eVariable, sksat.uVariable));
        expected.add(sksat.getFactory().computeVNF(t3bis, sksat.eVariable, sksat.uVariable));
        expected.add(sksat.getFactory().computeVNF(t4, sksat.eVariable, sksat.uVariable));
        expected.add(sksat.getFactory().computeVNF(sksat.getFactory().create(Set.of( T_x1x2x3 ), Set.of( P_x1 )), sksat.eVariable, sksat.uVariable));
        expected.add(sksat.getFactory().computeVNF(sksat.getFactory().create(Set.of( T_x1x2x3 ), Set.of( V_x1x2 )), sksat.eVariable, sksat.uVariable));
        expected.add(sksat.getFactory().computeVNF(sksat.getFactory().create(Set.of( R_x1 ), Set.of( P_x1 )), sksat.eVariable, sksat.uVariable));
        expected.add(sksat.getFactory().computeVNF(sksat.getFactory().create(Set.of( R_x1, S_x1 ), Set.of( M_x1 )), sksat.eVariable, sksat.uVariable));

        assertEquals(expected, result);
        
        }
    /**
     * Inspired by the example 2 of GSatTest by transforming 
     * the input TGDs into single head TGDs
     */    
    @Test
    public void example2() {

        /**
         * intput TGDs:
         * ∀ x1,x2 R(x1,x2) → ∃ y1,y2 H1(x1,x2,y1,y2)
         * ∀ x1,x2,y1,y2 H1(x1,x2,y1,y2) → S(x1,x2,y1,y2)
         * ∀ x1,x2,y1,y2 H1(x1,x2,y1,y2) → T(x1,x2,y2)        
         * ∀ x1,x2,x3,x4 S(x1,x2,x3,x4) → U(x4)
         * ∀ z1,z2,z3 T(z1,z2,z3) ∧ U(z3) → P(z1)
        */
		Q t1 = sksat.getFactory().create(Set.of( R_x1x2 ), Set.of( H1_x1x2y1y2 ));
        Q t1bis = sksat.getFactory().create(Set.of( H1_x1x2y1y2 ), Set.of( S_x1x2y1y2 ));
        Q t1ter = sksat.getFactory().create(Set.of( H1_x1x2y1y2 ), Set.of( T_x1x2y2 ));
		Q t2 = sksat.getFactory().create(Set.of( S_x1x2x3x4 ), Set.of( U_x4 ));
		Q t3 = sksat.getFactory().create(Set.of( T_z1z2z3, U_z3 ), Set.of( P_z1 ));

		Collection<Dependency> initial = new HashSet<>();
		initial.add(t1);
        initial.add(t1bis);
        initial.add(t1ter);
		initial.add(t2);
		initial.add(t3);

		Collection<Q> result = sksat.run(initial);

        /**
         * expected TGDs: 
         * ∀ u1,u2,u3,u4 H1(u1,u2,u3,u4) → S(u1,u2,u3,u4)
         * ∀ u1,u2,y1,y2 H1(u1,u2,u3,u4) → T(u1,u2,u4)        
         * ∀ u1,u2,u3 T(u1,u2,u3) ∧ U(u3) → P(u1)
         * ∀ u1,u2,u3,u4 S(u1,u2,u3,u4) → U(u4)
         * ∀ u1,u2 R(u1,u2) → P(u1)
        */
        HashSet<TGD> expected = new HashSet<TGD>();
        expected.add(sksat.getFactory().computeVNF(t1bis, sksat.eVariable, sksat.uVariable));
        expected.add(sksat.getFactory().computeVNF(t1ter, sksat.eVariable, sksat.uVariable));
        expected.add(sksat.getFactory().computeVNF(t2, sksat.eVariable, sksat.uVariable));
        expected.add(sksat.getFactory().computeVNF(t3, sksat.eVariable, sksat.uVariable));
        expected.add(sksat.getFactory().computeVNF(sksat.getFactory().create(Set.of( R_x1x2 ), Set.of( P_x1 )), sksat.eVariable, sksat.uVariable));

        assertEquals(expected, result);
    }
}
