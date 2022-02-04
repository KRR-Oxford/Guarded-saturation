package uk.ac.ox.cs.gsat;

import java.io.File;
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

import org.apache.commons.io.FilenameUtils;

import uk.ac.ox.cs.gsat.api.SaturationAlgorithm;
import uk.ac.ox.cs.gsat.api.io.Parser;
import uk.ac.ox.cs.gsat.api.io.Serializer;
import uk.ac.ox.cs.gsat.api.io.TGDFilter;
import uk.ac.ox.cs.gsat.api.io.TGDProcessing;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.io.ParserFactory;
import uk.ac.ox.cs.gsat.io.PredicateDependenciesBasedFilter;
import uk.ac.ox.cs.gsat.io.SerializerFactory;
import uk.ac.ox.cs.gsat.io.TGDProcessingBuilder;
import uk.ac.ox.cs.gsat.satalg.SaturationAlgorithmFactory;
import uk.ac.ox.cs.gsat.satalg.SaturationConfig;
import uk.ac.ox.cs.pdq.fol.Predicate;

public class Saturator {

    /** The help. */
    @Parameter(names = { "-h", "--help" }, help = true, description = "Displays this help message.")
    private boolean help;

    @Parameter(names = { "-c", "--config" }, required = false, description = "Path to the configuration file.")
    private String configFile = "config.properties";

    @Parameter(names = { "-i", "--input" }, required = true, description = "Path to the input file.")
    private String inputPath;

    @Parameter(names = { "-o", "--output" }, required = true, description = "Path to the output file.")
    private String outputPath;

    @Parameter(names = { "-q",
            "--queries" }, required = false, description = "Path to the queries file used to filter the input.")
    private String queriesFile;

    private Saturator(String... args) throws Exception {
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

        SaturationConfig saturationConfig;
        if (new File(configFile).exists()) {
            saturationConfig = new SaturationConfig(configFile);
        } else {
            saturationConfig = new SaturationConfig();
        }

        File inputFile = new File(inputPath);
        File outputFile = new File(outputPath);

        if (!inputFile.exists())
            throw new IllegalArgumentException("the input file or directory do not exists.");

        if (inputFile.isDirectory()) {
            if (outputFile.exists() && !outputFile.isDirectory()) {
                throw new IllegalArgumentException(
                        "Since the input is a directory the output should also be a directory.");
            }

            Path inputDirectoryPath = Paths.get(inputPath);
            Path outputDirectoryPath = Paths.get(outputPath);
            List<String> singleInputPaths = Files.find(inputDirectoryPath, 999, (p, bfa) -> bfa.isRegularFile())
                    .map(p -> p.toString()).filter(p -> TGDFileFormat.matchesAny(p)).collect(Collectors.toList());

            // sort the input paths
            Collections.sort(singleInputPaths);
            
            for (String singleInput : singleInputPaths) {
                String singleOutputPath = getSingleOutputPath(singleInput, inputDirectoryPath, outputDirectoryPath);
                runSingleFile(saturationConfig, singleInput, singleOutputPath);
            }
        } else {
            runSingleFile(saturationConfig, inputPath, outputPath);
        }
    }

    /**
     * In case the input and the output are directories, we generate the name of the
     * single output files
     * @param singleInput 
     * @param inputDirectoryPath 
     * @param outputDirectoryPath 
     */
    private static String getSingleOutputPath(String singleInput, Path inputDirectoryPath, Path outputDirectoryPath) {
        Path singleInputPath = Paths.get(singleInput);
        Path relativeInputPath = inputDirectoryPath.relativize(singleInputPath);
        Path relativeOutputPath = Paths.get(FilenameUtils.getBaseName(singleInput) + "-sat.dlgp");

        if (relativeInputPath.getParent() != null) {
            relativeOutputPath = relativeInputPath.getParent().resolve(relativeOutputPath);
        }
        
        String singleOutputPath = outputDirectoryPath.resolve(relativeOutputPath).toString();
        return singleOutputPath;
    }

    private void runSingleFile(SaturationConfig saturationConfig, String input, String output) throws Exception {
        Collection<? extends TGD> saturationFullTGDs = computeSaturationFromTGDPath(input, queriesFile,
                saturationConfig);
        writeTGDsToFile(output, saturationFullTGDs);
    }

    public static void writeTGDsToFile(String outputPath, Collection<? extends TGD> tgds) throws Exception {
        TGDFileFormat outputFormat = TGDFileFormat.getFormatFromPath(outputPath);
        if (outputFormat == null) {
            String message = String.format("The output file should use one of these extensions %s",
                    Arrays.asList(TGDFileFormat.values()), TGDFileFormat.getExtensions());
            throw new IllegalArgumentException(message);
        }


        // create the output directory
        Path outputDirPath = Paths.get(outputPath).getParent();
        if (outputDirPath != null) {
            File outputDir = outputDirPath.toFile();
            if (!outputDir.exists())
                outputDir.mkdirs();
        }

        Serializer serializer = SerializerFactory.instance().create(outputFormat);

        serializer.open(outputPath);
        serializer.writeTGDs(tgds);
        serializer.close();

    }

    public static Collection<? extends TGD> computeSaturationFromTGDPath(String inputPath, String queriesPath,
            SaturationConfig config) throws Exception {

        TGDFileFormat inputFormat = TGDFileFormat.getFormatFromPath(inputPath);

        if (inputFormat == null) {
            String message = String.format(
                    "The input file format should be of one of %s and should use one of these extensions %s",
                    Arrays.asList(TGDFileFormat.values()), TGDFileFormat.getExtensions());
            throw new IllegalArgumentException(message);
        }

        Parser parser = ParserFactory.instance().create(inputFormat);

        parser.parse(inputPath, Configuration.isSaturationOnly(), Configuration.includeNegativeConstraint());

        List<TGDFilter<TGD>> filters = new ArrayList<>();

        if (queriesPath != null) {
            Parser queryParser = ParserFactory.instance().create(TGDFileFormat.DLGP);
            queryParser.parse(queriesPath, true, false);
            Set<Predicate> wantedPredicates = queryParser.getConjunctiveQueries().stream().map(a -> a.getPredicate())
                    .collect(Collectors.toSet());
            filters.add(new PredicateDependenciesBasedFilter<>(wantedPredicates));
        }

        TGDProcessing tgdProcessing = TGDProcessingBuilder.instance().setParser(parser).setFilters(filters).build();

        SaturationAlgorithm algorithm = SaturationAlgorithmFactory.instance().create(config);

        return algorithm.run(tgdProcessing.getTGDs());
    }

    public static void main(String... args) throws Exception {
        new Saturator(args);
    }
}
