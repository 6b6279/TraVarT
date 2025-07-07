package at.jku.cps.travart.core.benchmarking;

import org.slf4j.event.Level;

import com.google.common.eventbus.EventBus;

public interface IEmitting {
	
	void setBus(EventBus bus);
	
	EventBus getBus();
	
	void setVerbosity(Level level);
	
    Level getVerbosity();
	
	void triggerMuteEvent();
	
	void triggerUnmuteEvent();

}
