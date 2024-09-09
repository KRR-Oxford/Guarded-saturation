package uk.ac.ox.cs.gsat.kaon2;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
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

        Reasoner reasoner = getReasoner(processName, inputPath);

        ExecutorService executorKAON2 = Executors.newSingleThreadExecutor();
        Callable<Collection<Rule>> call = getCallable(processName, reasoner);
        Future<Collection<Rule>> futureKAON2 = executorKAON2.submit(call);

        try {
            if (config.getTimeout() != null)
                saturationRules.addAll(futureKAON2.get(config.getTimeout(), TimeUnit.SECONDS));
            else
                saturationRules.addAll(futureKAON2.get());
        } catch (TimeoutException e) {
            futureKAON2.cancel(true);
            reasoner.interrupt();
            // wait 1s for the execution to terminate
            executorKAON2.awaitTermination(1, TimeUnit.SECONDS);

            if (statisticsCollector != null)
                statisticsCollector.put(processName, SaturationStatColumns.TIME, "TIMEOUT");

        } catch (InterruptedException | ExecutionException e) {
            futureKAON2.cancel(true);
            throw e;
        }

        executorKAON2.shutdownNow();

        if (statisticsCollector != null)
            statisticsCollector.put(processName, SaturationStatColumns.OUTPUT_SIZE, saturationRules.size());

        return Set.of();
        // return KAON2Convertor.getTGDFromKAON2Rules(saturationRules);
    }

    @Override
    public Collection<? extends TGD> saturate(String inputPath) throws Exception {
        return saturate("", inputPath);
    }

    @Override
    public void setStatisticCollector(StatisticsCollector<SaturationStatColumns> statisticsCollector) {
        this.statisticsCollector = statisticsCollector;
    }

    protected Reasoner getReasoner(String processName, String inputPath) throws KAON2Exception, InterruptedException {
        KAON2Connection connection = KAON2Manager.newConnection();
        DefaultOntologyResolver resolver = new DefaultOntologyResolver();
        resolver.registerReplacement("http://bkhigkhghjbhgiyfgfhgdhfty", "file:" + inputPath.replace("\\", "/"));
        connection.setOntologyResolver(resolver);

        Ontology ontology = connection.openOntology("http://bkhigkhghjbhgiyfgfhgdhfty", new HashMap<String, Object>());
        if (statisticsCollector != null)
            statisticsCollector.put(processName, SaturationStatColumns.AXIOM_NB,
                    ontology.createAxiomRequest().sizeAll());
        return ontology.createReasoner();
    }

    protected Callable<Collection<Rule>> getCallable(String processName, Reasoner reasoner) {
        return new Callable<Collection<Rule>>() {
            @Override
            public Collection<Rule> call() throws Exception {
                Collection<Rule> reductionToDLP = new LinkedList<>();

                KAON2Statistics monitor = null;

                if (statisticsCollector != null)
                    monitor = new KAON2Statistics(processName, statisticsCollector);

                if (monitor != null)
                    reasoner.setParameter("theoremProverMonitor", monitor);

                // we start the time watch
                if (statisticsCollector != null)
                    statisticsCollector.start(processName);

                try {
                    reductionToDLP = reasoner.getReductionToDisjunctiveDatalog(false, false, false, true);
                } finally {
                    reasoner.dispose();
                }

                if (statisticsCollector != null)
                    statisticsCollector.tick(processName, SaturationStatColumns.TIME);

                return reductionToDLP;
            }
        };
    }
}
