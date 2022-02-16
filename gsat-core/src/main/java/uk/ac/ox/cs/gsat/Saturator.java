package uk.ac.ox.cs.gsat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.hp.hpl.jena.reasoner.IllegalParameterException;

import org.apache.commons.io.FilenameUtils;

import uk.ac.ox.cs.gsat.api.SaturationProcess;
import uk.ac.ox.cs.gsat.api.SaturationStatColumns;
import uk.ac.ox.cs.gsat.api.io.Parser;
import uk.ac.ox.cs.gsat.api.io.Serializer;
import uk.ac.ox.cs.gsat.api.io.TGDTransformation;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.io.ParserFactory;
import uk.ac.ox.cs.gsat.io.PredicateDependenciesBasedFilter;
import uk.ac.ox.cs.gsat.io.SerializerFactory;
import uk.ac.ox.cs.gsat.statistics.DefaultStatisticsCollector;
import uk.ac.ox.cs.gsat.statistics.StatisticsCollector;
import uk.ac.ox.cs.gsat.statistics.StatisticsColumn;
import uk.ac.ox.cs.gsat.statistics.StatisticsLogger;
import uk.ac.ox.cs.pdq.fol.Predicate;

/**
 * Saturator is the main class of the core module
 */
public class Saturator {

    protected static final String STATS_FILENAME = "stats.csv";
    protected static final String CONF_FILENAME = "config.properties";

    @Parameter(names = { "-h", "--help" }, help = true, description = "Displays this help message.")
    protected boolean help;

    @Parameter(names = { "-c", "--config" }, required = false, description = "Path to the configuration file.")
    protected String configFile;

    @Parameter(names = { "-t", "--tgds" }, required = true, description = "Path to the input file.")
    protected String inputPath;

    @Parameter(names = { "-o", "--output" }, required = true, description = "Path to the output file.")
    protected String outputPath;

    @Parameter(names = { "-q",
            "--queries" }, required = false, description = "Path to the queries file used to filter the input.")
    protected String queriesPath;

    @Parameter(names = { "-cb", "--chaseBench" }, required = false, description = "Chase bench input")
    protected boolean chaseBenchInput;

    // collector of the satistics of saturation algorithm
    protected StatisticsCollector<SaturationStatColumns> statisticsCollector;

    protected SaturationProcess saturationProcess;

    protected File inputFile;

    protected File outputFile;

    protected SaturatorWatcher watcher;

    private boolean initialized = false;

    public Saturator(String configPath, String inputPath, String outputPath) throws Exception {

        this.configFile = configPath;
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        
        init();
    }
    
    Saturator(String... args) throws Exception {
        JCommander jc = new JCommander(this);

        try {
            jc.parse(args);
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            jc.usage();
            System.exit(1);
        }

        if (this.help) {
            jc.usage();
            System.exit(0);
        }

        init();
    }

    protected void init() throws Exception {

        statisticsCollector = new DefaultStatisticsCollector<>();
        inputFile = new File(inputPath);

        outputFile = new File(outputPath);
        this.initialized = true;
    }
    
    public void run() throws Exception {

        if (!this.initialized)
            return;
        
        if (isInputDirectory()) {
            // run on the root directory
            runSingleDirectory(inputPath, outputPath);

            // run on the sub directories
            File[] subDirectories = inputFile.listFiles(File::isDirectory);

            for (File subDirectory : subDirectories) {
                Log.GLOBAL.info("Run saturation in directory: " + subDirectory);

                Path inputDirRelativePath = Paths.get(inputPath).relativize(subDirectory.toPath());
                Path outputSubDirectory = Paths.get(outputPath).resolve(inputDirRelativePath);

                outputSubDirectory.toFile().mkdirs();

                runSingleDirectory(subDirectory.getCanonicalPath(), outputSubDirectory.toString());
            }
        } else {
            setConfiguration(".");
            StatisticsLogger statsLogger = getStatisticsLogger(statisticsCollector, null, null);
            statsLogger.printHeader();
            runSingleFile(inputPath, outputPath);
            statsLogger.printRow(getRowName(inputPath));
        }
    }

    protected void setConfiguration(String currentDirectoryPath) throws Exception {
        String saturationConfigPath = getConfigurationPath(currentDirectoryPath);
        SaturationProcessConfiguration saturationConfig;
        if (saturationConfigPath != null)
            saturationConfig = new SaturationProcessConfiguration(saturationConfigPath);
        else
            saturationConfig = new SaturationProcessConfiguration();
        saturationProcess = new CoreSaturationProcess(saturationConfig, getTransformations());
        saturationProcess.setStatisticCollector(statisticsCollector);
        if (watcher != null)
            watcher.changeConfiguration(saturationConfigPath);
    }
    
    protected String getConfigurationPath(String currentDirectoryPath) throws IOException  {
        String saturationConfigPath;

        // if the configuration file is given as an input, it overrides the others
        if (configFile != null) {
            if (new File(configFile).exists()) {
                saturationConfigPath = configFile;
            } else {
                String message = String.format("Configuration file %s do not exists", configFile);
                throw new IllegalParameterException(message);
            }
        } else {
            // if there is a configuration file in the current directory, we use it
            Path currentDirectoryConfigPath = Paths.get(currentDirectoryPath).resolve(CONF_FILENAME);

            if (currentDirectoryConfigPath.toFile().exists()) {
                saturationConfigPath = currentDirectoryConfigPath.toString();
            } else {
                saturationConfigPath = null;
            }
        }

        return saturationConfigPath;
    }

    private static String getRowName(String singleInput) {
        return FilenameUtils.getBaseName(singleInput);
    }

