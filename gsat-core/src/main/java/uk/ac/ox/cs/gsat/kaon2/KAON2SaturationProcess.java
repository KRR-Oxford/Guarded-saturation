package uk.ac.ox.cs.gsat.kaon2;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.semanticweb.kaon2.api.DefaultOntologyResolver;
import org.semanticweb.kaon2.api.KAON2Connection;
import org.semanticweb.kaon2.api.KAON2Exception;
import org.semanticweb.kaon2.api.KAON2Manager;
import org.semanticweb.kaon2.api.Ontology;
import org.semanticweb.kaon2.api.logic.Rule;
import org.semanticweb.kaon2.api.reasoner.Reasoner;

import uk.ac.ox.cs.gsat.SaturationProcessConfiguration;
import uk.ac.ox.cs.gsat.api.SaturationProcess;
import uk.ac.ox.cs.gsat.api.SaturationStatColumns;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.statistics.StatisticsCollector;

public class KAON2SaturationProcess implements SaturationProcess {

    private SaturationProcessConfiguration config;
    private StatisticsCollector<SaturationStatColumns> statisticsCollector;

    public KAON2SaturationProcess(SaturationProcessConfiguration config) {
        this.config = config;
    }

    @Override
    public Collection<? extends TGD> saturate(String processName, String inputPath) throws Exception {
        final Collection<Rule> saturationRules = new LinkedList<>();

        ExecutorService executorKAON2 = Executors.newSingleThreadExecutor();
        Future<String> futureKAON2 = executorKAON2.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                try {
                    KAON2Statistics monitor = null;

                    if (statisticsCollector != null)
                        monitor = new KAON2Statistics(processName, statisticsCollector);

                    saturationRules.addAll(runKAON2(processName, inputPath, monitor));
                } catch (Exception e) {
                    e.printStackTrace();
                    return "Failed!";
                }
                return "Completed!";
            }
        });

        try {
            futureKAON2.get(config.getTimeout(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            futureKAON2.cancel(true);
        } catch (InterruptedException | ExecutionException e) {
            futureKAON2.cancel(true);

        }

        executorKAON2.shutdownNow();

        return KAON2Convertor.getTGDFromKAON2Rules(saturationRules);
    }

    @Override
    public Collection<? extends TGD> saturate(String inputPath) throws Exception {
        return saturate("", inputPath);
    }

    private Collection<Rule> runKAON2(String processName, String input_file, KAON2Statistics monitor)
            throws KAON2Exception, InterruptedException {
        KAON2Connection connection = KAON2Manager.newConnection();

        DefaultOntologyResolver resolver = new DefaultOntologyResolver();
        resolver.registerReplacement("http://bkhigkhghjbhgiyfgfhgdhfty", "file:" + input_file.replace("\\", "/"));
        connection.setOntologyResolver(resolver);

        Ontology ontology = connection.openOntology("http://bkhigkhghjbhgiyfgfhgdhfty", new HashMap<String, Object>());
        System.out.println("Initial axioms in the ontology: " + ontology.createAxiomRequest().sizeAll());
        Reasoner reasoner = ontology.createReasoner();

        if (monitor != null)
            reasoner.setParameter("theoremProverMonitor", monitor);

        statisticsCollector.tick(processName, SaturationStatColumns.OTHER_TIME);
        Collection<Rule> reductionToDLP = new LinkedList<>();
        try {
            reductionToDLP = reasoner.getReductionToDisjunctiveDatalog(false, false, false, true);
        } finally {
            reasoner.dispose();
        }
        statisticsCollector.tick(processName, SaturationStatColumns.TIME);
        return reductionToDLP;
    }

    @Override
    public void setStatisticCollector(StatisticsCollector<SaturationStatColumns> statisticsCollector) {
        this.statisticsCollector = statisticsCollector;
    }

}
