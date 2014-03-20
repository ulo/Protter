package at.omasits.proteomics.protter.ranges;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.omasits.util.Log;
import at.omasits.util.Util;


import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;

public class RangeParserPeptideAtlas implements IRangeParser {
	protected static Map<String,Set<String>> paHex = new HashMap<String,Set<String>>();
	protected static String currentBuild;

	@Override
	public boolean matches(String rangeString) {
		return rangeString.toLowerCase().startsWith("pa.");
	}

	@Override
	public List<? extends Range> parse(String rangeString, String sequence, UniProtEntry up, Map<String,String> parms) throws Exception {
		String build = rangeString.substring(3);
		if (! build.equals(currentBuild)) {
			paHex = new HashMap<String,Set<String>>();
			File buildFile = new File("peptideAtlas",build+".fasta");
			if (!buildFile.exists())
				throw new Exception("PeptideAtlas build '"+build+"' not found.");
			BufferedReader in = Util.reader(buildFile);
			String line;
			int i=0;
			while ((line = in.readLine()) != null) {
				if (line.charAt(0)=='>') {
					line = in.readLine();
					String hex = line.substring(0,6);
					if (!paHex.containsKey(hex))
						paHex.put(hex, new HashSet<String>());
					paHex.get(hex).add(line);
					i++;
				}
			}
			Log.info("Loaded PeptideAtlas Build '"+build+"' with "+i+" peptides matched to "+paHex.size()+" hexapeptides");
		}
		
		List<Range> ranges = new ArrayList<Range>();
		for (int j=0; j<=sequence.length()-6; j++) {
			String hex = sequence.substring(j, j+6);
			if (paHex.containsKey(hex)) {
				for (String cand : paHex.get(hex)) {
					if ((sequence.length()>=j+cand.length()) && (sequence.substring(j, j+cand.length()).equals(cand)))
						ranges.add(new Range(j+1, j+cand.length()));
				}
			}
		}
		return ranges;
	}

}
