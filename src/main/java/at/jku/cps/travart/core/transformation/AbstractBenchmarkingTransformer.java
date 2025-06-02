package at.jku.cps.travart.core.transformation;

import java.time.Instant;

import com.google.common.eventbus.EventBus;

import at.jku.cps.travart.core.benchmarking.IEmitting;
import at.jku.cps.travart.core.benchmarking.TransformationBeginEvent;
import at.jku.cps.travart.core.benchmarking.TransformationEndEvent;
import at.jku.cps.travart.core.common.IModelTransformer;
import at.jku.cps.travart.core.common.IModelTransformer.STRATEGY;
import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import de.vill.model.FeatureModel;

public abstract class AbstractBenchmarkingTransformer<T> implements IEmitting, IModelTransformer<T> {
	
	private EventBus bus;
	private int verbosity;
	private boolean muted = false;

	@Override
	final public FeatureModel transform(T model, String modelName, STRATEGY level)
			throws NotSupportedVariabilityTypeException {
		// FIXME Introduce additional ICountableModel interface to ensure model size can be always calculated?
		FeatureModel transformationResult;
		bus.post(new TransformationBeginEvent(Instant.now(), modelName, model.hashCode(), 0));
		try {
			transformationResult = transformInner(model, modelName, level);
		} catch (Exception e) {
			bus.post(new TransformationEndEvent(Instant.now(), modelName, model.hashCode(), 0, false));
			throw e;
		}
		bus.post(new TransformationEndEvent(Instant.now(), modelName, model.hashCode(), 0, true));
		return transformationResult;
	}

	@Override
	final public T transform(FeatureModel model, String modelName, STRATEGY level)
			throws NotSupportedVariabilityTypeException {
		T transformationResult;
		bus.post(new TransformationBeginEvent(Instant.now(), modelName, model.hashCode(), model.getFeatureMap().size()));
		try {
			transformationResult = transformInner(model, modelName, level);
		} catch (Exception e) {
			bus.post(new TransformationEndEvent(Instant.now(), modelName, model.hashCode(), model.getFeatureMap().size(), false));
			throw e;
		}
		bus.post(new TransformationEndEvent(Instant.now(), modelName, model.hashCode(), model.getFeatureMap().size(), true));
		return transformationResult;
	}
	
	public abstract T transformInner(FeatureModel model, String modelName, STRATEGY level)
			throws NotSupportedVariabilityTypeException;
	
	public abstract FeatureModel transformInner(T model, String modelName, STRATEGY level)
			throws NotSupportedVariabilityTypeException;

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
