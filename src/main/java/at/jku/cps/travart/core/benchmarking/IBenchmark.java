package at.jku.cps.travart.core.benchmarking;

import org.pf4j.ExtensionPoint;

import com.google.common.eventbus.EventBus;

public interface IBenchmark<T> {
	
	// TODO Return true/false instead?
	void activateBenchmark(EventBus bus);
	
	public T getResults();
	
	public String getId();

}
