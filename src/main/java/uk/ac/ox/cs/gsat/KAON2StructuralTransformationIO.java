package uk.ac.ox.cs.gsat;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.kaon2.api.Axiom;
import org.semanticweb.kaon2.api.Cursor;
import org.semanticweb.kaon2.api.DefaultOntologyResolver;
import org.semanticweb.kaon2.api.KAON2Connection;
import org.semanticweb.kaon2.api.KAON2Exception;
import org.semanticweb.kaon2.api.KAON2Manager;
import org.semanticweb.kaon2.api.Ontology;
import org.semanticweb.kaon2.api.Request;
import org.semanticweb.kaon2.api.owl.axioms.ClassMember;
import org.semanticweb.kaon2.api.owl.axioms.DataPropertyMember;
import org.semanticweb.kaon2.api.owl.axioms.DifferentIndividuals;
import org.semanticweb.kaon2.api.owl.axioms.ObjectPropertyMember;
import org.semanticweb.kaon2.api.owl.axioms.SameIndividual;
import org.semanticweb.kaon2.api.owl.elements.Description;
import org.semanticweb.kaon2.api.owl.elements.OWLClass;
import org.semanticweb.kaon2.apicore.OntologyCore;
import org.semanticweb.kaon2.reasoner.AxiomClausifier;
import org.semanticweb.kaon2.reasoner.ClausificationListener;
import org.semanticweb.kaon2.reasoner.ClausificationState;
import org.semanticweb.kaon2.reasoner.DescriptionManager;
import org.semanticweb.kaon2.reasoner.PropertyManager;
import org.semanticweb.kaon2.saturation.SClause;
import org.semanticweb.kaon2.saturation.SLiteral;
import org.semanticweb.kaon2.saturation.STerm;
import org.semanticweb.kaon2.saturation.SVariable;
import org.semanticweb.kaon2.saturation.TermFactory;
import org.semanticweb.kaon2.saturation.owl.OWLSClauseManager;
import org.semanticweb.kaon2.util.InterruptFlag;

import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.Function;
import uk.ac.ox.cs.pdq.fol.FunctionTerm;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

public class KAON2StructuralTransformationIO extends DLGPIO {

    public KAON2StructuralTransformationIO(String path, boolean saturationOnly) {
        super(path, saturationOnly);
    }


    /** The monitor for clausification events. */
    protected ClausificationListener m_clausificationListener;
    /** The ordering for predicates. */
    protected static AxiomClausifier.PredicateOrdering m_predicateOrdering  = AxiomClausifier.PredicateOrdering.MORE_FREQUENT_ARE_SMALLER;

    public static void main(String[] args) throws Exception {
        KAON2Connection connection = KAON2Manager.newConnection();

        DefaultOntologyResolver resolver = new DefaultOntologyResolver();
        resolver.registerReplacement("http://bkhigkhghjbhgiyfgfhgdhfty", "file:" + args[0].replace("\\", "/"));
        connection.setOntologyResolver(resolver);

        TermFactory m_termFactory = new TermFactory(4);
        
        Ontology ontology = connection.openOntology("http://bkhigkhghjbhgiyfgfhgdhfty", new HashMap<String, Object>());
        System.out.println("Initial axioms in the ontology: " + ontology.createAxiomRequest().sizeAll());
        List<SClause> clauses = apply((OntologyCore) ontology, m_termFactory);

        PrintWriter m_out = new PrintWriter(System.out);
        for (SClause clause : clauses) {
            System.out.println(clause);
            // KAON2Statistics.print(m_out, clause, m_termFactory);
            System.out.println(createTGD(clause, m_termFactory));
        }
    }

    @Override
    public Collection<Dependency> getRules() throws Exception {

        KAON2Connection connection = KAON2Manager.newConnection();

        DefaultOntologyResolver resolver = new DefaultOntologyResolver();
        resolver.registerReplacement("http://bkhigkhghjbhgiyfgfhgdhfty", "file:" + this.path.replace("\\", "/"));
        connection.setOntologyResolver(resolver);

        TermFactory m_termFactory = new TermFactory(4);
        
        Ontology ontology = connection.openOntology("http://bkhigkhghjbhgiyfgfhgdhfty", new HashMap<String, Object>());
        System.out.println("Initial axioms in the ontology: " + ontology.createAxiomRequest().sizeAll());
        List<SClause> clauses = apply((OntologyCore) ontology, m_termFactory);

        List<Dependency> tgds = new ArrayList<>();
        for (SClause clause : clauses) {
            tgds.add(createTGD(clause, m_termFactory));
        }

        return tgds;
    }

