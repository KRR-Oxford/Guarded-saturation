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
    private static boolean isSaturationOnly;
    private static boolean debugMode;
    private static boolean negativeConstraint;
    private static newTGDStructure newTGDStructure;
    private static boolean writeOutput;
    private static boolean applyStructuralTransformation;

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

                if (Configuration.prop != null) {
                    isSaturationOnly = Configuration.prop.containsKey("saturation_only")
                            ? Boolean.parseBoolean(Configuration.prop.getProperty("saturation_only"))
                            : true;
                    writeOutput = Configuration.prop.containsKey("write_output")
                            ? Boolean.parseBoolean(Configuration.prop.getProperty("write_output"))
                            : false;

                    debugMode = Configuration.prop.containsKey("debug")
                            ? Boolean.parseBoolean(Configuration.prop.getProperty("debug"))
                            : false;

                    negativeConstraint = Configuration.prop.containsKey("negative_constraint")
                            ? Boolean.parseBoolean(Configuration.prop.getProperty("negative_constraint"))
                            : true;


                    applyStructuralTransformation = Configuration.prop
                            .containsKey("optimization.apply_structural_transformation")
                                    ? Boolean.parseBoolean(Configuration.prop
                                            .getProperty("optimization.apply_structural_transformation"))
                                    : false;
                }
            }
    }

    public static boolean isSaturationOnly() {

        Configuration.initialize();
        return isSaturationOnly;
    }

    public static boolean isDebugMode() {

        Configuration.initialize();

        return debugMode;
    }

    /**
     * are negative constraints included
     */
    public static boolean includeNegativeConstraint() {
        Configuration.initialize();
        return negativeConstraint;
    }

    public static boolean writeOutputDatalog() {
        Configuration.initialize();
        return writeOutput;
    }

    public static boolean applyKAON2StructuralTransformation() {
        Configuration.initialize();
        return applyStructuralTransformation;
    }

    public static enum newTGDStructure {
        STACK,
        /**
         * In evolved based algorithms, if true, the new TGDs (right and left) are
         * stored in ordered sets such that the TGDs with smallest body and largest head
         * come first
         */
        ORDERED_BY_ATOMS_NB, SET
    }

}
