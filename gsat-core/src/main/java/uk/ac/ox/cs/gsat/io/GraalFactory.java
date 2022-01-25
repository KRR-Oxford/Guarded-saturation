package uk.ac.ox.cs.gsat.io;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.Predicate;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.core.factory.DefaultAtomFactory;
import fr.lirmm.graphik.graal.core.factory.DefaultPredicateFactory;
import fr.lirmm.graphik.graal.core.factory.DefaultRuleFactory;
import fr.lirmm.graphik.graal.core.term.DefaultTermFactory;
import uk.ac.ox.cs.gsat.TGD;


class GraalFactory {

    public static Rule createRule(TGD tgd) {

        int bodyLength = tgd.getBodyAtoms().length;
        int headLength = tgd.getHeadAtoms().length;
        Atom[] body = new Atom[bodyLength];
        Atom[] head = new Atom[headLength];

        for (int bi = 0; bi < bodyLength; bi++) {
            body[bi] = createAtom(tgd.getBodyAtom(bi));
        }

        for (int hi = 0; hi < bodyLength; hi++) {
            head[hi] = createAtom(tgd.getHeadAtom(hi));
        }
        
        return DefaultRuleFactory.instance().create(body, head);
    }

    public static Atom createAtom(uk.ac.ox.cs.pdq.fol.Atom atom) {
        Predicate predicate = DefaultPredicateFactory.instance().create(atom.getPredicate(), atom.getPredicate().getArity());

        List<Term> terms = new ArrayList<>();
        for (uk.ac.ox.cs.pdq.fol.Term term : atom.getTerms()) {
            terms.add(createTerm(term));
        }
        
        return DefaultAtomFactory.instance().create(predicate, terms);
    }

    private static Term createTerm(uk.ac.ox.cs.pdq.fol.Term term) {
        if (term.isVariable()) {
            return DefaultTermFactory.instance().createVariable(term);
        } else {
            throw new NotImplementedException(String.format("no graal factory is defined for the term %s of class %s", term, term.getClass()));
        }
    }
}
