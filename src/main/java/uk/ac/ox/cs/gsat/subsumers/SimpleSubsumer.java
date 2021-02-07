package uk.ac.ox.cs.gsat.subsumers;

import java.util.Collection;
import java.util.HashSet;

import uk.ac.ox.cs.gsat.TGDGSat;
import uk.ac.ox.cs.gsat.filters.FormulaFilter;

public class SimpleSubsumer implements Subsumer {

    FormulaFilter filter;
    private long num_filter_discarded = 0, num_subsumed = 0;

    public SimpleSubsumer(FormulaFilter filter) {
        this.filter = filter;
    }

    @Override
    public Collection<TGDGSat> subsumesAny(TGDGSat newTGD) {
        Collection<TGDGSat> subsumed = new HashSet<>();

        var bodyN = newTGD.getBodySet();
        var headN = newTGD.getHeadSet();

        for (TGDGSat tgd : filter.getSubsumedCandidates(newTGD)) {
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
    public boolean subsumed(TGDGSat newTGD) {
        var bodyN = newTGD.getBodySet();
        var headN = newTGD.getHeadSet();

        for (TGDGSat tgd : filter.getSubsumingCandidates(newTGD)) {
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

    public void add(TGDGSat newTGD) {
        filter.add(newTGD);
    }

    public Collection<TGDGSat> getAll() {
        return filter.getAll();
    }

    public long getNumberSubsumed() {
        return num_subsumed;
    }

    public long getFilterDiscarded() {
        return num_filter_discarded;
    }

}