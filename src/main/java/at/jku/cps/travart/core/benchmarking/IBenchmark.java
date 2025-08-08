package at.jku.cps.travart.core.benchmarking;

import java.util.Collection;
import java.util.List;

import org.pf4j.ExtensionPoint;

import com.google.common.eventbus.EventBus;

public interface IBenchmark<T> {
	
	// TODO Return true/false instead?
	void activateBenchmark(EventBus bus);
	
	// Use List interface to allow indexed access
	public List<T> getResults();
	
	default List<String> getResultsHeader() {
		return List.of(getId());
	}
	
	public String getId();

}
