package uk.ac.ox.cs.gsat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import uk.ac.ox.cs.gsat.mat.MaterializerType;

public class MaterializationConfiguration {

    protected final Properties prop = new Properties();
    private String solverName = "idlv";
    private String solverPath = "executables" + File.separator + "idlv";
    private String solverOptionsGrounding = "--t --check-edb-duplication";
    private String solverOptionsQuery = "--query";
    private boolean fullGrounding = true;
    private MaterializerType materializerType = MaterializerType.SOLVER;

    public MaterializationConfiguration() {
    }
    
    public MaterializationConfiguration(String configurationPath) throws IOException {
        FileInputStream inStream = new FileInputStream(configurationPath);
        prop.load(inStream);

        if (prop.containsKey("solver.name"))
            this.solverName = prop.getProperty("solver.name");

        if (prop.containsKey("solver.path"))
            this.solverPath = prop.getProperty("solver.path");

        if (prop.containsKey("solver.options.grounding"))
            this.solverOptionsGrounding = prop.getProperty("solver.options.grounding");

        if (prop.containsKey("solver.options.query"))
            this.solverOptionsQuery = prop.getProperty("solver.options.query");

        if (prop.containsKey("solver.full_grounding"))
            this.fullGrounding = Boolean.parseBoolean(prop.getProperty("solver.full_grounding"));

        if (prop.containsKey("materializer.type"))
            this.materializerType = MaterializerType.valueOf(prop.getProperty("materializer.type"));
    }

    public String getSolverName() {
        return solverName;
    }

    public void setSolverName(String solverName) {
        this.solverName = solverName;
    }

    public String getSolverPath() {
        return solverPath;
    }

    public void setSolverPath(String solverPath) {
        this.solverPath = solverPath;
    }

    public String getSolverOptionsGrounding() {
        return solverOptionsGrounding;
    }

    public void setSolverOptionsGrounding(String solverOptionsGrounding) {
        this.solverOptionsGrounding = solverOptionsGrounding;
    }

    public String getSolverOptionsQuery() {
        return solverOptionsQuery;
    }

    public void setSolverOptionsQuery(String solverOptionsQuery) {
        this.solverOptionsQuery = solverOptionsQuery;
    }

    public boolean isFullGrounding() {
        return fullGrounding;
    }

    public void setFullGrounding(boolean fullGrounding) {
        this.fullGrounding = fullGrounding;
    }

    public MaterializerType getMaterializerType() {
        return materializerType;
    }

    public void setMaterializerType(MaterializerType materializerType) {
        this.materializerType = materializerType;
    }

}
