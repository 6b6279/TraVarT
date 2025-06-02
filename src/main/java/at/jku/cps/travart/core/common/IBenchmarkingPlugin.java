package at.jku.cps.travart.core.common;

import at.jku.cps.travart.core.transformation.AbstractBenchmarkingTransformer;

public interface IBenchmarkingPlugin extends IPlugin {
	
	// FIXME Interface depends on abstract class
	AbstractBenchmarkingTransformer getBenchmarkingTransformer();

}
