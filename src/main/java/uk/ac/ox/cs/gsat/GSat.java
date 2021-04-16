package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.ox.cs.gsat.subsumers.Subsumer;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * GSat
 */
public class GSat extends AbstractGSat {

    private static final GSat INSTANCE = new GSat();

    /**
     * Private construtor, we want this class to be a Singleton
     */
    private GSat() {
    }

    /**
     *
     * @return Singleton instace of GSat
     */
    public static GSat getInstance() {
        return INSTANCE;
    }

    protected void initialization(Collection<GTGD> selectedTGDs, Set<GTGD> fullTGDsSet, Collection<GTGD> newNonFullTGDs,
            Map<Predicate, Set<GTGD>> fullTGDsMap, Subsumer<GTGD> fullTGDsSubsumer,
            Subsumer<GTGD> nonFullTGDsSubsumer) {
        for (GTGD tgd : selectedTGDs)
            for (GTGD hnf : FACTORY.computeHNF(tgd)) {
                GTGD currentGTGD = FACTORY.computeVNF(hnf, eVariable, uVariable);
                if (Logic.isFull(currentGTGD)) {
                    addFullTGD(currentGTGD, fullTGDsMap, fullTGDsSet);
                    fullTGDsSubsumer.add(currentGTGD);
                } else {
                    nonFullTGDsSubsumer.add(currentGTGD);
                    newNonFullTGDs.add(currentGTGD);
                }
            }
    }

    /**
     *
     * @param nftgd non-full TGD (guarded)
     * @param ftgd  full TGD (guarded)
     * @return the derived rules of nftgd and ftgd according to the EVOLVE inference
     *         rule
     */
    public Collection<GTGD> evolveNew(GTGD nftgd, GTGD ftgd) {

        ftgd = evolveRename(ftgd);

        App.logger.fine("Composing:\n" + nftgd + "\nand\n" + ftgd);

        Atom guard = new GTGD(ftgd.getBodySet(), ftgd.getHeadSet()).getGuard();
        Collection<GTGD> results = new HashSet<>();

        for (Atom H : nftgd.getHeadAtoms()) {

            Map<Term, Term> guardMGU = getGuardMGU(guard, H);

            if (guardMGU != null && !guardMGU.isEmpty()) {

                final GTGD new_nftgd = Logic.applyMGU(nftgd, guardMGU);
                final GTGD new_ftgd = Logic.applyMGU(ftgd, guardMGU);

                final List<Variable> new_nftgd_existentials = Arrays.asList(new_nftgd.getExistential());

                var new_nftgd_head_atoms = new_nftgd.getHeadSet();
                var new_nftgd_body_atoms = new_nftgd.getBodySet();
                var new_ftgd_head_atoms = new_ftgd.getHeadSet();
                var new_ftgd_body_atoms = new_ftgd.getBodySet();

                Set<Atom> new_body = new HashSet<>(new_ftgd_body_atoms);
                Atom new_guard = (Atom) Logic.applySubstitution(guard, guardMGU);
                new_body.remove(new_guard);
                List<Atom> Sbody = getSbody(new_body, new_nftgd_existentials);
                new_body.addAll(new_nftgd_body_atoms);
                Set<Atom> new_head = new HashSet<>(new_nftgd_head_atoms);
                new_head.addAll(new_ftgd_head_atoms);

                List<List<Atom>> Shead = getShead(new_nftgd_head_atoms, Sbody, new_nftgd_existentials);

                // if Sbody is empty, then Shead is empty, and we take this short-cut;
                // in fact, we should never have Shead == null and Sbody.isEmpty
                if (Shead == null || Shead.isEmpty()) {
                    if (Sbody.isEmpty()) {
						for (GTGD hnf : FACTORY.computeHNF(new GTGD(new_body, new_head)))
							results.add(FACTORY.computeVNF(hnf, eVariable, uVariable));
					}
                    // no matching head atom for some atom in Sbody -> continue
                    continue;
                }

                App.logger.fine("Shead:" + Shead.toString());

                for (List<Atom> S : getProduct(Shead)) {

                    App.logger.fine("Non-Full:" + new_nftgd.toString() + "\nFull:" + new_ftgd.toString() + "\nSbody:"
                            + Sbody + "\nS:" + S);

                    Map<Term, Term> mgu = getVariableSubstitution(S, Sbody);
                    if (mgu == null)
                        // unification failed -> continue with next sequence
                        continue;

                    new_body.removeAll(Sbody);

                    if (mgu.isEmpty())
                        // no need to apply the MGU
						for (GTGD hnf : FACTORY.computeHNF(new GTGD(new_body, new_head)))
							results.add(FACTORY.computeVNF(hnf, eVariable, uVariable));
                    else
						for (GTGD hnf : FACTORY.computeHNF(Logic.applyMGU(new_body, new_head, mgu)))
							results.add(FACTORY.computeVNF(hnf, eVariable, uVariable));

                }

            }

        }

        return results;

    }

