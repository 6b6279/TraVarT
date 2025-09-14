package at.jku.cps.travart.core.benchmarking;

import java.time.Instant;

public class MuteEvent extends AbstractBenchmarkEvent {

	public MuteEvent(Instant time, String msg, int ctx) {
		super(time, msg, ctx);
	}

}
