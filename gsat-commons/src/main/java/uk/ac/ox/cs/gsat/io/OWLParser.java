package uk.ac.ox.cs.gsat.io;

import java.io.File;

import fr.lirmm.graphik.graal.io.owl.OWL2Parser;
import uk.ac.ox.cs.gsat.api.io.ParserResult;

public class OWLParser extends DLGPParser {

    protected OWLParser(boolean skipFacts, boolean includeNegativeConstraints) {
        super(skipFacts, includeNegativeConstraints);
    }

    @Override
    public ParserResult parse(String file) throws Exception {
        fr.lirmm.graphik.graal.api.io.Parser<Object> parser = new OWL2Parser(new File(file));
        return new DefaultParserResult(parser);
    }

}
