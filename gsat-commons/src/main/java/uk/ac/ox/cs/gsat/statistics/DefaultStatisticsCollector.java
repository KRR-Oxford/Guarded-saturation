package uk.ac.ox.cs.gsat.statistics;

import java.util.Collection;
import java.util.Map;
import java.util.Observable;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;

/**
 * A statistictic collection, which keeps various values (times, cardinalities) in a table 
 * structure.
 */
public class DefaultStatisticsCollector<T extends StatisticsColumn> extends Observable implements StatisticsCollector<T> {

	/** The values. */
	private final Table<String, T, Object> cells = HashBasedTable.create();

	/** The stop watches currently in use. */
	private final Map<String, StopWatch> stopWatchs = Maps.newLinkedHashMap();
	
	/**
	 * Clear all statistics.
	 */
	public void clear() {
		this.cells.clear();
	}
	
	/**
	 * @return the whole statistics table
	 */
	public Table<String, T, Object> cells() {
		return this.cells;
	}
	
	/**
	 * Puts a value in the given row and column.
	 *
	 * @param row the row
	 * @param col the column
	 * @param val the value
	 */
	public void put(Object row, T col, Object val) {
		String key = String.valueOf(row);
		this.cells.put(key, col, val);
	}
	
    /**
	 * Increment the value (assuming integer) in the given row and column.
	 *
	 * @param row the row
	 * @param col the column
	 */
	public void incr(Object row, T col) {
        String key = String.valueOf(row);
        Integer oldValue = (Integer) this.cells.get(row, col);
        int value;

        if (oldValue != null) {
            value = Integer.valueOf(oldValue) + 1;
        } else {
            value = 1;
        }

        this.cells.put(key, col, value);
	}

	/**
	 * Notifies that the given has had from update
	 *
	 * @param row the row to which the notification applies.
	 */
	public void report(Object row) {
		report(String.valueOf(row));
	}

	/**
	 * Notifies that the given has had from update
	 *
	 * @param row the row to which the notification applies.
	 */
	public void report(String row) {
		setChanged();
		notifyObservers(row);
	}
	
	/**
	 * Notifies all observers that object has changed.
	 */
	public void reportAll() {
		setChanged();
		notifyObservers();
	}
	
	/**
	 * Gets or create a stop watch for the given row.
	 *
	 * @param row the row's name
	 * @return a (possibly fresh) stop watch for the given row.
	 */
	private StopWatch getOrCreate(String row) {
		StopWatch sw = this.stopWatchs.get(row);
		if (sw == null) {
			this.stopWatchs.put(row, (sw = new StopWatch()));
		}
		return sw;
	}
	
	/**
	 * Gets a stop watch for the given row or fails if no stop watch exists for this row.
	 *
	 * @param row the row's name
	 * @return the stop watch for the given row.
	 */
	private StopWatch getOrFail(String row) {
		StopWatch sw = this.stopWatchs.get(row);
		if (sw == null) {
			throw new IllegalArgumentException("No such stop watch: " + row);
		}
		return sw;
	}
	
	/**
	 * Starts the stop watch on the given row.
	 *
	 * @param row the row's name
	 */
	public void start(Object row) {
		String key = String.valueOf(row);
		getOrCreate(key).start();
	}
	
	/**
	 * Pauses the stop watch on the given row.
	 *
	 * @param row the row's name
	 */
	public void pause(Object row) {
		String key = String.valueOf(row);
		getOrFail(key).pause();
	}
	
	/**
	 * Resumes the stop watch on the given row.
	 *
	 * @param row the row's name
	 */
	public void resume(Object row) {
		String key = String.valueOf(row);
		getOrFail(key).resume();
	}

	/**
	 * Ticks the stop watch on the given row for the given key, i.e. accumulates on the given key
	 * the time spent since the start or the last tick, whichever comes last.
	 *
	 * @param row the row's name
	 * @param col the column
	 */
	public void tick(Object row, T col) {
		String key = String.valueOf(row);
		StopWatch sw = getOrFail(key);
		long lap = sw.lap();
		Object existing = cells.get(key, col);
		if (existing != null) {
			lap += Long.valueOf(String.valueOf(existing));
		}
		cells.put(key, col, lap);
	}

	/**
	 * Stops the stop watch on the given row.
	 *
	 * @param row the row's name
	 */
	public void stop(Object row, T totalTimeColumn) {
		String key = String.valueOf(row);
		StopWatch sw = getOrFail(key);
		cells.put(key, totalTimeColumn, sw.total());
	}

	/**
	 * Returns the total time for the stop watch on the given row.
	 *
	 * @param row the row's name
	 * @return the total time recorded by the stop watch on the given row.
	 */
	public Long total(Object row) {
		String key = String.valueOf(row);
		return getOrFail(key).total();
	}
	
	/**
	 * Gets the value on the given row and column
	 *
	 * @param row the row's name
	 * @param key the column's key
	 * @return the value of the given row and column
	 */
	public Object get(String row, T col) {
		return this.cells.get(row, col);
	}

    @Override
    public Map<? extends StatisticsColumn, Object> getRow(String rowName) {
        return this.cells().row(rowName);
    }

    @Override
    public Collection<String> getRows() {
        return this.cells().rowKeySet();
    }

    @Override
    public Collection<T> getColumns() {
        return this.cells().columnKeySet();
    }

}
