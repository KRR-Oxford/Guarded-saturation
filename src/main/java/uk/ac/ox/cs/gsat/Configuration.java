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

    public static String getSubsumptionMethod() {

        Configuration.initialize();

        if (Configuration.prop == null)
            return "simple";

        return Configuration.prop.getProperty("subsumption_method");

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

    public static boolean isSaturationOnly() {

        Configuration.initialize();

        if (Configuration.prop == null)
            return true;

        return Boolean.parseBoolean(Configuration.prop.getProperty("saturation_only"));

    }

    public static String getSaturationAlg() {

        Configuration.initialize();

        if (Configuration.prop == null)
            return "gsat";

        String value = Configuration.prop.getProperty("saturation_alg");
        return (value != null) ? value : "gsat";
    }

    public static void setSaturationAlg(String value) {

        Configuration.initialize();

        if (Configuration.prop != null)
            Configuration.prop.setProperty("saturation_alg", value);
    }

    public static boolean isDebugMode() {

        Configuration.initialize();

        if (Configuration.prop == null)
            return false;

        return Boolean.parseBoolean(Configuration.prop.getProperty("debug"));

    }

    public static int getOptimizationValue() {

        Configuration.initialize();

        if (Configuration.prop == null)
            return 0;

        return Integer.parseInt(Configuration.prop.getProperty("optimization"));

        // 0: no optimizations
        // 1: subsumption check
        // 2: ordered sets to store the new TGDs to evaluate
        // (stored in `newFullTGDs` and `newNonFullTGDs`)
        // 3: stop to evolve a TGD if we found a new one that subsumes it
        // 4: ordered sets to store the "possible evolving" TGDs
        // (stored in `fullTGDsMap` and `nonFullTGDsMap`)
        // 5: stacks to store the new TGDs to evaluate
        //
        // the order of 2 and 4 is conceived to evaluate earlier rules with bigger heads
        // and smaller bodies (i.e. rules that can easily subsume other rule),
        // see `comparator` in `GSat`

    }

    /**
     * Set the behaviour of computeVNF in {@link TGD}
     */
    public static boolean isSortedVNF() {
        Configuration.initialize();

        if (Configuration.prop == null || !Configuration.prop.containsKey("sorted_vnf"))
            return true;

        return Boolean.parseBoolean(Configuration.prop.getProperty("sorted_vnf"));
    }

    /** 
     * Set if Simple sat filters the full TGDs it generates 
     * such that their body always contains at least a predicate 
     * appearing in a non full TGDs head
     */
    public static boolean isSimpleSatPredicateFilterEnabled() {
        Configuration.initialize();

        if (Configuration.prop == null || !Configuration.prop.containsKey("simple_sat_predicate_filter"))
            return true;

        return Boolean.parseBoolean(Configuration.prop.getProperty("simple_sat_predicate_filter"));
    }

    /**
     * Get the timeout in seconds
     */
    public static Long getTimeout() {
        Configuration.initialize();

        if (Configuration.prop == null)
            return null;

        String value = Configuration.prop.getProperty("timeout");
        return (value != null) ? Long.parseLong(value) : null;
    }

}
