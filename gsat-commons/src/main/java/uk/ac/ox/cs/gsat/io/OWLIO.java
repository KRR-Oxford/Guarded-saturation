package uk.ac.ox.cs.gsat.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import fr.lirmm.graphik.graal.io.owl.OWL2Parser;
import fr.lirmm.graphik.graal.io.sparql.SparqlConjunctiveQueryParser;
import uk.ac.ox.cs.pdq.fol.Dependency;

/**
 * OWLIO
 */
public class OWLIO extends DLGPIO {

    private String query;

    public OWLIO(String path, boolean saturationOnly, boolean includeNegativeConstraints) {
        this(path, "", saturationOnly, includeNegativeConstraints);
    }

    public OWLIO(String path, String query, boolean saturationOnly, boolean includeNegativeConstraints) {
        super(path, saturationOnly, includeNegativeConstraints);
        this.query = query;
    }

    @Override
    public Collection<Dependency> getRules() throws Exception {

        parseInput(new OWL2Parser(new File(path)));

        if (query != null && !query.equals(""))
            queries.add(new SparqlConjunctiveQueryParser(Files.readString(Path.of(query))).getConjunctiveQuery());

        return getPDQTGDsFromGraalRules(rules, this.prefixes);

    }

    @Override
    public void writeData(String path) throws IOException {

        Collection<uk.ac.ox.cs.pdq.fol.Atom> pdqAtoms = getPDQAtomsFromGraalAtomSets(atomSets, this.prefixes);
        System.out.println("# PDQ Atoms: " + pdqAtoms.size());

        IO.writeDatalogFacts(pdqAtoms, path);

    }

}
