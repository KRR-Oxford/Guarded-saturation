package uk.ac.ox.cs.gsat.fol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Function;
import uk.ac.ox.cs.pdq.fol.FunctionTerm;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Term;

public class OrderedSkGTGD extends SkGTGD {
    private static final Map<Predicate, Integer> PREDICATE_INDEXES = new HashMap<>();
    private static final Map<Function, Integer> FUNCTION_INDEXES = new HashMap<>();

    private Atom[] maxAtoms;
    private Boolean areMaxAtomsInBody;

    private OrderedSkGTGD(Set<Atom> body, Set<Atom> head) {
        super(body, head);
    }

    public static OrderedSkGTGD create(Set<Atom> body, Set<Atom> head) {
        return Cache.ordskgtgd.retrieve(new OrderedSkGTGD(body, head));
    }

    public Atom[] getMaxOrSelectedAtoms() {
        if (maxAtoms == null)
            initMaxOrSelectedAtoms();
        return maxAtoms;
    }

    public Boolean areMaxAtomInBody() {
        if (areMaxAtomsInBody == null)
            initMaxOrSelectedAtoms();
        return areMaxAtomsInBody;
    }

    
    private void initMaxOrSelectedAtoms() {
        if (this.isNonFull) {
            this.maxAtoms = this.getHeadAtoms();
            areMaxAtomsInBody = false;
        } else {
            if (this.isFunctional) {
                // in this case, there are functional atoms in the body and the head
                // we compute the maximal atoms among all
                List<Atom> maxAtoms;

                // if (Configuration.isOrderedSkolemSatSelectSkolemBodyAtom())
                    // maxAtoms = computeMaxAtom(this.getBodyAtoms());
                // else
                    maxAtoms = computeMaxAtom(this.getAtoms());

                // if the head's atom is maximal
                if (maxAtoms.containsAll(this.getHeadSet())) {
                    if (maxAtoms.size() > 1) {
                        // I hope there is no case
                        throw new IllegalStateException();
                    }
                    this.maxAtoms = this.getHeadAtoms();
                    areMaxAtomsInBody = false;
                } else {
                    this.maxAtoms = maxAtoms.toArray(new Atom[0]);
                    areMaxAtomsInBody = true;
                }
                
            } else {
                this.maxAtoms = new Atom[] { computeGuard() };
                areMaxAtomsInBody = true;
            }
        }
        // System.out.println("max " + this + " : " + Arrays.toString(this.maxAtoms) + " " + areMaxAtomsInBody);
    }

    /*
     * compute the max atom of a collection of atoms that contains at least one
     * functional term
     */
    private List<Atom> computeMaxAtom(Atom[] atoms) {
        int maxPredicateIndex = -1;
        List<Atom> maxAtoms = new ArrayList<Atom>();
        Atom maxAtom = null;
        int maxFunctionIndex = -1;
        int minFunctionPosition = Integer.MAX_VALUE;
        FunctionTerm maxFunctionTerm = null;
        for (Atom atom : atoms) {

            Predicate p = atom.getPredicate();
            int pIndex = getPredicateIndex(p);
            for (int pos = 0; pos < p.getArity(); pos++) {
                Term term = atom.getTerm(pos);
                if (term instanceof FunctionTerm) {
                    FunctionTerm fTerm = ((FunctionTerm) term);

                    int comp = compareWithMaximal(fTerm, maxFunctionTerm, maxFunctionIndex, pIndex, maxPredicateIndex,
                                                  pos, minFunctionPosition);
                    // System.out.println("-> "
                    // + comp
                    // + " " + getFunctionIndex(fTerm.getFunction()) + " " + fTerm + " " +
                    // maxFunctionTerm + " " +
                    // + maxFunctionIndex + " " + pIndex + " " + maxPredicateIndex + " " + pos + " "
                    // + minFunctionPosition);

                    if (comp > 0) {
                        maxPredicateIndex = pIndex;
                        minFunctionPosition = pos;
                        maxFunctionIndex = getFunctionIndex(fTerm.getFunction());
                        maxFunctionTerm = ((FunctionTerm) term);
                        maxAtom = atom;
                        maxAtoms.clear();
                        maxAtoms.add(maxAtom);
                    } else if (comp == 0) {
                        maxAtoms.add(maxAtom);
                    }
                }
            }
        }
        return maxAtoms;
    }

    private int compareWithMaximal(FunctionTerm fTerm, FunctionTerm maxFunctionTerm, int maxFunctionIndex,
            int pIndex, int maxPredicateIndex, int fPosition, int minFunctionPosition) {

        int fIndex = getFunctionIndex(fTerm.getFunction());
        if (fIndex > maxFunctionIndex) {
            return 1;
        } else if (fIndex == maxFunctionIndex) {
            if (pIndex > maxPredicateIndex) {
                return 1;
            } else if (pIndex == maxPredicateIndex) {
                if (fPosition < minFunctionPosition) {
                    return 1;
                } else if (fPosition == minFunctionPosition) {
                    Term[] fSubTerms = fTerm.getTerms();
                    Term[] maxSubTerms = maxFunctionTerm.getTerms();
                    for (int pos = 0; pos < fSubTerms.length; pos++) {
                        if (fSubTerms[pos].isVariable() && maxSubTerms[pos].isVariable() ||
                            fSubTerms[pos].equals(maxSubTerms[pos]))
                            continue;

                        if (!fSubTerms[pos].isVariable() && maxSubTerms[pos].isVariable())
                            return 1;

                        if (fSubTerms[pos].isVariable() && !maxSubTerms[pos].isVariable())
                            return -1;

                        int comp = fSubTerms[pos].toString().compareTo(maxSubTerms[pos].toString());
                        if (comp == 0)
                            continue;
                        else
                            return comp;
                    }
                    return 0;
                }
            }
        }

        return -1;
    }

    private int getPredicateIndex(Predicate p) {
        if (!PREDICATE_INDEXES.containsKey(p)) {
            PREDICATE_INDEXES.put(p, PREDICATE_INDEXES.size());
        }
        return PREDICATE_INDEXES.get(p);
    }

    private int getFunctionIndex(Function p) {
        if (!FUNCTION_INDEXES.containsKey(p)) {
            FUNCTION_INDEXES.put(p, FUNCTION_INDEXES.size());
        }
        return FUNCTION_INDEXES.get(p);
    }

}
