package uk.ac.ox.cs.gsat.statistics;

import static com.google.common.base.Preconditions.checkState;

/**
 * StopWatch used with timing the execution of functions.
 */
public class StopWatch {

	/** The last tick. */
	private long tick;
	
	/** The total time. */
	private long total;
	
	/** Whether the stop watch is currently paused. */
	private boolean paused = false;
	
	/**
	 * Starts the stop watch.
	 */
	public void start() {
		this.resume();
		this.total = 0;
		this.paused = false;
	}

	/**
	 * Resumes the stop watch.
	 */
	public void resume() {
		this.tick = System.currentTimeMillis();
		this.paused = false;
	}
	
	/**
	 * Pauses the stop watch.
	 */
	public void pause() {
		this.paused = true;
	}

	/**
	 * @return the amount of times spent since the last lap or start whichever is the latest.
	 */
	public long lap() {
		checkState(!paused, "Attempting to apply tick to paused stop watch");
		long now = System.currentTimeMillis();
		long lastLap = tick;
		tick = now;
		long result = now - lastLap;
		total += result;
		return result;
	}

	/**
	 * @return the total time spent since the start of the stop match.
	 */
	public long total() {
		return total;
	}
}
