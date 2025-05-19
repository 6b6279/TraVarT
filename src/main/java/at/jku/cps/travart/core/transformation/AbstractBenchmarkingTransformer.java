package at.jku.cps.travart.core.transformation;

import com.google.common.eventbus.EventBus;

import at.jku.cps.travart.core.benchmarking.IEmitting;
import at.jku.cps.travart.core.common.IModelTransformer;

public abstract class AbstractBenchmarkingTransformer<T> implements IEmitting, IModelTransformer<T> {
	
	private EventBus bus;
	private int verbosity;
	private boolean muted = false;

	@Override
	public void setBus(EventBus bus) {
		this.bus = bus;
	}

	@Override
	public EventBus getBus() {
		return bus;
	}

	@Override
	public void setVerbosity(int level) {
		this.verbosity = level;
	}

	@Override
	public int getVerbosity() {
		return verbosity;
	}

	@Override
	public void toggleMute() {
		this.muted = !muted;
	}

}
