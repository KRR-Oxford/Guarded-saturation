package uk.ac.ox.cs.gsat.satalg;

import uk.ac.ox.cs.gsat.api.SaturationAlgorithm;

public class SaturationAlgorithmFactory {

    protected static final SaturationAlgorithmFactory INSTANCE = new SaturationAlgorithmFactory();

    private SaturationAlgorithmFactory() {
    }

    public static SaturationAlgorithmFactory instance() {
        return INSTANCE;
    }

    public SaturationAlgorithm create(SaturationAlgorithmType type) {
        return create(type, new SaturationConfig());
    }

    public SaturationAlgorithm create(SaturationAlgorithmType type, SaturationConfig config) {
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
