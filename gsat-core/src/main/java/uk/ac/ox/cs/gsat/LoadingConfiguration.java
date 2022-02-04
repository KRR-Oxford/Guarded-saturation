package uk.ac.ox.cs.gsat;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class LoadingConfiguration {

    private boolean applyStructuralTransformation = false;

    public LoadingConfiguration(String configPath) throws IOException {
        Properties prop = new Properties();
        FileInputStream inStream = new FileInputStream(configPath);
        prop.load(inStream);

        if (prop.containsKey("optimization.apply_structural_transformation"))
            applyStructuralTransformation = Boolean
                    .parseBoolean(prop.getProperty("optimization.apply_structural_transformation"));

    }
}
