package uk.ac.ox.cs.gsat.unification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import uk.ac.ox.cs.gsat.fol.SkGTGD;
import uk.ac.ox.cs.gsat.fol.TGDFactory;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Constant;
import uk.ac.ox.cs.pdq.fol.Function;
import uk.ac.ox.cs.pdq.fol.FunctionTerm;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.UntypedConstant;
import uk.ac.ox.cs.pdq.fol.Variable;

public class AtomPathUnificationIndexTest {

    private TGDFactory<SkGTGD> factory;

    public AtomPathUnificationIndexTest() {
        this.factory = TGDFactory.getSkGTGDInstance(true);
    }

    // Variables
    private static final Variable x1 = Variable.create("x1");
    private static final Variable x2 = Variable.create("x2");
    private static final Variable x3 = Variable.create("x3");
    private static final Variable z1 = Variable.create("z1");
    private static final Variable z2 = Variable.create("z2");
    private static final Variable z3 = Variable.create("z3");

    // Functions
    private static final Function f = new Function("f", 1);
    private static final Function g = new Function("g", 1);

    // Constants
    private static final Constant a = UntypedConstant.create("a");
    private static final Constant b = UntypedConstant.create("b");

    
    // Terms
    private static final Term f_x1 = FunctionTerm.create(f, new Variable[] { x1 });
    private static final Term f_a = FunctionTerm.create(f, new Constant[] { a });
    private static final Term f_b = FunctionTerm.create(f, new Constant[] { b });
    private static final Term f_z1 = FunctionTerm.create(f, new Variable[] { z1 });
    private static final Term g_x1 = FunctionTerm.create(g, new Variable[] { x1 });
    private static final Term g_z1 = FunctionTerm.create(g, new Variable[] { z1 });
    private static final Term g_a = FunctionTerm.create(g, new Constant[] { a });
    private static final Term g_b = FunctionTerm.create(g, new Constant[] { b });


    private static final Atom R_x1 = Atom.create(Predicate.create("R", 1), x1);
    private static final Atom R_a = Atom.create(Predicate.create("R", 1), a);
    private static final Atom R_b = Atom.create(Predicate.create("R", 1), b);
    private static final Atom R_z1 = Atom.create(Predicate.create("R", 1), z1);
    private static final Atom R_f_x1 = Atom.create(Predicate.create("R", 1), f_x1);
    private static final Atom R_f_z1 = Atom.create(Predicate.create("R", 1), f_z1);
    private static final Atom R_g_x1 = Atom.create(Predicate.create("R", 1), g_x1);
    private static final Atom R_g_z1 = Atom.create(Predicate.create("R", 1), g_z1);
    private static final Atom T_x1x2x3 = Atom.create(Predicate.create("T", 3), x1, x2, x3);
    private static final Atom T_x1f_x1g_x1 = Atom.create(Predicate.create("T", 3), x1, f_x1, g_x1);
    private static final Atom T_x1f_ag_x1 = Atom.create(Predicate.create("T", 3), x1, f_a, g_x1);
    private static final Atom T_x1f_ag_b = Atom.create(Predicate.create("T", 3), x1, f_a, g_b);
    private static final Atom T_x1f_bg_b = Atom.create(Predicate.create("T", 3), x1, f_b, g_b);
    private static final Atom T_x1f_x1g_a = Atom.create(Predicate.create("T", 3), x1, f_x1, g_a);
    private static final Atom T_z1f_z1g_z1 = Atom.create(Predicate.create("T", 3), z1, f_z1, g_z1);
    private static final Atom T_z1g_z1f_z1 = Atom.create(Predicate.create("T", 3), z1, g_z1, f_z1);
    private static final Atom T_z1z2g_z1 = Atom.create(Predicate.create("T", 3), z1, z2, g_z1);
    private static final Atom T_z1f_z1z3 = Atom.create(Predicate.create("T", 3), z1, f_z1, z3);
    private static final Atom T_z1z2f_z1 = Atom.create(Predicate.create("T", 3), z1, z2, f_z1);
    private static final Atom T_z1g_z1z3 = Atom.create(Predicate.create("T", 3), z1, g_z1, z3);
    private static final Atom T_z1z2z3 = Atom.create(Predicate.create("T", 3), z1, z2, z3);


    @Test
    public void testWithoutSkolemFunction() {
        SkGTGD t = factory.create(Set.of(R_x1), Set.of(T_x1x2x3));

        UnificationIndex<SkGTGD> index = new AtomPathUnificationIndex<>();
        index.put(T_x1x2x3, t);

        assertFalse(index.get(T_z1z2z3).isEmpty());

        assertTrue(index.get(R_x1).isEmpty());
    }

