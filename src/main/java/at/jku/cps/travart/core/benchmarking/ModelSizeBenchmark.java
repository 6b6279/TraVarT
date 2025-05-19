package at.jku.cps.travart.core.benchmarking;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class ModelSizeBenchmark implements IBenchmark<Integer> {
	
	private int currentSize;
	private EventBus registeredBus;
	private int bitMask; // Use a bitmask for filtering specific plugins?
	
	@Subscribe
	private void count(NewFeatureEvent event) {
		currentSize++;
	}
	
	@Subscribe
	private void count(FeatureDeletedEvent event) {
		currentSize--;
	}
	
	@Subscribe
	private void initialSize(TransformationBeginEvent event) {
		currentSize = event.initialSize;
	}
	
	@Subscribe
	private void endOfTransformation(TransformationEndEvent event) {
		// FIXME React if currentSize != event.finalSize?
		registeredBus.unregister(this);
	}

	@Override
	public void activateBenchmark(EventBus bus) {
		bus.register(this);
		this.registeredBus = bus;
	}

	@Override
	public Integer getResults() {
		return currentSize;
	}

}
