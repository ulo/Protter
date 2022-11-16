package at.omasits.proteomics.protter.uniprot;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.features.Feature;
import uk.ac.ebi.kraken.interfaces.uniprot.features.FeatureType;
import uk.ac.ebi.kraken.interfaces.uniprot.features.TopoDomFeature;
import uk.ac.ebi.uniprot.dataservice.client.Client;
import uk.ac.ebi.uniprot.dataservice.client.QueryResult;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.QuerySpec;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtQueryBuilder;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtService;
import at.omasits.proteomics.protter.Prot.Nterm;
import at.omasits.util.Log;
import at.omasits.util.UOUniProtEntry;
import at.omasits.util.Util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class UniProtProvider {
	// init uniprot
	//public static UniProtQueryService upqs = UniProtJAPI.factory.getUniProtQueryService();
	public static UniProtService ups = Client.getServiceFactoryInstance().getUniProtQueryService();

	// a list of all loaded uniprot entries
	protected static LoadingCache<String,UniProtEntry> entries = CacheBuilder.newBuilder().maximumSize(500).build(new UniProtCacheLoader());
	protected static Cache<String,String> unknowns = CacheBuilder.newBuilder().maximumSize(10000).build();
	
	public static UniProtEntry get(String identifier) throws Exception {
		String strCachedError = unknowns.getIfPresent(identifier);
		if (strCachedError!=null) {
			throw new Exception(strCachedError);
		}
		try {
			return entries.get(identifier);
		} catch (Exception e) {
//			if (e.getCause() instanceof RemoteAccessException) {
//				e = new Exception("Could not access UniProt as it looks like its API was updated. Please report to protter@imsb.biol.ethz.ch!");
//				if (Boolean.parseBoolean(Config.get("mail_report_errors", "false"))) {
//					Util.sendMail(Config.get("mail_from"), "UniProt RemoteAccessException on "+identifier, "");
//				}
//			} else {
				unknowns.put(identifier, e.getMessage()); // do not cache remoteaccess exceptions
//			}
			throw e;
		}
	}
	
	// see: http://www.uniprot.org/manual/topo_dom
	public static Set<String> extraTopologies = Sets.newHashSet(
			"Extracellular",
			"Lumenal",
			"Lumenal, thylakoid",
			"Lumenal, vesicle",
			"Lumenal, melanosome",
			"Peroxisomal",
			"Peroxisomal matrix",
			"Vacuolar",
			"Intravacuolar",
			"Virion surface",
			"Vesicular"
	);
	public static Set<String> intraTopologies = Sets.newHashSet(
			"Cytoplasmic",
			"Intravirion",
			"Mitochondrial matrix",
			"Nuclear",
			"Stromal",
			"Mother cell cytoplasmic"
	);
	public static Set<String> betweenTopologies = Sets.newHashSet(
			"Chloroplast intermembrane",
			"Mitochondrial intermembrane",
			"Periplasmic",
			"Perinuclear space",
			"Forespore intermembrane space"
	);

	public static Nterm inferNterm(UOUniProtEntry up, String conformation) throws Exception {
		List<String> topoStrings = getTopoStrings(up, conformation);
		if (topoStrings==null) {
			Log.error("UniProt has no info on N-terminus location.");
			throw new UniprotException("UniProt has no info on N-terminus location.");
		}
		
		String firstTopo = topoStrings.get(0);
		String secondTopo = topoStrings.get(1);
		if (extraTopologies.contains(firstTopo))
			return Nterm.extra;
		else if (intraTopologies.contains(firstTopo))
			return Nterm.intra;
		else if (betweenTopologies.contains(firstTopo)) {
			if (extraTopologies.contains(secondTopo))
				return Nterm.intra;
			else if (intraTopologies.contains(secondTopo))
				return Nterm.extra;
		} else if (firstTopo.equals("")) {
			if (extraTopologies.contains(secondTopo))
				return Nterm.intra;
			else if (intraTopologies.contains(secondTopo))
				return Nterm.extra;
			else if (betweenTopologies.contains(secondTopo)) {
				return Nterm.extra;
			}
		}
		Log.error("unkown UniProt topology: "+firstTopo+" "+secondTopo);
		throw new UniprotException("unkown UniProt topology: "+firstTopo+" "+secondTopo);
	}
	public static Nterm inferNterm(UOUniProtEntry up) throws Exception {
		return inferNterm(up, null);
	}
	public static class UniprotException extends Exception {
		private static final long serialVersionUID = 1773334022458932724L;
		public UniprotException(String message) {
			super(message);
		}
	}
	
	public static List<String> getTopoStrings(UOUniProtEntry up) throws Exception {
		return getTopoStrings(up, null);
	}
	public static List<String> getTopoStrings(UOUniProtEntry up, String conformation) throws Exception {
		List<String> res = new ArrayList<String>();
		SortedSet<String> conformations = new TreeSet<String>();
		String prevConf = "";
		Integer firstTopoDomStart = null;
		Integer firstTmStart = null;
		for (Feature ft : up.getFeatures(FeatureType.TOPO_DOM)) {
			String fDesc = ((TopoDomFeature) ft).getFeatureDescription().getValue();
			if (ft.getFeatureLocation().getEnd() < up.offsetFrom || ft.getFeatureLocation().getStart() > up.offsetTo) {
				prevConf = fDesc.split(";")[0]; // not within selected range
			} else {
				if (fDesc.indexOf(';') >= 0) {
					Matcher m = Pattern.compile("\\s+IN\\s(.+)\\sCONFORMATION").matcher(fDesc.substring(fDesc.indexOf(';')+1).toUpperCase().replace("NOTE=", ""));
					if (m.matches()) {
						String fConf = m.group(1);
						conformations.add(fConf);
						if (conformation != null && ! fConf.equalsIgnoreCase(conformation))
							continue; // if there is a conformation which does NOT match, then skip this domain 
					}
				}
				if (firstTopoDomStart==null) 
					firstTopoDomStart = ft.getFeatureLocation().getStart();
				res.add(fDesc.split(";")[0]);
			}
		}
		if (conformation==null && conformations.size()>0) {
			Log.warn("UniProt provides multiple topological conformations for '"+up.getUniProtId().getValue()+"'.");
			throw new Exception("UniProt provides multiple topological conformations for '"+up.getUniProtId().getValue()+"':\n"+Util.join(conformations, "\n"));
		}
		
		if (res.size()==0)
			return null;
		else if (res.size()==1)
			res.add(prevConf);
		// if the first TOPO_DOM comes after a FeatureType.TRANSMEM then this TOPO_DOM is actually second topo
		for (Feature ft : up.getFeatures(FeatureType.TRANSMEM)) {
			if (ft.getFeatureLocation().getStart() <= up.offsetTo && ft.getFeatureLocation().getEnd() >= up.offsetFrom) {
				firstTmStart = ft.getFeatureLocation().getStart();
				if (firstTmStart < firstTopoDomStart)
					return Lists.newArrayList(res.get(1), res.get(0));
				else
					break;
			}
		}
		return Lists.newArrayList(res.get(0), res.get(1));
	}
	
	protected static class UniProtCacheLoader extends CacheLoader<String,UniProtEntry> {
		@Override
		public UniProtEntry load(String identifier) throws Exception {
			if (identifier.contains("|")) // e.g: sp|P68716|VG07_VACCW
				identifier = identifier.split("\\|")[1];
			List<String> foundIds = new ArrayList<String>();
			QueryResult<UniProtEntry> results;
			UniProtEntry up = null;
			
			ups.start();
			//Query query = UniProtQueryBuilder.buildExactMatchIdentifierQuery(identifier.toUpperCase());
			//EntryIterator<UniProtEntry> results = upqs.getEntryIterator(query);
			
			//QueryResult<UniProtEntry> entries = ups.getEntries( UniProtQueryBuilder.id("BST2_HUMAN") );
			//while (entries.hasNext()) {
			//	UniProtEntry entry = entries.next();
			//	System.out.println(entry.getUniProtId().getValue() + "\t" + entry.getPrimaryUniProtAccession().getValue());
			//}
			
			
			up = ups.getEntry(identifier.toUpperCase());
			
			if (up != null) {
				foundIds.add(up.getPrimaryUniProtAccession().getValue());
			} else {
				results = ups.getEntries(UniProtQueryBuilder.anyAccession(identifier.toUpperCase()), EnumSet.of(QuerySpec.WithIsoform));
				if (!results.hasNext())
					results = ups.getEntries(UniProtQueryBuilder.id(identifier.toUpperCase()), EnumSet.of(QuerySpec.WithIsoform));
				while (results.hasNext()) {
					up = results.next();
					foundIds.add(up.getPrimaryUniProtAccession().getValue());
				}
			}
			
			if (foundIds.size()==0) {
				// no exactMatchIdentifier -> try protein name / gene name / dbase cross refs
				results = ups.getEntries(UniProtQueryBuilder.gene(identifier.toUpperCase()));
				if (!results.hasNext())
					results = ups.getEntries(UniProtQueryBuilder.xref(identifier.toUpperCase()));
				if (!results.hasNext())
					results = ups.getEntries(UniProtQueryBuilder.proteinName(identifier.toUpperCase()));
				while (results.hasNext()) {
					up = results.next();
					String acc = (up.getUniProtId()!=null) ? (up.getUniProtId().getValue()) : ("");
					String gene = (up.getGenes().size()>0) ? (up.getGenes().get(0).getGeneName().getValue()) : ("");
					String organism = (up.getOrganism()!=null) ? (up.getOrganism().getCommonName().getValue()) : ("");
					foundIds.add(acc + " (" + gene + "; " + organism + ")");
					if (foundIds.size()>=100) // stop after 100 entries
						break;
				}
			}
			
			if (foundIds.size()==1 && foundIds.get(0).endsWith("-1")) // instead of Q13740-1 we want Q13740
				up = ups.getEntry(foundIds.get(0).substring(0, foundIds.get(0).length()-2));
						
			ups.stop();
			
			if (foundIds.size()==0) {
				Log.errorThrow("No UniProt entry found for '"+identifier+"'.");
			} else if (foundIds.size()>1) { // more than 1 hit
				Log.warn("Found multiple UniProt entries for '"+identifier+"'.");
				throw new Exception("Found multiple UniProt entries for '"+identifier+"':\n"+Util.join(foundIds, "\n"));
			}
			
			return up;
		}
	}
}
