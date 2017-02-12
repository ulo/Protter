package at.omasits.proteomics.protter.ranges;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import at.omasits.util.Log;
import at.omasits.util.UOUniProtEntry;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class RangeParserHumanProteinAtlas implements IRangeParser {
	protected static LoadingCache<String,List<String>> antigenEntries = CacheBuilder.newBuilder().maximumSize(1000).build(new HPACacheLoader());
	
	@Override
	public boolean matches(String rangeString) {
		return rangeString.toLowerCase().startsWith("hpa");
	}

	@Override
	public List<? extends Range> parse(String rangeString, String sequence, List<UOUniProtEntry> up, Map<String,String> parms) throws Exception {
		if (up.isEmpty())
			return new ArrayList<Range>(); // no error, just ignore
		
		String feature = rangeString.toUpperCase().substring(4);
		
		if (feature.equals("ANTIGEN")) {
			List<Range> ranges = new ArrayList<Range>();
			for (UOUniProtEntry upEntry : up) {
				String upID = upEntry.getPrimaryUniProtAccession().getValue();
				List<String> antigens = antigenEntries.get(upID);
				for (String antigen : antigens) {
					int start = sequence.toUpperCase().indexOf(antigen) + 1;
					Range range = new Range(start, start+antigen.length()-1);
					ranges.add(range);
				}
			}
			return ranges;
		} else {
			return new ArrayList<Range>();
		}
	}

	protected static class HPACacheLoader extends CacheLoader<String,List<String>> {
		@Override
		public List<String> load(String upID) throws Exception {
			try {
				// http://www.proteinatlas.org/search_download.php?format=xml&query1=P16070
				URL url = new URL("http://www.proteinatlas.org/search_download.php?format=xml&query1="+upID);
				Log.debug("loading "+url);
				
				URLConnection con = url.openConnection();
				con.setConnectTimeout(10000);
				con.setReadTimeout(10000); //10 seconds
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				
				List<String> antigens = new ArrayList<String>();
				String line;
				while ((line = in.readLine()) != null) {
					if (line.indexOf("<antigenSequence>") >= 0) {
						String antigen = line.substring(line.indexOf("<antigenSequence>")+17, line.indexOf("</antigenSequence>")).toUpperCase();
						if (antigen.length()>0)
							antigens.add(antigen);
					}
				}
				
				in.close();
				return antigens;
			} catch (IOException e) {
				Log.errorThrow("Sorry, the Human Protein Atlas seems to be offline...");
			}
			return null;
		}
	}
}
