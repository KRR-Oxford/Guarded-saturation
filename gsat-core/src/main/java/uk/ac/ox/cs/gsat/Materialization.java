package uk.ac.ox.cs.gsat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import org.apache.commons.io.FilenameUtils;

import uk.ac.ox.cs.gsat.api.MaterializationStatColumns;
import uk.ac.ox.cs.gsat.api.Materializer;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.mat.MaterializerFactory;
import uk.ac.ox.cs.gsat.mat.MaterializerType;
import uk.ac.ox.cs.gsat.satalg.SaturationConfig;
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

    @Parameter(names = { "-d", "--data" }, required = true, description = "Path to the input data file.\n Need to be in a format compatible with the materializer: NTriple for RDFox and datalog facts for DLV")
    private String dataPath;

    @Parameter(names = { "-o", "--output" }, required = true, description = "Path to the output materialization file.")
    private String materializationPath;

    @Parameter(names = { "-q",
            "--queries" }, required = false, description = "Path to the queries file used to filter the input.")
    private String queriesFile;

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
        run();
    }

    private void run() throws Exception {

        String rowName = getRowName(tgdsPath);
        // String inputDataPath = getInputPath(tgdsPath);
        // String outputPath = getMaterializationPath(tgdsPath);

        SaturationConfig saturationConfig = new SaturationConfig(configFile);
        Collection<? extends TGD> saturationFullTGDs = Saturator.computeSaturationFromTGDPath(tgdsPath, queriesFile, saturationConfig);

        StatisticsCollector<MaterializationStatColumns> statsCollector = new StatisticsCollector<>();
        StatisticsLogger statsLogger = getStatisticsLogger(statsCollector, null);

        materializeToFile(dataPath, saturationFullTGDs, materializationPath, statsCollector, rowName);
        
        statsLogger.printHeader();
        statsLogger.printRow(getRowName(tgdsPath));

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

    public static String getRowName(String tgdPath) {
        return FilenameUtils.getBaseName(tgdPath);
    }

    public static String getInputPath(String tgdPath) {
        return Paths.get(tgdPath).getParent().resolve(FilenameUtils.getBaseName(tgdPath) + "-input.nt").toString();
    }

    public static String getMaterializationPath(String tgdPath) {
        String result = Paths.get(tgdPath).getParent().resolve(FilenameUtils.getBaseName(tgdPath) + "-mat.nt")
                .toString();
        System.out.println(String.format("The materialization path is: %s", result));
        return result;
    }

    public static void main(String... args) throws Exception {
        new Materialization(args);
    }

}