    @Test
    public void testWithOneSkolemFunction() {
        SkGTGD t = factory.create(Set.of(T_x1x2x3), Set.of(R_f_x1));

        UnificationIndex<SkGTGD> index = new AtomPathUnificationIndex<>();
        index.put(R_f_x1, t);

        assertTrue(index.get(T_z1z2z3).isEmpty());

        assertFalse(index.get(R_z1).isEmpty());

        assertFalse(index.get(R_f_z1).isEmpty());

        assertTrue(index.get(R_g_z1).isEmpty());
    }

    @Test
    public void testWithTwoSkolemFunctions() {
        SkGTGD t = factory.create(Set.of(R_x1), Set.of(T_x1f_x1g_x1));

        UnificationIndex<SkGTGD> index = new AtomPathUnificationIndex<>();
        index.put(T_x1f_x1g_x1, t);

        assertFalse(index.get(T_z1z2z3).isEmpty());
        assertFalse(index.get(T_z1f_z1g_z1).isEmpty());
        assertFalse(index.get(T_z1z2g_z1).isEmpty());
        assertFalse(index.get(T_z1f_z1z3).isEmpty());
        
        assertTrue(index.get(R_z1).isEmpty());
        assertTrue(index.get(T_z1g_z1f_z1).isEmpty());
        assertTrue(index.get(T_z1z2f_z1).isEmpty());
        assertTrue(index.get(T_z1g_z1z3).isEmpty());

    }

    @Test
    public void testWithConstant() {
        SkGTGD t = factory.create(Set.of(R_x1), Set.of(R_a));

        UnificationIndex<SkGTGD> index = new AtomPathUnificationIndex<>();
        index.put(R_a, t);

        assertFalse(index.get(R_a).isEmpty());
        assertFalse(index.get(R_z1).isEmpty());

        assertTrue(index.get(R_b).isEmpty());
    }

    @Test
    public void testWithConstantNestedInSkolemFunction() {
        SkGTGD t = factory.create(Set.of(R_x1), Set.of(T_x1f_ag_x1));

        UnificationIndex<SkGTGD> index = new AtomPathUnificationIndex<>();
        index.put(T_x1f_ag_x1, t);

        assertFalse(index.get(T_x1f_ag_b).isEmpty());
        assertFalse(index.get(T_x1f_ag_b).isEmpty());
        assertFalse(index.get(T_x1f_x1g_x1).isEmpty());

        assertTrue(index.get(T_x1f_bg_b).isEmpty());
    }

    @Test
    public void testRemove() {
        SkGTGD t1 = factory.create(Set.of(R_x1), Set.of(T_x1x2x3));
        SkGTGD t2 = factory.create(Set.of(R_x1), Set.of(T_x1f_ag_x1));
        SkGTGD t3 = factory.create(Set.of(R_x1), Set.of(T_x1f_x1g_a));

        UnificationIndex<SkGTGD> index = new AtomPathUnificationIndex<>();
        index.put(T_x1f_ag_x1, t1);
        index.put(T_x1x2x3, t2);

        assertEquals(2, index.get(T_x1f_ag_b).size());
        assertEquals(2, index.get(T_x1f_ag_b).size());
        assertEquals(2, index.get(T_x1f_x1g_x1).size());
        assertEquals(1, index.get(T_x1f_bg_b).size());

        index.remove(T_x1x2x3, t2);
        assertEquals(1, index.get(T_x1f_ag_b).size());
        assertEquals(1, index.get(T_x1f_ag_b).size());
        assertEquals(1, index.get(T_x1f_x1g_x1).size());
        assertEquals(0, index.get(T_x1f_bg_b).size());

        index.remove(T_x1f_ag_x1, t1);
        assertEquals(0, index.get(T_x1f_ag_b).size());
        assertEquals(0, index.get(T_x1f_ag_b).size());
        assertEquals(0, index.get(T_x1f_x1g_x1).size());
        assertEquals(0, index.get(T_x1f_bg_b).size());

        index.put(T_x1f_x1g_a, t3);
        assertEquals(0, index.get(T_x1f_ag_b).size());
        assertEquals(0, index.get(T_x1f_ag_b).size());
        assertEquals(1, index.get(T_x1f_x1g_x1).size());
        assertEquals(1, index.get(T_x1f_x1g_a).size());
        assertEquals(0, index.get(T_x1f_bg_b).size());

    }

}
