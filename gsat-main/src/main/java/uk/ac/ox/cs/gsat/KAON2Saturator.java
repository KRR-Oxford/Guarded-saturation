package uk.ac.ox.cs.gsat;

import uk.ac.ox.cs.gsat.kaon2.KAON2SaturationProcess;

public class KAON2Saturator extends Saturator {

    KAON2Saturator(String[] args) throws Exception {
        super(args);
    }

    KAON2Saturator(String configPath, String inputPath, String outputPath) throws Exception {
        super(configPath, inputPath, outputPath);
    }

    @Override
    protected void setConfiguration(String currentDirectoryPath) throws Exception {
        String saturationConfigPath = getConfigurationPath(currentDirectoryPath);
        SaturationProcessConfiguration saturationConfig;
        if (saturationConfigPath != null)
            saturationConfig = new SaturationProcessConfiguration(saturationConfigPath);
        else
            saturationConfig = new SaturationProcessConfiguration();
        saturationProcess = new KAON2SaturationProcess(saturationConfig);
        saturationProcess.setStatisticCollector(statisticsCollector);
    }

    public static void main(String[] args) throws Exception {
        KAON2Saturator saturator;
        saturator = new KAON2Saturator(args);
        saturator.run();
    }
}