    protected void runSingleFile(String input, String output) throws Exception {
        Log.GLOBAL.info("Saturation of " + input);
        
        String rowName = getRowName(input);
        Collection<? extends TGD> saturationFullTGDs = saturationProcess.saturate(rowName, input);
        writeTGDsToFile(output, saturationFullTGDs);

        // report about the performed saturation
        if (this.watcher != null)
            this.watcher.singleSaturationDone(rowName, input, output, saturationFullTGDs);

    }

    /**
     * run the saturation on the files contained in a directory (without sub-directory files) 
     * Its create a statistics file for this specific directory
     * @throws Exception
     */    
    protected void runSingleDirectory(String inputDirectoryPath, String outputDirectoryPath) throws Exception {

        setConfiguration(inputDirectoryPath);
        // report about the new directory
        if (this.watcher != null)
            this.watcher.changeDirectory(inputDirectoryPath, outputDirectoryPath);
        
        // clear the statistics collector
        statisticsCollector.clear();
        // create a statistics logger writing the statistics in <outputdirectory>/stats.csv 
        StatisticsLogger statsLogger = getStatisticsLogger(statisticsCollector, outputDirectoryPath, STATS_FILENAME);
        statsLogger.printHeader();
    
        
        List<String> singleInputPaths = Files.find(Paths.get(inputDirectoryPath), 1, (p, bfa) -> bfa.isRegularFile())
                .map(p -> p.toString()).filter(p -> TGDFileFormat.matchesAny(p)).collect(Collectors.toList());
    
        // sort the input paths
        Collections.sort(singleInputPaths);
    
        for (String singleInput : singleInputPaths) {
            String rowName = getRowName(singleInput);
            String singleOutputPath = getSingleOutputPath(singleInput, inputDirectoryPath, outputDirectoryPath);
            try {
                runSingleFile(singleInput, singleOutputPath);
            } catch (Exception e) {
                statisticsCollector.put(rowName, SaturationStatColumns.TIME, "ERROR");
                e.printStackTrace();
            }
            statsLogger.printRow(rowName);
        }
    
    }
    
    public static <T extends StatisticsColumn> StatisticsLogger getStatisticsLogger(
            StatisticsCollector<T> statsCollector, String outputDirectory, String statsFileName)
            throws FileNotFoundException {
    
        StatisticsLogger statsLogger;
        if (outputDirectory != null) {
            String statsFilePath = Paths.get(outputDirectory).resolve(statsFileName).toString();
            new File(statsFilePath).delete();
            PrintStream statsStream = new PrintStream(new FileOutputStream(statsFilePath, true));
            statsLogger = new StatisticsLogger(statsStream, statsCollector);
        } else {
            statsLogger = new StatisticsLogger(System.out, statsCollector);
        }
        statsLogger.setSortedHeader(Arrays.asList(SaturationStatColumns.values()));
    
        return statsLogger;
    }
    
    /**
     * In case the input and the output are directories, we generate the name of the
     * single output files
     * 
     * @param singleInput
     * @param inputDirectoryPath
     * @param outputDirectoryPath
     */
    public static String getSingleOutputPath(String singleInput, String inputDirectoryPath, String outputDirectoryPath) {
        Path singleInputPath = Paths.get(singleInput);
        Path relativeInputPath = Paths.get(inputDirectoryPath).relativize(singleInputPath);
        Path relativeOutputPath = Paths.get(FilenameUtils.getBaseName(singleInput) + "-sat.dlgp");

        if (relativeInputPath.getParent() != null) {
            relativeOutputPath = relativeInputPath.getParent().resolve(relativeOutputPath);
        }

        String singleOutputPath = Paths.get(outputDirectoryPath).resolve(relativeOutputPath).toString();
        return singleOutputPath;
    }

    public static void writeTGDsToFile(String outputPath, Collection<? extends TGD> tgds) throws Exception {
        TGDFileFormat outputFormat = TGDFileFormat.getFormatFromPath(outputPath);
        if (outputFormat == null) {
            String message = String.format("The output file should use one of these extensions %s",
                    TGDFileFormat.getExtensions());
            throw new IllegalArgumentException(message);
        }

        Serializer serializer = SerializerFactory.instance().create(outputFormat);

        serializer.open(outputPath);
        serializer.writeTGDs(tgds);
        serializer.close();

    }

    /**
     * generate the TGD transformations list according to the inputs
     */
    protected List<TGDTransformation<TGD>> getTransformations() throws Exception {
        List<TGDTransformation<TGD>> transformations = new ArrayList<>();

        if (queriesPath != null) {
            Parser queryParser = ParserFactory.instance().create(TGDFileFormat.DLGP, false, false);
            Set<Predicate> wantedPredicates = queryParser.parse(queriesPath).getConjunctiveQueries().stream()
                    .map(a -> a.getPredicate()).collect(Collectors.toSet());
            transformations.add(new PredicateDependenciesBasedFilter<>(wantedPredicates));
        }

        return transformations;
    }

    protected boolean isInputDirectory() {
        if (!inputFile.exists())
            throw new IllegalArgumentException("The input file or directory do not exists.");

        if (inputFile.isDirectory() && !chaseBenchInput) {
            if (outputFile.exists() && !outputFile.isDirectory()) {
                throw new IllegalArgumentException(
                        "Since the input is a directory the output should also be a directory.");
            }
            return true;
        }
        return false;
    }

    public static void main(String... args) throws Exception {
        Saturator saturator = new Saturator(args);
        saturator.run();
    }

    public void setWatcher(SaturatorWatcher watcher) {
        this.watcher = watcher;
    }
}
