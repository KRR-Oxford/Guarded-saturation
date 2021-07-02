package uk.ac.ox.cs.gsat.subsumers;

import java.util.Collection;
import java.util.HashSet;

import uk.ac.ox.cs.gsat.TGD;
import uk.ac.ox.cs.gsat.filters.FormulaFilter;
import uk.ac.ox.cs.gsat.filters.TreePredicateFilter;

/**
 * A subsumer that uses an index to filter out only candidates for subsumption,
 * then identifies tgd a as subsumed by tgd b if b.head is contained in a.head,
 * and a.body is contained in b.body (without any unification).
 */
public class SimpleSubsumer<Q extends TGD> implements Subsumer<Q> {

    final FormulaFilter<Q> filter;
    private long num_filter_discarded = 0, num_subsumed = 0;

    public SimpleSubsumer(FormulaFilter<Q> filter) {
        this.filter = filter;
    }

    @Override
    public Collection<Q> subsumesAny(Q newTGD) {
        Collection<Q> subsumed = new HashSet<>();

        var bodyN = newTGD.getBodySet();
        var headN = newTGD.getHeadSet();

        long start = System.nanoTime();
        long candidatesCount = 0;
        for (Q tgd : filter.getSubsumedCandidates(newTGD)) {
            candidatesCount++;
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

        // System.out.println("subsumed " + candidatesCount + "  " + subsumed.size() + "  " + (System.nanoTime() - start) + "ns");
        
        return subsumed;
    }

    @Override
    public boolean subsumed(Q newTGD) {
        var bodyN = newTGD.getBodySet();
        var headN = newTGD.getHeadSet();
        long start = System.nanoTime();
        long candidatesCount = 0;
        for (Q tgd : filter.getSubsumingCandidates(newTGD)) {
            candidatesCount++;
            var body = tgd.getBodySet();
            var head = tgd.getHeadSet();

            if (bodyN.size() < body.size() || head.size() < headN.size()) {
                num_filter_discarded += 1;
                continue;
            }

            if (bodyN.containsAll(body) && head.containsAll(headN)) {
                num_subsumed += 1;
                // System.out.println("subsuming " + candidatesCount + "  " + 0 + "  " + (System.nanoTime() - start) + "ns");
                return true;
            } else {
                num_filter_discarded += 1;
            }
        }

        // System.out.println("subsuming " + candidatesCount + "  " + 0 + "  " + (System.nanoTime() - start) + "ns");
        return false;
    }

    public void add(Q newTGD) {
        filter.add(newTGD);
    }

    public Collection<Q> getAll() {
        return filter.getAll();
    }

    public long getNumberSubsumed() {
        return num_subsumed;
    }

    public long getFilterDiscarded() {
        return num_filter_discarded;
    }

    public void printIndex() {
        if (filter instanceof TreePredicateFilter)
            ((TreePredicateFilter) filter).printIndex("index.dot");
    }
}
