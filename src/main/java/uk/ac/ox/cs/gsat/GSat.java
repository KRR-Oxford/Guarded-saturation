package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * GSat
 */
public class GSat {

    public static Collection<TGDGSat> runGSat(Dependency[] allDependencies) {

        System.out.println("Running GSat...");
        final long startTime = System.nanoTime();

        Collection<TGDGSat> newTGDs = new HashSet<>();

        int discarded = 0;

        for (Dependency d : allDependencies)
            if (d instanceof TGD && ((TGD) d).isGuarded()) // Adding only Guarded TGDs
                // if (!(d instanceof EGD))
                newTGDs.addAll(VNFs(HNF((TGD) d)));
            else
                discarded++;

        App.logger.info("GSat discarded rules : " + discarded + "/" + allDependencies.length + " = "
                + String.format(Locale.UK, "%.3f", (float) discarded / allDependencies.length * 100) + "%");

        App.logger.debug("# initial TGDs: " + newTGDs.size());
        newTGDs.forEach(App.logger::debug);

        Collection<TGDGSat> nonFullTGDs = new HashSet<>();
        Collection<TGDGSat> fullTGDs = new HashSet<>();

        // for (TGD d : allDependencies)
        // if (isFull(d))
        // fullTGDs.add(VNF(d));
        // else {
        // Collection<TGD> hnf = HNF(d);
        // TGD[] dh = hnf.toArray(new TGD[hnf.size()]);
        // if (hnf.size() == 1)
        // nonFullTGDs.add(VNF(dh[0]));
        // else {
        // nonFullTGDs.add(VNF(dh[0]));
        // fullTGDs.add(VNF(dh[1]));
        // }
        // }
        //
        // App.logger.debug("# nonFullTGDs: " + nonFullTGDs.size());
        // nonFullTGDs.forEach(App.logger.debug);
        // App.logger.debug("# fullTGDs: " + fullTGDs.size());
        // fullTGDs.forEach(App.logger.debug);
        // if (!nonFullTGDs.isEmpty())
        // App.logger.debug("First non full TGD: " + nonFullTGDs.toArray()[0]);
        // if (!fullTGDs.isEmpty())
        // App.logger.debug("First full TGD: " + fullTGDs.toArray()[0]);
        //
        // newTGDs.addAll(nonFullTGDs);
        // newTGDs.addAll(fullTGDs);

        while (!newTGDs.isEmpty()) {
            // System.out.print('.');

            App.logger.warn("# new TGDs: " + newTGDs.size());
            newTGDs.forEach(App.logger::debug);

            TGDGSat currentTGD = newTGDs.iterator().next();
            App.logger.debug("current TGD: " + currentTGD);
            newTGDs.remove(currentTGD);

            Collection<TGDGSat> tempTGDsSet = new ArrayList<>();

            if (Logic.isFull(currentTGD)) {
                fullTGDs.add(currentTGD);
                for (TGDGSat nftgd : nonFullTGDs)
                    tempTGDsSet.addAll(VNFs(HNF(evolve(nftgd, currentTGD))));
            } else {
                nonFullTGDs.add(currentTGD);
                for (TGDGSat ftgd : fullTGDs)
                    tempTGDsSet.addAll(VNFs(HNF(evolve(currentTGD, ftgd))));
            }

            for (TGDGSat d : tempTGDsSet)
                if ((Logic.isFull(d) && !fullTGDs.contains(d)) || (!Logic.isFull(d) && !nonFullTGDs.contains(d))) {
                    App.logger.debug("adding new TGD: " + d + "\t" + d.equals(currentTGD) + ": "
                            + nonFullTGDs.contains(currentTGD) + ": " + nonFullTGDs.contains(d) + ": "
                            + Objects.equals(d, currentTGD));
                    newTGDs.add(d);
                }

        }
        // System.out.println();

        final long stopTime = System.nanoTime();

        long totalTime = stopTime - startTime;

        App.logger.info("GSat total time : " + String.format(Locale.UK, "%.0f", totalTime / 1E6) + " ms = "
                + String.format(Locale.UK, "%.2f", totalTime / 1E9) + " s");

        return fullTGDs;

    }

    public static Collection<TGD> HNF(TGD tgd) {

        Collection<TGD> result = new HashSet<>();

        if (tgd == null)
            return result;

        Variable[] eVariables = tgd.getExistential();

        Collection<Atom> eHead = new HashSet<>();
        Collection<Atom> fHead = new HashSet<>();

        for (Atom a : tgd.getHeadAtoms())
            if (Logic.containsAny(a, eVariables))
                eHead.add(a);
            else
                fHead.add(a);

        if (eHead.isEmpty() || fHead.isEmpty())
            result.add(tgd);
        else {
            result.add(TGD.create(tgd.getBodyAtoms(), eHead.toArray(new Atom[eHead.size()])));
            result.add(TGD.create(tgd.getBodyAtoms(), fHead.toArray(new Atom[fHead.size()])));
        }

        return result;

    }

    public static Collection<TGDGSat> VNFs(Collection<TGD> tgds) {

        return tgds.stream().map((tgd) -> VNF(tgd)).collect(Collectors.toList());

    }

    public static TGDGSat VNF(TGD tgd) {

        if (tgd == null)
            throw new IllegalArgumentException("Null TGD in VNF");

        Variable[] uVariables = tgd.getUniversal();
        Variable[] eVariables = tgd.getExistential();
        App.logger.trace(uVariables);
        App.logger.trace(eVariables);

        Map<Term, Term> substitution = new HashMap<>();
        int counter = 1;
        for (Variable v : uVariables)
            substitution.put(v, Variable.create("u" + counter++));
        counter = 1;
        for (Variable v : eVariables)
            substitution.put(v, Variable.create("e" + counter++));

        App.logger.debug("VNF substitution:\n" + substitution);

        TGDGSat applySubstitution = new TGDGSat((TGD) Logic.applySubstitution(tgd, substitution));
        App.logger.debug("VNF: " + tgd + "===>>>" + applySubstitution);
        return applySubstitution;

    }

