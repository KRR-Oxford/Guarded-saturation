package uk.ac.ox.cs.gsat.mat;

import uk.ac.ox.cs.gsat.api.Materializer;

public class MaterializerFactory {

    public static Materializer create(MaterializerType type) {

        switch (type) {
        case RDFOX:
            return new RDFoxMaterializer();
        case SOLVER:
            return new SolverMaterializer();
        default:
            String message = String.format("Unknown materializer type %s", type);
            throw new IllegalStateException(message);
        }
    }
}
