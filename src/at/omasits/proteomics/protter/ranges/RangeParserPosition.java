package at.omasits.proteomics.protter.ranges;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import at.omasits.util.UOUniProtEntry;

public class RangeParserPosition implements IRangeParser {

	@Override
	public boolean matches(String rangeString) {
		return rangeString.matches("\\d+"); // single position
	}

	@Override
	public List<Range> parse(String rangeString, String sequence, List<UOUniProtEntry> up, Map<String,String> parms) {
		int pos = Integer.valueOf(rangeString);
		return Arrays.asList(new Range(pos, pos));
	}

}
