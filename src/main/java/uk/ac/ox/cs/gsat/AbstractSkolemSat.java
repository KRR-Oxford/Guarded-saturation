package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uk.ac.ox.cs.gsat.unification.UnificationIndexType;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Term;

public abstract class AbstractSkolemSat<Q extends SkGTGD> extends EvolveBasedSat<Q> {

    protected AbstractSkolemSat(TGDFactory<Q> factory, String name) {
        super(name, factory, UnificationIndexType.ATOM_PATH_INDEX , UnificationIndexType.ATOM_PATH_INDEX);
    }

    @Override
    protected Collection<Q> transformInputTGDs(Collection<Q> inputTGDs) {
        Collection<Q> result = new ArrayList<>();

        for (Q tgd : inputTGDs) {
            for (Q shnf : factory.computeSHNF(tgd)) {
                result.add(factory.computeVNF(shnf, eVariable, uVariable));
            }
        }
        return result;
    }

    @Override
    protected Collection<Q> getOutput(Collection<Q> rightTGDs) {
        Collection<Q> output = new HashSet<>();

        for (Q tgd : rightTGDs)
            if (!tgd.isFunctional())
                output.add(tgd);

        return output;
    }

    @Override
    public Collection<Q> evolveNew(Q leftTGD, Q rightTGD) {

        Collection<Q> results = new HashSet<>();

        rightTGD = evolveRename(rightTGD);

        Collection<Atom> selectedBodyAtoms = new ArrayList<>();
        for (Atom atom : getUnifiableBodyAtoms(rightTGD))
            selectedBodyAtoms.add(atom);

        for (Atom B : selectedBodyAtoms) {
            for (Atom H : leftTGD.getHeadAtoms()) {

                Map<Term, Term> mgu = Logic.getMGU(B, H);

                if (mgu != null) {
                    // System.out.println("mgu " + mgu);
                    Set<Atom> new_body = new HashSet<>();
                    for (Atom batom : rightTGD.getBodySet())
                        if (!batom.equals(B))
                            new_body.add((Atom) Logic.applySubstitution(batom, mgu));

                    for (Atom batom : leftTGD.getBodySet())
                        new_body.add((Atom) Logic.applySubstitution(batom, mgu));

                    Set<Atom> new_head = new HashSet<>();
                    for (Atom hatom : rightTGD.getHeadSet())
                        new_head.add((Atom) Logic.applySubstitution(hatom, mgu));

                    Q new_tgd = factory.create(new_body, new_head);
                    new_tgd = factory.computeVNF(new_tgd, eVariable, uVariable);

                    results.add(new_tgd);
                }
            }
        }
        return results;
    }

}
