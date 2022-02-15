package uk.ac.ox.cs.gsat.mat;

import uk.ac.ox.cs.gsat.MaterializationConfiguration;
import uk.ac.ox.cs.gsat.api.Materializer;

public class MaterializerFactory {

    public static Materializer create(MaterializationConfiguration config) {

        switch (config.getMaterializerType()) {
        case RDFOX:
            return new RDFoxMaterializer(config);
        case SOLVER:
            return new SolverMaterializer(config);
        default:
            String message = String.format("Unknown materializer type %s", config.getMaterializerType());
            throw new IllegalStateException(message);
        }
    }
}
