package it.softsolutions.bestx.management.statistics;

import com.codahale.metrics.Histogram;

public class StatisticsSnapshot {

	private Histogram histogram;
	private long last;
	
	public StatisticsSnapshot(Histogram histogram) {
		this.histogram = histogram;
	}
	
	public long getCount() {
		return histogram.getCount();
	}
	public long getMin() {
		return histogram.getSnapshot().getMin();
	}
	public long getMax() {
		return histogram.getSnapshot().getMax();
	}
	public double getMean() {
		return histogram.getSnapshot().getMean();
	}
	public long getLast() {
		return last;
	}
	public void setLast(long last) {
		this.last = last;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("sample count= ");
		sb.append(this.getCount());
		sb.append(", min value= ");
		sb.append(this.getMin());
		sb.append(", max value= ");
		sb.append(this.getMax());
		sb.append(", mean value= ");
		sb.append(this.getMean());
		sb.append(", last value= ");
		sb.append(this.getLast());
		return sb.toString();
	}
	
}