    public static SkGTGD createTGD(SClause clause, TermFactory termFactory) {
        Set<Atom> body = new HashSet<>();
        Set<Atom> head = new HashSet<>();

        for (int pos = 0; pos < clause.getNumberOfLiterals(); pos++) {
            SLiteral literal = clause.getLiteral(pos);
            // atom
            STerm term = literal.getFirstTerm();
            String symbol = termFactory.getSymbolForID(term.getPrecedenceIndex()).toString();
            Predicate predicate = Predicate.create(symbol, term.getArity());
            Term[] terms = new Term[term.getArity()];

            for (int i = 0; i < term.getArity(); i++) {
                Term t = createTerm(term.getSubTerm(i), termFactory);
                terms[i] = t;
            }

            if (literal.isPositive()) {
                head.add(Atom.create(predicate, terms));
            } else {
                body.add(Atom.create(predicate, terms));
            }
        }

        if (head.isEmpty()) {
            head.add(GTGD.Bottom);
        }
        return new SkGTGD(body, head);
    }

    public static Term createTerm(STerm term, TermFactory termFactory) {
        if (term instanceof SVariable) {
            int variableID = ((SVariable) term).getVariableID();
            if (variableID == 0)
                return Variable.create("X");
            else if (variableID == 1)
                return Variable.create("Y");
            else {
                return Variable.create("Y" + variableID);
            }
        } else {
            String symbol = termFactory.getSymbolForID(term.getPrecedenceIndex()).toString();
            Function function = new Function(symbol, term.getArity());
            Term[] terms = new Term[term.getArity()];
            for(int i = 0; i < term.getArity(); i++) {
                terms[i] = createTerm(term.getSubTerm(i), termFactory);
            }
            return FunctionTerm.create(function, terms);
        }
    }

    public static List<SClause> apply(OntologyCore m_ontology, TermFactory termFactory) throws KAON2Exception, InterruptedException {
        DescriptionManager descriptionManager = new DescriptionManager("Q");
        Set<Axiom> axioms = new HashSet<Axiom>();
        Map<Description, OWLClass> definitionsOfComplexABoxPredicates = new HashMap<Description, OWLClass>();
        loadAxiomsFromAllOntologies(axioms, m_ontology, descriptionManager, true, true, false,
                definitionsOfComplexABoxPredicates);
        PropertyManager propertyManager = new PropertyManager();
        final List<SClause> clauses = new ArrayList<SClause>();
        // List<Rule> rules = new ArrayList<Rule>();
        // appendTransitivityRulesFromAllOntologies(m_ontology,rules);
        // if (includeRules)
        // loadRulesFromAllOntologies(m_ontology, descriptionManager, rules);
        // if (additionalRules != null) {
        // for (Rule rule : additionalRules) {
        // Rule changedRule = replaceNonAtomicConcepts(rule, descriptionManager);
        // rules.add(changedRule);
        // }
        // }
        ClausificationState clausificationState = new ClausificationState();
        OWLSClauseManager owlClauseManager = new OWLSClauseManager();

        AxiomClausifier axiomClausifier = new AxiomClausifier(new InterruptFlag(), termFactory, owlClauseManager,
                descriptionManager, propertyManager, clausificationState, new ClausificationListenerStub() {
                    protected void consumeClause(SClause clause) {
                        clauses.add(clause);
                    }

                    @Override
                    public void endClausification() {
                    }

                    @Override
                    public void endClausifyingAxiom(Axiom axiom) {
                    }

                    @Override
                    public void processClause(SClause clause) {
                        consumeClause(clause);
                    }

                    @Override
                    public void startClausification(TermFactory arg0) {
                    }

                    @Override
                    public void startClausifyingAxiom(Axiom arg0) {
                    }
                }, m_predicateOrdering);
        axiomClausifier.clausify(axioms, true);
        return clauses;
    }

    protected static void loadAxiomsFromAllOntologies(Collection<Axiom> axioms, OntologyCore ontology,
            DescriptionManager descriptionManager, boolean processTBoxRBox, boolean processABoxDefinitions,
            boolean processABoxAssertions, Map<Description, OWLClass> definitionsOfComplexABoxPredicates)
            throws KAON2Exception, InterruptedException {
        loadAxiomsFromOntology(axioms, ontology, descriptionManager, processTBoxRBox, processABoxDefinitions,
                processABoxAssertions, definitionsOfComplexABoxPredicates);
        for (Ontology importedOntology : ontology.getAllImportedOntologies())
            loadAxiomsFromOntology(axioms, (OntologyCore) importedOntology, descriptionManager, processTBoxRBox,
                    processABoxDefinitions, processABoxAssertions, definitionsOfComplexABoxPredicates);
    }

