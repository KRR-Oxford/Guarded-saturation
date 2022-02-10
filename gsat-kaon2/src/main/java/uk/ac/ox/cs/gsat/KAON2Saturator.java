package uk.ac.ox.cs.gsat;

import uk.ac.ox.cs.gsat.kaon2.KAON2SaturationProcess;

public class KAON2Saturator extends Saturator {

    KAON2Saturator(String[] args) throws Exception {
        super(args);

        saturationProcess = new KAON2SaturationProcess(saturationConfig);
        saturationProcess.setStatisticCollector(statisticsCollector);
    }

    KAON2Saturator(String configPath, String inputPath, String outputPath) throws Exception {
        super(configPath, inputPath, outputPath);

        saturationProcess = new KAON2SaturationProcess(saturationConfig);
        saturationProcess.setStatisticCollector(statisticsCollector);
    }

    public static void main(String[] args) {
        KAON2Saturator saturator;
        try {
            saturator = new KAON2Saturator(args);
            saturator.run();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
