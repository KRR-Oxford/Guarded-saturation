package uk.ac.ox.cs.gsat;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import fr.lirmm.graphik.graal.io.owl.OWL2Parser;
import uk.ac.ox.cs.pdq.fol.Dependency;

/**
 * OWLIO
 */
public class OWLIO extends DLGPIO {

    public OWLIO(String path, boolean gSatOnly) {
        super(path, gSatOnly);
    }

    @Override
    public Collection<Dependency> getRules() throws Exception {

        parseInput(new OWL2Parser(new File(path)));

        return getPDQTGDsFromGraalRules(rules);

    }

    @Override
    public void writeData(String path) throws IOException {

        Collection<uk.ac.ox.cs.pdq.fol.Atom> pdqAtoms = getPDQAtomsFromGraalAtomSets(atomSets);
        System.out.println("# PDQ Atoms: " + pdqAtoms.size());

        IO.writeDatalogFacts(pdqAtoms, path);

    }

}