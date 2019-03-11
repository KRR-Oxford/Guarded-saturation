import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

public class App {

	static final Logger logger = LogManager.getLogger("Guarded saturation");

	public static void main(String[] args) throws Exception {

		System.out.println("Starting GSat...");

		// String baseTest = "tgds";
		// String baseTest = "deep";
		// String baseTest = "doctors";
		String baseTest = "LUBM";

		// String basePath = "test" + File.separator + "ChaseBench" + File.separator +
		// "scenarios" + File.separator
		// + "correctness" + File.separator + baseTest + File.separator;
		// String basePath = ".." + File.separator + "pdq" + File.separator +
		// "regression" + File.separator + "test"
		// + File.separator + "chaseBench" + File.separator + baseTest + File.separator
		// + "100" + File.separator;
		// String basePath = ".." + File.separator + "pdq" + File.separator +
		// "regression" + File.separator + "test"
		// + File.separator + "chaseBench" + File.separator + baseTest + File.separator;
		String basePath = ".." + File.separator + "pdq" + File.separator + "regression" + File.separator + "test"
				+ File.separator + "chaseBench" + File.separator + baseTest + File.separator;

		String fact_querySize = "";
		// String fact_querySize = "10k";

		// for (String baseTest : new String[] { "tgds" , "tgds5", "tgdsEgds",
		// "tgdsEgdsLarge", "vldb2010", "weak" })
		// {
		// String basePath = "test" + File.separator + "ChaseBench" + File.separator +
		// "scenarios" + File.separator
		// + "correctness" + File.separator + baseTest + File.separator;
		executeChaseBenchTest(baseTest, basePath, fact_querySize);
		// }

	}

	public static void executeChaseBenchTest(String baseTest, String basePath, String fact_querySize) {
		logger.info("Reading from: '" + basePath + "'");

		Schema schema = Utility.readSchemaAndDependenciesChaseBench(basePath, baseTest);
		Dependency[] allDependencies = schema.getAllDependencies();

		logger.info("# Dependencies: " + allDependencies.length);
		logger.trace(schema);

		Collection<Atom> facts = Utility.readFactsChaseBench(basePath, fact_querySize, schema);
		logger.info("# Facts: " + facts.size());
		logger.trace(facts);

		Collection<TGD> queriesRules = Utility.readQueriesChaseBench(basePath, fact_querySize, schema);
		logger.info("# Queries: " + queriesRules.size());
		logger.debug(queriesRules);

		Collection<TGD> guardedSaturation = runGSat(
				ArrayUtils.addAll(allDependencies, queriesRules.toArray(new TGD[queriesRules.size()])));
		logger.info("Rewriting completed!");
		System.out.println("Guarded saturation:");
		System.out.println("=========================================");
		guardedSaturation.forEach(System.out::println);
		System.out.println("=========================================");

		String baseOutputPath = "test" + File.separator + "datalog" + File.separator;
		new File(baseOutputPath).mkdirs();
		Utility.writeDatalogRules(guardedSaturation, baseOutputPath + baseTest + ".rul");
		Utility.writeDatalogFacts(facts, baseOutputPath + baseTest + ".data");

		System.out.println("Performing the full grounding...");
		Output solverOutput = Utility.invokeSolver("executables" + File.separator + "idlv_1.1.3_windows_x86-64.exe",
				"--t --no-facts --check-edb-duplication", // "dlv.mingw.exe", "-nofacts",
				Arrays.asList(baseOutputPath + baseTest + ".rul", baseOutputPath + baseTest + ".data"));
		// System.out.println(solverOutput);
		System.out.println(
				"Output size: " + solverOutput.getOutput().length() + ", " + solverOutput.getErrors().length());
		Utility.writeOutput(solverOutput, baseOutputPath + baseTest + ".idlv.output" // ".dlv.output"
		);

		for (TGD query : queriesRules) {

			Utility.writeChaseBenchDatalogQueries(Arrays.asList(query), baseOutputPath + baseTest + "_queries.rul");

			solverOutput = Utility.invokeSolver("executables" + File.separator + "idlv_1.1.3_windows_x86-64.exe",
					"--t --no-facts --check-edb-duplication", // "dlv.mingw.exe", "-nofacts",
					Arrays.asList(baseOutputPath + baseTest + ".rul", baseOutputPath + baseTest + ".data",
							baseOutputPath + baseTest + "_queries.rul"));
			// System.out.println(solverOutput);
			System.out.println(
					"Output size: " + solverOutput.getOutput().length() + ", " + solverOutput.getErrors().length());
			Utility.writeOutput(solverOutput,
					baseOutputPath + baseTest + "." + query.getHead().getAtoms()[0].getPredicate() + ".idlv.output" // ".dlv.output"
			);

		}

	}

