package at.jku.cps.travart.core.common;

import at.jku.cps.travart.core.transformation.AbstractBenchmarkingTransformer;

public interface IBenchmarkingPlugin<T> extends IPlugin<T> {
	
	// FIXME Interface depends on abstract class
	AbstractBenchmarkingTransformer getBenchmarkingTransformer();

}
