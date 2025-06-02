package at.jku.cps.travart.core.benchmarking;

import com.google.auto.service.AutoService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

@AutoService(IBenchmark.class)
public class ModelSizeBenchmark implements IBenchmark<Integer> {
	
	private int currentSize;
	private EventBus registeredBus;
	private int bitMask; // Use a bitmask for filtering specific plugins?
	
	@Override
	public String getId() {
		return "modelSize"; // Used to identify the benchmark while invoking over CLI
	}
	
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
