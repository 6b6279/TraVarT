package at.jku.cps.travart.core.benchmarking;

import java.time.Instant;

public interface IBenchmarkingEvent {
	
	String getDetails(); // Alternatively getMessage
	Instant getTimestamp();
	int getContext(); // Return session ID or plugin identifier when running sequentially

}