    protected static void loadAxiomsFromOntology(Collection<Axiom> axioms, OntologyCore ontology,
            DescriptionManager descriptionManager, boolean processTBoxRBox, boolean processABoxDefinitions,
            boolean processABoxAssertions, Map<Description, OWLClass> definitionsOfComplexABoxPredicates)
            throws KAON2Exception, InterruptedException {
        if (processABoxDefinitions || processABoxAssertions) {
            Set<Description> complexABoxDescriptions = ontology.getComplexABoxDescriptionsNoTransaction();
            for (Description description : complexABoxDescriptions)
                processABoxDescription(description, descriptionManager, definitionsOfComplexABoxPredicates);
        }
        if (processTBoxRBox || processABoxAssertions) {
            Request<Axiom> axiomRequest = ontology.createAxiomRequest();
            if (!processABoxAssertions)
                axiomRequest.addCondition("skipABoxAxioms", Boolean.TRUE);
            Cursor<Axiom> axiomCursor = axiomRequest.openCursor();
            try {
                while (axiomCursor.hasNext()) {
                    Axiom axiom = axiomCursor.next();
                    loadAxiom(axioms, axiom, descriptionManager, processTBoxRBox, processABoxDefinitions,
                            processABoxAssertions, definitionsOfComplexABoxPredicates);
                    // m_interruptFlag.checkInterrupted();
                }
            } finally {
                axiomCursor.close();
            }
        }
    }

    protected static void loadAxiom(Collection<Axiom> axioms, Axiom axiom, DescriptionManager descriptionManager,
            boolean processTBoxRBox, boolean processABoxDefinitions, boolean processABoxAssertions,
            Map<Description, OWLClass> definitionsOfComplexABoxPredicates) throws KAON2Exception {
        if (isABoxAxiom(axiom)) {
            if (processABoxAssertions) {
                if (axiom instanceof ClassMember) {
                    ClassMember classMember = (ClassMember) axiom;
                    OWLClass individualClass = processABoxDescription(classMember.getDescription(), descriptionManager,
                            definitionsOfComplexABoxPredicates);
                    axioms.add(KAON2Manager.factory().classMember(individualClass, classMember.getIndividual()));
                } else
                    axioms.add(axiom);
            }
        } else if (processTBoxRBox)
            axioms.add(axiom);
    }

    protected static OWLClass processABoxDescription(Description description, DescriptionManager descriptionManager,
            Map<Description, OWLClass> definitionsOfComplexABoxPredicates) {
        OWLClass individualClass;
        if (description instanceof OWLClass)
            individualClass = (OWLClass) description;
        else {
            individualClass = descriptionManager.getClassForPositivePolarity(description);
            definitionsOfComplexABoxPredicates.put(description, individualClass);
        }
        return individualClass;
    }

    protected static boolean isABoxAxiom(Axiom axiom) {
        return axiom instanceof ClassMember || axiom instanceof ObjectPropertyMember
                || axiom instanceof DataPropertyMember || axiom instanceof SameIndividual
                || axiom instanceof DifferentIndividuals;
    }


    protected static abstract class ClausificationListenerStub implements ClausificationListener {
        // public void startClausification(TermFactory termFactory) {
        //     if (m_clausificationListener != null)
        //         m_clausificationListener.startClausification(termFactory);
        // }

        // public void startClausifyingAxiom(Axiom axiom) {
        //     if (m_clausificationListener != null)
        //         m_clausificationListener.startClausifyingAxiom(axiom);
        // }

        // public void processClause(SClause clause) {
        //     consumeClause(clause);
        //     if (m_clausificationListener != null)
        //         m_clausificationListener.processClause(clause);
        // }

        // public void endClausifyingAxiom(Axiom axiom) {
        //     if (m_clausificationListener != null)
        //         m_clausificationListener.endClausifyingAxiom(axiom);
        // }

        // public void endClausification() {
        //     if (m_clausificationListener != null)
        //         m_clausificationListener.endClausification();
        // }

        protected abstract void consumeClause(SClause clause);
    }

}
