package uk.ac.ox.cs.gsat.statistics;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class NullStatisticsCollector<Q extends StatisticsColumn> implements StatisticsCollector<Q> {

    @Override
    public void clear() {
    }

    @Override
    public void put(Object row, Q col, Object val) {
    }

    @Override
    public void incr(Object row, Q col) {
    }

    @Override
    public void report(Object row) {
    }

    @Override
    public void report(String row) {
    }

    @Override
    public void reportAll() {
    }

    @Override
    public void start(Object row) {
    }

    @Override
    public void pause(Object row) {
    }

    @Override
    public void resume(Object row) {
    }

    @Override
    public void tick(Object row, Q col) {
    }

    @Override
    public void stop(Object row, Q totalTimeColumn) {
    }

    @Override
    public Long total(Object row) {
        return (long) 0;
    }

    @Override
    public Object get(String row, Q col) {
        return null;
    }

    @Override
    public Collection<Q> getColumns() {
        return Set.of();
    }

    @Override
    public Map<? extends StatisticsColumn, Object> getRow(String rowName) {
        return Map.of();
    }

    @Override
    public Collection<String> getRows() {
        return Set.of();
    }
}
