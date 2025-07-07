package at.jku.cps.travart.core.transformation;

import java.time.Instant;

import java.util.Objects;

import org.slf4j.event.Level;

import com.google.common.eventbus.EventBus;

import at.jku.cps.travart.core.FeatureModelStatistics;
import at.jku.cps.travart.core.benchmarking.IEmitting;
import at.jku.cps.travart.core.benchmarking.MuteEvent;
import at.jku.cps.travart.core.benchmarking.TransformationBeginEvent;
import at.jku.cps.travart.core.benchmarking.TransformationEndEvent;
import at.jku.cps.travart.core.benchmarking.UnmuteEvent;
import at.jku.cps.travart.core.common.IModelTransformer;
import at.jku.cps.travart.core.common.IStatistics;
import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import de.vill.model.FeatureModel;

public abstract class AbstractBenchmarkingTransformer<T> implements IEmitting, IModelTransformer<T> {
	
	protected EventBus bus;
	protected Level verbosity;

	@Override
	final public FeatureModel transform(T model, String modelName, STRATEGY strategy)
			throws NotSupportedVariabilityTypeException {
		FeatureModel transformationResult;
		// FIXME Somehow use `post` utility method similiar to KconfigModelOneWayGraphTransformer
		bus.post(new TransformationBeginEvent(Instant.now(), modelName, model.hashCode(), getTargetStatistics().getVariabilityElementsCount(model)));
		try {
			transformationResult = transformInner(model, modelName, strategy);
		} catch (Exception e) {
			bus.post(new TransformationEndEvent(Instant.now(), modelName, model.hashCode(), 0, false));
			throw e;
		}
		bus.post(new TransformationEndEvent(Instant.now(), modelName, model.hashCode(), FeatureModelStatistics.getInstance().getConstraintsCount(transformationResult), true));
		return transformationResult;
	}

	@Override
	final public T transform(FeatureModel model, String modelName, STRATEGY strategy)
			throws NotSupportedVariabilityTypeException {
		T transformationResult;
		bus.post(new TransformationBeginEvent(Instant.now(), modelName, model.hashCode(), model.getFeatureMap().size()));
		try {
			transformationResult = transformInner(model, modelName, strategy);
		} catch (Exception e) {
			bus.post(new TransformationEndEvent(Instant.now(), modelName, model.hashCode(), model.getFeatureMap().size(), false));
			throw e;
		}
		bus.post(new TransformationEndEvent(Instant.now(), modelName, model.hashCode(), model.getFeatureMap().size(), true));
		return transformationResult;
	}
	
	public abstract T transformInner(FeatureModel model, String modelName, STRATEGY strategy)
			throws NotSupportedVariabilityTypeException;
	
	public abstract FeatureModel transformInner(T model, String modelName, STRATEGY strategy)
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
	public void setVerbosity(Level level) {
		this.verbosity = level;
	}

	@Override
	public Level getVerbosity() {
		return verbosity;
	}
	
	@Override
	public void triggerUnmuteEvent() {
		if (Objects.nonNull(bus)) {
			bus.post(new UnmuteEvent());
		}
	}
	
	@Override
	public void triggerMuteEvent() {
		if (Objects.nonNull(bus)) {
			bus.post(new MuteEvent());
		}
	}
	
	public abstract IStatistics<T> getTargetStatistics();

}
