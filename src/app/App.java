import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

public class App {

	private static final Logger logger = LogManager.getLogger("Guarded saturation");

	public static void main(String[] args) throws Exception {

		System.out.println("Starting GSat...");

		String basePath = "test" + File.separator + "ChaseBench" + File.separator + "scenarios" + File.separator
				+ "correctness" + File.separator + "tgds5" + File.separator;

		logger.info("Reading from: '" + basePath + "'");

		Schema schema = Utility.readSchema(basePath, "tgds5");
		Dependency[] allDependencies = schema.getAllDependencies();

		logger.info("# Dependencies: " + allDependencies.length);
		logger.debug(schema);

		Collection<Atom> facts = Utility.readFacts(basePath, schema);
		logger.info("# Facts: " + facts.size());
		logger.debug(facts);

		Collection<ConjunctiveQuery> queries = Utility.readQueries(basePath, schema);
		logger.info("# Queries: " + queries.size());
		logger.debug(queries);

		Collection<Dependency> guardedSaturation = runGSat(allDependencies);
		logger.info("Rewriting completed!");
		System.out.println("Guarded saturation:");
		System.out.println("=========================================");
		guardedSaturation.forEach(System.out::println);
		System.out.println("=========================================");

	}

	public static Collection<Dependency> runGSat(Dependency[] allDependencies) {

		System.out.println("Running GSat...");

		Collection<Dependency> newDependencies = new HashSet<>();

		for (Dependency d : allDependencies)
			newDependencies.addAll(VNF(HNF(d)));

		logger.debug("# initial dependencies: " + newDependencies.size());
		newDependencies.forEach(logger::debug);

		Collection<Dependency> nonFullTGDs = new HashSet<>();
		Collection<Dependency> fullTGDs = new HashSet<>();

		// for (Dependency d : allDependencies)
		// if (isFull(d))
		// fullTGDs.add(VNF(d));
		// else {
		// Collection<Dependency> hnf = HNF(d);
		// Dependency[] dh = hnf.toArray(new Dependency[hnf.size()]);
		// if (hnf.size() == 1)
		// nonFullTGDs.add(VNF(dh[0]));
		// else {
		// nonFullTGDs.add(VNF(dh[0]));
		// fullTGDs.add(VNF(dh[1]));
		// }
		// }
		//
		// logger.debug("# nonFullTGDs: " + nonFullTGDs.size());
		// nonFullTGDs.forEach(logger.debug);
		// logger.debug("# fullTGDs: " + fullTGDs.size());
		// fullTGDs.forEach(logger.debug);
		// if (!nonFullTGDs.isEmpty())
		// logger.debug("First non full TGD: " + nonFullTGDs.toArray()[0]);
		// if (!fullTGDs.isEmpty())
		// logger.debug("First full TGD: " + fullTGDs.toArray()[0]);
		//
		// newDependencies.addAll(nonFullTGDs);
		// newDependencies.addAll(fullTGDs);

		while (!newDependencies.isEmpty()) {
			logger.info("# new dependencies: " + newDependencies.size());
			newDependencies.forEach(logger::info);

			Dependency currentDependency = newDependencies.iterator().next();
			newDependencies.remove(currentDependency);

			Set<Dependency> tempDependenciesSet = new HashSet<>();

			if (isFull(currentDependency)) {
				fullTGDs.add(currentDependency);
				for (Dependency nftgd : nonFullTGDs)
					tempDependenciesSet.addAll(VNF(HNF(evolve(nftgd, currentDependency))));
			} else {
				nonFullTGDs.add(currentDependency);
				for (Dependency ftgd : fullTGDs)
					tempDependenciesSet.addAll(VNF(HNF(evolve(currentDependency, ftgd))));
			}

			for (Dependency d : tempDependenciesSet)
				if (isFull(d) && !fullTGDs.contains(d) || !isFull(d) && !nonFullTGDs.contains(d))
					newDependencies.add(d);
		}

		return fullTGDs;

	}

	public static Collection<Dependency> HNF(Dependency dependency) {

		Collection<Dependency> result = new ArrayList<>();

		if (dependency == null)
			return result;

		Variable[] eVariables = dependency.getExistential();

		Collection<Atom> eHead = new LinkedList<>();
		Collection<Atom> fHead = new LinkedList<>();

		for (Atom a : dependency.getHeadAtoms())
			if (Utility.containsAny(a, eVariables))
				eHead.add(a);
			else
				fHead.add(a);

		if (eHead.isEmpty() || fHead.isEmpty())
			result.add(dependency);
		else {
			result.add(Dependency.create(dependency.getBodyAtoms(), eHead.toArray(new Atom[eHead.size()])));
			result.add(Dependency.create(dependency.getBodyAtoms(), fHead.toArray(new Atom[fHead.size()])));
		}

		return result;

	}

