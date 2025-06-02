package at.jku.cps.travart.core.benchmarking;

import java.time.Instant;

public interface IBenchmarkingEvent<T> {

	String getMessage(); // Might not be personalized, short description of event
	T getDetails(); // Content of the event, for example, current model state
	Instant getTimestamp();
	int getContext(); // Return session ID or plugin identifier when running sequentially

}
