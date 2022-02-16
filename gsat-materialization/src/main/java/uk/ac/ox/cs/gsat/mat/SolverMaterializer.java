package uk.ac.ox.cs.gsat.mat;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.NotImplementedException;

import uk.ac.ox.cs.gsat.MaterializationConfiguration;
import uk.ac.ox.cs.gsat.api.MaterializationStatColumns;
import uk.ac.ox.cs.gsat.api.Materializer;
import uk.ac.ox.cs.gsat.api.io.ParserResult;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.io.DatalogSerializer;
import uk.ac.ox.cs.gsat.statistics.StatisticsCollector;

class SolverMaterializer implements Materializer {

    protected final static String DATALOG_PATH = "sat.rul";
    
    protected String statsRowName;
    protected StatisticsCollector<MaterializationStatColumns> statsCollector;

    protected final DatalogSerializer datalogSerializer;
    protected final MaterializationConfiguration config;

    public SolverMaterializer(MaterializationConfiguration config) {
        this.datalogSerializer = new DatalogSerializer();
        this.config = config;
    }

    @Override
    public long materialize(ParserResult parsedData, Collection<? extends TGD> fullTGDs, OutputStream outputStream)
            throws Exception {
        throw new NotImplementedException("");
    }

    @Override
    public long materialize(ParserResult parsedData, Collection<? extends TGD> fullTGDs, String outputFile)
            throws Exception {

        // write the full TGDs in datalog format 
        this.datalogSerializer.writeTGDs(fullTGDs);

        // write the data in datalog format
        this.datalogSerializer.writeAtoms(parsedData.getAtoms());
        statsCollector.tick(statsRowName, MaterializationStatColumns.MAT_DATA_LOAD_TIME);

        
        // run the solver on the datalog file
        SolverOutput solverOutput = Utils.invokeSolver(config.getSolverPath(), config.getSolverOptionsGrounding(),
                Arrays.asList(DATALOG_PATH));
        statsCollector.tick(statsRowName, MaterializationStatColumns.MAT_TIME);

        long materializationSize = solverOutput.getNumberOfLinesOutput();
        statsCollector.put(statsRowName, MaterializationStatColumns.MAT_SIZE, materializationSize);

        Utils.writeSolverOutput(solverOutput, outputFile);
        statsCollector.tick(statsRowName, MaterializationStatColumns.MAT_WRITING_TIME);

        
        return materializationSize;
    }

    @Override
    public void init() throws Exception {
        this.datalogSerializer.open(DATALOG_PATH);
        this.statsCollector.tick(statsRowName, MaterializationStatColumns.MAT_INIT_TIME);
    }

    @Override
    public void setStatsCollector(String rowName, StatisticsCollector<MaterializationStatColumns> statsCollector) {
        this.statsRowName = rowName;
        this.statsCollector = statsCollector;
    }
}
