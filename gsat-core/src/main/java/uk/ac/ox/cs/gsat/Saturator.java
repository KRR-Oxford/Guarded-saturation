package uk.ac.ox.cs.gsat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import uk.ac.ox.cs.gsat.api.SaturationAlgorithm;
import uk.ac.ox.cs.gsat.api.io.Parser;
import uk.ac.ox.cs.gsat.api.io.Serializer;
import uk.ac.ox.cs.gsat.api.io.TGDProcessing;
import uk.ac.ox.cs.gsat.io.ParserFactory;
import uk.ac.ox.cs.gsat.io.SerializerFactory;
import uk.ac.ox.cs.gsat.io.TGDProcessingBuilder;
import uk.ac.ox.cs.gsat.satalg.SaturationAlgorithmFactory;
import uk.ac.ox.cs.gsat.satalg.SaturationAlgorithmType;

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
        TGDFileFormat inputFormat = TGDFileFormat.getFormatFromPath(this.inputFile);

        if (inputFormat == null) {
            String message = String.format("The input file format should be of one of %s and should use one of these extensions %s", Arrays.asList(TGDFileFormat.values()), TGDFileFormat.getExtensions());
            throw new IllegalArgumentException(message);
        }

        TGDFileFormat outputFormat = TGDFileFormat.getFormatFromPath(this.outputFile);

        if (outputFormat == null) {
            String message = String.format("The output file should use one of these extensions %s", Arrays.asList(TGDFileFormat.values()), TGDFileFormat.getExtensions());
            throw new IllegalArgumentException(message);
        }

        Parser parser = ParserFactory.instance().create(inputFormat);

        parser.parse(inputFile, Configuration.isSaturationOnly(), Configuration.includeNegativeConstraint());

        TGDProcessing tgdProcessing = TGDProcessingBuilder.instance()
            .setParser(parser)
            .setFilters(new ArrayList<>())
            .build();

        SaturationAlgorithmType algorithmType = Configuration.getSaturatonAlgType();
        SaturationAlgorithm algorithm = SaturationAlgorithmFactory.instance().create(algorithmType);

        Serializer serializer = SerializerFactory.instance().create(outputFormat);

        serializer.open(this.outputFile);
        serializer.writeTGDs(algorithm.run(tgdProcessing.getTGDs()));
        serializer.close();
    }

    private boolean isInputDirectory() {
        return new File(this.inputFile).isDirectory();
    }

    public static void main(String... args) throws Exception {
		new Saturator(args);
	}
}
