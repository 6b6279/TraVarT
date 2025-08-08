package at.jku.cps.travart.core.benchmarking;

import java.util.Collection;
import java.util.List;

import com.google.auto.service.AutoService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

@AutoService(IBenchmark.class)
public class ModelSizeBenchmark extends AbstractBenchmark<Integer> {
	
	private int initialSize;
	private int finalSize;
	private int diff;
	
	@Override
	public String getId() {
		return "modelSize"; // Used to identify the benchmark while invoking over CLI
	}
	
	@Subscribe
	private void count(NewFeatureEvent event) {
		diff++;
	}
	
	@Subscribe
	private void count(FeatureDeletedEvent event) {
		diff--;
	}
	
	@Subscribe
	private void initialSize(TransformationBeginEvent event) {
		log("Just recieved: TransformationBeginEvent = " + event.getDetails() + ", size: " + event.initialSize);
		this.initialSize = event.initialSize;
	}
	
	@Subscribe
	private void endOfTransformation(TransformationEndEvent event) {
		// FIXME React if currentSize != event.finalSize?
		log("Just recieved: TransformationEndEvent = " + event.getDetails() + ", size: " + event.finalSize);
		if (event.intermediate) return;
		if ((initialSize + diff) != event.finalSize) 
			LOGGER.warn("Feature count doesn't match with count reported by TransformationEndEvent: " + diff + " != " + event.finalSize);
		this.finalSize = event.finalSize;
		registeredBus.unregister(this);
	}

	@Override
	public void activateBenchmark(EventBus bus) {
		bus.register(this);
		this.registeredBus = bus;
	}

	@Override
	public List<Integer> getResults() {
		return List.of(initialSize, diff, finalSize);
	}

	@Override
	public List<String> getResultsHeader() {
		return List.of("initialSize", "expectedDiff", "finalSize");
	}

}
