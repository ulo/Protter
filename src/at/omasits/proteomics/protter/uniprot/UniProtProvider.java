package at.omasits.proteomics.protter.uniprot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.remoting.RemoteAccessException;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.features.Feature;
import uk.ac.ebi.kraken.interfaces.uniprot.features.FeatureType;
import uk.ac.ebi.kraken.interfaces.uniprot.features.TopoDomFeature;
import uk.ac.ebi.kraken.uuw.services.remoting.EntryIterator;
import uk.ac.ebi.kraken.uuw.services.remoting.Query;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtJAPI;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryBuilder;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryService;
import at.omasits.proteomics.protter.Prot.Nterm;
import at.omasits.util.Config;
import at.omasits.util.Log;
import at.omasits.util.Util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class UniProtProvider {
	// init uniprot
	public static UniProtQueryService upqs = UniProtJAPI.factory.getUniProtQueryService();

	// a list of all loaded uniprot entries
	protected static LoadingCache<String,UniProtEntry> entries = CacheBuilder.newBuilder().maximumSize(500).build(new UniProtCacheLoader());
	protected static Cache<String,String> unknowns = CacheBuilder.newBuilder().maximumSize(10000).build();
	//public static Map<String,UniProtEntry> entries = new HashMap<String,UniProtEntry>();
	
	public static UniProtEntry get(String identifier) throws Exception {
		String strCachedError = unknowns.getIfPresent(identifier);
		if (strCachedError!=null) {
			throw new Exception(strCachedError);
		}
		try {
			return entries.get(identifier);
		} catch (Exception e) {
			if (e.getCause() instanceof RemoteAccessException) {
				e = new Exception("Could not access UniProt as it looks like its API was updated. Please report to protter@imsb.biol.ethz.ch!");
				Util.sendMail(Config.get("mail_from"), "UniProt RemoteAccessException on "+identifier, "");
			}
			unknowns.put(identifier, e.getMessage());
			throw e;
		}
	}
	
//	public static UniProtEntry load(String identifier) throws Exception {
//		
//	}
	
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

	public static Nterm inferNterm(UniProtEntry up) throws Exception {
		List<String> topoStrings = getTopoStrings(up);
		if (topoStrings==null)
			Log.errorThrow("UniProt has no info on N-terminus location.");
		
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
		Log.errorThrow("unkown UniProt topology: "+firstTopo+" "+secondTopo);
		
		return null;
	}
	
	public static List<String> getTopoStrings(UniProtEntry up) throws Exception {
		LinkedHashSet<String> res = new LinkedHashSet<String>();
		for (Feature ft : up.getFeatures(FeatureType.TOPO_DOM)) {
			res.add(((TopoDomFeature) ft).getFeatureDescription().getValue());
		}
		List<String> lst = Lists.newArrayList(res);
		if (lst.size()==0)
			return null;
		else if (lst.size()==1)
			lst.add("");
		// if the first TOPO_DOM comes after a FeatureType.TRANSMEM then this TOPO_DOM is actually second topo
		if (up.getFeatures(FeatureType.TRANSMEM).size()>0) {
			int firstTmStart = up.getFeatures(FeatureType.TRANSMEM).iterator().next().getFeatureLocation().getStart();
			int firstTopoDomStart = up.getFeatures(FeatureType.TOPO_DOM).iterator().next().getFeatureLocation().getStart();
			if (firstTmStart < firstTopoDomStart)
				return Lists.newArrayList(lst.get(1), lst.get(0));
		}
		return Lists.newArrayList(lst.get(0), lst.get(1));
	}
	
	protected static class UniProtCacheLoader extends CacheLoader<String,UniProtEntry> {
		@Override
		public UniProtEntry load(String identifier) throws Exception {
			if (identifier.contains("|")) // e.g: sp|P68716|VG07_VACCW
				identifier = identifier.split("\\|")[1];
			//if (!entries.containsKey(identifier)) {
				List<String> foundIds = new ArrayList<String>();
				UniProtEntry up = null;
				
				Query query = UniProtQueryBuilder.buildExactMatchIdentifierQuery(identifier.toUpperCase());
				EntryIterator<UniProtEntry> results = upqs.getEntryIterator(query);
				for (UniProtEntry e : results) {
					foundIds.add(e.getPrimaryUniProtAccession().getValue());
					up = e;
				}
				if (foundIds.size()==0) {
					// no exactMatchIdentifier -> try dbase cross refs
					query = UniProtQueryBuilder.buildDatabaseCrossReferenceQuery(identifier);
					results = upqs.getEntryIterator(query);
					for (UniProtEntry e : results) {
						//String id = e.getPrimaryUniProtAccession().getValue();
						String acc = (e.getUniProtId()!=null) ? (e.getUniProtId().getValue()) : ("");
						String gene = (e.getGenes().size()>0) ? (e.getGenes().get(0).getGeneName().getValue()) : ("");
						String organism = (e.getOrganism()!=null) ? (e.getOrganism().getCommonName().getValue()) : ("");
						foundIds.add(acc + " (" + gene + "; " + organism + ")");
						up = e;
						if (foundIds.size()>=100) // stop after 100 entries
							break;
					}
				}
				if (foundIds.size()==0) {
					Log.errorThrow("No UniProt entry found for '"+identifier+"'.");
				} else if (foundIds.size()>1) { // more than 1 hit
					Log.warn("Found multiple UniProt entries for '"+identifier+"'.");
					throw new Exception("Found multiple UniProt entries for '"+identifier+"':\n"+Util.join(foundIds, "\n"));
				} else {
					//entries.put(identifier, up);
					//if (! identifier.equals(foundIds.get(0)) )
					//		entries.put(foundIds.get(0), up);
				}
			//}
			//return entries.get(identifier);
			return up;
		}
	}
}
