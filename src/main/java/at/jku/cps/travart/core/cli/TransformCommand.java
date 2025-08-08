/*******************************************************************************
 * This Source Code Form is subject to the terms of the Mozilla
 * Public License, v. 2.0. If a copy of the MPL was not distributed
 * with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Contributors:
 *     @author Kevin Feichtinger
 *
 * Command line tool command transform a variability artifact.
 *
 * Copyright 2023 Johannes Kepler University Linz
 * LIT Cyber-Physical Systems Lab
 * All rights reserved
 *******************************************************************************/
package at.jku.cps.travart.core.cli;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mockito.Mockito;
import org.slf4j.event.Level;

import com.google.common.eventbus.EventBus;

import at.jku.cps.travart.core.common.IModelTransformer;
import at.jku.cps.travart.core.common.IModelTransformer.STRATEGY;
import at.jku.cps.travart.core.common.IPlugin;
import at.jku.cps.travart.core.common.IBenchmarkingPlugin;
import at.jku.cps.travart.core.benchmarking.IBenchmark;
import at.jku.cps.travart.core.benchmarking.ResultsWriter;
import at.jku.cps.travart.core.common.IDeserializer;
import at.jku.cps.travart.core.common.ISerializer;
import at.jku.cps.travart.core.common.IValidate;
import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.jku.cps.travart.core.exception.TransformationException;
import at.jku.cps.travart.core.helpers.TraVarTPluginManager;
import at.jku.cps.travart.core.io.FileUtils;
import at.jku.cps.travart.core.io.UVLDeserializer;
import at.jku.cps.travart.core.io.UVLSerializer;
import at.jku.cps.travart.core.transformation.AbstractBenchmarkingTransformer;
import de.vill.model.FeatureModel;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@SuppressWarnings("rawtypes")
@Command(name = "transform", version = "0.0.1", description = "Transforms the given variability artifacts into another type.")
public class TransformCommand implements Callable<Integer> {

	private static final Logger LOGGER = LogManager.getLogger(TransformCommand.class);

	private static final String CORE_MODEL_UVL = "UVL";

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

	@Parameters(index = "0", description = "The source path to the variability artifact to transform. If the path is a folder, each variability artifact of the given type (-soureType) is transformed.")
	private Path sourcePath;

	@Parameters(index = "1", description = "The output path to which the variability artifact is transformed. If the source is given as a folder, this parameter must be a folder too.")
	private Path targetPath;

	@Option(names = { "-st", "-sourceType", "--st",
			"--sourceType" }, required = true, description = "The mandatory type of the source variability artifacts, as listed in the plugin command.")
	private String sourceType;

	@Option(names = { "-tt", "-targetType", "--tt",
			"--targetType" }, required = true, description = "The mandatory target type of the transformed variability artifacts, as listed by the plugin command.")
	private String targetType;

	@Option(names = { "-b",
			"--benchmark" }, split = ",", required = false, description = "Name of the respective benchmarks to use (comma-seperated). A list of available benchmarks are provided by the bench command.")
	private List<String> benchmarks;

	@Option(names = { "-wb",
			"--write-benchmarks" }, required = false, description = "Path to file to which the benchmark results should be written in CSV format. This is optional.")
	private Path benchmarkResultsFile;

	@Option(names = {
			"--strategy" }, required = false, defaultValue = "ONE_WAY", description = "Transformation strategy to use: ROUNDTRIP or ONE_WAY. Will default to ONE_WAY if none given.")
	private STRATEGY strategy;
	
	@Option(names = {
			"--roundtrip" }, description = "Set only if --strategy was set to ROUNDTRIP. Does an in-place roundtrip transformation over the targetType.")
	private boolean inPlaceRoundtrip;
	
	@Option(names = {
			"--no-serialize" }, description = "Do not serialize after transformation. Especially useful if the plugin used does not have a robust serializer, or only the benchmarking results are relevant.")
	private boolean skipSerialization;
	
	@Option(names = {
			"--reverse-transformation" }, 
			description = "Set only if --no-serialize is set and --strategy is ROUNDTRIP. Reverses transformation directly after, benchmarking only the reverse transformation."
	)
	private boolean reverseTransformation;

	@Option(names = {
			"--strict" }, description = "Whether TraVarT should stop transformation if the underlying plugin throws an exception. This is only relevant when multiple models are being transformed.")
	private boolean strict;
	
