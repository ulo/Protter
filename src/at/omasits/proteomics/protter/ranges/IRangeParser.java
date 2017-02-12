package at.omasits.proteomics.protter.ranges;

import java.util.List;
import java.util.Map;

import at.omasits.util.UOUniProtEntry;

public interface IRangeParser {
	public boolean matches(String rangeString);
	public List<? extends Range> parse(String rangeString, String sequence, List<UOUniProtEntry> up, Map<String,String> parms) throws Exception;
}