	public static Collection<Dependency> VNF(Collection<Dependency> dependencies) {

		Collection<Dependency> result = new LinkedList<>();

		for (Dependency d : dependencies)
			result.add(VNF(d));

		return result;

	}

	public static Dependency VNF(Dependency dependency) {

		assert dependency != null;

		Variable[] uVariables = dependency.getUniversal();
		Variable[] eVariables = dependency.getExistential();
		logger.trace(uVariables);
		logger.trace(eVariables);

		Map<Term, Term> substitution = new HashMap<>();
		int counter = 1;
		for (Variable v : uVariables)
			substitution.put(v, Variable.create("u" + counter++));
		counter = 1;
		for (Variable v : eVariables)
			substitution.put(v, Variable.create("e" + counter++));

		logger.debug("VNF substitution:\n" + substitution);

		// Dependency applySubstitution = (Dependency)
		// Utility.applySubstitution(dependency, substitution);
		// logger.debug("VNF: " + dependency + "===>>>" + applySubstitution);
		// return applySubstitution;
		return (Dependency) Utility.applySubstitution(dependency, substitution);

	}

	public static boolean isFull(Dependency dependency) {

		return dependency.getExistential().length == 0;

	}

	public static Dependency evolve(Dependency nftgd, Dependency ftgd) {

		ftgd = evolveRename(ftgd);

		logger.debug("Composing:\n" + nftgd + "\nand\n" + ftgd);

		Collection<Atom> joinAtoms = getJoinAtoms(nftgd.getHeadAtoms(), ftgd.getBodyAtoms());
		if (joinAtoms.isEmpty())
			return null;
		logger.debug("Join atoms:");
		joinAtoms.forEach(logger::debug);

		// Dependency evolveRule =
		// if (existentialVariableCheck(evolveRule, joinAtoms))
		// return evolveRule;
		return getEvolveRule(nftgd, ftgd, joinAtoms);

	}

	public static Dependency evolveRename(Dependency ftgd) {

		Variable[] uVariables = ftgd.getUniversal();

		Map<Term, Term> substitution = new HashMap<>();
		int counter = 1;
		for (Variable v : uVariables)
			substitution.put(v, Variable.create("z" + counter++));

		return (Dependency) Utility.applySubstitution(ftgd, substitution);

	}

	public static Collection<Atom> getJoinAtoms(Atom[] headAtoms, Atom[] bodyAtoms) {

		Collection<Atom> result = new LinkedList<>();

		for (Atom bodyAtom : bodyAtoms)
			for (Atom headAtom : headAtoms)
				if (bodyAtom.getPredicate().equals(headAtom.getPredicate())) {
					result.add(bodyAtom);
					continue;
				}

		return result;

	}

	public static Dependency getEvolveRule(Dependency nftgd, Dependency ftgd, Collection<Atom> joinAtoms) {

		Collection<Atom> nftgdBodyAtoms = new ArrayList<>(Arrays.asList(nftgd.getBodyAtoms()));
		Collection<Atom> nftgdHeadAtoms = new ArrayList<>(Arrays.asList(nftgd.getHeadAtoms()));
		Collection<Atom> ftgdBodyAtoms = new ArrayList<>(Arrays.asList(ftgd.getBodyAtoms()));
		Collection<Atom> ftgdHeadAtoms = new ArrayList<>(Arrays.asList(ftgd.getHeadAtoms()));

		ftgdBodyAtoms.removeAll(joinAtoms);
		nftgdBodyAtoms.addAll(ftgdBodyAtoms);
		nftgdHeadAtoms.addAll(ftgdHeadAtoms);

		Map<Term, Term> mgu = getMGU(nftgd.getHeadAtoms(), ftgd.getBodyAtoms(), joinAtoms,
				Arrays.asList(nftgd.getExistential()));

		logger.debug("MGU: " + mgu);

		if (mgu != null) {
			Dependency newDependency = Dependency.create(applyMGU(nftgdBodyAtoms, mgu), applyMGU(nftgdHeadAtoms, mgu));
			logger.debug(newDependency);
			return newDependency;
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
							// if (existentials.contains(currentTermHead)) // Identity on y
							// ;
							// else
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

		// evc
		for (Atom a : bodyAtoms)
			if (!joinAtoms.contains(a))
				for (Term t : a.getTerms())
					if (result.containsKey(t) && existentials.contains(result.get(t)))
						return null;

		return result;

	}

	public static Atom[] applyMGU(Collection<Atom> nftgdHeadAtoms, Map<Term, Term> mgu) {

		Collection<Atom> result = new LinkedList<>();

		nftgdHeadAtoms.forEach(atom -> result.add((Atom) Utility.applySubstitution(atom, mgu)));

		return result.toArray(new Atom[result.size()]);

	}

}
