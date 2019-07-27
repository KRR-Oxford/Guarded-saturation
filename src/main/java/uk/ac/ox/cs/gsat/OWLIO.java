package uk.ac.ox.cs.gsat;

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

    public OWLIO(String path, boolean gSatOnly) {
        this(path, "", gSatOnly);
    }

    public OWLIO(String path, String query, boolean gSatOnly) {
        super(path, gSatOnly);
        this.query = query;
    }

    @Override
    public Collection<Dependency> getRules() throws Exception {

        parseInput(new OWL2Parser(new File(path)));

        if (query != "")
            queries.add(new SparqlConjunctiveQueryParser(Files.readString(Path.of(query))).getConjunctiveQuery());

        return getPDQTGDsFromGraalRules(rules);

    }

    @Override
    public void writeData(String path) throws IOException {

        Collection<uk.ac.ox.cs.pdq.fol.Atom> pdqAtoms = getPDQAtomsFromGraalAtomSets(atomSets);
        System.out.println("# PDQ Atoms: " + pdqAtoms.size());

        IO.writeDatalogFacts(pdqAtoms, path);

    }

}