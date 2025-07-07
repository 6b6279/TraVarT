package at.jku.cps.travart.core.benchmarking;

import java.time.Duration;
import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.auto.service.AutoService;
import com.google.common.eventbus.Subscribe;

@AutoService(IBenchmark.class)
public class TransformationTimeBenchmark extends AbstractBenchmark<Duration> {
	
	private Instant startedAt;
	private Instant end;
	
	@Subscribe
	private void initialSize(TransformationBeginEvent event) {
		log("Just recieved: TransformationBeginEvent = " + event.getDetails() + ", size: " + event.initialSize);
		startedAt = event.getTimestamp();
	}
	
	@Subscribe
	private void endOfTransformation(TransformationEndEvent event) {
		// FIXME React if currentSize != event.finalSize?
		end = event.getTimestamp();
	}

	@Override
	public Duration getResults() {
		return Duration.between(startedAt, end).abs();
	}

	@Override
	public String getId() {
		return "transformationTime";
	}

}
