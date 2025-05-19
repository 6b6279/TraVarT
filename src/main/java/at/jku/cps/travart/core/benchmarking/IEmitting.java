package at.jku.cps.travart.core.benchmarking;

import com.google.common.eventbus.EventBus;

public interface IEmitting {
	
	void setBus(EventBus bus);
	
	EventBus getBus();
	
	void setVerbosity(int level);
	
	int getVerbosity();
	
	void toggleMute();

}
