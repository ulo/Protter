package at.omasits.proteomics.protter.ranges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import at.omasits.util.UOUniProtEntry;

public class RangeParserExperiment implements IRangeParser {

	@Override
	public boolean matches(String rangeString) {
		return rangeString.toLowerCase().startsWith("ex.");
	}

	@Override
	public List<? extends Range> parse(String rangeString, String sequence, List<UOUniProtEntry> up, Map<String,String> parms) throws Exception {
		String target = rangeString.substring(3);
		Map<String,String> parmsLC = new HashMap<String, String>();
		for(Entry<String,String> e : parms.entrySet())
			parmsLC.put(e.getKey().toLowerCase(), e.getValue());
		
		if (target.toLowerCase().equals("modall")) {
			// all modifications
			List<Range> allMods = new ArrayList<Range>();
			for(Entry<String,String> e : parmsLC.entrySet()) {
				if (e.getKey().startsWith("mod"))
					allMods.addAll(Range.parseMultiRangeString(e.getValue(), sequence, up, "", parms));
			}
			return allMods;
		} else {
			if (parmsLC.containsKey(target.toLowerCase()))
				return Range.parseMultiRangeString(parmsLC.get(target.toLowerCase()), sequence, up, "", parms);
		}
		return new ArrayList<Range>();
	}

}
