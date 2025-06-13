package at.jku.cps.travart.core.benchmarking;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.google.common.collect.ObjectArrays;

public class ResultsWriter {

	// FIXME Add more defaultHeaders
	private final String[] defaultHeaders = {"fileName"};
	private final CSVFormat benchmarkResultsFormat;
	private final CSVPrinter csvPrinter;
	
	@SuppressWarnings("rawtypes")
	// Try to work with List arguments although we need to use an Array for specifying CSV headers
	public ResultsWriter(List<IBenchmark> benchmarks, Path targetFile) throws IOException {
		var benchmarkIds = benchmarks.stream().map(e -> e.getId()).toArray(String[]::new);
		benchmarkResultsFormat = CSVFormat.DEFAULT.builder().setHeader(ObjectArrays.concat(defaultHeaders, benchmarkIds, String.class)).get();
		csvPrinter = new CSVPrinter(new FileWriter(targetFile.toString()), benchmarkResultsFormat);
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
}
