package uk.ac.ox.cs.gsat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import uk.ac.ox.cs.gsat.EvolveBasedSat.newTGDStructure;

/**
 * From Angry-HEX code
 */
public class Configuration {

    private static final String file = "config.properties";
    private static Properties prop = null;
	private static boolean isSaturationOnly;
	private static boolean debugMode;
	private static boolean sortedVNF;
	private static boolean simpleSatPredicateFilter;
	private static Long timeout;
	private static boolean negativeConstraint;
	private static uk.ac.ox.cs.gsat.EvolveBasedSat.newTGDStructure newTGDStructure;
	private static boolean stopEvolvingIfSubsumed;
	private static boolean evolvingTGDOrdering;
	private static boolean discardUselessTGD;

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
            return "tree_predicate";

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

                if (Configuration.prop != null) {
                isSaturationOnly = Configuration.prop.containsKey("saturation_only") ? Boolean.parseBoolean(Configuration.prop.getProperty("saturation_only")) : true;

                debugMode = Configuration.prop.containsKey("debug") ? Boolean.parseBoolean(Configuration.prop.getProperty("debug")) : false;

                sortedVNF = Configuration.prop.containsKey("sorted_vnf") ? Boolean.parseBoolean(Configuration.prop.getProperty("sorted_vnf")) : true;

                simpleSatPredicateFilter = Configuration.prop.containsKey("simple_sat_predicate_filter")
                    ? Boolean.parseBoolean(Configuration.prop.getProperty("simple_sat_predicate_filter"))
                    : true;

                timeout = Configuration.prop.containsKey("timeout") ? Long.parseLong(Configuration.prop.getProperty("timeout")): null;

                negativeConstraint = Configuration.prop.containsKey("negative_constraint")
                        ? Boolean.parseBoolean(Configuration.prop.getProperty("negative_constraint"))
                        : true;

                newTGDStructure = Configuration.prop.containsKey("optimization.new_tgd_structure")
                    ? EvolveBasedSat.newTGDStructure.valueOf(prop.getProperty("optimization.new_tgd_structure"))
                    : EvolveBasedSat.newTGDStructure.SET;

                stopEvolvingIfSubsumed = Configuration.prop.containsKey("optimization.stop_evolving_if_subsumed") ?
                    Boolean.parseBoolean(Configuration.prop.getProperty("optimization.stop_evolving_if_subsumed"))
                    : true;

                evolvingTGDOrdering = Configuration.prop.containsKey("optimization.evolving_tgd_ordering") ?
                    Boolean.parseBoolean(Configuration.prop.getProperty("optimization.evolving_tgd_ordering"))
                    : true;

                discardUselessTGD = Configuration.prop.containsKey("optimization.discard_useless_tgd") ?
                    Boolean.parseBoolean(Configuration.prop.getProperty("optimization.discard_useless_tgd"))
                    : true;

                }
            }
    }

    public static boolean isSaturationOnly() {

        Configuration.initialize();
        return isSaturationOnly;
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

        return debugMode;
    }

    /**
     * Set the behaviour of computeVNF in {@link TGD}
     */
    public static boolean isSortedVNF() {
        Configuration.initialize();
        return sortedVNF;
    }

    /** 
     * Set if Simple sat filters the full TGDs it generates 
     * such that their body always contains at least a predicate 
     * appearing in a non full TGDs head
     */
    public static boolean isSimpleSatPredicateFilterEnabled() {
        Configuration.initialize();
        return simpleSatPredicateFilter;
    }

    /**
     * Get the timeout in seconds
     */
    public static Long getTimeout() {
        Configuration.initialize();
        return timeout;
    }

    /**
     * are negative constraints included
     */
    public static boolean includeNegativeConstraint() {
        Configuration.initialize();
        return negativeConstraint;
    }

    public static boolean isEvolvingTGDOrderingEnabled() {
        Configuration.initialize();
        return evolvingTGDOrdering;
    }

    public static EvolveBasedSat.newTGDStructure getNewTGDStrusture() {
        Configuration.initialize();
        return newTGDStructure;
    }

    /**
     * In evolved based algorithms, the new TGDs can be subsumed by the TGDs outputed by an 
     * evolve application on this new TGD and others TGDs. This parameter allows stop to apply evolve
     * using this new TGDs, when it is subsumed by one of the outputed TGDs.
     */
	public static boolean isStopEvolvingIfSubsumedEnabled() {
        Configuration.initialize();
        return stopEvolvingIfSubsumed;
	}

    public static boolean isDiscardUselessTGDEnabled() {
        Configuration.initialize();
        return discardUselessTGD;
	}

}
