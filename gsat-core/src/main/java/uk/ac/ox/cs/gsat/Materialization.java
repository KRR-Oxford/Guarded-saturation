package uk.ac.ox.cs.gsat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import uk.ac.ox.cs.gsat.api.MaterializationStatColumns;
import uk.ac.ox.cs.gsat.api.Materializer;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.mat.MaterializerFactory;
import uk.ac.ox.cs.gsat.mat.MaterializerType;
import uk.ac.ox.cs.gsat.statistics.DefaultStatisticsCollector;
import uk.ac.ox.cs.gsat.statistics.StatisticsCollector;
import uk.ac.ox.cs.gsat.statistics.StatisticsColumn;
import uk.ac.ox.cs.gsat.statistics.StatisticsLogger;

public class Materialization {

    private final static String STATS_FILENAME = "mat-stats.csv";

    /** The help. */
    @Parameter(names = { "-h", "--help" }, help = true, description = "Displays this help message.")
    private boolean help;

    @Parameter(names = { "-c", "--config" }, required = false, description = "Path to the configuration file.")
    private String configFile;

    @Parameter(names = { "-t", "--tgds" }, required = true, description = "Path to the tgds file.")
    private String tgdsPath;

    @Parameter(names = { "-d", "--data" }, required = false, description = "Path to the input data file.\n Need to be in a format compatible with the materializer: NTriple for RDFox and datalog facts for DLV")
    private String dataPath;

    @Parameter(names = { "-o", "--output" }, required = false, description = "Path to the output saturation file.")
    private String materializationPath;

    @Parameter(names = { "-q",
            "--queries" }, required = false, description = "Path to the queries file used to filter the input.")
    private String queriesFile;

    private Saturator saturator;
    private StatisticsCollector<MaterializationStatColumns> statsCollector;
    private StatisticsLogger statsLogger;

    private Materialization(String... args) throws Exception {
        JCommander jc = new JCommander(this);

        try {
            jc.parse(args);
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            jc.usage();
            return;
        }
        if (this.help) {
            jc.usage();
            return;
        }
        saturator = new Saturator(configFile, tgdsPath, materializationPath);
        statsCollector = new DefaultStatisticsCollector<>();
        statsLogger = getStatisticsLogger(statsCollector, null);

        saturator.setWatcher(new SaturatorWatcherForMaterialization());
        saturator.run();
    }

    public static void materializeToFile(String dataPath, Collection<? extends TGD> saturationFullTGDs, String materializationPath,
            StatisticsCollector<MaterializationStatColumns> statsCollector, String rowName)
            throws Exception {

        Materializer materializer = MaterializerFactory.create(MaterializerType.SOLVER);
        statsCollector.start(rowName);
        materializer.setStatsCollector(rowName, statsCollector);
        materializer.init();
        statsCollector.put(rowName, MaterializationStatColumns.MAT_FTGD_NB, saturationFullTGDs.size());

        materializer.materialize(dataPath, saturationFullTGDs, materializationPath);
        statsCollector.stop(rowName, MaterializationStatColumns.MAT_TOTAL);
    }

    public static <T extends StatisticsColumn> StatisticsLogger getStatisticsLogger(
            StatisticsCollector<T> statsCollector, String inputDirectory) throws FileNotFoundException {

        StatisticsLogger statsLogger;
        if (inputDirectory != null) {
            String statsFilePath = Paths.get(inputDirectory).resolve(STATS_FILENAME).toString();
            new File(statsFilePath).delete();
            PrintStream statsStream = new PrintStream(new FileOutputStream(statsFilePath, true));
            statsLogger = new StatisticsLogger(statsStream, statsCollector);
        } else {
            statsLogger = new StatisticsLogger(System.out, statsCollector);
        }
        statsLogger.setSortedHeader(Arrays.asList(MaterializationStatColumns.values()));

        return statsLogger;
    }

    /**
     * compute the path of the materialization from the path of the saturation and the row name
     */
    private static String getMaterializationPath(String saturationPath, String rowName) {
        Path saturation = Paths.get(saturationPath);
        String materializationFileName = rowName + "-mat.txt";
        if (saturation.getParent() != null)
            return saturation.getParent().resolve(materializationFileName).toString();

        return materializationFileName;
    }

    /**
     * compute the path of the data input from the path of the tgds input
     */
    private String getInputPath(String inputPath) {

        if (dataPath != null)
            return dataPath;
        
        // Path saturation = Paths.get(saturationPath);
        // String dataFileName = rowName + "-data.dlgp";
        // if (saturation.getParent() != null)
        //     return saturation.getParent().resolve(dataFileName).toString();

        return inputPath;
    }
    
    public static void main(String... args) throws Exception {
        new Materialization(args);
    }

    class SaturatorWatcherForMaterialization implements SaturatorWatcher {

        public void changeDirectory(String inputDirectoryPath, String outputDirectoryPath) throws FileNotFoundException {
            statsLogger = getStatisticsLogger(statsCollector, outputDirectoryPath);
            statsLogger.printHeader();
        }
        
        public void singleSaturationDone(String rowName, String inputPath, String outputPath, Collection<? extends TGD> saturationFullTGDs) throws Exception {
            materializeToFile(getInputPath(inputPath), saturationFullTGDs, getMaterializationPath(outputPath, rowName), statsCollector, rowName);
            statsLogger.printRow(rowName);
        }
    }
}