    static Map<Term, Term> getVariableSubstitution(List<Atom> head, List<Atom> body) {

        Map<Term, Term> sigma = new HashMap<>();

        if (head.size() != body.size())
            throw new IllegalArgumentException();

        // assume they are all in the same order
        for (int i = 0; i < head.size(); i++) {
            Atom atom_h = head.get(i);
            Atom atom_b = body.get(i);

            if (!atom_h.getPredicate().equals(atom_b.getPredicate()))
                throw new IllegalArgumentException();

            sigma = Logic.getMGU(atom_h, atom_b, sigma);

        }

        return sigma;
    }

    /**
     * Mainly from https://stackoverflow.com/a/9496234
     *
     * @param shead
     * @return
     */
    static List<List<Atom>> getProduct(List<List<Atom>> shead) {

        List<List<Atom>> resultLists = new ArrayList<List<Atom>>();
        if (shead.size() == 0) {
            resultLists.add(new ArrayList<Atom>());
            return resultLists;
        } else {
            List<Atom> firstList = shead.get(0);
            List<List<Atom>> remainingLists = getProduct(shead.subList(1, shead.size()));
            for (Atom condition : firstList) {
                for (List<Atom> remainingList : remainingLists) {
                    ArrayList<Atom> resultList = new ArrayList<Atom>(1 + remainingList.size());
                    resultList.add(condition);
                    resultList.addAll(remainingList);
                    resultLists.add(resultList);
                }
            }
        }
        return resultLists;
    }

    static List<List<Atom>> getShead(Collection<Atom> headAtoms, Collection<Atom> sbody, Collection<Variable> eVariables) {

        List<List<Atom>> resultLists = new ArrayList<List<Atom>>();

        for (Atom bodyAtom : sbody) {

            List<Atom> temp = new ArrayList<>();

            for (Atom headAtom : headAtoms)
                if (headAtom.getPredicate().equals(bodyAtom.getPredicate())) {
                    boolean valid = true;
                    Term[] headTerms = headAtom.getTerms();
                    for (int i = 0; i < headTerms.length; i++) {
                        Term bodyTerm = bodyAtom.getTerm(i);
                        Term headTerm = headTerms[i];
                        // check if constants and existentials match
                        if (!bodyTerm.equals(headTerm) && (headTerm.isUntypedConstant() && bodyTerm.isUntypedConstant()
                                || eVariables.contains(headTerm) || eVariables.contains(bodyTerm))) {
                            valid = false;
                            break;
                        }
                    }

                    if (valid)
                        temp.add(headAtom);

                }

            if (temp.isEmpty())
                return null;

            resultLists.add(temp);

        }

        return resultLists;

    }

    static List<Atom> getSbody(Collection<Atom> new_bodyAtoms, Collection<Variable> eVariables) {

        return new_bodyAtoms.stream().filter(atom -> containsY(atom, eVariables)).collect(Collectors.toList());

    }

    private static boolean containsY(Atom atom, Collection<Variable> eVariables) {

        for (Term term : atom.getTerms())
            if (eVariables.contains(term))
                return true;

        return false;

    }


    private Map<Term, Term> getGuardMGU(Atom guard, Atom h) {
        return getGuardMGU(guard, h, eVariable, uVariable);
    }
    
    static Map<Term, Term> getGuardMGU(Atom guard, Atom h, String eVariable, String uVariable) {
        Map<Term, Term> mgu = Logic.getMGU(guard, h);

        if (mgu == null)
            return null;

        for (Entry<Term, Term> entry : mgu.entrySet()) {

            Term key = entry.getKey();
            boolean isVariable = key.isVariable();
            if (isVariable) {
                String symbol = ((Variable) key).getSymbol();

                // identity on y
                if (symbol.startsWith(eVariable))
                    return null;

                // evc xθ ∩ y = ∅
                Term value = entry.getValue();
                if (value.isVariable() && symbol.startsWith(uVariable)
                        && ((Variable) value).getSymbol().startsWith(eVariable))
                    return null;

            }

        }

        return mgu;

    }

	@Override
	protected boolean isFull(GTGD newTGD) {
		return Logic.isFull(newTGD);
	}

	@Override
	protected boolean isNonFull(GTGD newTGD) {
		return !Logic.isFull(newTGD);
	}

	@Override
	protected Collection<Predicate> getUnifiableBodyPredicates(GTGD tgd) {
		return List.of(tgd.getGuard().getPredicate());
	}

}