	public static Collection<TGD> runGSat(Dependency[] allDependencies) {

		System.out.println("Running GSat...");

		Collection<TGD> newTGDs = new HashSet<>();

		for (Dependency d : allDependencies)
			if (d instanceof TGD && ((TGD) d).isGuarded()) // Adding only Guarded TGDs
				// if (!(d instanceof EGD))
				newTGDs.addAll(VNF(HNF((TGD) d)));

		logger.debug("# initial TGDs: " + newTGDs.size());
		newTGDs.forEach(logger::debug);

		Collection<TGD> nonFullTGDs = new HashSet<>();
		Collection<TGD> fullTGDs = new HashSet<>();

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
		// logger.debug("# nonFullTGDs: " + nonFullTGDs.size());
		// nonFullTGDs.forEach(logger.debug);
		// logger.debug("# fullTGDs: " + fullTGDs.size());
		// fullTGDs.forEach(logger.debug);
		// if (!nonFullTGDs.isEmpty())
		// logger.debug("First non full TGD: " + nonFullTGDs.toArray()[0]);
		// if (!fullTGDs.isEmpty())
		// logger.debug("First full TGD: " + fullTGDs.toArray()[0]);
		//
		// newTGDs.addAll(nonFullTGDs);
		// newTGDs.addAll(fullTGDs);

		while (!newTGDs.isEmpty()) {
			logger.debug("# new TGDs: " + newTGDs.size());
			newTGDs.forEach(logger::debug);

			TGD currentTGD = newTGDs.iterator().next();
			logger.debug("current TGD: " + currentTGD);
			newTGDs.remove(currentTGD);

			Set<TGD> tempTGDsSet = new HashSet<>();

			if (isFull(currentTGD)) {
				fullTGDs.add(currentTGD);
				for (TGD nftgd : nonFullTGDs)
					tempTGDsSet.addAll(VNF(HNF(evolve(nftgd, currentTGD))));
			} else {
				nonFullTGDs.add(currentTGD);
				for (TGD ftgd : fullTGDs)
					tempTGDsSet.addAll(VNF(HNF(evolve(currentTGD, ftgd))));
			}

			for (TGD d : tempTGDsSet)
				if (isFull(d) && !fullTGDs.contains(d) || !isFull(d) && !nonFullTGDs.contains(d))
					newTGDs.add(d);
		}

		return fullTGDs;

	}

	public static Collection<TGD> HNF(TGD tgd) {

		Collection<TGD> result = new ArrayList<>();

		if (tgd == null)
			return result;

		Variable[] eVariables = tgd.getExistential();

		Collection<Atom> eHead = new LinkedList<>();
		Collection<Atom> fHead = new LinkedList<>();

		for (Atom a : tgd.getHeadAtoms())
			if (Utility.containsAny(a, eVariables))
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

	public static Collection<TGD> VNF(Collection<TGD> tgds) {

		Collection<TGD> result = new LinkedList<>();

		for (TGD d : tgds)
			result.add(VNF(d));

		return result;

	}

	public static TGD VNF(TGD tgd) {

		assert tgd != null;

		Variable[] uVariables = tgd.getUniversal();
		Variable[] eVariables = tgd.getExistential();
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

		TGD applySubstitution = (TGD) Utility.applySubstitution(tgd, substitution);
		logger.debug("VNF: " + tgd + "===>>>" + applySubstitution);
		return applySubstitution;

	}

	public static boolean isFull(TGD tgd) {

		return tgd.getExistential().length == 0;

	}

	public static TGD evolve(TGD nftgd, TGD ftgd) {

		ftgd = evolveRename(ftgd);

		logger.debug("Composing:\n" + nftgd + "\nand\n" + ftgd);

		Collection<Atom> joinAtoms = getJoinAtoms(nftgd.getHeadAtoms(), ftgd.getBodyAtoms());
		if (joinAtoms.isEmpty())
			return null;
		logger.debug("Join atoms:");
		joinAtoms.forEach(logger::debug);

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

		return (TGD) Utility.applySubstitution(ftgd, substitution);

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

	public static TGD getEvolveRule(TGD nftgd, TGD ftgd, Collection<Atom> joinAtoms) {

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
			TGD newTGD = TGD.create(applyMGU(nftgdBodyAtoms, mgu), applyMGU(nftgdHeadAtoms, mgu));
			logger.debug("After applying MGU: " + newTGD);
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
