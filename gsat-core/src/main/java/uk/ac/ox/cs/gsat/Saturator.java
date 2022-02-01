package uk.ac.ox.cs.gsat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

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
import uk.ac.ox.cs.gsat.satalg.SaturationAlgorithmType;
import uk.ac.ox.cs.pdq.fol.Predicate;

public class Saturator {

    /** The help. */
    @Parameter(names = { "-h", "--help" }, help = true, description = "Displays this help message.")
    private boolean help;

    @Parameter(names = { "-c", "--config" }, required = false, description = "Path to the configuration file.")
    private String configFile;

    @Parameter(names = { "-i", "--input" }, required = true, description = "Path to the input file.")
    private String inputFile;

    @Parameter(names = { "-o", "--output" }, required = true, description = "Path to the output file.")
    private String outputFile;

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

        Collection<? extends TGD> saturationFullTGDs = computeSaturationFromTGDPath(inputFile, queriesFile);


        writeTGDsToFile(outputFile, saturationFullTGDs);
    }

    public static void writeTGDsToFile(String outputPath, Collection<? extends TGD> tgds) throws Exception {
        TGDFileFormat outputFormat = TGDFileFormat.getFormatFromPath(outputPath);
        if (outputFormat == null) {
            String message = String.format("The output file should use one of these extensions %s",
                    Arrays.asList(TGDFileFormat.values()), TGDFileFormat.getExtensions());
            throw new IllegalArgumentException(message);
        }

        Serializer serializer = SerializerFactory.instance().create(outputFormat);

        serializer.open(outputPath);
        serializer.writeTGDs(tgds);
        serializer.close();

    }

    public static Collection<? extends TGD> computeSaturationFromTGDPath(String inputPath, String queriesPath) throws Exception {

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

        SaturationAlgorithmType algorithmType = Configuration.getSaturatonAlgType();
        SaturationAlgorithm algorithm = SaturationAlgorithmFactory.instance().create(algorithmType);

        return algorithm.run(tgdProcessing.getTGDs());
    }

    private boolean isInputDirectory() {
        return new File(this.inputFile).isDirectory();
    }

    public static void main(String... args) throws Exception {
        new Saturator(args);
    }
}
