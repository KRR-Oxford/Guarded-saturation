package uk.ac.ox.cs.gsat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * From Angry-HEX code
 */
public class Configuration {

    private static final String file = "config.properties";
    private static Properties prop = null;

    public static String getSolverName() {

        Configuration.initialize();

        if (Configuration.prop == null)
            return "idlv";

        return Configuration.prop.getProperty("solver.name");

    }

    public static String getSolverPath() {

        Configuration.initialize();

        if (Configuration.prop == null)
            return "executables" + File.separator + "idlv";

        return Configuration.prop.getProperty("solver.path");

    }

    public static String getSolverOptionsGrounding() {

        Configuration.initialize();

        if (Configuration.prop == null)
            return "--t --no-facts --check-edb-duplication";

        return Configuration.prop.getProperty("solver.options.grounding");

    }

    public static String getSolverOptionsQuery() {

        Configuration.initialize();

        if (Configuration.prop == null)
            return "--query";

        return Configuration.prop.getProperty("solver.options.query");

    }

    public static boolean isSolverOutputToFile() {

        Configuration.initialize();

        if (Configuration.prop == null)
            return true;

        return Boolean.parseBoolean(Configuration.prop.getProperty("solver.output.to_file"));

    }

    public static boolean isFullGrounding() {

        Configuration.initialize();

        if (Configuration.prop == null)
            return true;

        return Boolean.parseBoolean(Configuration.prop.getProperty("solver.full_grounding"));

    }

    private static synchronized void initialize() {

        FileInputStream inStream = null;
        if (Configuration.prop == null)
            try {
                Configuration.prop = new Properties();
                App.logger.fine("loading configuration from '" + Configuration.file + "'");
                inStream = new FileInputStream(Configuration.file);
                Configuration.prop.load(inStream);
            } catch (final IOException e) {
                App.logger.warning("Could not open configuration file.");
                App.logger.warning(e.toString());
                App.logger.warning("Falling back to defaults.");
                Configuration.prop = null;
            } finally {
                if (inStream != null)
                    try {
                        inStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }

    }

    public static boolean isGSatOnly() {

        Configuration.initialize();

        if (Configuration.prop == null)
            return true;

        return Boolean.parseBoolean(Configuration.prop.getProperty("gsat_only"));

    }

}