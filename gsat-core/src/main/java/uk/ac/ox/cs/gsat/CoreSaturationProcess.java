package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.ox.cs.gsat.api.SaturationAlgorithm;
import uk.ac.ox.cs.gsat.api.SaturationProcess;
import uk.ac.ox.cs.gsat.api.SaturationStatColumns;
import uk.ac.ox.cs.gsat.api.io.Parser;
import uk.ac.ox.cs.gsat.api.io.TGDTransformation;
import uk.ac.ox.cs.gsat.api.io.TGDProcessor;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.io.DefaultTGDProcessor;
import uk.ac.ox.cs.gsat.io.ParserFactory;
import uk.ac.ox.cs.gsat.io.PredicateDependenciesBasedFilter;
import uk.ac.ox.cs.gsat.satalg.SaturationAlgorithmFactory;
import uk.ac.ox.cs.gsat.statistics.StatisticsCollector;
import uk.ac.ox.cs.pdq.fol.Predicate;

/**
 * The core saturation process wraps together the TGDs preprocessing phase (parsing, filtering, etc) 
 * and the saturation phase using one of the saturation algorithm of the core module
 */
public class CoreSaturationProcess implements SaturationProcess {

    protected final SaturationProcessConfiguration config;
    protected final SaturationAlgorithm saturationAlgorithm;
    protected final TGDProcessor tgdProcessor;

    public CoreSaturationProcess (SaturationProcessConfiguration config) {
        this(config, new ArrayList<>());
    }

    public CoreSaturationProcess (SaturationProcessConfiguration config, List<TGDTransformation<TGD>> transformations) {
        this.config = config;
        this.tgdProcessor = new DefaultTGDProcessor(transformations, config.isSkipingFacts(), config.isNegativeConstraint());
        
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
    private static Collection<TGD> processTGDs(String inputPath, String queriesPath, SaturationProcessConfiguration config) throws Exception {

        // create the transformations for the TGDs
        List<TGDTransformation<TGD>> transformations = new ArrayList<>();

        if (queriesPath != null) {
            Parser queryParser = ParserFactory.instance().create(TGDFileFormat.DLGP, false, false);
            Set<Predicate> wantedPredicates = queryParser.parse(queriesPath).getConjunctiveQueries().stream().map(a -> a.getPredicate())
                .collect(Collectors.toSet());
            transformations.add(new PredicateDependenciesBasedFilter<>(wantedPredicates));
        }

        TGDProcessor tgdProcessor = new DefaultTGDProcessor(transformations, config.isSkipingFacts(), config.isNegativeConstraint());

        return tgdProcessor.getProcessedTGDs(inputPath);
    }

    public void setStatisticCollector(StatisticsCollector<SaturationStatColumns> statisticsCollector) {
        this.saturationAlgorithm.setStatsCollector(statisticsCollector);
    }
}
