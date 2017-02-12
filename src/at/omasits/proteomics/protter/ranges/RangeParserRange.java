package at.omasits.proteomics.protter.ranges;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import at.omasits.util.UOUniProtEntry;

public class RangeParserRange implements IRangeParser {

	@Override
	public boolean matches(String rangeString) {
		return rangeString.matches("\\d+-(\\d+)?");
	}

	@Override
	public List<Range> parse(String rangeString, String sequence, List<UOUniProtEntry> up, Map<String,String> parms) {
		int begin = Integer.valueOf(rangeString.substring(0, rangeString.indexOf('-')));
		int end = sequence.length();
		if ( ! rangeString.endsWith("-"))
			end = Integer.valueOf(rangeString.substring(rangeString.indexOf('-')+1, rangeString.length()));
		return Arrays.asList(new Range(begin, end));
	}

}
