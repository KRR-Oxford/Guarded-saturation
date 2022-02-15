package uk.ac.ox.cs.gsat.regression;

import java.io.File;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import uk.ac.ox.cs.gsat.Saturator;

public class App {

    @Parameter(names = { "-h", "--help" }, help = true, description = "Displays this help message.")
    private boolean help;

    @Parameter(names = { "-i", "--input" }, required = true, description = "Path to the input directory.")
    private String inputPath;

    @Parameter(names = { "-o", "--output" }, required = true, description = "Path to the output directory.")
    private String outputPath;

    @Parameter(names = { "-e", "--expected" }, required = true, description = "Path to the expected directory.")
    private String expectedPath;

    private File inputDir;

    private File outputDir;

    private File expectedDir;

    private Saturator saturator;

    App(String... args) throws Exception {
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

        inputDir = new File(inputPath);
        outputDir = new File(outputPath);
        expectedDir = new File(expectedPath);

        if (!inputDir.exists() || !inputDir.isDirectory()) {
            String message = String.format("The input directory path %s do not exist or is not a directory", inputPath);
            System.out.println(message);
            return;
        }

        if (!outputDir.exists() || !outputDir.isDirectory()) {
            String message = String.format("The output directory path %s do not exist or is not a directory", outputPath);
            System.out.println(message);
            return;
        }

        if (!expectedDir.exists() || !expectedDir.isDirectory()) {
            String message = String.format("The expected directory path %s do not exist or is not a directory", expectedPath);
            System.out.println(message);
            return;
        }

        saturator = new Saturator(null, inputPath, outputPath);
        
        run();
    }

    void run() throws Exception {
        saturator.setWatcher(new RegressionSaturatorWatcher(inputPath, outputPath, expectedPath));
        saturator.run();
    }
    
    public static void main(String[] args) throws Exception {
        new App(args);
    }
}
