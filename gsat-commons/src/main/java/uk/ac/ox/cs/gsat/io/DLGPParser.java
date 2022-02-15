package uk.ac.ox.cs.gsat.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.AtomSet;
import fr.lirmm.graphik.graal.api.core.ConjunctiveQuery;
import fr.lirmm.graphik.graal.api.core.NegativeConstraint;
import fr.lirmm.graphik.graal.api.core.Predicate;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.io.ParseException;
import fr.lirmm.graphik.graal.io.dlp.DlgpParser;
import fr.lirmm.graphik.util.Prefix;
import fr.lirmm.graphik.util.stream.IteratorException;
import uk.ac.ox.cs.gsat.Log;
import uk.ac.ox.cs.gsat.api.io.Parser;
import uk.ac.ox.cs.gsat.api.io.ParserResult;
import uk.ac.ox.cs.gsat.fol.TGD;

public class DLGPParser implements Parser {

    protected final boolean skipFacts;
    protected final boolean includeNegativeConstraints;
    
    protected DLGPParser(boolean skipFacts, boolean includeNegativeConstraints) {
        this.skipFacts = skipFacts;
        this.includeNegativeConstraints = includeNegativeConstraints;
    }

    @Override
    public ParserResult parse(String file) throws Exception {
        return new DLGPParserResult(file);
    }

    class DLGPParserResult implements ParserResult {
        protected String path;
        protected HashSet<Atom> atoms;
        protected HashSet<AtomSet> atomSets;
        protected HashSet<Rule> rules;
        protected HashSet<ConjunctiveQuery> queries;
        protected Map<String, String> prefixes = new HashMap<>();

        public DLGPParserResult(String path) throws ParseException, FileNotFoundException {
            parseInput(new DlgpParser(new File(path)));
        }
        
        @Override
        public Set<TGD> getTGDs() throws IteratorException {
            return GraalConvertor.getPDQTGDsFromGraalRules(rules, prefixes);
        }

        @Override
        public Set<uk.ac.ox.cs.pdq.fol.Atom> getAtoms() throws IteratorException {
            return GraalConvertor.getPDQAtomsFromGraalAtoms(atoms, prefixes);
        }

        @Override
        public Set<uk.ac.ox.cs.pdq.fol.Atom> getConjunctiveQueries() throws IteratorException {
            return GraalConvertor.getPDQAtomsFromGraalQueries(queries, prefixes);
        }

        protected void parseInput(fr.lirmm.graphik.graal.api.io.Parser<Object> parser) throws ParseException {

            prefixes = new HashMap<>();
            atoms = new HashSet<>();
            atomSets = new HashSet<>();
            rules = new HashSet<>();
            queries = new HashSet<>();

            while (parser.hasNext()) {
                Object o = parser.next();

                if (o instanceof Prefix) {
                    Prefix p = (Prefix) o;
                    prefixes.put(p.getPrefixName(), p.getPrefix());
                }
            
                if (o instanceof Atom && !skipFacts) {
                    // App.logger.fine("Atom: " + ((Atom) o));
                    atoms.add((Atom) o);
                } else if (o instanceof AtomSet && !skipFacts) {
                    // App.logger.fine("Atom Set: " + (AtomSet) o);
                    atomSets.add((AtomSet) o);
                } else if (o instanceof Rule) {
                    // App.logger.fine("Rule: " + ((Rule) o));
                    if (!((Rule) o).getHead().iterator().next().getPredicate().equals(Predicate.BOTTOM) || includeNegativeConstraints)
                        rules.add((Rule) o);
                } else if (o instanceof ConjunctiveQuery) {
                    // App.logger.fine("Conjunctive Query: " + ((Query) o));
                    queries.add((ConjunctiveQuery) o);
                } else if (o instanceof NegativeConstraint) {
                    // App.logger.fine("Negative Constraint: " + ((NegativeConstraint) o));
                    if (includeNegativeConstraints)
                        rules.add((NegativeConstraint) o);
                }
            }

            parser.close();

            Log.GLOBAL.fine("# Rules: " + rules.size() + "; # Atoms: " + atoms.size() + "; # AtomSets: "
                               + atomSets.size() + "; # Queries: " + queries.size() + "; # Constraints: "
                               + rules.stream().filter(r -> r instanceof NegativeConstraint).count());

        }

    }
}
