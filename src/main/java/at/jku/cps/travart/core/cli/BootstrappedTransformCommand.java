package at.jku.cps.travart.core.cli;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import at.jku.cps.travart.core.common.IDeserializer;
import at.jku.cps.travart.core.common.IModelTransformer;
import at.jku.cps.travart.core.common.ISerializer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "transform", version = "0.0.1", 
	description = "Transforms the given variability artifacts into another type. "
		+ "When run over a plugin, target type is limited to the transformer provided by the plugin; "
		+ "i.e., from/to UVL from/to the format supported by the plugin.")
public class BootstrappedTransformCommand implements Callable<Integer> {
	private static final Logger LOGGER = LogManager.getLogger(TransformCommand.class);

	private static final String CORE_MODEL_UVL = "UVL";

	@SuppressWarnings("unused")
	private static String toStringList(final Iterable<String> fileExtensions) {
		StringBuilder builder = new StringBuilder();
		builder.append("{ ");
		for (String extension : fileExtensions) {
			builder.append(extension).append(",");
		}
		builder.deleteCharAt(builder.lastIndexOf(","));
		builder.append(" }");
		return builder.toString();
	}

	@Parameters(index = "0", description = "The source path to the variability artifact to transform. Folders are not supported.")
	private Path sourcePath;

	@Parameters(index = "1", description = "The output path to which the variability artifact is transformed.")
	private Path targetPath;

	private final IDeserializer deserializer;
	private final ISerializer serializer;
	private final IModelTransformer transformer;

	// private boolean startUVL = false;

	public BootstrappedTransformCommand(IDeserializer bDeserializer, ISerializer bSerializer,
			IModelTransformer bTransformer) {
		this.deserializer = bDeserializer;
		this.serializer = bSerializer;
		this.transformer = bTransformer;
	}

	@Override
	public Integer call() throws Exception {
		// TODO Implement generic command-line handling with bootstrapped transformer etc.
		return 0;
	}
}
