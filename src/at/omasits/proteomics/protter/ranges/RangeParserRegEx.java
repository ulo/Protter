package at.omasits.proteomics.protter.ranges;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;


public class RangeParserRegEx implements IRangeParser {

	@Override
	public boolean matches(String rangeString) {
		Pattern.compile(rangeString);
		return true;
	}

	@Override
	public List<Range> parse(String rangeString, String sequence, UniProtEntry up, Map<String,String> parms) {
		// try to interpret as regex; range whole match or - if capturing groups are specified - only the groups
		List<Range> ranges = new ArrayList<Range>();
		Matcher m = Pattern.compile(rangeString).matcher(sequence);
		int position = 0;
		while(m.find(position)) {
			position = m.start()+1;
			if (m.groupCount()>0) {
				for (int i=1; i<=m.groupCount(); i++) {
					int begin = m.start(i)+1;
					int end = m.end(i);
					ranges.add(new Range(begin, end));
				}
			} else {
				int begin = m.start()+1;
				int end = m.end();
				ranges.add(new Range(begin, end));
			}
		}
		return ranges;
	}

}