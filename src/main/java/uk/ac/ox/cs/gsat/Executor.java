package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.semanticweb.kaon2.api.Axiom;
import org.semanticweb.kaon2.api.DefaultOntologyResolver;
import org.semanticweb.kaon2.api.KAON2Exception;
import org.semanticweb.kaon2.api.KAON2Factory;
import org.semanticweb.kaon2.api.KAON2Manager;
import org.semanticweb.kaon2.api.Ontology;
import org.semanticweb.kaon2.api.OntologyChangeEvent;
import org.semanticweb.kaon2.api.OntologyManager;
import org.semanticweb.kaon2.api.logic.Rule;
import org.semanticweb.kaon2.api.owl.elements.OWLClass;
import org.semanticweb.kaon2.api.owl.elements.ObjectProperty;
import org.semanticweb.kaon2.api.owl.elements.ObjectSome;
import org.semanticweb.kaon2.api.reasoner.Reasoner;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.TGD;

public class Executor {

	/**
	 *
	 */
	private static final KAON2Factory FACTORY = KAON2Manager.factory();
	private static final Level level = Level.INFO;
	static final Logger logger = Logger.getLogger("Executor");

	public static void main(String[] args) throws Exception {
		Handler handlerObj = new ConsoleHandler();
		handlerObj.setLevel(level);
		logger.addHandler(handlerObj);
		logger.setLevel(level);
		logger.setUseParentHandlers(false);

		System.out.println("Starting the KAON2-GSat Executor...");

		try {
			Class.forName("uk.ac.ox.cs.pdq.fol.TGD");
		} catch (ClassNotFoundException e) {
			System.err.println("PDQ library not found. The system will now terminate.");
			System.exit(1);
		}

		try {

			if (args.length > 0)
				if (args[0].equals("compare"))
					if (args.length == 2) {
						compare(args[1]);
					} else
						printHelp("Wrong number of parameters for compare");
				else
					printHelp("Wrong command (i.e. first argument)");
			else
				printHelp("No arguments provided");

		} catch (Throwable t) {
			System.err.println("Unknown error. The system will now terminate.");
			logger.severe(t.getLocalizedMessage());
			System.exit(1);
		}

	}

	private static void compare(String input_file) {
		DLGPIO dlgpio = new DLGPIO(input_file, true);
		try {
			Collection<Dependency> rules = dlgpio.getRules();
			Collection<TGDGSat> TGDRules = discardNonTGDRules(rules);
			Collection<TGD> normalisedTGDRules = normaliseTGDRules(TGDRules);
			System.out.println("Rules: " + TGDRules.size() + " vs. " + normalisedTGDRules.size());

			// System.out.println("GSat Start, # rules: " + TGDRules.size());
			// final long startTimeGSat = System.nanoTime();
			// Collection<TGDGSat> runGSat = GSat.getInstance().runGSat(TGDRules.toArray(new
			// TGDGSat[TGDRules.size()]));
			// final long stopTimeGSat = System.nanoTime();
			// final long totalTimeGSat = stopTimeGSat - startTimeGSat;
			// System.out.println("GSat End; # rules: " + runGSat.size());
			// System.out.println("GSat total time : " + String.format(Locale.UK, "%.0f",
			// totalTimeGSat / 1E6) + " ms = "
			// + String.format(Locale.UK, "%.2f", totalTimeGSat / 1E9) + " s");

			OntologyManager ontologyManager = KAON2Manager.newOntologyManager();
			DefaultOntologyResolver resolver = new DefaultOntologyResolver();
			ontologyManager.setOntologyResolver(resolver);
			resolver.registerReplacement("http://bkhigkhghjbhgiyfgfhgdhfty", "file:bkhigkhghjbhgiyfgfhgdhfty.xml");
			Ontology ontology = ontologyManager.createOntology("http://bkhigkhghjbhgiyfgfhgdhfty",
					new HashMap<String, Object>());

			// Collection<Rule> kaon2Rules = getKAON2Rules(TGDRules);
			Collection<Axiom> kaon2Axioms = getKAON2Axioms(normalisedTGDRules);
			// System.out.println(kaon2Axioms);

			List<OntologyChangeEvent> changes = new ArrayList<OntologyChangeEvent>();
			// kaon2Rules.forEach(r -> changes.add(new OntologyChangeEvent(r,
			// OntologyChangeEvent.ChangeType.ADD)));
			kaon2Axioms.forEach(a -> changes.add(new OntologyChangeEvent(a, OntologyChangeEvent.ChangeType.ADD)));
			ontology.applyChanges(changes);

			System.out.println("KAON2 Start, # axioms: " + kaon2Axioms.size());
			final long startTimeKAON2 = System.nanoTime();
			Reasoner reasoner = ontology.createReasoner();
			// reasoner.getOntology().saveOntology(OntologyFileFormat.OWL_RDF, System.out,
			// "UTF-8");
			Collection<Rule> reductionToDLP = reasoner.getReductionToDisjunctiveDatalog(false, false, false, true);
			final long stopTimeKAON2 = System.nanoTime();
			final long totalTimeKAON2 = stopTimeKAON2 - startTimeKAON2;
			System.out.println("KAON2 End; # rules: " + reductionToDLP.size());
			System.out.println("KAON2 total time : " + String.format(Locale.UK, "%.0f", totalTimeKAON2 / 1E6) + " ms = "
					+ String.format(Locale.UK, "%.2f", totalTimeKAON2 / 1E9) + " s");

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error during execution");
		}

	}

