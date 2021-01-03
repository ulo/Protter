package at.omasits.proteomics.protter.phobius;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.omasits.proteomics.protter.Prot.Nterm;
import at.omasits.proteomics.protter.ranges.Range;
import at.omasits.util.Config;
import at.omasits.util.Log;


public class PhobiusProvider {
	// a list of all loaded phobius predictions
	public static Map<String, Map<String,List<Range>>> phobiusPredictions = new HashMap<String, Map<String,List<Range>>>();
	public static final String[] phobiusServers = Config.get("phobiusServer", "https://phobius.sbc.su.se/cgi-bin/predict.pl;https://phobius.binf.ku.dk/cgi-bin/predict.pl").split(";");
	static String phobiusServer = phobiusServers[0]; //Config.get("phobiusServer", "http://phobius.sbc.su.se/cgi-bin/predict.pl");
	
	public static Map<String, List<Range>> getPhobius(String sequence) throws Exception {
		if (!phobiusPredictions.containsKey(sequence.toUpperCase())) {
			Map<String, List<Range>> features = new HashMap<String, List<Range>>();
			URL url = new URL(phobiusServer);
			String urlParameters = "format=short&protseq="+sequence.toUpperCase();
			try {
				Log.debug("PhobiusProvider: loading "+url);
//				BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
				URLConnection con = url.openConnection();
				con.setDoOutput(true);
				con.setConnectTimeout(10000);
				con.setReadTimeout(10000); //10 seconds
				OutputStreamWriter post = new OutputStreamWriter(con.getOutputStream());
				post.write(urlParameters);
				post.flush();

				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				Pattern p = Pattern.compile("UNNAMED\\s+(.+?)\\s+(.+?)\\s+(.+)");
				String line;
				while((line = in.readLine()) != null) {
					Matcher m = p.matcher(line);
					if (m.matches()) {
						// *not used* int tmCount = Integer.valueOf(m.group(1));
						boolean sigPep = (m.group(2).equals("Y") ? true : false);
						String phob = m.group(3);
						
//						if ((tmCount>0) || (sigPep)) { //Note: phobius still classifies the proteins to intra-/extracellular...
							// there are features to annotate!
							if (sigPep) { //"n5-16c34/" 1-4: N-REGION. 5-16: H-REGION. 17-34: C-REGION.
								Matcher mSig = Pattern.compile("n\\d+-\\d+c(\\d+)/(\\d+)(.+)").matcher(phob);
								if (mSig.matches()) {
									int signalEnd = Integer.valueOf(mSig.group(1));
									features.put("SP", Arrays.asList( new Range(1, signalEnd) ));
									int start = Integer.valueOf(mSig.group(2));
									phob = (start-1)+mSig.group(3); //remaining phobius prediction
								} else {
									Log.errorThrow("Phobius signal peptide specified but not found in: "+line);
								}
							} else {
								phob = "0"+phob; //"o306-324i390-411o" => "0o306-324i390-411o"
							}
							phob = phob+(sequence.length()+1); //"0o306-324i390-411o" => "0o306-324i390-411o500"
							
							Integer start=null;
							Integer end=null;
							Matcher mTM = Pattern.compile("(-)?(\\d+)([oi])(\\d+)").matcher(phob);
							while(mTM.find()) { // scan through phobius predicted topology domains
								if (mTM.group(1) != null) { // annotate transmembrane stretch
									start = end+1;
									end = Integer.valueOf(mTM.group(2));
									if ( ! features.containsKey("TM"))
										features.put("TM", new ArrayList<Range>());
									features.get("TM").add(new Range(start, end));
								}
								// annotate cytoplasmic / non-cytoplasmic topology domain
								start = Integer.valueOf(mTM.group(2)) + 1;
								end = Integer.valueOf(mTM.group(4)) - 1;
								
								String key; 	
								if (mTM.group(3).equals("i")) {
									key = "C";
								} else if (mTM.group(3).equals("o")) {
									key = "NC";
								} else {
									Log.errorThrow("Invalid Phobius format in '"+phob+"'");
									return null;
								}
								if ( ! features.containsKey(key))
									features.put(key, new ArrayList<Range>());
								features.get(key).add(new Range(start, end));
							}
//						} // end if: (tmCount>0) || (sigPep)
						
						break;
					}
				}
				phobiusPredictions.put(sequence, features);
				in.close();
				post.close();
			} catch (IOException e) {
				Log.warn("Current Phobius server '"+phobiusServer+"' is offline.");
				// try all mirrors before giving up!
				for (String server : phobiusServers) {
					if (isPhobiusServerOnline(server)) {
						phobiusServer = server;
						Log.info("Switching to Phobius server '"+phobiusServer+"'.");
						return getPhobius(sequence); // try again with online server
					}
				}
				// none worked
				Log.errorThrow("Sorry, all Phobius servers seem to be offline...");
				//Log.errorThrow("Could not access Phobius server at '"+url+"'.");
			}
		}
		
		return phobiusPredictions.get(sequence);
	}
	
	public static Nterm inferNterm(String sequence) throws Exception {
		Map<String, List<Range>> features = getPhobius(sequence);
		if (features.containsKey("SP"))
			return Nterm.extra;
		if (features.containsKey("C") && features.containsKey("NC")) {
			if (features.get("C").get(0).from < features.get("NC").get(0).from)
				return Nterm.intra;
			else 
				return Nterm.extra; 
		}
		if (features.containsKey("C"))
			return Nterm.intra;
		if (features.containsKey("NC"))
			return Nterm.extra;
		Log.errorThrow("Phobius does not know topology for the given sequence.");
		return null;
	}
	
	public static boolean isPhobiusServerOnline(String server) {
		try {
			URLConnection con = new URL(server+"?format=short&protseq=A").openConnection();
			con.setConnectTimeout(1000);
			con.setReadTimeout(1000); //1 second
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			while(in.readLine() != null) {}
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}
