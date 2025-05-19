package at.jku.cps.travart.core.benchmarking;

import com.google.common.eventbus.EventBus;

public interface IBenchmark<T> {
	
	// TODO Return true/false instead?
	void activateBenchmark(EventBus bus);
	
	public T getResults();

}
