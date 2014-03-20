package at.omasits.proteomics.protter.ranges;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.omasits.util.Config;
import at.omasits.util.Log;
import at.omasits.util.Util;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;

public class RangeParserPeptideCutter implements IRangeParser {
	private static HashMap<List<String>,List<Range>> buffer = new HashMap<List<String>, List<Range>>();

	@Override
	public boolean matches(String rangeString) {
		return rangeString.toLowerCase().startsWith("peptidecutter");
	}

	@Override
	public List<? extends Range> parse(String rangeString, String sequence, UniProtEntry up, Map<String,String> parms) throws Exception {
		List<Range> cutPositions = new ArrayList<Range>();
		String enzyme = rangeString.substring(14);
		
		if (buffer.containsKey(Arrays.asList(sequence, enzyme)))
			cutPositions = buffer.get(Arrays.asList(sequence, enzyme));
		else {
			try {
				StringBuilder strUrl = new StringBuilder(Config.get("peptideCutter","http://web.expasy.org/cgi-bin/peptide_cutter/peptidecutter.pl"));
				strUrl.append("?alphtable=alphtable&cleave_number=all");
				strUrl.append("&protein=").append(sequence);
				strUrl.append("&enzyme=").append(enzyme);
		
				URL url = new URL(strUrl.toString());
				Log.debug("loading "+url);
				BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
				String line;
				while ((line = in.readLine()) != null) {
					if (line.startsWith("<tr><td><A HREF=\"/peptide_cutter/peptidecutter_enzymes.html")) {
						String strCutPositions = Util.substringBetweenStrings(line, "</td><td>", "</td><tr>");
						for (String strCutPosition : strCutPositions.split(" ")) {
							int cutPos = Integer.valueOf(strCutPosition);
							cutPositions.add(new Range(cutPos, cutPos));
						}
						break;
					}
				}
				buffer.put(Arrays.asList(sequence, enzyme), cutPositions);
			} catch (Exception e) {
				Log.errorThrow("Could not access PeptideCutter. Check the internet connection and the ProtterServer configuration file.");
			}
		}
		return cutPositions;
	}

}
