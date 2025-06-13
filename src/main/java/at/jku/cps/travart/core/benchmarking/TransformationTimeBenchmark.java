
package at.jku.cps.travart.core.benchmarking;

import java.time.Duration;
import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.auto.service.AutoService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

@AutoService(IBenchmark.class)
public class TransformationTimeBenchmark implements IBenchmark<Duration> {
	
	private Instant startedAt;
	private Instant end;
	private EventBus registeredBus;
	private int bitMask; // Use a bitmask for filtering specific plugins?
	
	static final Logger LOGGER = LogManager.getLogger();
	
	@Override
	public String getId() {
		return "transformationTime"; // Used to identify the benchmark while invoking over CLI
	}
	
	@Subscribe
	private void initialSize(TransformationBeginEvent event) {
		LOGGER.debug("Just recieved: TransformationBeginEvent = " + event.getDetails() + ", size: " + event.initialSize);
		startedAt = event.getTimestamp();
	}
	
	@Subscribe
	private void endOfTransformation(TransformationEndEvent event) {
		// FIXME React if currentSize != event.finalSize?
		end = event.getTimestamp();
	}

	@Override
	public void activateBenchmark(EventBus bus) {
		bus.register(this);
		this.registeredBus = bus;
	}

	// FIXME Somehow prevent premature resolution of results
	@Override
	public Duration getResults() {
		return Duration.between(startedAt, end);
	}

}
