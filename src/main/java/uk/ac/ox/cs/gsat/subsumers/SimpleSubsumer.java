package uk.ac.ox.cs.gsat.subsumers;

import java.util.Collection;
import java.util.HashSet;

import uk.ac.ox.cs.gsat.TGDGSat;
import uk.ac.ox.cs.gsat.filters.FormulaFilter;

public class SimpleSubsumer implements Subsumer {

    FormulaFilter filter;

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

            if (body.size() < bodyN.size() || headN.size() < head.size())
                continue;

            if (body.containsAll(bodyN) && headN.containsAll(head))
                subsumed.add(tgd);
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

            if (bodyN.size() < body.size() || head.size() < headN.size())
                continue;

            if (bodyN.containsAll(body) && head.containsAll(headN))
                return true;
        }

        return false;
    }

    public void add(TGDGSat newTGD) {
        filter.add(newTGD);
    }

    public Collection<TGDGSat> getAll() {
        return filter.getAll();
    }

}