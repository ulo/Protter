package at.omasits.proteomics.protter.ranges;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.features.CarbohydFeature;
import uk.ac.ebi.kraken.interfaces.uniprot.features.Feature;
import uk.ac.ebi.kraken.interfaces.uniprot.features.FeatureType;
import uk.ac.ebi.kraken.interfaces.uniprot.features.TransmemFeature;

public class RangeParserUniProt implements IRangeParser {

	@Override
	public boolean matches(String rangeString) {
		return rangeString.toLowerCase().startsWith("up");
	}

	@Override
	public List<? extends Range> parse(String rangeString, String sequence, UniProtEntry up, Map<String,String> parms) throws Exception {
		List<Range> ranges = new ArrayList<Range>();
		if (up == null)
			return ranges; // no error, just ignore UP styles
			//Log.errorThrow("UniProt range requested, but no UniProt entry provided!");
		String[] rangeElems = rangeString.toUpperCase().split("\\."); // e.g: UP.CARBOHYD.NITROGEN
		String upFeature = rangeElems[1];
		String upSubFeature = (rangeElems.length>2) ? rangeElems[2] : null;
		for (Feature f : up.getFeatures(FeatureType.valueOf(upFeature))) {
			if (f.getFeatureLocation().getStart()==-1 || f.getFeatureLocation().getEnd()==-1)
				continue;
			if (upFeature.equals("CARBOHYD") && upSubFeature != null) {
				String linkTyp = ((CarbohydFeature)f).getCarbohydLinkType().name();
				if (upSubFeature.equals(linkTyp))
					ranges.add(new Range(f.getFeatureLocation().getStart(), f.getFeatureLocation().getEnd()));
			} else if (upFeature.equals("DISULFID")) {
				ranges.add(new Range(f.getFeatureLocation().getStart(), f.getFeatureLocation().getStart()));
				ranges.add(new Range(f.getFeatureLocation().getEnd(), f.getFeatureLocation().getEnd()));
			} else if ((upFeature.equals("TRANSMEM") || upFeature.equals("INTRAMEM")) && upSubFeature != null) {
				String fDesc = ((TransmemFeature) f).getFeatureDescription().getValue().toUpperCase();
				if (fDesc.indexOf(';')>=0) {
					fDesc = fDesc.substring(fDesc.indexOf(';')+1).replace("NOTE=", "");
					Matcher m = Pattern.compile("\\s+IN\\s(.+)\\sCONFORMATION").matcher(fDesc);
					if (m.matches()) {
						String conformation = m.group(1);
						if ( ! conformation.equals(upSubFeature))
							continue; // if there is a conformation which does NOT match the subFeature, then skip this domain 
					}
				}
				ranges.add(new Range(f.getFeatureLocation().getStart(), f.getFeatureLocation().getEnd()));
			} else {
				ranges.add(new Range(f.getFeatureLocation().getStart(), f.getFeatureLocation().getEnd()));
			}
		}
		return ranges;
	}
}