	private static Collection<TGD> normaliseTGDRules(Collection<TGDGSat> tGDRules) {

		final long startTime = System.nanoTime();

		Collection<TGD> selectedTGDs = new HashSet<>();

		tGDRules.forEach(tgd -> selectedTGDs.addAll(GSat.getInstance().HNF(tgd)));

		final long stopTime = System.nanoTime();

		final long totalTime = stopTime - startTime;

		App.logger.info("Normalisation total time : " + String.format(Locale.UK, "%.0f", totalTime / 1E6) + " ms = "
				+ String.format(Locale.UK, "%.2f", totalTime / 1E9) + " s");

		return selectedTGDs;
	}

	private static Collection<Axiom> getKAON2Axioms(Collection<TGD> normalisedTGDRules) throws KAON2Exception {

		Collection<Axiom> axioms = new LinkedList<>();

		for (TGD tgdgSatRule : normalisedTGDRules) {
			int numberOfBodyAtoms = tgdgSatRule.getNumberOfBodyAtoms();
			int numberOfHeadAtoms = tgdgSatRule.getNumberOfHeadAtoms();
			Atom bodyAtom = tgdgSatRule.getBodyAtom(0);
			Atom headAtom = tgdgSatRule.getHeadAtom(0);
			uk.ac.ox.cs.pdq.fol.Predicate predicateBody = bodyAtom.getPredicate();
			uk.ac.ox.cs.pdq.fol.Predicate predicateHead = headAtom.getPredicate();
			int arityPredicateBody = predicateBody.getArity();
			int arityPredicateHead = predicateHead.getArity();
			if (numberOfBodyAtoms == 1 && numberOfHeadAtoms == 1 && arityPredicateBody == 1
					&& arityPredicateHead == 1) {
				// CI: Concept Inclusion - B(x) :- A(x)

				if (bodyAtom.getTerm(0).equals(headAtom.getTerm(0))) {
					OWLClass owlClassBody = FACTORY.owlClass(predicateBody.getName());
					OWLClass owlClassHead = FACTORY.owlClass(predicateHead.getName());
					axioms.add(FACTORY.subClassOf(owlClassBody, owlClassHead));
				} else {
					System.err.println("ERROR IN CI: " + tgdgSatRule);
					System.exit(1);
				}

			} else if (numberOfBodyAtoms == 1 && numberOfHeadAtoms == 1 && arityPredicateBody == 1
					&& arityPredicateHead == 2) {
				// MR: Mandatory Role - p(x,y) :- A(x)
				// IMR: Inverse Mandatory Role - p(x,y) :- A(y)

				if (bodyAtom.getTerm(0).equals(headAtom.getTerm(0))) {
					OWLClass owlClassBody = FACTORY.owlClass(predicateBody.getName());
					ObjectSome objectSomeHead = FACTORY.objectSome(FACTORY.objectProperty(predicateHead.getName()),
							FACTORY.owlClass(OWLClass.OWL_THING));
					axioms.add(FACTORY.subClassOf(owlClassBody, objectSomeHead));
				} else if (bodyAtom.getTerm(0).equals(headAtom.getTerm(1))) {
					// TODO
				} else {
					System.err.println("ERROR IN MR IMR: " + tgdgSatRule);
					System.exit(1);
				}

			} else if (numberOfBodyAtoms == 1 && numberOfHeadAtoms == 1 && arityPredicateBody == 2
					&& arityPredicateHead == 1) {
				// Dom: Domain - A(x) :- p(x,y)
				// Rng: Range - A(y) :- p(x,y)

				if (bodyAtom.getTerm(0).equals(headAtom.getTerm(0))) {
					ObjectProperty objectPropertyBody = FACTORY.objectProperty(predicateBody.getName());
					axioms.add(FACTORY.objectPropertyDomain(objectPropertyBody, FACTORY.owlClass(OWLClass.OWL_THING)));
				} else if (bodyAtom.getTerm(1).equals(headAtom.getTerm(0))) {
					// TODO
				} else {
					System.err.println("ERROR IN Dom Rng: " + tgdgSatRule);
					System.exit(1);
				}

			} else if (numberOfBodyAtoms == 1 && numberOfHeadAtoms == 1 && arityPredicateBody == 2
					&& arityPredicateHead == 2) {
				// RI: Role Inclusion - r(x,y) :- p(x,y)
				// Inv: Inverse Role - r(y,x) :- p(x,y)

				if (bodyAtom.getTerm(0).equals(headAtom.getTerm(0))
						&& bodyAtom.getTerm(1).equals(headAtom.getTerm(1))) {
					ObjectProperty objectPropertyBody = FACTORY.objectProperty(predicateBody.getName());
					ObjectProperty objectPropertyHead = FACTORY.objectProperty(predicateHead.getName());
					axioms.add(FACTORY.subObjectPropertyOf(objectPropertyBody, objectPropertyHead));
				} else if (bodyAtom.getTerm(0).equals(headAtom.getTerm(1))
						&& bodyAtom.getTerm(1).equals(headAtom.getTerm(0))) {
					ObjectProperty objectPropertyBody = FACTORY.objectProperty(predicateBody.getName());
					ObjectProperty objectPropertyHead = FACTORY.objectProperty(predicateHead.getName());
					axioms.add(FACTORY.inverseObjectProperties(objectPropertyBody, objectPropertyHead));
				} else {
					System.err.println("ERROR IN RI Inv: " + tgdgSatRule);
					System.exit(1);
				}

			} else if (numberOfBodyAtoms == 1 && numberOfHeadAtoms == 2) {
				// ERC: Existential quantification in rule heads - p(x,y), B(y) :- A(x)
				// IERC: ERC with Inverse role - p(y,x), B(y) :- A(x)

				// TODO
			} else if (numberOfBodyAtoms == 2 && numberOfHeadAtoms == 1 && headAtom.equals(TGDGSat.Bottom)) {
				// DC: Disjoint Concept - ! :- A(x), B(x)
				// DR: Disjoint Role - ! :- p(x,y), r(x,y)

				// TODO
			} else if (numberOfBodyAtoms == 1 && numberOfHeadAtoms == 1 && headAtom.equals(TGDGSat.Bottom)) {
				// ! :- OWL#Nothing(X)

				// TODO
			} else if (numberOfBodyAtoms == 2 && numberOfHeadAtoms == 1) {
				// B(X0) :- ActsOn(X0,X3), A(X3).

				// TODO
			} else if (numberOfBodyAtoms == 3 && numberOfHeadAtoms == 1) {
				// C(X0) :- HasA(X0,X3), B(X0), A(X3).

				// TODO
			} else if (numberOfBodyAtoms == 1 && numberOfHeadAtoms == 4) {
				// HasA(X0, X3), A(X3), IsA(X3, X4), B(X4) :- C(X0).

				// TODO
			} else if (numberOfBodyAtoms == 1 && numberOfHeadAtoms == 6) {
				// HasA(X0, X3), A(X3), IsAB(X3, X4), B(X4), IsAC(X3, X5), C(X5) :- D(X0).

				// TODO
			} else if (numberOfBodyAtoms == 1 && numberOfHeadAtoms == 8) {
				// HasA(X0, X3), A(X3), IsAB(X3, X4), B(X4), IsAC(X3, X5), C(X5), IsCD(X5, X6),
				// D(X6) :- E(X0).

				// TODO
			} else if (numberOfBodyAtoms == 1 && numberOfHeadAtoms == 10) {
				// HasA(X0, X3), A(X3), IsAB(X3, X4), B(X4), IsAC(X3, X5), C(X5), IsCD(X5, X6),
				// D(X6), IsCE(X5, X7), E(X7) :- F(X0).

				// TODO
			} else {
				System.err.println("RULE NOT IMPLEMENTED!! " + tgdgSatRule);
				// System.exit(1);
			}
		}

		return axioms;
	}

