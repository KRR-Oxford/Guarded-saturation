package uk.ac.ox.cs.gsat.mat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import tech.oxfordsemantic.jrdfox.Prefixes;
import tech.oxfordsemantic.jrdfox.client.ConnectionFactory;
import tech.oxfordsemantic.jrdfox.client.Cursor;
import tech.oxfordsemantic.jrdfox.client.DataStoreConnection;
import tech.oxfordsemantic.jrdfox.client.ServerConnection;
import tech.oxfordsemantic.jrdfox.client.TransactionType;
import tech.oxfordsemantic.jrdfox.client.UpdateType;
import tech.oxfordsemantic.jrdfox.exceptions.JRDFoxException;
import tech.oxfordsemantic.jrdfox.logic.datalog.Rule;
import uk.ac.ox.cs.gsat.api.MaterializationStatColumns;
import uk.ac.ox.cs.gsat.api.Materializer;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.statistics.StatisticsCollector;

class RDFoxMaterializer implements Materializer {

    protected final static String SERVER_URL = "rdfox:local";
    protected final String exportFormat = "application/n-triples"; //"text/turtle";
    protected final String roleName = "admin";
    protected final String password = "admin";
    protected final String dataStoreName = "store";
    protected final String dirPath = "RDFox-data";

    protected final Map<String, String> serverParameters = new HashMap<>();
    protected ServerConnection sConn;
    protected DataStoreConnection dsConn;
    protected final Prefixes prefixes = new Prefixes();
    private StatisticsCollector<MaterializationStatColumns> statsCollector;
    private String statsRowName;

    public RDFoxMaterializer() {

    }

    @Override
    public long materialize(String inputDataFile, Collection<? extends TGD> fullTGDs, OutputStream outputStream)
            throws FileNotFoundException, JRDFoxException {

        load(inputDataFile, fullTGDs);

        HashMap<String, String> exportParameters = new HashMap<String, String>();
        exportParameters.put("fact-domain", "IDB");
        dsConn.exportData(prefixes, outputStream, exportFormat, exportParameters);
        statsCollector.tick(statsRowName, MaterializationStatColumns.MAT_WRITING_TIME);

        long materializationSize = getTripleCount(dsConn, "IDB");
        statsCollector.stop(statsRowName, MaterializationStatColumns.MAT_TOTAL);
        statsCollector.put(statsRowName, MaterializationStatColumns.MAT_SIZE, materializationSize);

        return materializationSize;
    }

    @Override
    public long materialize(String inputDataFile, Collection<? extends TGD> fullTGDs, String outputFile)
            throws JRDFoxException, FileNotFoundException {
        return materialize(inputDataFile, fullTGDs, new BufferedOutputStream(new FileOutputStream(outputFile)));
    }

    protected void load(String inputDataFile, Collection<? extends TGD> fullTGDs) throws JRDFoxException, FileNotFoundException {
        // clear every data and rule
        reset();

        // import the data file
        InputStream dataStream = new BufferedInputStream(new FileInputStream(inputDataFile));
        dsConn.importData(UpdateType.ADDITION, prefixes, dataStream);
        statsCollector.tick(statsRowName, MaterializationStatColumns.MAT_DATA_LOAD_TIME);
        
        // import the rules generated from the fullTGDs
        Collection<Rule> rules = new ArrayList<>();
        for (TGD fullTGD : fullTGDs) {
            for (Rule generatedRule : RDFoxFactory.createDatalogRule(fullTGD)) {
                rules.add(generatedRule);
            }
        }
        dsConn.addRules(rules);
        statsCollector.tick(statsRowName, MaterializationStatColumns.MAT_TIME);
    }

    protected void reset() throws JRDFoxException {
        dsConn.clear();
        prefixes.clear();
    }

    protected static long getTripleCount(DataStoreConnection dsConn, String queryDomain) throws JRDFoxException {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("fact-domain", queryDomain);

        try (Cursor cursor = dsConn.createCursor(null, Prefixes.s_emptyPrefixes, "SELECT ?s ?p ?o WHERE {?s ?p ?o}",
                parameters)) {
            dsConn.begin(TransactionType.READ_ONLY);
            try {
                long result = 0;
                for (long multiplicity = cursor.open(); multiplicity != 0; multiplicity = cursor.advance()) {
                    result += multiplicity;
                }

                return result;
            } finally {
                dsConn.rollbackTransaction();
            }
        }

    }

    @Override
    public void setStatsCollector(String rowName, StatisticsCollector<MaterializationStatColumns> statsCollector) {
        this.statsCollector = statsCollector;
        this.statsRowName = rowName;
    }

    @Override
    public void init() throws JRDFoxException {
        String dataDir = new File(dirPath).getAbsolutePath();
        serverParameters.put("persist-ds", "off");
        serverParameters.put("persist-roles", "off");
        serverParameters.put("server-directory", dataDir);

        // raise an exception if the server is already started
        try {
            String[] warnings = ConnectionFactory.startLocalServer(serverParameters);
        } catch (JRDFoxException e) {
        }

        if (ConnectionFactory.getNumberOfLocalServerRoles() == 0) {
            ConnectionFactory.createFirstLocalServerRole(roleName, password);
        }

        sConn = ConnectionFactory.newServerConnection(SERVER_URL, roleName, password);

        if (!sConn.containsDataStore(dataStoreName)) {
            sConn.createDataStore(dataStoreName, new HashMap<String, String>());
        }

        dsConn = sConn.newDataStoreConnection(dataStoreName);
                
        statsCollector.tick(statsRowName, MaterializationStatColumns.MAT_INIT_TIME);
    }

}