    public static TGD evolve(TGD nftgd, TGD ftgd) {

        ftgd = evolveRename(ftgd);

        App.logger.debug("Composing:\n" + nftgd + "\nand\n" + ftgd);

        Collection<Atom> joinAtoms = getJoinAtoms(nftgd.getHeadAtoms(), ftgd.getBodyAtoms());
        if (joinAtoms.isEmpty())
            return null;
        App.logger.debug("Join atoms:");
        joinAtoms.forEach(App.logger::debug);

        // TGD evolveRule =
        // if (existentialVariableCheck(evolveRule, joinAtoms))
        // return evolveRule;
        return getEvolveRule(nftgd, ftgd, joinAtoms);

    }

    public static TGD evolveRename(TGD ftgd) {

        Variable[] uVariables = ftgd.getUniversal();

        Map<Term, Term> substitution = new HashMap<>();
        int counter = 1;
        for (Variable v : uVariables)
            substitution.put(v, Variable.create("z" + counter++));

        return (TGD) Logic.applySubstitution(ftgd, substitution);

    }

    public static Collection<Atom> getJoinAtoms(Atom[] headAtoms, Atom[] bodyAtoms) {

        Collection<Atom> result = new HashSet<>();

        for (Atom bodyAtom : bodyAtoms)
            for (Atom headAtom : headAtoms)
                if (bodyAtom.getPredicate().equals(headAtom.getPredicate())) {
                    result.add(bodyAtom);
                    continue;
                }

        return result;

    }

    public static TGD getEvolveRule(TGD nftgd, TGD ftgd, Collection<Atom> joinAtoms) {

        Collection<Atom> nftgdBodyAtoms = new HashSet<>(Arrays.asList(nftgd.getBodyAtoms()));
        Collection<Atom> nftgdHeadAtoms = new HashSet<>(Arrays.asList(nftgd.getHeadAtoms()));
        Collection<Atom> ftgdBodyAtoms = new HashSet<>(Arrays.asList(ftgd.getBodyAtoms()));
        Collection<Atom> ftgdHeadAtoms = new HashSet<>(Arrays.asList(ftgd.getHeadAtoms()));

        ftgdBodyAtoms.removeAll(joinAtoms);
        nftgdBodyAtoms.addAll(ftgdBodyAtoms);
        nftgdHeadAtoms.addAll(ftgdHeadAtoms);

        Map<Term, Term> mgu = getMGU(nftgd.getHeadAtoms(), ftgd.getBodyAtoms(), joinAtoms,
                Arrays.asList(nftgd.getExistential()));

        App.logger.debug("MGU: " + mgu);

        if (mgu != null) {
            TGD newTGD = TGD.create(applyMGU(nftgdBodyAtoms, mgu), applyMGU(nftgdHeadAtoms, mgu));
            App.logger.debug("After applying MGU: " + newTGD);
            return newTGD;
        }

        return null;

    }

    public static Map<Term, Term> getMGU(Atom[] headAtoms, Atom[] bodyAtoms, Collection<Atom> joinAtoms,
            Collection<Variable> existentials) {
        // FIXME it works only if there are no duplicate atoms in the 2 arrays

        Map<Term, Term> result = new HashMap<>();

        for (Atom bodyAtom : joinAtoms)
            for (Atom headAtom : headAtoms)
                if (bodyAtom.getPredicate().equals(headAtom.getPredicate()))
                    for (int i = 0; i < bodyAtom.getPredicate().getArity(); i++) {
                        Term currentTermBody = bodyAtom.getTerm(i);
                        Term currentTermHead = headAtom.getTerm(i);
                        if (currentTermBody.isVariable() && currentTermHead.isVariable())
                            if (result.containsKey(currentTermBody)) {
                                if (!result.get(currentTermBody).equals(currentTermHead))
                                    return null;
                            } else
                                result.put(currentTermBody, currentTermHead);
                        else if (!currentTermBody.isVariable() && !currentTermHead.isVariable()) {
                            if (!currentTermBody.equals(currentTermHead)) // Clash
                                return null;
                        } else if (!currentTermBody.isVariable())// currentTermBody is the constant
                            if (existentials.contains(currentTermHead)) // Identity on y
                                return null;
                            else if (result.containsKey(currentTermHead)) {
                                if (!result.get(currentTermBody).equals(currentTermHead))
                                    return null;
                            } else
                                result.put(currentTermHead, currentTermBody);
                        else // currentTermHead is the constant
                        if (result.containsKey(currentTermBody)) {
                            if (!result.get(currentTermBody).equals(currentTermHead))
                                return null;
                        } else
                            result.put(currentTermBody, currentTermHead);

                    }

        // existential variable check (evc)
        for (Atom a : bodyAtoms)
            if (!joinAtoms.contains(a))
                for (Term t : a.getTerms())
                    if (result.containsKey(t) && existentials.contains(result.get(t)))
                        return null;

        return result;

    }

    public static Atom[] applyMGU(Collection<Atom> nftgdHeadAtoms, Map<Term, Term> mgu) {

        Collection<Atom> result = new HashSet<>();

        nftgdHeadAtoms.forEach(atom -> result.add((Atom) Logic.applySubstitution(atom, mgu)));

        return result.toArray(new Atom[result.size()]);

    }

}