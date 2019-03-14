import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * From Angry-HEX code
 */
public class Configuration {

    private static final String file = "config.properties";
    static final Logger logger = LogManager.getLogger("Configuration");
    private static Properties prop = null;

    public static String getSolverName() {

        Configuration.initialize();

        if (Configuration.prop == null)
            return "";

        return Configuration.prop.getProperty("solver.name");

    }

    public static String getSolverPath() {

        Configuration.initialize();

        if (Configuration.prop == null)
            return "";

        return Configuration.prop.getProperty("solver.path");

    }

    public static String getSolverOptionsGrounding() {

        Configuration.initialize();

        if (Configuration.prop == null)
            return "";

        return Configuration.prop.getProperty("solver.options.grounding");

    }

    public static String getSolverOptionsQuery() {

        Configuration.initialize();

        if (Configuration.prop == null)
            return "";

        return Configuration.prop.getProperty("solver.options.query");

    }

    private static void initialize() {

        if (Configuration.prop == null)
            try {
                Configuration.prop = new Properties();
                Configuration.logger.info("loading configuration from '" + Configuration.file + "'");
                Configuration.prop.load(new FileInputStream(Configuration.file));
            } catch (final IOException e) {
                Configuration.logger.warn("Could not open configuration file.");
                Configuration.logger.warn(e.toString());
                Configuration.logger.warn("Falling back to defaults.");
                Configuration.prop = null;
            }

    }

}