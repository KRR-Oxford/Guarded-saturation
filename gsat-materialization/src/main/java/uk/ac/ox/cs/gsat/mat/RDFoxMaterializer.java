package uk.ac.ox.cs.gsat.mat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import tech.oxfordsemantic.jrdfox.Prefixes;
import tech.oxfordsemantic.jrdfox.client.ConnectionFactory;
import tech.oxfordsemantic.jrdfox.client.Cursor;
import tech.oxfordsemantic.jrdfox.client.DataStoreConnection;
import tech.oxfordsemantic.jrdfox.client.ServerConnection;
import tech.oxfordsemantic.jrdfox.client.TransactionType;
import tech.oxfordsemantic.jrdfox.client.UpdateType;
import tech.oxfordsemantic.jrdfox.exceptions.JRDFoxException;
import tech.oxfordsemantic.jrdfox.logic.datalog.Rule;
import tech.oxfordsemantic.jrdfox.logic.sparql.pattern.TriplePattern;
import uk.ac.ox.cs.gsat.Log;
import uk.ac.ox.cs.gsat.MaterializationConfiguration;
import uk.ac.ox.cs.gsat.api.MaterializationStatColumns;
import uk.ac.ox.cs.gsat.api.Materializer;
import uk.ac.ox.cs.gsat.api.io.ParserResult;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.statistics.StatisticsCollector;
import uk.ac.ox.cs.pdq.fol.Atom;

class RDFoxMaterializer implements Materializer {

    protected final static String DEFAULT_DATA_PATH = ".rdfox-data.nt";
    protected final static String SERVER_URL = "rdfox:local";
    protected final String exportFormat = "application/n-triples"; // "text/turtle";
    protected final String roleName = "admin";
    protected final String password = "admin";
    protected final String dataStoreName = "store";
    protected final String dirPath = "RDFox-data";

    protected final Map<String, String> serverParameters = new HashMap<>();
    protected final MaterializationConfiguration config;
    protected ServerConnection sConn;
    protected DataStoreConnection dsConn;
    protected final Prefixes prefixes = new Prefixes();
    protected StatisticsCollector<MaterializationStatColumns> statsCollector;
    protected String statsRowName;


    public RDFoxMaterializer(MaterializationConfiguration config) {
        this.config = config;
    }

    @Override
    public long materialize(ParserResult parsedData, Collection<? extends TGD> fullTGDs, OutputStream outputStream)
            throws IOException, Exception {

        load(parsedData, fullTGDs);

        HashMap<String, String> exportParameters = new HashMap<String, String>();
        exportParameters.put("fact-domain", "IDB");
        dsConn.exportData(prefixes, outputStream, exportFormat, exportParameters);
        if (statsCollector != null)
            statsCollector.tick(statsRowName, MaterializationStatColumns.MAT_WRITING_TIME);

        long materializationSize = getTripleCount(dsConn, "IDB");
        if (statsCollector != null)
            statsCollector.put(statsRowName, MaterializationStatColumns.MAT_SIZE, materializationSize);

        return materializationSize;
    }

    @Override
    public long materialize(ParserResult parsedData, Collection<? extends TGD> fullTGDs, String outputFile)
            throws IOException, Exception {
        OutputStream stream = new BufferedOutputStream(new FileOutputStream(outputFile));
        long size = materialize(parsedData, fullTGDs, stream);
        stream.close();
        return size;
    }

    /**
     * load the data and perform the materialization
     */    
    protected void load(ParserResult parsedData, Collection<? extends TGD> fullTGDs)
            throws IOException, Exception {
        // clear every data and rule
        reset();
    
        // write the data to the input data file
        String inputDataFile = DEFAULT_DATA_PATH;
        writeData(parsedData.getAtoms(), inputDataFile);

        // import the data file
        InputStream dataStream = new BufferedInputStream(new FileInputStream(inputDataFile));
        dsConn.importData(UpdateType.ADDITION, prefixes, dataStream);
        if (statsCollector != null)
            statsCollector.tick(statsRowName, MaterializationStatColumns.MAT_DATA_LOAD_TIME);
    
        // import the rules generated from the fullTGDs
        Collection<Rule> rules = new ArrayList<>();
        for (TGD fullTGD : fullTGDs) {
            for (Rule generatedRule : RDFoxFactory.createDatalogRule(fullTGD)) {
                rules.add(generatedRule);
            }
        }
        dsConn.addRules(rules);
        if (statsCollector != null)
            statsCollector.tick(statsRowName, MaterializationStatColumns.MAT_TIME);
    }
    
    protected void reset() throws JRDFoxException {
        dsConn.clear();
        prefixes.clear();
    }
    
    /**
     * Compute the current number of triples
     */    
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
            if (!e.getMessage().contains("A local server instance has already been started.")) {
                Log.GLOBAL.severe(e.getMessage());
                e.printStackTrace();
            }
        }
    
        if (ConnectionFactory.getNumberOfLocalServerRoles() == 0) {
            ConnectionFactory.createFirstLocalServerRole(roleName, password);
        }
    
        sConn = ConnectionFactory.newServerConnection(SERVER_URL, roleName, password);
    
        if (!sConn.containsDataStore(dataStoreName)) {
            sConn.createDataStore(dataStoreName, new HashMap<String, String>());
        }
    
        dsConn = sConn.newDataStoreConnection(dataStoreName);

        if (statsCollector != null)
            statsCollector.tick(statsRowName, MaterializationStatColumns.MAT_INIT_TIME);
    }
    
    /**
     * write to file a set of atoms with unary or binary predicates as a triples
     * @throws IOException
     */
    private static void writeData(Set<Atom> atoms, String dataFile) throws IOException {
        // write the triples to a file
        File file = new File(dataFile);
        file.delete();
        file.createNewFile();

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        for (Atom atom : atoms) {
            TriplePattern triple = RDFoxFactory.pdqAtomAsRDFoxTriple(atom);
            writer.write(triple.toString(Prefixes.s_emptyPrefixes) + " .");
            writer.write("\n");
        }

        writer.close();
    }
}
