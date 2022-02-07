package uk.ac.ox.cs.gsat.mat;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.NotImplementedException;

import uk.ac.ox.cs.gsat.Configuration;
import uk.ac.ox.cs.gsat.TGDFileFormat;
import uk.ac.ox.cs.gsat.api.MaterializationStatColumns;
import uk.ac.ox.cs.gsat.api.Materializer;
import uk.ac.ox.cs.gsat.api.io.Parser;
import uk.ac.ox.cs.gsat.api.io.ParserResult;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.io.DatalogSerializer;
import uk.ac.ox.cs.gsat.io.ParserFactory;
import uk.ac.ox.cs.gsat.statistics.StatisticsCollector;

class SolverMaterializer implements Materializer {

    protected final static String DATALOG_PATH = "sat.rul";
    
    protected String statsRowName;
    protected StatisticsCollector<MaterializationStatColumns> statsCollector;

    protected final DatalogSerializer datalogSerializer;

    public SolverMaterializer() {
        this.datalogSerializer = new DatalogSerializer();
    }

    @Override
    public long materialize(String inputDataFile, Collection<? extends TGD> fullTGDs, OutputStream outputStream)
            throws Exception {
        throw new NotImplementedException("");
    }

    @Override
    public long materialize(String inputDataFile, Collection<? extends TGD> fullTGDs, String outputFile)
            throws Exception {

        // write the full TGDs in datalog format 
        this.datalogSerializer.writeTGDs(fullTGDs);

        //read and write the data in datalog format
        TGDFileFormat inputFormat = TGDFileFormat.getFormatFromPath(inputDataFile);
        Parser parser = ParserFactory.instance().create(inputFormat, false, false);
        ParserResult parsedData = parser.parse(inputDataFile);
        this.datalogSerializer.writeAtoms(parsedData.getAtoms());
        statsCollector.tick(statsRowName, MaterializationStatColumns.MAT_DATA_LOAD_TIME);

        
        // run the solver on the datalog file
        SolverOutput solverOutput = Utils.invokeSolver(Configuration.getSolverPath(), Configuration.getSolverOptionsGrounding(),
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
