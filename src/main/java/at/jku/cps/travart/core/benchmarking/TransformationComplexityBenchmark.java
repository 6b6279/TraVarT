package at.jku.cps.travart.core.benchmarking;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.auto.service.AutoService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

@AutoService(IBenchmark.class)
public class TransformationComplexityBenchmark extends AbstractBenchmark<Integer> {
	
	private int hits;
	
	@Override
	public String getId() {
		return "complexity"; // Used to identify the benchmark while invoking over CLI
	}
	
	@Subscribe
	private void count(OneToNTransformationEvent event) {
		hits++;
	}
	
	@Subscribe
	private void count(AdditionalConstraintEvent event) {
		hits += event.getFactor();
	}

	@Override
	public void activateBenchmark(EventBus bus) {
		bus.register(this);
		this.registeredBus = bus;
	}

	@Override
	public List<Integer> getResults() {
		return List.of(hits);
	}

}
