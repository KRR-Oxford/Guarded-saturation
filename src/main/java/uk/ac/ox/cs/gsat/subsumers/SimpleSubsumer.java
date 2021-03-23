package uk.ac.ox.cs.gsat.subsumers;

import java.util.Collection;
import java.util.HashSet;

import uk.ac.ox.cs.gsat.GTGD;
import uk.ac.ox.cs.gsat.filters.FormulaFilter;

/**
 * A subsumer that uses an index to filter out only candidates for subsumption,
 * then identifies tgd a as subsumed by tgd b if b.head is contained in a.head,
 * and a.body is contained in b.body (without any unification).
 */
public class SimpleSubsumer implements Subsumer {

    final FormulaFilter filter;
    private long num_filter_discarded = 0, num_subsumed = 0;

    public SimpleSubsumer(FormulaFilter filter) {
        this.filter = filter;
    }

    @Override
    public Collection<GTGD> subsumesAny(GTGD newTGD) {
        Collection<GTGD> subsumed = new HashSet<>();

        var bodyN = newTGD.getBodySet();
        var headN = newTGD.getHeadSet();

        for (GTGD tgd : filter.getSubsumedCandidates(newTGD)) {
            var body = tgd.getBodySet();
            var head = tgd.getHeadSet();

            if (body.size() < bodyN.size() || headN.size() < head.size()) {
                num_filter_discarded += 1;
                continue;
            }

            if (body.containsAll(bodyN) && headN.containsAll(head)) {
                num_subsumed += 1;
                subsumed.add(tgd);
            } else {
                num_filter_discarded += 1;
            }

        }
        filter.removeAll(subsumed);
        return subsumed;
    }

    @Override
    public boolean subsumed(GTGD newTGD) {
        var bodyN = newTGD.getBodySet();
        var headN = newTGD.getHeadSet();

        for (GTGD tgd : filter.getSubsumingCandidates(newTGD)) {
            var body = tgd.getBodySet();
            var head = tgd.getHeadSet();

            if (bodyN.size() < body.size() || head.size() < headN.size()) {
                num_filter_discarded += 1;
                continue;
            }

            if (bodyN.containsAll(body) && head.containsAll(headN)) {
                num_subsumed += 1;
                return true;
            } else {
                num_filter_discarded += 1;
            }
        }

        return false;
    }

    public void add(GTGD newTGD) {
        filter.add(newTGD);
    }

    public Collection<GTGD> getAll() {
        return filter.getAll();
    }

    public long getNumberSubsumed() {
        return num_subsumed;
    }

    public long getFilterDiscarded() {
        return num_filter_discarded;
    }

}
