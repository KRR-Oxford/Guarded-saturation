package uk.ac.ox.cs.gsat.io;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import uk.ac.ox.cs.gsat.api.io.Parser;
import uk.ac.ox.cs.gsat.api.io.TGDFilter;
import uk.ac.ox.cs.gsat.api.io.TGDProcessing;
import uk.ac.ox.cs.gsat.fol.TGD;

public class TGDProcessingBuilder {

    private static final TGDProcessingBuilder INSTANCE = new TGDProcessingBuilder();
    private Parser parser;
    private List<TGDFilter<TGD>> filters;

    private TGDProcessingBuilder() {
    }

    public static TGDProcessingBuilder instance() {
        return INSTANCE;
    }

    public TGDProcessingBuilder setParser(Parser parser) {
        this.parser = parser;
        return this;
    }

    public TGDProcessingBuilder setFilters(List<TGDFilter<TGD>> filters) {
        this.filters = filters;
        return this;
    }

    public TGDProcessing build() {

        if (this.parser == null || this.filters == null) {
            String message = "parser and filter have to be set before building";
            throw new IllegalStateException(message);
        }
        
        TGDProcessing result = new SimpleTGDProcessing(parser, filters);
        this.filters = null;
        this.parser = null;
        
        return result;
    }

    static class SimpleTGDProcessing implements TGDProcessing {

        private Parser parser;
        private List<TGDFilter<TGD>> filters;

        SimpleTGDProcessing(Parser parser, List<TGDFilter<TGD>> filters) {
            this.parser = parser;
            this.filters = filters;
        }

        @Override
        public Collection<TGD> getTGDs() {
            Collection<TGD> result = new HashSet<>();

            for(TGD tgd : parser.getTGDs()) {
                boolean isKept = true;
                for (TGDFilter<TGD> filter : filters) {
                    isKept = filter.isKept(tgd);
                    if (!isKept)
                        break;
                }

                if (isKept)
                    result.add(tgd);
            }

            return result;
        }

    }

}
