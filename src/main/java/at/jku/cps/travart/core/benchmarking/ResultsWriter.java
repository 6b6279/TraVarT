package at.jku.cps.travart.core.benchmarking;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ObjectArrays;

public class ResultsWriter {
	
	private static final Logger LOGGER = LogManager.getLogger(ResultsWriter.class);

	// FIXME Add more defaultHeaders
	private final String[] defaultHeaders = {"fileName", "targetType", "result", "deserializationTime"};
	private final CSVFormat benchmarkResultsFormat;
	private final CSVPrinter csvPrinter;
	private final Path resultsFile;
	
	public Path getResultsFile() {
		return resultsFile;
	}

	// Try to work with List arguments although we need to use an Array for specifying CSV headers
	public ResultsWriter(List<IBenchmark> benchmarks, Path targetFile) throws IOException {
		resultsFile = targetFile;
		boolean exists = targetFile.toFile().exists();
		if (exists) {
			LOGGER.debug("Benchmark writeback file already exists, the transformer will skip already benchmarked files.");
		}
		String[] benchmarkColumns = (String[]) benchmarks.stream().<String>flatMap(e -> e.getResultsHeader().stream()).toArray(String[]::new);
		benchmarkResultsFormat = CSVFormat.DEFAULT.builder().setHeader(ObjectArrays.concat(defaultHeaders, benchmarkColumns, String.class)).get();
		csvPrinter = new CSVPrinter(new FileWriter(targetFile.toString(), true), 
				exists ?
						benchmarkResultsFormat.withSkipHeaderRecord() : benchmarkResultsFormat);
	}
	
	public void writeResults(Map<String, Object> dict) throws IOException {
		// FIXME Why use a map when printRecord can't match headers?
		csvPrinter.printRecord(dict.values());
	}
	
	public void dispose() throws IOException {
		csvPrinter.close();
	}
	
	public List<String> getRecordTemplate() {
		return Arrays.asList(benchmarkResultsFormat.getHeader());
	}
	
	// Users should check before inserting duplicates with this method
	public boolean entryAlreadyExists(String column, String key, Path targetFile) {
		try (FileReader r = new FileReader(targetFile.toString())) {
			List<CSVRecord> records = benchmarkResultsFormat.parse(r).getRecords();
			LOGGER.debug("Benchmark writeback file already has " + records.size() + " entries!");
			for (CSVRecord rec : records) {
				if (key.equals(rec.get(column))) {
					return true;
				}
			}
		} catch (IOException e) {
			return false;
		}
		return false;
	}
}