	@Option(names = {
			"--timeout" }, defaultValue = "5", description = "Timeout for (de-)serializing models. Defaults to 5 seconds.")
	private long timeout;

	private IDeserializer deserializer;
	private ISerializer serializer;
	private final Deque<IModelTransformer> transformers = new ArrayDeque<>();
	private ResultsWriter rw;

	private boolean startUVL = false;

	@Override
	public Integer call() throws Exception {
		assert sourcePath != null;
		assert sourceType != null;
		assert targetPath != null;
		assert targetType != null;
		LOGGER.debug("Verify parameters...");
		if (sourceType.equalsIgnoreCase(targetType)) {
			LOGGER.error("Source and Target type are equal, no transformation needed");
			return 1;
		}
		LOGGER.debug("Verify the given paths...");
		if (!(Files.isRegularFile(sourcePath) || Files.isDirectory(sourcePath))) {
			LOGGER.error("Given source path is not a valid one!");
			return 2;
		}
		if (!(Files.isRegularFile(targetPath) || Files.isDirectory(targetPath))) {
			if (Files.exists(targetPath)) {
				LOGGER.error("Given target path is not a valid one!");
				return 3;
			}
			targetPath.toFile().mkdirs();
		}
		// Collect necessary information for transformations
		LOGGER.debug("Initialize transformations...");
		int init = initializeTransformations();
		if (init != 0) {
			LOGGER.error("Unable to initialize plugins! Check installed plugins using command \"plugin\".");
			return 4;
		}
		// Run transformations
		LOGGER.debug("Starting transformation of variability artifacts...");
		try {
			if (Files.isRegularFile(sourcePath)) {
				return transformSingleFile(sourcePath);
			}
			return transformDirectory();
		} catch (IOException | NotSupportedVariabilityTypeException ex) {
			LOGGER.error("Error while handling files...");
			LOGGER.error(ex.toString());
			throw new TransformationException(ex);
		} finally {
			// Need to close ResultsWriter here if it's set
			if (Objects.nonNull(rw)) {
				LOGGER.debug("Used ResultsWriter, closing enclosed FileWriter...");
				rw.dispose();
			}
		}
	}
	
	private int initializeTransformations() {
		if (Objects.nonNull(benchmarks)) {
			return initializeTransformationsInner(() -> findPlugin(targetType, true), () -> findPlugin(sourceType, true) );
		} else {
			return initializeTransformationsInner(() -> findPlugin(targetType, false), () -> findPlugin(sourceType, false) );
		}
	}
	
	private int initializeTransformationsInner(Supplier<IPlugin> targetTypePluginSupplier, Supplier<IPlugin> sourceTypePluginSupplier) {		
		if (CORE_MODEL_UVL.equalsIgnoreCase(sourceType)) {
			LOGGER.debug("Detected source type UVL...");
			deserializer = new UVLDeserializer();
			startUVL = true;
		} else {
			var sourceTypePlugin = sourceTypePluginSupplier.get();
			if (sourceTypePlugin == null) {
				LOGGER.error("Could not find plugin for given source type!");
				return 1;
			}
			LOGGER.debug(String.format("Detected source type %s...", sourceTypePlugin.getName()));
			deserializer = sourceTypePlugin.getDeserializer();
			transformers.add(sourceTypePlugin.getTransformer());
		}
		
		if (CORE_MODEL_UVL.equalsIgnoreCase(targetType)) {
			LOGGER.debug("Detected target type UVL...");
			serializer = new UVLSerializer();
		} else {
			var targetTypePlugin = targetTypePluginSupplier.get();
			if (targetTypePlugin == null) {
				LOGGER.error("Could not find plugin for given target type!");
				return 2;
			}
			LOGGER.debug(String.format("Detected target type %s...", targetTypePlugin.getName()));
			serializer = targetTypePlugin.getSerializer();
			// FIXME Handle "reverseTransformation"
			transformers.add(targetTypePlugin.getTransformer());
		}
		
		if (inPlaceRoundtrip || reverseTransformation) {
			/*
			if (strategy != STRATEGY.ROUNDTRIP) {
				LOGGER.error("--roundtrip set, but --strategy isn't ROUNDTRIP!");
				return -1;
			} else {
			*/
				// Add "return trip" to queue
				transformers.addAll(transformers.reversed());
			//}
		}
		
		if (skipSerialization) {
			serializer = mock(ISerializer.class);
			LOGGER.info("Using mock serializer, won't write target model!");
		}
		
		LOGGER.info("Scheduled transformation order: " + transformers.toString());
		
		return 0;
	}

