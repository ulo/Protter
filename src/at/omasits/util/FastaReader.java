package at.omasits.util;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

public class FastaReader implements LineProcessor<Map<String,String>> {
	Function<String,String> headerProcessor;
	Map<String,String> m = Maps.newLinkedHashMap();
	String header = null;
	StringBuilder sequence = new StringBuilder(256);
	
	
	public FastaReader(Function<String, String> headerProcessor) {
		this.headerProcessor = headerProcessor;
	}

	@Override
	public Map<String,String> getResult() {
		processEntry();
		return m;
	}

	@Override
	public boolean processLine(String line) throws IOException {
		if (line.startsWith(">")) {
			if (header != null) { // process previous entry
				processEntry();
			}
			header = headerProcessor.apply(line.substring(1));
			sequence.setLength(0);
		} else if ( ! line.startsWith("#")) {
			sequence.append( CharMatcher.WHITESPACE.removeFrom(line) );
		}
		return true;
	}

	private void processEntry() {
		m.put(header, sequence.toString());
	}
	
	public static Map<String,String> readFile(File file, Function<String,String> headerProcessor) throws IOException {
		return Files.readLines(file, Charsets.UTF_8, new FastaReader(headerProcessor));
	}
	
	public static final Function<String,String> headerUpToFirstWhitespace = new Function<String, String>() {
		@Override
		public String apply(String header) {
			return Util.substringUpTo(header, CharMatcher.WHITESPACE);
		}
	};
	public static final Function<String,String> headerComplete = new Function<String, String>() {
		@Override
		public String apply(String header) {
			return header;
		}
	};
}
