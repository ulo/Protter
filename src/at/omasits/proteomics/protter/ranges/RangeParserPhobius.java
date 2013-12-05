package at.omasits.proteomics.protter.ranges;

import java.util.List;
import java.util.Map;

import at.omasits.proteomics.protter.phobius.PhobiusProvider;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;

public class RangeParserPhobius implements IRangeParser {

	@Override
	public boolean matches(String rangeString) {
		return rangeString.toLowerCase().startsWith("phobius");
	}

	@Override
	public List<Range> parse(String rangeString, String sequence, UniProtEntry up, String tag, Map<String,String> parms) throws Exception {
		String feature = rangeString.toUpperCase().substring(8);
		
		Map<String, List<Range>> features = PhobiusProvider.getPhobius(sequence);
		if (features.containsKey(feature))
			return features.get(feature);
		else
			return null;
	}
}
