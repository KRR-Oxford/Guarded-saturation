package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.ox.cs.gsat.api.SaturationAlgorithm;
import uk.ac.ox.cs.gsat.api.SaturationStatColumns;
import uk.ac.ox.cs.gsat.api.io.Parser;
import uk.ac.ox.cs.gsat.api.io.TGDFilter;
import uk.ac.ox.cs.gsat.api.io.TGDProcessor;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.io.DefaultTGDProcessor;
import uk.ac.ox.cs.gsat.io.ParserFactory;
import uk.ac.ox.cs.gsat.io.PredicateDependenciesBasedFilter;
import uk.ac.ox.cs.gsat.satalg.SaturationAlgorithmFactory;
import uk.ac.ox.cs.gsat.statistics.StatisticsCollector;
import uk.ac.ox.cs.pdq.fol.Predicate;

/**
 * Saturation process wraps together the TGDs processing phase (parsing, filtering, etc) and the saturation phase
 */
public class SaturationProcess {

    protected final SaturationProcessConfiguration config;
    protected final SaturationAlgorithm saturationAlgorithm;
    protected final TGDProcessor tgdProcessor;

    public SaturationProcess (SaturationProcessConfiguration config) {
        this(config, new ArrayList<>());
    }

    public SaturationProcess (SaturationProcessConfiguration config, List<TGDFilter<TGD>> filters) {
        this.config = config;
        this.tgdProcessor = new DefaultTGDProcessor(filters, config.isSkipingFacts(), config.isNegativeConstraint());
        
        this.saturationAlgorithm = SaturationAlgorithmFactory.instance().create(config);
    }

    public Collection<? extends TGD> saturate(String processName, String inputPath) throws Exception {
        Collection<TGD> processedTGDs = tgdProcessor.getProcessedTGDs(inputPath);

        return saturationAlgorithm.run(processName, processedTGDs);
    }
    
    public Collection<? extends TGD> saturate(String inputPath) throws Exception {
        Collection<TGD> processedTGDs = tgdProcessor.getProcessedTGDs(inputPath);

        return saturationAlgorithm.run(processedTGDs);
    }

    /**
     * parse and filter the TGDs from an input path
     * @throws Exception
     */
    protected static Collection<TGD> processTGDs(String inputPath, String queriesPath, SaturationProcessConfiguration config) throws Exception {

        // create the filters for the TGDs
        List<TGDFilter<TGD>> filters = new ArrayList<>();

        if (queriesPath != null) {
            Parser queryParser = ParserFactory.instance().create(TGDFileFormat.DLGP, false, false);
            Set<Predicate> wantedPredicates = queryParser.parse(queriesPath).getConjunctiveQueries().stream().map(a -> a.getPredicate())
                .collect(Collectors.toSet());
            filters.add(new PredicateDependenciesBasedFilter<>(wantedPredicates));
        }

        TGDProcessor tgdProcessor = new DefaultTGDProcessor(filters, config.isSkipingFacts(), config.isNegativeConstraint());

        return tgdProcessor.getProcessedTGDs(inputPath);
    }

    public void setStatisticCollector(StatisticsCollector<SaturationStatColumns> statisticsCollector) {
        this.saturationAlgorithm.setStatsCollector(statisticsCollector);
    }
}
