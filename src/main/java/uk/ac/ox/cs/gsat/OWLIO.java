package uk.ac.ox.cs.gsat;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import fr.lirmm.graphik.graal.api.core.AtomSet;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.io.owl.OWL2Parser;
import uk.ac.ox.cs.pdq.fol.Dependency;

/**
 * OWLIO
 */
public class OWLIO extends DLGPIO {

    private HashSet<AtomSet> atomSets;

    public OWLIO(String path, boolean gSatOnly) {
        super(path, gSatOnly);
    }

    @Override
    public Collection<Dependency> getRules() throws Exception {
        // FIXME we can have the same of the super-class
        rules = new HashSet<>();
        atomSets = new HashSet<>();

        OWL2Parser parser = new OWL2Parser(new File(path));

        while (parser.hasNext()) {
            Object o = parser.next();
            // logger.debug("Object:" + o);
            if (o instanceof Rule) {
                App.logger.fine("Rule: " + (Rule) o);
                rules.add((Rule) o);
            } else if (o instanceof AtomSet && !gSatOnly) {
                App.logger.fine("Atom: " + (AtomSet) o);
                atomSets.add((AtomSet) o);
            }
        }

        parser.close();

        System.out.println("# Rules: " + rules.size() + "; # AtomSets: " + atomSets.size());

        return getPDQTGDsFromGraalRules(rules);

    }

    @Override
    public void writeData(String path) throws IOException {

        Collection<uk.ac.ox.cs.pdq.fol.Atom> pdqAtoms = getPDQAtomsFromGraalAtomSets(atomSets);
        System.out.println("# PDQ Atoms: " + pdqAtoms.size());

        IO.writeDatalogFacts(pdqAtoms, path);

    }

}