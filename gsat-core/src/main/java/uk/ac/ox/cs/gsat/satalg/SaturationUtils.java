package uk.ac.ox.cs.gsat.satalg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.ac.ox.cs.gsat.filters.FormulaFilter;
import uk.ac.ox.cs.gsat.filters.IdentityFormulaFilter;
import uk.ac.ox.cs.gsat.filters.MinAtomFilter;
import uk.ac.ox.cs.gsat.filters.MinPredicateFilter;
import uk.ac.ox.cs.gsat.filters.TreePredicateFilter;
import uk.ac.ox.cs.gsat.fol.GTGD;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.subsumers.DisabledSubsumer;
import uk.ac.ox.cs.gsat.subsumers.ExactAtomSubsumer;
import uk.ac.ox.cs.gsat.subsumers.SimpleSubsumer;
import uk.ac.ox.cs.gsat.subsumers.Subsumer;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.Predicate;

class SaturationUtils {

    /**
     * Mainly from https://stackoverflow.com/a/9496234
     *
     * @param input
     * @return
     */
    static <T> List<List<T>> getProduct(List<? extends Collection<T>> input) {

        List<List<T>> resultLists = new ArrayList<List<T>>();
        if (input.size() == 0) {
            resultLists.add(new ArrayList<>());
            return resultLists;
        } else {
            Collection<T> firstList = input.get(0);
            List<List<T>> remainingLists = getProduct(input.subList(1, input.size()));
            for (T condition : firstList) {
                for (List<T> remainingList : remainingLists) {
                    ArrayList<T> resultList = new ArrayList<>(1 + remainingList.size());
                    resultList.add(condition);
                    resultList.addAll(remainingList);
                    resultLists.add(resultList);
                }
            }
        }
        return resultLists;
    }

    /**
     * check whether tgd1 is subsumed by tgd2 by checking the inclusion of the atom
     * sets formed by the body and the head ie. without homomrphism check
     */
    static <Q extends GTGD> boolean subsumed(Q tgd1, Q tgd2) {

        var body1 = tgd1.getBodySet();
        var headN = tgd1.getHeadSet();

        var body = tgd2.getBodySet();
        var head = tgd2.getHeadSet();

        if (body1.size() < body.size() || head.size() < headN.size())
            return false;

        if (body1.containsAll(body) && head.containsAll(headN))
            return true;

        return false;
    }

    /**
     * return the predicates appearing the body atoms of the inputted TGDs
     */
    static Collection<Predicate> getBodyPredicates(Collection<? extends GTGD> tgds) {
        Collection<Predicate> result = new HashSet<>();

        for (GTGD tgd : tgds)
            for (Atom a : tgd.getBodyAtoms())
                result.add(a.getPredicate());

        return result;
    }

    /**
     * check whether the inputted dependency is a guarded TGD.
     */    
    static boolean isSupportedRule(Dependency d) {
        // Adding only Guarded TGDs
        return d instanceof uk.ac.ox.cs.pdq.fol.TGD && ((uk.ac.ox.cs.pdq.fol.TGD) d).isGuarded(); // Adding only Guarded
                                                                                                  // TGDs
    }
    
    /**
     * Comparator of TGD allows to sort the TGDs by ascending head atoms number and
     * by descending body atom bumber
     */
    protected static Comparator<GTGD> comparator = (tgd1, tgd2) -> {

        int numberOfHeadAtoms1 = tgd1.getNumberOfHeadAtoms();
        int numberOfHeadAtoms2 = tgd2.getNumberOfHeadAtoms();
        if (numberOfHeadAtoms1 != numberOfHeadAtoms2)
            return numberOfHeadAtoms2 - numberOfHeadAtoms1;

        int numberOfBodyAtoms1 = tgd1.getNumberOfBodyAtoms();
        int numberOfBodyAtoms2 = tgd2.getNumberOfBodyAtoms();
        if (numberOfBodyAtoms1 != numberOfBodyAtoms2)
            return numberOfBodyAtoms1 - numberOfBodyAtoms2;

        if (tgd1.equals(tgd2))
            return 0;

        int compareTo = tgd1.hashCode() - tgd2.hashCode();
        if (compareTo != 0)
            return compareTo;

        // in case of clash
        compareTo = tgd1.toString().compareTo(tgd2.toString());
        return compareTo;
    };

    /**
     * Create a subsumer initialized with all the TGDs
     * @param initialTGDs - set of all TGDs
     * @param config - saturation configuration
     */
    static <P extends TGD> Subsumer<P> createSubsumer(Collection<P> initialTGDs, SaturationAlgorithmConfiguration config) {

        String subsumptionMethod = config.getSubsumptionMethod();
        Subsumer<P> subsumer;

        if (subsumptionMethod.equals("tree_atom")) {
            subsumer = new ExactAtomSubsumer<P>();
        } else if (subsumptionMethod.equals("disabled")) {
            subsumer = new DisabledSubsumer<P>();
        } else {
            FormulaFilter<P> filter;
            if (subsumptionMethod.equals("min_predicate")) {
                filter = new MinPredicateFilter<P>();
            } else if (subsumptionMethod.equals("min_atom")) {
                filter = new MinAtomFilter<P>();
            } else if (subsumptionMethod.equals("tree_predicate")) {
                filter = new TreePredicateFilter<P>(config);
            } else if (subsumptionMethod.equals("identity")) {
                filter = new IdentityFormulaFilter<P>();
            } else {
                throw new IllegalStateException("Subsumption method " + subsumptionMethod + " is not supported.");
            }
            filter.init(initialTGDs);
            subsumer = new SimpleSubsumer<P>(filter);
        }

        return subsumer;
    }

}
