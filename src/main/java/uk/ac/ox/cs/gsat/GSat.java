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

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * Guarded Saturation (GSat)
 * The input guarded TGDs are first translated 
 * into Head Normal Formal.
 * The evolve function takes as inputs: 
 * - left: a non full TGD
 * - right: a full TGD
 */
public class GSat extends EvolveBasedSat<GTGD> {

    protected static final TGDFactory<GTGD> FACTORY = TGDFactory.getGTGDInstance();
    private static final String NAME = "GSat";
    private static final GSat INSTANCE = new GSat();

    /**
     * Private construtor, we want this class to be a Singleton
     */
    private GSat() {
        super(NAME, FACTORY, EvolveStatistics.getFactory());
    }

    /**
     *
     * @return Singleton instace of GSat
     */
    public static GSat getInstance() {
        return INSTANCE;
    }

    @Override
    protected Collection<GTGD> transformInputTGDs(Collection<GTGD> inputTGDs) {
        Collection<GTGD> result = new ArrayList<>();

        for(GTGD tgd : inputTGDs)
            for (GTGD hnf : FACTORY.computeHNF(tgd))
                result.add(FACTORY.computeVNF(hnf, eVariable, uVariable));

        return result;
    }
    
    @Override
    protected Collection<GTGD> getOutput(Collection<GTGD> fullTGDs) {
        return fullTGDs;
    }

    /**
     *
     * @param nftgd non-full TGD (guarded)
     * @param ftgd  full TGD (guarded)
     * @return the derived rules of nftgd and ftgd according to the EVOLVE inference
     *         rule
     */
    public Collection<GTGD> evolveNew(GTGD nftgd, GTGD ftgd) {

        ftgd = renameVariable(ftgd);

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

                Set<Atom> new_body = new HashSet<>();
                new_body.addAll(new_ftgd_body_atoms);
                Atom new_guard = (Atom) Logic.applySubstitution(guard, guardMGU);
                new_body.remove(new_guard);

                List<Atom> Sbody = getSbody(new_body, new_nftgd_existentials);
                new_body.addAll(new_nftgd_body_atoms);
                Set<Atom> new_head = new HashSet<>(new_nftgd_head_atoms);
                // we save if new atoms have been added from the new non full TGD head
                boolean isNewHeadNFTGDHead = !new_head.addAll(new_ftgd_head_atoms);

                List<List<Atom>> Shead = getShead(new_nftgd_head_atoms, Sbody, new_nftgd_existentials);

                // if Sbody is empty, then Shead is empty, and we take this short-cut;
                // in fact, we should never have Shead == null and Sbody.isEmpty
                if (Shead == null || Shead.isEmpty()) {
                    // we skip the cases, where the full TGD is linear and the new head is equals to
                    // the new non full TGD head, since the resulting TGD is u(B1) -> u(H1)
                    // which is subsumed by the non full TGD
                    if (Sbody.isEmpty() && !(new_ftgd_body_atoms.size() == 1 && isNewHeadNFTGDHead)) {
						for (GTGD hnf : FACTORY.computeHNF(new GTGD(new_body, new_head)))
							results.add(FACTORY.computeVNF(hnf, eVariable, uVariable));
					}
                    // no matching head atom for some atom in Sbody -> continue
                    continue;
                }

                App.logger.fine("Shead:" + Shead.toString());

                for (List<Atom> S : SaturationUtils.getProduct(Shead)) {

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
	protected boolean isRightTGD(GTGD newTGD) {
		return Logic.isFull(newTGD);
	}

	@Override
	protected boolean isLeftTGD(GTGD newTGD) {
		return !Logic.isFull(newTGD);
	}

	@Override
	protected Atom[] getUnifiableBodyAtoms(GTGD rightTGD) {
        return new Atom[] { rightTGD.getGuard() };
	}

}
