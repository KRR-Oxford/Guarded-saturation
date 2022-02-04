package uk.ac.ox.cs.gsat.satalg;

import uk.ac.ox.cs.gsat.api.SaturationAlgorithm;

public class SaturationAlgorithmFactory {

    protected static final SaturationAlgorithmFactory INSTANCE = new SaturationAlgorithmFactory();

    private SaturationAlgorithmFactory() {
    }

    public static SaturationAlgorithmFactory instance() {
        return INSTANCE;
    }

    /**
     * Create a saturation algorithm with the given type and the default configuration
     */
    public SaturationAlgorithm create(SaturationAlgorithmType type) {

        SaturationConfig config = new SaturationConfig();
        config.setSaturationAlgorithmType(type);
        
        return create(config);
    }

    /**
     * Create a saturation algorithm following the given configuration
     */
    public SaturationAlgorithm create(SaturationConfig config) {

        SaturationAlgorithmType type = config.getSaturatonAlgType();
        
        switch(type) {
        case GSAT:
            return new GSat(config);
        case HYPER_SAT:
            return new HyperResolutionBasedSat(config);
        case ORDERED_SKOLEM_SAT:
            return new OrderedSkolemSat(config);
        case SIMPLE_SAT:
            return new SimpleSat(config);
        case SKOLEM_SAT:
            return new SkolemSat(config);
        default:
            String message = String.format("Unsupported saturation algorithm type %s", type);
            throw new IllegalStateException(message);
        }
    }
}
