package uk.ac.ox.cs.gsat.io;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uk.ac.ox.cs.gsat.api.io.TGDTransformation;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Predicate;

/**
 * Filter TGDs based on a set of predicates by keeping only the TGDs that contain one initial predicate in their head or inductively one predicate contained in the body of a kept TGD.
 */
public class PredicateDependenciesBasedFilter<T extends TGD> implements TGDTransformation<T> {

    private Collection<Predicate> predicates;

    public PredicateDependenciesBasedFilter(Collection<Predicate> predicates) {
        this.predicates = predicates;
    }

    @Override
    public Set<T> apply(Collection<T> tgds) {
        Set<T> result = new HashSet<T>();
        Set<Predicate> requiredHeadPredicates = requiredHeadPrecidates(tgds);

        for (T tgd: tgds) {
            boolean kept = false;
            for (Atom ha : tgd.getHeadAtoms()) {
                kept = requiredHeadPredicates.contains(ha.getPredicate());
                if (kept)
                    break;
            }
            if (kept)
                result.add(tgd);
        }
        return result;
    }

    protected Set<Predicate> requiredHeadPrecidates(Collection<T> tgds) {
        Set<Predicate> result = new HashSet<>();

        Set<Predicate> gathered = new HashSet<>(this.predicates);

        // graph of head predicates to body predicates
        Map<Predicate, Set<Predicate>> predicatesGraph = new HashMap<>();
        for (T tgd : tgds) {
            for (Atom ha : tgd.getHeadAtoms()) {
                for (Atom ba : tgd.getBodyAtoms()) {
                    if (predicatesGraph.containsKey(ha.getPredicate())) {
                        predicatesGraph.get(ha.getPredicate()).add(ba.getPredicate());
                    } else {
                        Set<Predicate> targets = new HashSet<>();
                        targets.add(ba.getPredicate());
                        predicatesGraph.put(ha.getPredicate(), targets);
                    }
                }
            }
        }
        
        while(!gathered.isEmpty()) {
            Set<Predicate> reached = new HashSet<>();
            for (Predicate p : gathered) {
                if (predicatesGraph.containsKey(p))
                    reached.addAll(predicatesGraph.get(p));
            }
            result.addAll(gathered);
            gathered = reached;
            gathered.removeAll(result);
        }

        return result;
    }
}