	// private static Collection<Rule> getKAON2Rules(Collection<TGDGSat> tGDRules) {

	// Collection<Rule> rules = new LinkedList<>();

	// for (TGDGSat tgdgSatRule : tGDRules) {

	// Collection<Literal> head = new LinkedList<>();
	// for (Atom atom : tgdgSatRule.getHeadAtoms()) {
	// List<Term> terms = getKAON2Terms(atom);
	// head.add(FACTORY.literal(true,
	// FACTORY.predicateSymbol(atom.getPredicate().getName(),
	// atom.getPredicate().getArity()), terms));
	// }

	// Collection<Literal> body = new LinkedList<>();
	// for (Atom atom : tgdgSatRule.getBodyAtoms()) {
	// List<Term> terms = getKAON2Terms(atom);
	// head.add(FACTORY.literal(true,
	// FACTORY.predicateSymbol(atom.getPredicate().getName(),
	// atom.getPredicate().getArity()), terms));
	// }

	// rules.add(
	// FACTORY.rule(head.toArray(new Literal[head.size()]), true, body.toArray(new
	// Literal[body.size()])));

	// }

	// return rules;

	// }
	//
	// private static List<Term> getKAON2Terms(Atom atom) {
	// List<Term> terms = new LinkedList<>();
	// for (uk.ac.ox.cs.pdq.fol.Term term : atom.getTerms()) {
	// if (term.isVariable())
	// terms.add(FACTORY.variable(((uk.ac.ox.cs.pdq.fol.Variable)
	// term).getSymbol()));
	// else if (term.isUntypedConstant())
	// terms.add(FACTORY.constant(((UntypedConstant) term).getSymbol()));
	// else if (term instanceof TypedConstant && ((TypedConstant) term).getValue()
	// instanceof String)
	// terms.add(FACTORY.constant((String) ((TypedConstant) term).getValue()));
	// else
	// throw new IllegalArgumentException("Term type not supported: " + term + " : "
	// + term.getClass());
	// }
	// return terms;
	// }

