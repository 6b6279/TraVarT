package at.jku.cps.travart.core.benchmarking;

import java.time.Instant;

public class UnmuteEvent extends AbstractBenchmarkEvent {
	
	public UnmuteEvent(Instant time, String msg, int ctx) {
		super(time, msg, ctx);
	}

}
