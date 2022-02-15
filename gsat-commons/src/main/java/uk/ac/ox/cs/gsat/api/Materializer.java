package uk.ac.ox.cs.gsat.api;

import java.io.OutputStream;
import java.util.Collection;

import uk.ac.ox.cs.gsat.api.io.ParserResult;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.statistics.StatisticsCollector;


/**
 * Materialize the facts induced by a set of full TGDs and output them 
 */
public interface Materializer {

    /**
     * Returns the number of materialized facts
     */
    public long materialize(ParserResult parserResult, Collection<? extends TGD> fullTGDs, OutputStream outputStream) throws Exception;

    /**
     * Returns the number of materialized facts
     */
    public long materialize(ParserResult parserResult, Collection<? extends TGD> fullTGDs, String outputFile) throws Exception;


    /**
     * Initialize the materializer, it has to be called once before materializying
     */
    public void init() throws Exception;
    
    public void setStatsCollector(String rowName, StatisticsCollector<MaterializationStatColumns> statsCollector);
}
