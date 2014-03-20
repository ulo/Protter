package at.omasits.proteomics.protter.ranges;

import java.util.List;
import java.util.Map;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;

public interface IRangeParser {
	public boolean matches(String rangeString);
	public List<? extends Range> parse(String rangeString, String sequence, UniProtEntry up, Map<String,String> parms) throws Exception;
}