	private static IPlugin findPlugin(final String type, boolean benchmarking) {
		LOGGER.debug(String.format("Try to find plugin for type %s...", type));
		Optional<IPlugin> plugin = Optional.empty();
		if (!benchmarking) {
			LOGGER.debug("Non-benchmarking mode: Looking for IPlugin extensions...");
			plugin = matchPluginName(type, TraVarTPluginManager::getAvailablePlugins);					
		} else {
			LOGGER.debug("Benchmarking plugins found: " + TraVarTPluginManager.getBenchmarkingPlugins().size());
			TraVarTPluginManager.getBenchmarkingPlugins().values()
					.forEach(e -> LOGGER.debug("Found benchmarking plugin with name " + e.getName()));
			plugin = matchPluginName(type, TraVarTPluginManager::getBenchmarkingPlugins);
		}
		if (plugin.isPresent()) {
			return plugin.get();
		}
		LOGGER.debug(String.format("No matching plugin for given type = %s found.", type));
		return null;
	}
	
	// Utility method for deduplicating if-block in findPlugin
	private static Optional<IPlugin> matchPluginName(final String name, Supplier<Map<String, IPlugin>> supplier) {
		return supplier.get().values().stream().filter(v -> v.getName().equalsIgnoreCase(name)).findFirst();
	}

	private Integer transformDirectory() throws IOException, NotSupportedVariabilityTypeException {
		Set<Path> files = new HashSet<>();
		int counter = 0;
		LOGGER.debug(String.format("Collect files of type %s...", toStringList(deserializer.fileExtensions())));
		for (Object elem : deserializer.fileExtensions()) {
			String extension = (String) elem;
			Set<Path> filesFound = FileUtils.getPathSet(sourcePath, extension);
			files.addAll(filesFound);
			LOGGER.debug(String.format("%d files with extension %s found...", filesFound.size(), extension));
		}
		LOGGER.debug(String.format("%d files to transform...", files.size()));
		for (Path file : files) {
			int result = transformSingleFile(file);
			if (result != 0 && strict) {
				LOGGER.error(String.format("Error during transformation of file %s...", file.getFileName()));
				return result;
			} else {
				// Use counter as the modifier on files.size()
				// Counter will be -1 if it isn't 0
				counter += result;
			}
		}
		if (!strict)
			System.out.println("Unstrict mode: " + (counter + files.size()) + " from " + files.size()
					+ " files could be successfully transformed.");
		return 0;
	}

