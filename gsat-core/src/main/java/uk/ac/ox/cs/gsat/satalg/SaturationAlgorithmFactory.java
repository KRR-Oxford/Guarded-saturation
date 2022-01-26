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
        switch(type) {
        case GSAT:
            return GSat.getInstance();
        case HYPER_SAT:
            return HyperResolutionBasedSat.getInstance();
        case ORDERED_SKOLEM_SAT:
            return OrderedSkolemSat.getInstance();
        case SIMPLE_SAT:
            return SimpleSat.getInstance();
        case SKOLEM_SAT:
            return SkolemSat.getInstance();
        default:
            String message = String.format("Unsupported saturation algorithm type %s", type);
            throw new IllegalStateException(message);
        }
    }
}
