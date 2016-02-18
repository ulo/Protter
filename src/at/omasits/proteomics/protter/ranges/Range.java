package at.omasits.proteomics.protter.ranges;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import at.omasits.util.Log;


import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;

public class Range implements Comparable<Range> {
	public int from;
	public int to;
	
	public Range(int from, int to) {
		this.from = from;
		this.to = to;
	}
	
	public int length() {
		return to-from+1;
	}
	
	public boolean validate(int maxTo) {
		return (from<=to) && (to<=maxTo);
	}
	
	@Override
	public String toString() {
		return from+"-"+to;
	}
	@Override
	public int compareTo(Range that) {
		if (this.from==that.from)
			return this.to - that.to;
		else
			return this.from - that.from;
	}
	
	
	public static List<? extends Range> parseMultiRangeString(String multiRangeString, String sequence, UniProtEntry up, String tag, Map<String,String> parms) throws Exception {
		List<Range> ranges = new ArrayList<Range>();
		multiRangeString = multiRangeString.replaceAll("(\\{\\d+),", "$1;"); // replace {N,} {N,M} with {N;} {N;M} 
		String[] arr = multiRangeString.split(",");
		for(String rangeString : arr) {
			if (rangeString.length()==0)
				continue;
			rangeString = rangeString.replaceAll("(\\{\\d+);", "$1,"); // replace back to comma
			IRangeParser rp = RangeParserProvider.getMatchingRangeProvider(rangeString);
			List<? extends Range> newParsedRanges = rp.parse(rangeString, sequence, up, parms);
			if (newParsedRanges != null && newParsedRanges.size()>0)
				ranges.addAll(newParsedRanges);
		}
		List<? extends Range> condensedRanges = validateAndCondense(ranges, sequence);
		
		if (tag.equalsIgnoreCase("im")) {
			// condense intramem ranges -- multiple of them can appear in a row (e.g. AQP1_MOUSE)
			List<Range> intramems = new ArrayList<Range>();
			for (Range range : condensedRanges) {
				if (range.length()>=14) {
					for (int i=range.from; i<=range.to; i+=4) {
						intramems.add(new Range(i, Math.min(i+2, range.to)));
					}
				} else {
					intramems.add(range);
				}
			}
			return intramems;
		} else {		
			return condensedRanges;
		}
	}
	
	public static List<? extends Range> validateAndCondense(List<? extends Range> rangeList, String sequence) throws Exception {
		for (Range r : rangeList) {
			if(!r.validate(sequence.length()))
				Log.errorThrow("invalid range: "+r);
		}
		
		Collections.sort(rangeList);
		int i=0;
		while(i<rangeList.size()-1) {
			Range r1 = rangeList.get(i);
			Range r2 = rangeList.get(i+1);
			if (r1.to>=r2.from-1) { // collapse
				r1.to=Math.max(r1.to,r2.to);
				rangeList.remove(i+1);
			} else { // next
				i++;
			}
		}
		return rangeList;
	}

	public static boolean contains(List<? extends Range> rangeList, int position) throws Exception {
		for (Range range : rangeList) {
			if (position >= range.from && position <= range.to)
				return true;
		}
		return false;
	}
}
