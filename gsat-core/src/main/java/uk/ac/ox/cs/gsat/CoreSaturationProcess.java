package uk.ac.ox.cs.gsat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import uk.ac.ox.cs.gsat.api.SaturationAlgorithm;
import uk.ac.ox.cs.gsat.api.SaturationProcess;
import uk.ac.ox.cs.gsat.api.SaturationStatColumns;
import uk.ac.ox.cs.gsat.api.io.TGDProcessor;
import uk.ac.ox.cs.gsat.api.io.TGDTransformation;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.io.DefaultTGDProcessor;
import uk.ac.ox.cs.gsat.satalg.SaturationAlgorithmFactory;
import uk.ac.ox.cs.gsat.statistics.StatisticsCollector;

/**
 * The core saturation process wraps together the TGDs preprocessing phase (parsing, filtering, etc) 
 * and the saturation phase using one of the saturation algorithm of the core module
 */
public class CoreSaturationProcess implements SaturationProcess {

    protected final SaturationProcessConfiguration config;
    protected final SaturationAlgorithm saturationAlgorithm;
    protected final TGDProcessor tgdProcessor;

    /**
     * Create a saturation process with a core saturation algorithm without TGD transformation
     */
    public CoreSaturationProcess(SaturationProcessConfiguration config) {
        this(config, new ArrayList<>());
    }

    /**
     * Create a saturation process with a core saturation algorithm with TGD transformations applied during a pre-processing step
     */
    public CoreSaturationProcess(SaturationProcessConfiguration config, List<TGDTransformation<TGD>> transformations) {
        this(config, new DefaultTGDProcessor(transformations, config.isSkipingFacts(),
                                             config.isNegativeConstraint()));
    }

    /**
     * Create a saturation process with a core saturation algorithm with TGD processor
     */
    public CoreSaturationProcess (SaturationProcessConfiguration config, TGDProcessor tgdProcessor) {
        this.config = config;
        this.tgdProcessor = tgdProcessor;
        
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

    public void setStatisticCollector(StatisticsCollector<SaturationStatColumns> statisticsCollector) {
        this.saturationAlgorithm.setStatsCollector(statisticsCollector);
    }
}
