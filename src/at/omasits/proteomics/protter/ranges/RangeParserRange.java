package at.omasits.proteomics.protter.ranges;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;


public class RangeParserRange implements IRangeParser {

	@Override
	public boolean matches(String rangeString) {
		return rangeString.matches("\\d+-\\d+");
	}

	@Override
	public List<Range> parse(String rangeString, String sequence, UniProtEntry up, Map<String,String> parms) {
		int begin = Integer.valueOf(rangeString.substring(0, rangeString.indexOf('-')));
		int end = Integer.valueOf(rangeString.substring(rangeString.indexOf('-')+1, rangeString.length()));
		return Arrays.asList(new Range(begin, end));
	}

}
