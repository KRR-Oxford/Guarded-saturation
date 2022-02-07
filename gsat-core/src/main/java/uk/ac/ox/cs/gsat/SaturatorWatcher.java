package uk.ac.ox.cs.gsat;

import java.util.Collection;

import uk.ac.ox.cs.gsat.fol.TGD;

/**
 * A saturator watcher reports the single action and results executed by the saturator.
 */
interface SaturatorWatcher {

    public void changeDirectory(String inputDirectoryPath, String outputDirectoryPath) throws Exception;
        
    public void singleSaturationDone(String rowName, String inputPath, String outputPath, Collection<? extends TGD> saturationFullTGD) throws Exception;
}