	private static Collection<TGDGSat> discardNonTGDRules(Collection<Dependency> allDependencies) {

		final long startTime = System.nanoTime();

		int discarded = 0;

		Collection<TGDGSat> selectedTGDs = new HashSet<>();
		for (Dependency d : allDependencies)
			if (d instanceof TGD && ((TGD) d).isGuarded())
				selectedTGDs.add(new TGDGSat(Set.of(d.getBodyAtoms()), Set.of(d.getHeadAtoms())));
			else
				discarded++;

		App.logger.info("Discarded rules : " + discarded + "/" + allDependencies.size() + " = "
				+ String.format(Locale.UK, "%.3f", (float) discarded / allDependencies.size() * 100) + "%");

		final long stopTime = System.nanoTime();

		final long totalTime = stopTime - startTime;

		App.logger.info("Discarding total time : " + String.format(Locale.UK, "%.0f", totalTime / 1E6) + " ms = "
				+ String.format(Locale.UK, "%.2f", totalTime / 1E9) + " s");

		return selectedTGDs;

	}

	/**
	 * Prints the help message of the program
	 * 
	 * @param message the error message
	 */
	private static void printHelp(String message) {

		System.err.println();
		System.err.println(message);
		System.err.println();
		System.err.println("Note that only these commands are currently supported:");
		System.err.println("compare \t to compare KAON2 and GSat");
		System.err.println();
		System.err.println("if compare is specified the following arguments must be provided, in this strict order:");
		System.err.println("<PATH OF THE DLGP FILE>");
		System.err.println();

	}

}
