package at.omasits.proteomics.protter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.omasits.proteomics.protter.Style.Color;
import at.omasits.proteomics.protter.Style.Shape;
import at.omasits.proteomics.protter.phobius.PhobiusProvider;
import at.omasits.proteomics.protter.ranges.Range;
import at.omasits.proteomics.protter.uniprot.UniProtProvider;
import at.omasits.proteomics.protter.uniprot.UniProtProvider.UniprotException;
import at.omasits.util.Config;
import at.omasits.util.Log;
import at.omasits.util.UOUniProtEntry;
import at.omasits.util.Util;
import at.omasits.util.Vec;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class Prot {
	public static enum Nterm {
		extra, 
		intra;
		public static Nterm fromString(String nterm) throws Exception {
			try { return Nterm.valueOf(nterm.toLowerCase()); }
			catch (IllegalArgumentException e) { Log.errorThrow("unkown nterm argument: "+nterm+". choose beteen intra and extra.");	}
			return null;
    	}
	};
	public static enum TMlabel {
		none,
		numcount,
		alphacount,
		Alphacount,
		romancount,
		Romancount;
		public static TMlabel fromString(String tmLabel) throws Exception {
			try { return TMlabel.valueOf(tmLabel); }
			catch (IllegalArgumentException e) { Log.errorThrow("unkown tmlabel argument: "+tmLabel+".");	}
			return null;
    	}
	}
	public static enum AminoAcids {
		A("Ala"),R("Arg"),N("Asn"),D("Asp"),C("Cys"),E("Glu"),Q("Gln"),G("Gly"),H("His"),
		I("Ile"),L("Leu"),K("Lys"),M("Met"),F("Phe"),P("Pro"),S("Ser"),T("Thr"),W("Trp"),
		Y("Tyr"),V("Val"),U("Sec"),O("Pyl"),B("Asx"),Z("Glx"),J("Xle"),X("Xaa");
	    public final String threeLetterCode;
	    AminoAcids(String threeLetterCode) { this.threeLetterCode = threeLetterCode; }
	    public static AminoAcids fromChar(char aminoAcid) throws Exception {
			try { return AminoAcids.valueOf(String.valueOf(aminoAcid).toUpperCase()); }
			catch (IllegalArgumentException e) { Log.errorThrow("Unkown amino acid: "+aminoAcid);	}
			return null;
	    }
	}
	
	public String seq;
	public List<String> uniprotIDs = new ArrayList<String>();
	public List<Range> seqRanges = new ArrayList<Range>();
	public List<Integer> upOffsets = new ArrayList<Integer>();
	public List<? extends Range> tmRegions = new ArrayList<Range>();
	public List<? extends Range> imRegions = new ArrayList<Range>();
	public List<? extends Range> anchorRegions = new ArrayList<Range>();
	public List<? extends Range> cutAtRegions = new ArrayList<Range>();
	public Nterm nterm;
	public LinkedHashMap<Style, List<? extends Range>> styleRanges;
	public Color membraneColor;
	//public boolean tmLabels;
	public Color tmLabelColor;
	public TMlabel tmLabel;
	public String lblIntra;
	public String lblExtra;
	public boolean lblNumbers;
	public String lblTitle;
	
	public boolean nonTMprotein = false;
	public boolean nonMprotein = false;
	
	public boolean showLegend = true;
	public String cutAtLegend = null;
	public String tmLegend = null;
	public String ntermLegend = null;
	
	public List<String> arrTextopo = new ArrayList<String>();
	
	public Prot(Map<String,String> parms) throws Exception {
		List<UOUniProtEntry> up = new ArrayList<UOUniProtEntry>();
		
		// parse the parameters and validate
		String seqValue = parms.get("seq");	
		String tmValue = parms.get("tm");
		String imValue = parms.get("im");
		String ntermValue = parms.get("nterm");
		String upValue = parms.get("up");
		String anchorValue = parms.get("anchor");
		String cutAtValue = parms.get("cutAt");
		cutAtLegend = cutAtValue;
		
		if (upValue != null) {
			String[] upValues = upValue.split("\\+");
			for (String upName : upValues) {
				Matcher m = Pattern.compile("(.+)\\[(\\d+)-(\\d*)\\]").matcher(upName);
				Integer offsetFrom = 1;
				Integer offsetTo = null;
				if (m.matches()) {
					upName = m.group(1);
					offsetFrom = Integer.valueOf(m.group(2));
					offsetTo = (m.group(3).length()==0) ? (null) : (Integer.valueOf(m.group(3)));
				}
				UOUniProtEntry upEntry = new UOUniProtEntry(UniProtProvider.get(upName), offsetFrom, offsetTo);
				if (this.seq==null)
					this.seq="";
				int start = this.seq.length();
				this.seq += upEntry.getSequenceString().toUpperCase();
				this.seqRanges.add(new Range(start, this.seq.length()-1));
				this.upOffsets.add(offsetFrom-1);
				up.add(upEntry);
				this.uniprotIDs.add(upEntry.getUniProtId().getValue());
			}
		}
		if (this.seq == null) {
			if (seqValue==null || seqValue.length()==0) {
				Log.errorThrow("no sequence specified! use the seq parameter for specifying a sequence.");
			} else {
				this.seq = seqValue.toUpperCase();			
			}
		}
		
		// check auto-topology
		if (tmValue!=null && tmValue.equalsIgnoreCase("auto")) {
			if (up.size()>0) {
				tmValue="UP.TRANSMEM";
				imValue="UP.INTRAMEM";
				if (anchorValue==null) anchorValue="UP.LIPID";
				if (up.get(0).getPrimaryUniProtAccession().toString().indexOf('-') != -1) {
					// uniprot isoform (e.g. Q13740-2)
					tmValue="PHOBIUS.TM";
					imValue=null;
					ntermValue="PHOBIUS.NTERM";
					anchorValue=null;
				}
				if (ntermValue==null) {
					try {
						UniProtProvider.inferNterm(up.get(0));
						ntermValue="UP.NTERM";
					} catch (UniprotException e) {
						ntermValue="PHOBIUS.NTERM";
					}
				}
			} else {
				tmValue="PHOBIUS.TM";
				if (ntermValue==null) {
					ntermValue="PHOBIUS.NTERM";
				}
			}
		}
		
		if (tmValue==null && imValue==null && ntermValue==null && anchorValue==null) {
			this.nonTMprotein = true;
			this.nonMprotein = true;
		//} else if (tmValue==null && anchorValue==null) { // && ntermValue != null
			//throw new Exception("no transmembrane regions or membrane anchots specified! use the tm parameter for specifying transmembrane regions and the anchor parameter for anchor regions.");
		} else if (ntermValue==null) {
			Log.errorThrow("no n-terminus position specified! use the nterm parameter for specifying the n-terminus position.");
		} else {
			if (ntermValue.equalsIgnoreCase("up.nterm")) {
				if (up.size()>0) {
					this.nterm = UniProtProvider.inferNterm(up.get(0));
					ntermLegend = "UniProt";
				} else {
					Log.errorThrow("Cannot load nterm information from UniProt as no UniProt accession was specified! Use the up parameter for specifying a UniProt accession or name.");
				}
			} else if (ntermValue.toLowerCase().startsWith("up.nterm.")) {
				if (up.size()>0) {
					String conformation = ntermValue.toLowerCase().substring("up.nterm.".length()); // e.g: UP.NTERM.EXTERNAL					
					this.nterm = UniProtProvider.inferNterm(up.get(0), conformation);
					ntermLegend = "UniProt ("+conformation+")";
				} else {
					Log.errorThrow("Cannot load nterm information from UniProt as no UniProt accession was specified! Use the up parameter for specifying a UniProt accession or name.");
				}
			} else if (ntermValue.equalsIgnoreCase("phobius.nterm")) {
				this.nterm = PhobiusProvider.inferNterm(this.seq);
				ntermLegend = "Phobius";
			} else {
				this.nterm = Nterm.fromString(ntermValue); // try to interpret string
				ntermLegend = "manual";
			}
			if (tmValue!=null || imValue!=null) {
				if (tmValue != null) {
					this.tmRegions = Range.parseMultiRangeString(tmValue, seq, up, "tm", parms);
					if (tmValue.equalsIgnoreCase("up.transmem")) {
						tmLegend = "UniProt";
					} else if (tmValue.toLowerCase().startsWith("up.transmem.")) {
						String conformation = tmValue.toLowerCase().substring("up.transmem.".length()); // e.g: UP.TRANSMEM.EXTERNAL
						tmLegend = "UniProt ("+conformation+")";
					} else if (tmValue.equalsIgnoreCase("phobius.tm"))
						tmLegend = "Phobius";
					else
						tmLegend = "manual";
				}
				if (imValue != null) {
					this.imRegions = Range.parseMultiRangeString(imValue, seq, up, "im", parms);
				}
				
				this.nonTMprotein = true;
				for (Range tmRegion : tmRegions) {
					if (tmRegion.length() >= 14) {
						this.nonTMprotein = false; // has to have at least one TM region >=14 aa
						break;
					}
				}
			} else {
				this.nonTMprotein = true;
			}
			if (anchorValue!=null)
				this.anchorRegions = Range.parseMultiRangeString(anchorValue, seq, up, "anchor", parms);
		}
		if (cutAtValue!=null)
			this.cutAtRegions = Range.parseMultiRangeString(cutAtValue, seq, up, "cutAt", parms);
		
		// parse the additional parameters and extract the styles
		LinkedHashMap<Style, List<? extends Range>> styles = new LinkedHashMap<Style, List<? extends Range>>();
		for (String param : parms.keySet()) { //(List<String>) Collections.list(parms.propertyNames())) {
			String value = parms.get(param);
			if (param.contains(":")) { // style
				Style style = Style.fromString(param);
				if (style != null) {
					if (style.name==null) style.name = value;
					styles.put(
						style,
						Range.parseMultiRangeString(value, seq, up, "style", parms)
					);
				}
			}
		}
		this.styleRanges = styles;
		
		// additional settings
		this.tmLabelColor = parms.containsKey("lc") ? Color.fromString(parms.get("lc")) : Color.fromString("blue");
		this.membraneColor = parms.containsKey("mc") ? Color.fromString(parms.get("mc")) : Color.fromString("lightsalmon");
		this.tmLabel = parms.containsKey("tml") ? TMlabel.fromString(parms.get("tml")) : TMlabel.numcount;
		this.lblNumbers = parms.containsKey("numbers");
		this.showLegend = parms.containsKey("legend");
		this.lblTitle = parms.get("title");
		if (parms.containsKey("tex")) {
			this.arrTextopo = Lists.newArrayList(Splitter.on(';').split(parms.get("tex")));	
		}
		
		// set membrane labels
		if (ntermValue!=null) {
			this.lblIntra = "intra";
			if (parms.containsKey("lblin")) {
				this.lblIntra = parms.get("lblin");
			} else {
				if (up.size()>0 && ntermValue.equalsIgnoreCase("up.nterm") && UniProtProvider.getTopoStrings(up.get(0))!=null) {
					if (this.nterm==Nterm.intra)
						this.lblIntra = UniProtProvider.getTopoStrings(up.get(0)).get(0);
					else
						this.lblIntra = UniProtProvider.getTopoStrings(up.get(0)).get(1);
				} else if (up.size()>0 && ntermValue.toLowerCase().startsWith("up.nterm.")) {
					String conformation = ntermValue.toLowerCase().substring("up.nterm.".length()); // e.g: UP.NTERM.EXTERNAL
					List<String> topos = UniProtProvider.getTopoStrings(up.get(0), conformation);
					if (topos != null) {
						if (this.nterm==Nterm.intra)
							this.lblIntra = topos.get(0);
						else
							this.lblIntra = topos.get(1);
					}
				}
			}
			
			this.lblExtra = "extra";
			if (parms.containsKey("lblout")) {
				this.lblExtra = parms.get("lblout");
			} else {
				if (up.size()>0 && ntermValue.equalsIgnoreCase("up.nterm") && UniProtProvider.getTopoStrings(up.get(0))!=null) {
					if (this.nterm==Nterm.extra)
						this.lblExtra = UniProtProvider.getTopoStrings(up.get(0)).get(0);
					else
						this.lblExtra = UniProtProvider.getTopoStrings(up.get(0)).get(1);
				} else if (up.size()>0 && ntermValue.toLowerCase().startsWith("up.nterm.")) {
					String conformation = ntermValue.toLowerCase().substring("up.nterm.".length()); // e.g: UP.NTERM.EXTERNAL
					List<String> topos = UniProtProvider.getTopoStrings(up.get(0), conformation);
					if (topos != null) {
						if (this.nterm==Nterm.extra)
							this.lblExtra = topos.get(0);
						else
							this.lblExtra = topos.get(1);
					}
				}
			}
		}
	}
	
	/*
	 * generates a vanilla textopo plot
	 * uses:
	 *   seq
	 *   tmLabel
	 *   nonTMprotein
	 *   nterm
	 *   tmRegions
	 *   anchorRegions
	 *   --> same as in ProtProvider.generateKey()
	 */
	public void generateTex(File outputFile) throws Exception {
		Log.info("generating tex file: '"+outputFile.getPath()+"'");
		try {
			BufferedWriter tex = new BufferedWriter(new FileWriter(outputFile));
			
			tex.write("\n \\documentclass[a4paper]{article}");
			if (seq.length() > 1500) { // increase standard a4 paper size for large proteins
				tex.write("\n \\special{papersize=1000mm,297mm}");
				// TODO: check for scale changes in generated svg file
			}
			tex.write("\n \\pagestyle{empty}");
			tex.write("\n \\usepackage[latin1]{inputenc}");
			tex.write("\n \\usepackage{textopo}");
			tex.write("\n \\begin{document}");
			tex.write("\n \\begin{textopo}");
			tex.write("\n 	\\scaletopo{0}");
			tex.write("\n 	\\hidelegend");
			if (tmLabel.equals(TMlabel.none))
				tex.write("\n 	\\hideTMlabels");
			else
				tex.write("\n 	\\labelTMs{\\"+tmLabel.toString()+"}");
			if (seq.length() > 1000) { // make loops extent more for larger proteins!
				//tex.write("\n	\\loopextent{"+(30+Math.round((seq.length()-1000)*0.02))+"}");
				// TODO: check for scale changes in generated svg file
				// UPDATE: removed so that vbox does not get too high...
			}
			String loopextent = Config.get("textopo_loopextent", "30");
			String loopdistance = Config.get("textopo_loopdistance", "5");
			tex.write("\n	\\loopextent{"+loopextent+"["+loopdistance+"]}");
			
			// print the sequence
			tex.write("\n	\\sequence{"+(nonTMprotein ? "AAAAAAAAAAAAAAAAAAAA" : "")+seq+"}");
			
			// print the n-terminus configuration
			String strNterm = "intra";
			if (nterm != null) {
				if (nonTMprotein)
					strNterm = nterm.equals(Nterm.extra) ? Nterm.intra.name().toLowerCase() : Nterm.extra.name().toLowerCase();
				else
					strNterm = nterm.name().toLowerCase();
			}
			tex.write("\n	\\Nterm{"+strNterm+"}");
			
			// print the transmembrane & anchor domains
			if (nonTMprotein)
				tex.write("\n	\\MRs{1..15}");
			
			for (Range tm : tmRegions) {
				tex.write("\n	\\MRs{"+(tm.from+(nonTMprotein?20:0))+".."+(tm.to+(nonTMprotein?20:0))+"}");
			}
			for (Range im : imRegions) {
				tex.write("\n	\\MRs{"+(im.from+(nonTMprotein?20:0))+".."+(im.to+(nonTMprotein?20:0))+"}");
			}
			for (Range anchor : anchorRegions) {
				for (int i=anchor.from; i<=anchor.to; i++)
					tex.write("\n	\\anchor{"+(i + (nonTMprotein ? 20 : 0))+"}");
			}
			
			// optional additional textopo commands 
			for (String texCmd : arrTextopo) {
				tex.write("\n	\\"+texCmd);
			}

			// print the closure
			tex.write("\n \\end{textopo}");
			tex.write("\n \\end{document}");
			
			tex.close();
		} catch (IOException e) {
			Log.errorThrow("Could not write topo.tex file");
		}
	}
	
	/*
	 * converts the tex file to a svg file
	 */
	public void generateSvg(File directory, String inputFile, String outputFile) throws Exception {
		try {
			BufferedReader input = null;
			String line;
			String exe;
			// run latex
			//Log.info("generating dvi file...");
			//exe = Config.get("latex")+" --extra-mem-top=50000000 --extra-mem-bot=20000000 --stack-size=50000 --param-size=50000 "+inputFile;
			//exe = "latex --extra-mem-top=50000000 --extra-mem-bot=20000000 --stack-size=50000 --param-size=50000 "+inputFile;
			exe = ProtterServer.latexPath + " " + inputFile;
			Log.info("calling: "+exe);
			Process pLatex = Runtime.getRuntime().exec(exe, null, directory);
				input = new BufferedReader(new InputStreamReader(pLatex.getInputStream()));
				while ((line = input.readLine()) != null) {
					Log.debug(line);
					if (line.length()>0 && line.charAt(0)=='!') { 
						// e.g: P08183: \MRs{328..346} => ! File ended while scanning use of \get@numnum.
						// e.g.: Q96K49: \MRs{286..311} => ! Arithmetic overflow.
						pLatex.destroy();
						Log.errorThrow("could not generate textopo image ('"+line+"')");
					}
				}
				input.close();
			if (pLatex.exitValue()!=0) {
				Log.errorThrow("could not generate textopo image: latex ended with error code "+pLatex.exitValue());
				return;
			}
			// run dvisvgm 
			//Log.info("generating svg file...");
			String inputFilePrefix = inputFile.substring(0,inputFile.lastIndexOf('.'));
			//exe = "dvisvgm -n -o "+outputFile+" "+inputFilePrefix+".dvi";
			exe = ProtterServer.dvisvgm + " -n -o "+outputFile+" "+inputFilePrefix+".dvi";
			Log.info("calling: "+exe);
			Process pDvi2Svg = Runtime.getRuntime().exec(exe, null, directory);
				input = new BufferedReader(new InputStreamReader(pDvi2Svg.getErrorStream()));
				while ((line = input.readLine()) != null) {
					Log.debug(line);
				}
				input.close();
			if (pDvi2Svg.exitValue()!=0) {
				Log.errorThrow("could not generate textopo image: dvisvgm ended with error code "+pDvi2Svg.exitValue());
				return;
			}
			return;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return;
	}
	
	// processes the svg file
	public File applyToSvg(File inputFile) throws Exception {
		try {
			String inputFilePrefix = inputFile.getName().substring(0,inputFile.getName().lastIndexOf('.'));
			File outputFile = new File(inputFile.getParentFile(), inputFilePrefix+".processed.svg");
			
			Style[] styles = condensedStyles();
			
			Log.info("processing '"+inputFile.getPath()+"' file...");
			// TODO: parse & recalculate viewport (center & cut non-membrane proteins) prior to style processing
			BufferedReader in = new BufferedReader(new FileReader(inputFile));
			BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
			String line;
			int aa = (nonTMprotein ? -20 : 0);
			Vec prevAaPos = null;
			String[] nh2 = null;
			Double svgHeight = null;
			Double svgWidth = null;
			Double[] viewBox = null; 
			double titleX = 0;
			double titleY = 0;
			double legendX = 0;
			double legendY = 0;
			
			while((line=in.readLine()) != null) {
				// process aminoacids
				if (line.equals("<g fill='#ffffff'>")) {
					in.readLine(); // skip the filling (e.g: <use x='189.745' xlink:href='#g515' y='130.967'/>)
					in.readLine(); // skip the </g>
					
					if (nonTMprotein && aa<0) {
						in.readLine(); // skip shape
						in.readLine(); // skip letter
						aa++;
						continue;
					}
					
					// parse the coordinates
					line = in.readLine(); // e.g: <use x='189.745' xlink:href='#g514' y='130.967'/>
					float x = Float.valueOf(Util.substringBetweenStrings(line, "x='", "'"));
					float y = Float.valueOf(Util.substringBetweenStrings(line, "y='", "'"));
					x += 2.1f; y -= 2.1f; // correct for offsets
					
					if (aa==0 && nonTMprotein) { // move H2N- label
						double dx = Double.parseDouble(Util.substringBetweenStrings(nh2[3], "x='", "'")) - x + 4;
						double dy = Double.parseDouble(Util.substringBetweenStrings(nh2[3], "y='", "'")) - y - 1;
						out.write("<use x='"+(Double.parseDouble(Util.substringBetweenStrings(nh2[0], "x='", "'"))-dx)+"' xlink:href='"+Util.substringBetweenStrings(nh2[0], "xlink:href='", "'")+"' y='"+(Double.parseDouble(Util.substringBetweenStrings(nh2[0], "y='", "'"))-dy)+"'/>"+"\n");
						out.write("<use x='"+(Double.parseDouble(Util.substringBetweenStrings(nh2[1], "x='", "'"))-dx)+"' xlink:href='"+Util.substringBetweenStrings(nh2[1], "xlink:href='", "'")+"' y='"+(Double.parseDouble(Util.substringBetweenStrings(nh2[1], "y='", "'"))-dy)+"'/>"+"\n");
						out.write("<use x='"+(Double.parseDouble(Util.substringBetweenStrings(nh2[2], "x='", "'"))-dx)+"' xlink:href='"+Util.substringBetweenStrings(nh2[2], "xlink:href='", "'")+"' y='"+(Double.parseDouble(Util.substringBetweenStrings(nh2[2], "y='", "'"))-dy)+"'/>"+"\n");
						out.write("<use x='"+(Double.parseDouble(Util.substringBetweenStrings(nh2[3], "x='", "'"))-dx)+"' xlink:href='"+Util.substringBetweenStrings(nh2[3], "xlink:href='", "'")+"' y='"+(Double.parseDouble(Util.substringBetweenStrings(nh2[3], "y='", "'"))-dy)+"'/>"+"\n");
					}
					
					// aa titles
					String title;
					if (seqRanges.size()>1 || (upOffsets.size()==1 && upOffsets.get(0)>0)) {
						int i = Range.contains(seqRanges, aa);
						title = aa+1 + " = " + (uniprotIDs.size()>1 ? uniprotIDs.get(i)+":" : "") + AminoAcids.fromChar(seq.charAt(aa)).threeLetterCode + (aa+1-seqRanges.get(i).from+upOffsets.get(i)) + (styles[aa].name != null ? ": "+styles[aa].name : "");
					} else {
						title = AminoAcids.fromChar(seq.charAt(aa)).threeLetterCode+(aa+1) + (styles[aa].name != null ? ": "+styles[aa].name : "");						
					}
					title = "<title>"+title+"</title>";
					
					// output the shape
					String shapeFill = "fill='"+styles[aa].backgroundColor.code+"' stroke='"+styles[aa].frameColor.code+"' stroke-width='0.3'";
					if (styles[aa].shape.equals(Shape.circ)) {
						out.write("<circle id='aa"+aa+"_symbol' r='1.6' cx='"+x+"' cy='"+y+"' ");
						out.write(shapeFill+">"+title+"</circle>\n");
					} else if (styles[aa].shape.equals(Shape.box)) {
						out.write("<rect id='aa"+aa+"_symbol' height='2.5' width='2.5' x='"+(x-1.25f)+"' y='"+(y-1.25f)+"' ");
						out.write(shapeFill+">"+title+"</rect>\n");
					} else if (styles[aa].shape.equals(Shape.diamond)) {
						out.write("<rect id='aa"+aa+"_symbol' height='2.5' width='2.5' x='"+(x-1.25f)+"' y='"+(y-1.25f)+"' transform='rotate(45,"+x+","+y+")' ");
						out.write(shapeFill+">"+title+"</rect>\n");
					} else {
						Log.errorThrow("unkown shape in "+styles[aa]);
					}
					//out.write("fill='"+styles[aa].backgroundColor.code+"' stroke='"+styles[aa].frameColor.code+"' stroke-width='0.3' />\n");
					
					// id the letter (<use x='250.679' xlink:href='#g268' y='203.443'/>)
					line = in.readLine();
					if (line.startsWith("<use "))
						out.write("<use id='aa"+aa+"_letter' fill='"+styles[aa].charColor.code+"' " + line.substring(5,line.length()-2) + ">"+title+"</use>\n");
					else
						Log.errorThrow("parsing error");
					
					// label residue numbers
					if (lblNumbers && (((aa+1) % 10)==0 || aa==seq.length()-1)) {
						Vec nv = Vec.diff(new Vec(x,y), prevAaPos).orthogonalized().normalized();
						String align = (nv.x>0) ? "start" : "end";
						Vec vtxt = new Vec(x,y).added(nv.scaled(2.0f));
						out.write("<text x='"+vtxt.x+"' y='"+vtxt.y+"' dy='0.7' font-family='sans-serif' font-size='1.7' fill='black' text-anchor='"+align+"'>"+(aa+1)+"</text>");							
					}
					
					// indicate proteolytic cleavage sites
					if (Range.contains(cutAtRegions, aa)>=0 &&  prevAaPos != null) {
						Vec mid = Vec.midpoint(new Vec(x,y), prevAaPos);
						Vec nv = Vec.diff(new Vec(x,y), prevAaPos).orthogonalized().normalized();
						Vec v1 = mid.added(nv.scaled(3.0f));
						Vec v2 = mid.added(nv.scaled(-3.0f));
						out.write("<line x1='"+v1.x+"' y1='"+v1.y+"' x2='"+v2.x+"' y2='"+v2.y+"' stroke='#000000' stroke-width='0.3' stroke-dasharray='0.3 0.5' stroke-linecap='round' />");	
					}
					
					// indicate fusion sites
					if (seqRanges.size()>1) {
						if (seqRanges.get(Range.contains(seqRanges, aa)).from==aa && prevAaPos != null) {
							Vec mid = Vec.midpoint(new Vec(x,y), prevAaPos);
							Vec nv = Vec.diff(new Vec(x,y), prevAaPos).orthogonalized().normalized();
							Vec v1 = mid.added(nv.scaled(4.0f));
							Vec v2 = mid.added(nv.scaled(-4.0f));
							out.write("<line x1='"+v1.x+"' y1='"+v1.y+"' x2='"+v2.x+"' y2='"+v2.y+"' stroke='#ff0000' stroke-width='0.5' stroke-linecap='round' />");
						}
					}
					
					prevAaPos = new Vec(x, y);
					aa++;
					
				// membrane rectangle
				} else if (line.startsWith("<rect fill='#ffffff'")) {
					if (!nonMprotein)
						out.write("<rect id='membraneFill' fill='" + membraneColor.code + "'" + line.substring(20) + "\n");
					
				// membrane lines
				} else if (line.startsWith("<rect height=")) {
					if (!nonMprotein) {
						out.write("<rect id='membraneLine' " + line.substring(6) + "\n");
					}
					
				// tm labels
				} else if (line.equals("<g fill='#0000ff'>")){
					if ((tmLabel != TMlabel.none) && !nonTMprotein) { // TODO: zweistellige TM nummern??
						out.write("<g fill='"+tmLabelColor.code+"'>\n");
						out.write(in.readLine()+"\n"); // the use
						out.write(in.readLine()+"\n"); // the close of g
					} else {
						in.readLine();
						in.readLine();
					}
				
				// intra, extra, H2N, COOH labels and lipid-anchors
				} else if (line.startsWith("<use x=")) {
					if (nh2==null) { // e
						nh2 = new String[4];
						if (!nonMprotein) {
							String x = Util.substringBetweenStrings(line, "x='", "'");
							String y = Util.substringBetweenStrings(line, "y='", "'");
							out.write("<text x='"+x+"' y='"+y+"' font-family='sans-serif' font-size='3' fill='black' text-anchor='start'>"+lblExtra+"</text>");
						}
						in.readLine(); //x
						in.readLine(); //t
						in.readLine(); //r
						in.readLine(); //a
						
						in.readLine(); //i
						in.readLine(); //n
						in.readLine(); //t
						in.readLine(); //r
						line = in.readLine(); //a
						if (!nonMprotein) {
							double x = Double.parseDouble(Util.substringBetweenStrings(line, "x='", "'"));
							String y = Util.substringBetweenStrings(line, "y='", "'");
							out.write("<text x='"+(x+2)+"' y='"+y+"' font-family='sans-serif' font-size='3' fill='black' text-anchor='end'>"+lblIntra+"</text>");
						}
						if (nonTMprotein) {
							nh2[0] = in.readLine(); //H
							nh2[1] = in.readLine(); //2
							nh2[2] = in.readLine(); //N
							nh2[3] = in.readLine(); //-
						}
					} else {
						out.write(line + "\n");
					}
//					} else if (aa==seq.length()) { // -COOH (always shown)
//						out.write(line + "\n"); //-
//						out.write(in.readLine() + "\n"); //C
//						out.write(in.readLine() + "\n"); //O
//						out.write(in.readLine() + "\n"); //O
//						out.write(in.readLine() + "\n"); //H
//					} else if (!nonMprotein) { // H2N- for TM proteins | lipid-anchors
//						out.write(line + "\n");
//					}
				// set a svg-title
				} else if (line.startsWith("<svg ")) {
					svgHeight = Double.valueOf(Util.substringBetweenStrings(line, "height='", "pt'"));
					svgWidth = Double.valueOf(Util.substringBetweenStrings(line, "width='", "pt'"));
					String[] strViewBox = Util.substringBetweenStrings(line, "viewBox='", "'").split(" ");
					viewBox = new Double[]{Double.valueOf(strViewBox[0]), Double.valueOf(strViewBox[1]), Double.valueOf(strViewBox[2]), Double.valueOf(strViewBox[3])};

					// general extension by 5pt
					viewBox[0] -= 5;
					viewBox[1] -= 5;
					viewBox[2] += 10;
					viewBox[3] += 10;
					svgWidth += 10;
					svgHeight += 10;
					
					if (showLegend) { // set legend position before adding title
						legendX = viewBox[0] + viewBox[2];
						legendY = viewBox[1] + 2; //(viewBox[1]+viewBox[3]/2);
					}

					// at font-size=5, a letter is 2.9pt wide
					if (lblTitle!=null && svgWidth < 2.9*lblTitle.length()) {
						double widthExtension = 2.9*lblTitle.length() - svgWidth;
						viewBox[0] -= widthExtension/2;
						viewBox[2] += widthExtension;
						svgWidth += widthExtension;
					}
					if (lblTitle!=null) {
						viewBox[1] -= 5;
						viewBox[3] += 5;
						svgHeight += 5;
						titleX = (viewBox[0]+viewBox[2]/2);
						titleY = viewBox[1];
					}
					
					if (showLegend) {
						int maxLegendLength = 0;
						for (Style style : styleRanges.keySet()) {
							maxLegendLength = Math.max(maxLegendLength, style.name.length());
						}
						double widthExtension = 3+2.6*maxLegendLength;
						viewBox[2] += widthExtension;
						svgWidth += widthExtension;
					}
					
					out.write("<svg version='1.1' height='"+svgHeight+"pt' width='"+svgWidth+"pt' viewBox='"+viewBox[0]+" "+viewBox[1]+" "+viewBox[2]+" "+viewBox[3]+"' xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink'>" + "\n");
					out.write("<title>generated using Protter</title>\n");
				// ...
				} else if (line.equals("</g>")) {
					if (lblTitle != null)
						out.write("<text x='"+titleX+"' y='"+titleY+"' dy='6' font-family='sans-serif' font-size='5' font-weight='bold' fill='black' text-anchor='middle'>"+lblTitle+"</text>\n");
					
					if (showLegend) {
						// add a legend
						int i=0;
						out.write("<g id='legend'>\n");
						double y = legendY;
						List<Style> legendStyles = new ArrayList<Style>(styleRanges.keySet());
						Collections.reverse(legendStyles);
						for (Style style : legendStyles) {
							i++;
							y += 4;
							out.write("<text x='"+(legendX+3f)+"' y='"+(y+1.0f)+"' font-family='sans-serif' font-size='3' fill='black'>"+style.name+"</text>\n");
							
							String shapeFill = (style.backgroundColor != null) ? (" fill='"+style.backgroundColor.code+"'") : (" fill='url(#grid)'");
							String shapeStroke = (style.frameColor != null) ? (" stroke='"+style.frameColor.code+"' stroke-width='0.3'") : (" stroke='#AAAAAA' stroke-width='0.3' stroke-dasharray='.5 .5'");
							if (style.shape==null || style.shape.equals(Shape.circ)) {
								out.write("<circle id='legend"+i+"' r='1.6' cx='"+legendX+"' cy='"+y+"'"+shapeFill+shapeStroke+" />\n");
							} else if (style.shape.equals(Shape.box)) {
								out.write("<rect id='legend"+i+"' height='2.5' width='2.5' x='"+(legendX-1.25f)+"' y='"+(y-1.25f)+"'"+shapeFill+shapeStroke+" />\n");
							} else if (style.shape.equals(Shape.diamond)) {
								out.write("<rect id='legend"+i+"' height='2.5' width='2.5' x='"+(legendX-1.25f)+"' y='"+(y-1.25f)+"' transform='rotate(45,"+legendX+","+y+")'"+shapeFill+shapeStroke+" />\n");
							}
							if (style.charColor != null) {
								out.write("<line x1='"+(legendX-0.5f)+"' y1='"+(y-0.5f)+"' x2='"+(legendX+0.5f)+"' y2='"+(y+0.5f)+"' stroke='"+style.charColor.code+"' stroke-width='0.3' stroke-linecap='round' />");
								out.write("<line x1='"+(legendX-0.5f)+"' y1='"+(y+0.5f)+"' x2='"+(legendX+0.5f)+"' y2='"+(y-0.5f)+"' stroke='"+style.charColor.code+"' stroke-width='0.3' stroke-linecap='round' />");
							}
						}
						if (cutAtLegend != null) {
							y += 4;
							if (cutAtLegend.toLowerCase().startsWith("peptidecutter.")) cutAtLegend = cutAtLegend.substring(14);
							out.write("<line x1='"+(legendX-1.9f)+"' y1='"+y+"' x2='"+(legendX+1.9f)+"' y2='"+y+"' stroke='#000000' stroke-width='0.3' stroke-dasharray='0.3 0.5' stroke-linecap='round' />");
							out.write("<text x='"+(legendX+3f)+"' y='"+(y+1)+"' font-family='sans-serif' font-size='3' fill='black'>"+cutAtLegend+"</text>\n");
						}
						if (seqRanges.size()>1) {
							y += 4;
							out.write("<line x1='"+(legendX-1.9f)+"' y1='"+y+"' x2='"+(legendX+1.9f)+"' y2='"+y+"' stroke='#ff0000' stroke-width='0.5' stroke-linecap='round' />");
							out.write("<text x='"+(legendX+3f)+"' y='"+(y+1)+"' font-family='sans-serif' font-size='3' fill='black'>fusion site</text>\n");
						}
						if (ntermLegend != null) {
							y += 4;
							out.write("<text x='"+(legendX-1.25)+"' y='"+(y+1)+"' font-family='sans-serif' font-size='3' fill='black'>N-term: "+ntermLegend+"</text>\n");
						}
						if (tmLegend != null) {
							y += 4;
							out.write("<text x='"+(legendX-1.25)+"' y='"+(y+1)+"' font-family='sans-serif' font-size='3' fill='black'>TMRs: "+tmLegend+"</text>\n");
						}
						
						out.write("</g>\n");
					}
					
					// add info to webapp client
					out.write("<text id='uniprotID' display='none'>"+(uniprotIDs.size()>0 ? Joiner.on('+').join(uniprotIDs) : "")+"</text>\n");
					out.write("<text id='sequence' display='none'>"+seq+"</text>\n");
					
					out.write("</g>\n");
				} else if (line.equals("</defs>")) {
					out.write("<pattern id='grid' width='1' height='1' patternUnits='userSpaceOnUse'>\n");
					out.write("<rect x='0' y='0' width='.5' height='.5' fill='#888888' /><rect x='.5' y='0' width='.5' height='.5' fill='#CCCCCC' />\n");
					out.write("<rect x='0' y='.5' width='.5' height='.5' fill='#CCCCCC' /><rect x='.5' y='.5' width='.5' height='.5' fill='#888888' />\n");
					out.write("</pattern>\n");
					out.write("</defs>\n");
				} else {
					out.write(line + "\n");				
				}
			}
			in.close();
			out.close();
			return outputFile;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	// condenses the styleRanges map to a single style array
	private Style[] condensedStyles() throws Exception {
		Style[] finalStyleArr = new Style[seq.length()];
		// initialize with defaults
		for (int i=0; i<finalStyleArr.length; i++) 
			finalStyleArr[i] = new Style(Shape.circ, Color.fromString("black"), Color.fromString("white"), Color.fromString("black"), null); 
		
		for (Style style : styleRanges.keySet()) {
			List<? extends Range> ranges = styleRanges.get(style);
			for (Range range : ranges) {
				for (int i = range.from; i<=range.to; i++) {
					finalStyleArr[i-1].overlayWithNewStyle(style);
				}
			}
		}
		return finalStyleArr;
	}
}