	private Integer transformSingleFile(final Path file) throws IOException, NotSupportedVariabilityTypeException {
		LOGGER.debug(String.format("Start transforming file %s...", file.getFileName()));
		EventBus bus = null; // Initialize only if benchmarking is required
		List<IBenchmark> activated = new ArrayList<IBenchmark>();
		LOGGER.debug("Attempting to deserizalize " + file.getFileName() + " with " + deserializer.toString());
		
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		Object model;
		Future<Object> maybeModel = executor.submit(() -> {
			return deserializer.deserializeFromFile(file);
		});
		
		try {
			model = maybeModel.get(timeout, TimeUnit.SECONDS);
		} catch (Exception e) {
			if (strict) {
				throw new Error(e);
			} else {
				System.err.println("Deserializer failed! " + file.getFileName() + ", unstrict mode -> continue with next model in batch");
				System.err.println("Deserializer reports: " + e.getClass().descriptorString() + " with " +  e.getMessage() + ", caused by");
				e.printStackTrace();
				return -1;
			}
		}
		
		if (!Objects.isNull(benchmarks) && benchmarks.size() != 0) {
			LOGGER.debug("Benchmarking option non-null (" + benchmarks + "), initializing event bus...");
			// Need to match and activate benchmarks
			bus = new EventBus();
			ServiceLoader<IBenchmark> allBenchmarks = ServiceLoader.load(IBenchmark.class);
			LOGGER.debug("Number of known benchmarks: " + allBenchmarks.stream().count());
			for (IBenchmark benchmark : allBenchmarks) {
				LOGGER.debug("Checking if " + benchmark.getId() + " should be activated...");
				if (benchmarks.contains(benchmark.getId())) {
					LOGGER.debug("Matched benchmark " + benchmark.getId());
					benchmark.activateBenchmark(bus);
					activated.add(benchmark); // Required to read results after transforming
				}
			}
			Collections.<IBenchmark>sort(activated, (IBenchmark b1, IBenchmark b2) -> b1.getId().compareTo(b2.getId()));
			// If ResultsWriter is unset, initialize it
			if (Objects.isNull(rw) && Objects.nonNull(benchmarkResultsFile)) {
				rw = new ResultsWriter(activated, benchmarkResultsFile);
			}
		}

		boolean fromUVL = startUVL;
		var transformationIterator = transformers.iterator();
		var transformationIndex = 0;
		LOGGER.info("Currently scheduled: " + transformers.size() + " transformation steps"); 
		while (transformationIterator.hasNext()) {
			transformationIndex++;
			var transformer = transformationIterator.next();
			LOGGER.info("Is current step intermediate? " + transformationIterator.hasNext()); 
			if (!Objects.isNull(benchmarks)) {
				AbstractBenchmarkingTransformer benchmarkingTransformer = (AbstractBenchmarkingTransformer) transformer;
				if (!reverseTransformation || transformationIndex >= transformers.size()) {
					LOGGER.info("This step transformation will be benchmarked!");
					benchmarkingTransformer.setBus(bus);
					// FIXME Verbosity should be set according to some command-line parameter
					benchmarkingTransformer.setVerbosity(Level.TRACE);
				} else {
					LOGGER.info("This step transformation won't be benchmarked, non-reverse transformation with --reverse-transformation set!");
				}
			}
			try {
				if (fromUVL) {
					model = transformer.transform((FeatureModel) model, file.getFileName().toString(), strategy, transformationIterator.hasNext());
					// intermediate is true as long as there are transformers left in the queue
				} else {
					model = transformer.transform(model, file.getFileName().toString(), strategy, transformationIterator.hasNext());
				}
				fromUVL = !fromUVL;
				// If last transformation was from UVL to target type, next one has to be from target type to UVL
				// Toggle after every transformation (pivot model principle)
			} catch (Exception e) {
				if (strict) {
					// Do not suppress after catching
					throw new Error(e);
				} else {
					System.err.println("Transformation failed: " + file.getFileName() + ", unstrict mode -> continue with next model in batch");
					System.err.println("Transformer reports: " + e.getClass().descriptorString() + " with " +  e.getMessage() + ", caused by");
					e.printStackTrace();
					return -1;
				}
			}
		}
		
		// Declare new final model variable (required for single-thread executor timeout)
		final var modelToBeSerialized = model;

		Path newPath = targetPath.resolve(file.getFileName() + serializer.getFileExtension());

		LOGGER.debug(String.format("Write transformed file to %s...", newPath.toAbsolutePath()));
		LOGGER.debug("Transformation might abort if serializer fails in strict mode!");

		// Reuse deserializer's executor
		Future<Object> serializedPath = executor.submit(() -> {
			return serializer.serializeToFile(modelToBeSerialized, newPath);
		});
		
		try {
			// serializedPath is a dummy feature, we don't care about the returned path
			serializedPath.get(timeout, TimeUnit.SECONDS);			
		} catch (Exception e) {
			if (strict) {
				// Do not suppress after catching
				throw new Error(e);
			} else {
				System.err.println("Serialization failed! " + file.getFileName() + ", unstrict mode -> continue with next model in batch");
				System.err.println("Serializer reports: " + e.getClass().descriptorString() + " with " +  e.getMessage() + ", caused by");
				e.printStackTrace();
				return -1;
			}
		}

		for (IBenchmark benchmark : activated) {
			System.out.println("Benchmark " + benchmark.getId() + " reports: " + benchmark.getResults().toString());
		}

		// If ResultsWriter is set, write results to CSV file
		if (Objects.nonNull(rw)) {
			Map<String, Object> record = new LinkedHashMap<>();
			record.put("filePath", file.getFileName());
			record.put("targetType", targetType);
			for (IBenchmark bench : activated) {
				for (int i = 0; i < bench.getResultsHeader().size(); i++) {
					//LOGGER.debug("Now writing benchmark result for " + bench.getId());
					record.put((String) bench.getResultsHeader().get(i), bench.getResults().get(i));
				}
			}			
			rw.writeResults(record);
		}
		
		return 0;
	}
}
