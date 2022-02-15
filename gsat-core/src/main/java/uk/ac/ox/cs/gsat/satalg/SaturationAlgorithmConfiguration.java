package uk.ac.ox.cs.gsat.satalg;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.satalg.AbstractSkolemSat.SkolemizationType;
import uk.ac.ox.cs.gsat.unification.UnificationIndexType;

public class SaturationAlgorithmConfiguration {

    protected final Properties prop = new Properties();
    
    protected boolean verbose = false;
    protected boolean simpleSatPredicateFilter = true;
    protected Long timeout = null;
    protected String subsumptionMethod = "tree_predicate";
    protected boolean evolvingTGDOrdering = true;
    protected NewTGDStructure newTGDStructure = NewTGDStructure.SET;
    protected boolean stopEvolvingIfSubsumed = true;
    protected boolean discardUselessTGD = true;
    protected SkolemizationType skolemizationType = SkolemizationType.NAIVE;
    protected boolean discardTautology = true;
    protected boolean orderedSkolemSatSelectSkolemBodyAtom = false;
    protected UnificationIndexType unificationIndexType = null;
    protected int maxPredicate = 0;
    protected boolean sortedVNF = true;
    protected SaturationAlgorithmType saturationAlgorithmType = SaturationAlgorithmType.GSAT;

    public SaturationAlgorithmConfiguration() {
    }
    
    public SaturationAlgorithmConfiguration(String configPath) throws IOException {
        FileInputStream inStream = new FileInputStream(configPath);
        prop.load(inStream);

        if (prop.containsKey("sorted_vnf"))
            sortedVNF = Boolean.parseBoolean(prop.getProperty("sorted_vnf"));

        if (prop.containsKey("simple_sat_predicate_filter"))
            simpleSatPredicateFilter = Boolean.parseBoolean(prop.getProperty("simple_sat_predicate_filter"));

        if (prop.containsKey("timeout") )
            timeout = Long.parseLong(prop.getProperty("timeout"));

        if (prop.containsKey("optimization.new_tgd_structure"))
            newTGDStructure = NewTGDStructure
                .valueOf(prop.getProperty("optimization.new_tgd_structure"));

        if (prop.containsKey("optimization.stop_evolving_if_subsumed"))
            stopEvolvingIfSubsumed = Boolean.parseBoolean(prop.getProperty("optimization.stop_evolving_if_subsumed"));

        if (prop.containsKey("optimization.evolving_tgd_ordering"))
            evolvingTGDOrdering = Boolean
                    .parseBoolean(prop.getProperty("optimization.evolving_tgd_ordif ering"));

        if (prop.containsKey("optimization.discard_useless_tgd"))
            discardUselessTGD = Boolean.parseBoolean(prop.getProperty("optimization.discard_useless_tgd"));

        if (prop.containsKey("optimization.maxPredicate"))
            maxPredicate = Integer.parseInt(prop.getProperty("optimization.maxPredicate"));

        if (prop.containsKey("skolemization_type"))
            skolemizationType = AbstractSkolemSat.SkolemizationType.valueOf(prop.getProperty("skolemization_type"));
        if (prop.containsKey("optimization.discard_tautology"))
            discardTautology = Boolean.parseBoolean(prop.getProperty("optimization.discard_tautology"));

        if (prop.containsKey("ordered_skolemsat_select_skolem_body_atom"))
            orderedSkolemSatSelectSkolemBodyAtom = Boolean.parseBoolean(prop.getProperty("ordered_skolemsat_select_skolem_body_atom"));

        if (prop.containsKey("optimization.unification_index_type"))
            unificationIndexType = UnificationIndexType.valueOf(prop.getProperty("optimization.unification_index_type"));
        if (prop.containsKey("saturation_alg")) {
            String value = prop.getProperty("saturation_alg").toUpperCase();
            Set<String> allowedValues = Arrays.stream(SaturationAlgorithmType.values()).map(t -> t.toString()).collect(Collectors.toSet());
            if (allowedValues.contains(value)) {
                SaturationAlgorithmType type = SaturationAlgorithmType.valueOf(value);
                this.saturationAlgorithmType = type;

            } else {
                String message = String.format("The value %s is not supported for 'saturation_alg'. It should be one in %s", value, allowedValues);
                throw new IllegalArgumentException(message);
            }
        }

    }

    /**
     * Set the behaviour of computeVNF in {@link TGD}
     */
    public boolean isSortedVNF() {
        return sortedVNF;
    }

    public void setSortedVNF(boolean sortedVNF) {
        this.sortedVNF = sortedVNF;
    }

    /**
     * Set if Simple sat filters the full TGDs it generates such that their body
     * always contains at least a predicate appearing in a non full TGDs head
     */
    public boolean isSimpleSatPredicateFilter() {
        return simpleSatPredicateFilter;
    }

    public void setSimpleSatPredicateFilter(boolean simpleSatPredicateFilter) {
        this.simpleSatPredicateFilter = simpleSatPredicateFilter;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    /**
     * Get the timeout in seconds
     */
    public Long getTimeout() {
        return timeout;
    }

    public String getSubsumptionMethod() {
        return subsumptionMethod;
    }

    public int getMaxPredicate() {
        return maxPredicate;
    }

    public boolean isEvolvingTGDOrderingEnabled() {
        return evolvingTGDOrdering;
    }

    public NewTGDStructure getNewTGDStrusture() {
        return newTGDStructure;
    }

    /**
     * In evolved based algorithms, the new TGDs can be subsumed by the TGDs
     * outputed by an evolve application on this new TGD and others TGDs. This
     * parameter allows stop to apply evolve using this new TGDs, when it is
     * subsumed by one of the outputed TGDs.
     */
    public boolean isStopEvolvingIfSubsumedEnabled() {
        return stopEvolvingIfSubsumed;
    }

    public boolean isDiscardUselessTGDEnabled() {
        return discardUselessTGD;
    }

    public SkolemizationType getSkolemizationType() {
        return skolemizationType;
    }

    public boolean isTautologyDiscarded() {
        return discardTautology;
    }

    public boolean isOrderedSkolemSatSelectSkolemBodyAtom() {
        return orderedSkolemSatSelectSkolemBodyAtom;
    }

    public UnificationIndexType getUnificationIndexType() {
        return unificationIndexType;
    }

    public SaturationAlgorithmType getSaturatonAlgType() {
        return saturationAlgorithmType;
    }

    public SaturationAlgorithmType getSaturationAlgorithmType() {
        return saturationAlgorithmType;
    }

    public void setSaturationAlgorithmType(SaturationAlgorithmType saturationAlgorithmType) {
        this.saturationAlgorithmType = saturationAlgorithmType;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
}
