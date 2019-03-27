package uk.ac.ox.cs.gsat;

import java.util.Set;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.TGD;

/**
 * TGDGSat
 */
public class TGDGSat extends TGD implements Comparable<TGDGSat> {

    private static final long serialVersionUID = 1L;

    protected TGDGSat(Atom[] body, Atom[] head) {
        super(body, head);
    }

    public TGDGSat(TGD tgd) {
        super(tgd.getBodyAtoms(), tgd.getHeadAtoms());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TGDGSat))
            return false;

        TGDGSat tgd2 = (TGDGSat) obj;

        var body1 = Set.of(this.bodyAtoms);
        var body2 = Set.of(tgd2.bodyAtoms);
        if (!body1.equals(body2))
            return false;

        var head1 = Set.of(this.headAtoms);
        var head2 = Set.of(tgd2.headAtoms);
        if (!head1.equals(head2))
            return false;

        return true;
    }

    @Override
    public int compareTo(TGDGSat o) {
        if (this.equals(o))
            return 0;

        return this.toString().compareTo(o.toString()); // Lexicographic order
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

}