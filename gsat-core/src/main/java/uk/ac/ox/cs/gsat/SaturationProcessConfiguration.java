package uk.ac.ox.cs.gsat;

import java.io.IOException;

import uk.ac.ox.cs.gsat.satalg.SaturationAlgorithmConfiguration;

/**
 * This configuration specifies in addition to the saturation algorithm configuration
 * the parameters of the TGDs loading and pre-processing.
 */
public class SaturationProcessConfiguration extends SaturationAlgorithmConfiguration {

    /**
     * true, if the negative constraint are included in TGDs loading
     */
    protected boolean negativeConstraint = false;
    /**
     * true, if the facts from the file storing the TGDs are not loaded with the TGDs
     * It has no impact on the saturation, it only concerns the loading performance
     */
    protected boolean skipingFacts = true;
    
    protected SaturationProcessConfiguration() {
    }

    protected SaturationProcessConfiguration(String configurationPath) throws IOException {
        super(configurationPath);

        if (prop.containsKey("negative_constraint")) {
            negativeConstraint = Boolean.parseBoolean(prop.getProperty("negative_constraint"));
        }

        if (prop.containsKey("saturation_only")) {
            skipingFacts = Boolean.parseBoolean(prop.getProperty("saturation_only"));
        }
    }

    public boolean isNegativeConstraint() {
        return negativeConstraint;
    }

    public void setNegativeConstraint(boolean negativeConstraint) {
        this.negativeConstraint = negativeConstraint;
    }

    public boolean isSkipingFacts() {
        return skipingFacts;
    }

    public void setSkipingFacts(boolean skipingFacts) {
        this.skipingFacts = skipingFacts;
    }


} 
