package uk.ac.ox.cs.gsat.regression;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;

import uk.ac.ox.cs.gsat.Log;
import uk.ac.ox.cs.gsat.SaturatorWatcher;
import uk.ac.ox.cs.gsat.TGDFileFormat;
import uk.ac.ox.cs.gsat.api.io.Parser;
import uk.ac.ox.cs.gsat.api.io.ParserResult;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.io.ParserFactory;

public class RegressionSaturatorWatcher implements SaturatorWatcher {

    private Path initialInputPath;
    private Path initialExpectedPath;
    private Parser parser;
    private Path initialOutputPath;

    public RegressionSaturatorWatcher(String initialInputPath, String initialOutputPath, String initialExpectedPath) {
        this.initialInputPath = Paths.get(initialInputPath);
        this.initialOutputPath = Paths.get(initialOutputPath);
        this.initialExpectedPath = Paths.get(initialExpectedPath);
        this.parser =  ParserFactory.instance().create(TGDFileFormat.DLGP, true, false);

    }

    @Override
    public void changeDirectory(String inputDirectoryPath, String outputDirectoryPath) throws Exception {
    }

    @Override
    public void singleSaturationDone(String rowName, String inputPath, String outputPath,
            Collection<? extends TGD> saturationFullTGD) throws Exception {

        Path outputRelativePath = initialOutputPath.relativize(Paths.get(outputPath));
        Path expectedFilePath = initialExpectedPath.resolve(outputRelativePath);

        if (!expectedFilePath.toFile().exists() || TGDFileFormat.getFormatFromPath(outputPath) != TGDFileFormat.DLGP) {
            String message = String.format("The expected DLGP file %s is missing.", expectedFilePath.toString());
            Log.GLOBAL.warning(message);
            return;
        }

        Log.GLOBAL.info("Regression comparison of " + outputPath + " with " + expectedFilePath);
        
        ParserResult expectedParserResult = parser.parse(expectedFilePath.toString());

        if (!expectedParserResult.getTGDs().containsAll(saturationFullTGD)) {
            Collection<TGD> unexpected = new HashSet<>(saturationFullTGD);
            unexpected.removeAll(expectedParserResult.getTGDs());
            Log.GLOBAL.warning("The following TGDs are in the saturation, but were not expected: ");
            for (TGD t : unexpected)
                Log.GLOBAL.warning(t.toString());

            Log.GLOBAL.warning("------------------------------------------------------------------");
        }

        if (!saturationFullTGD.containsAll(expectedParserResult.getTGDs())) {
            Collection<TGD> missing = new HashSet<>(expectedParserResult.getTGDs());
            missing.removeAll(saturationFullTGD);
            Log.GLOBAL.warning("The following TGDs are missing in the saturation: ");
            for (TGD t : missing)
                Log.GLOBAL.warning(t.toString());

            Log.GLOBAL.warning("------------------------------------------------------------------");
        }
    }
}
