import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

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
		Queue<Dependency> newDependencies = new LinkedList<>();

		for (Dependency d : allDependencies)
			newDependencies.addAll(VNF(HNF(d)));

		logger.debug("# initial dependencies: " + newDependencies.size());
		newDependencies.forEach(logger::debug);

		Collection<Dependency> nonFullTGDs = new LinkedList<>();
		Collection<Dependency> fullTGDs = new LinkedList<>();

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
			Dependency currentDependency = newDependencies.remove();
			if (isFull(currentDependency)) {
				fullTGDs.add(currentDependency);
				for (Dependency nftdg : nonFullTGDs)
					newDependencies.addAll(VNF(HNF(evolve(nftdg, currentDependency))));
			} else {
				nonFullTGDs.add(currentDependency);
				for (Dependency ftdg : fullTGDs)
					newDependencies.addAll(VNF(HNF(evolve(currentDependency, ftdg))));

				break; // FIXME remove it!
			}
		}

		return fullTGDs;
	}

	public static Dependency evolve(Dependency nftdg, Dependency ftgd) {
		logger.debug("Composing: " + nftdg + "and" + ftgd);
		// FIXME
		return ftgd;
	}

	public static boolean isFull(Dependency dependency) {
		return dependency.getExistential().length == 0;
	}

	public static Collection<Dependency> VNF(Collection<Dependency> dependencies) {
		Collection<Dependency> result = new LinkedList<>();
		for (Dependency d : dependencies)
			result.add(VNF(d));
		return result;
	}

	public static Dependency VNF(Dependency dependency) {
		Variable[] uVariables = dependency.getUniversal();
		Variable[] eVariables = dependency.getExistential();
		logger.trace(uVariables);
		logger.trace(eVariables);

		Map<Term, Term> substitution = new HashMap<>();
		int counter = 1;
		for (Variable v : uVariables) {
			substitution.put(v, Variable.create("u" + counter++));
		}
		counter = 1;
		for (Variable v : eVariables) {
			substitution.put(v, Variable.create("e" + counter++));
		}
		logger.debug("VNF substitution:\n" + substitution);

		// Dependency applySubstitution = (Dependency)
		// Utility.applySubstitution(dependency, substitution);
		// logger.debug("VNF: " + dependency + "===>>>" + applySubstitution);
		// return applySubstitution;
		return (Dependency) Utility.applySubstitution(dependency, substitution);
	}

	public static Collection<Dependency> HNF(Dependency dependency) {
		Collection<Dependency> result = new ArrayList<>();

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

}
