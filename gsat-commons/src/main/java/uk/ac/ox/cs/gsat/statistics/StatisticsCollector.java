package uk.ac.ox.cs.gsat.statistics;

import java.util.Collection;
import java.util.Map;

/**
 * A statistictic collection, which keeps various values (times, cardinalities) in table
 */
public interface StatisticsCollector<T extends StatisticsColumn> {

    public Collection<T> getColumns();

    public Map<? extends StatisticsColumn, Object> getRow(String rowName);

    public Collection<String> getRows();

    /**
	 * Clear all statistics.
	 */
    public void clear();

	/**
	 * Puts a value in the given row and column.
	 *
	 * @param row the row
	 * @param col the column
	 * @param val the value
	 */
    public void put(Object row, T col, Object val);

    /**
	 * Increment the value (assuming integer) in the given row and column.
	 *
	 * @param row the row
	 * @param col the column
	 */
    public void incr(Object row, T col);

	/**
	 * Notifies that the given has had from update
	 *
	 * @param row the row to which the notification applies.
	 */
    public void report(Object row);

	/**
	 * Notifies that the given has had from update
	 *
	 * @param row the row to which the notification applies.
	 */
    public void report(String row);
	
	/**
	 * Notifies all observers that object has changed.
	 */
    public void reportAll();
	
	/**
	 * Starts the stop watch on the given row.
	 *
	 * @param row the row's name
	 */
    public void start(Object row);

	/**
	 * Pauses the stop watch on the given row.
	 *
	 * @param row the row's name
	 */
    public void pause(Object row);
	
	/**
	 * Resumes the stop watch on the given row.
	 *
	 * @param row the row's name
	 */
    public void resume(Object row);

	/**
	 * Ticks the stop watch on the given row for the given key, i.e. accumulates on the given key
	 * the time spent since the start or the last tick, whichever comes last.
	 *
	 * @param row the row's name
	 * @param col the column
	 */
    public void tick(Object row, T col);

	/**
	 * Stops the stop watch on the given row.
	 *
	 * @param row the row's name
	 */
    public void stop(Object row, T totalTimeColumn);

	/**
	 * Returns the total time for the stop watch on the given row.
	 *
	 * @param row the row's name
	 * @return the total time recorded by the stop watch on the given row.
	 */
    public Long total(Object row);
	
	/**
	 * Gets the value on the given row and column
	 *
	 * @param row the row's name
	 * @param key the column's key
	 * @return the value of the given row and column
	 */
    public Object get(String row, T col);
}
