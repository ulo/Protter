package at.omasits.proteomics.protter;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import at.omasits.util.Log;
import at.omasits.util.PowerpointExporter;
import at.omasits.util.Util;


public class ProtProvider {
	// a list of all generated Prot svg files
	public static Map<String,String> entries = new HashMap<String,String>();
	public final static File protsDir = new File("prots");
	static {
		if (! protsDir.exists())
			protsDir.mkdir();
	}
	
	public static ProtFile getFile(Map<String,String> parms) throws Exception {
		Prot prot = new Prot(parms);
		String title = "custom sequence";
		if (prot.lblTitle != null)
			title = prot.lblTitle;
		else if (prot.uniprotID != null)
			title = prot.uniprotID;
		String fileName = "protter "+title;
		
		String key = generateKey(prot).toString();
		boolean isCached = entries.containsKey(key) && new File(protsDir, entries.get(key)+".svg").exists();
		//isCached=false;
		if (!isCached) {
			String uuid = UUID.randomUUID().toString();
			prot.generateTex(new File(protsDir, uuid+".tex"));
			prot.generateSvg(protsDir, uuid+".tex", uuid+".svg");
			//new File(protsDir, uuid+".tex").delete();
			new File(protsDir, uuid+".aux").delete();
			new File(protsDir, uuid+".dvi").delete();
			new File(protsDir, uuid+".log").delete();
			entries.put(key, uuid);
		} else
			Log.info("found previous prot: "+entries.get(key));
		String uuid = entries.get(key);
		File svg = prot.applyToSvg(new File(protsDir, uuid+".svg")); // process query to pregenerated SVG
		
		String format = parms.containsKey("format") ? parms.get("format") : "svg";
		if (format.toLowerCase().equals("png")) {
			Log.info("transcoding and returning '"+svg.getName()+".png'");
			File png = new File(svg.getParentFile(),svg.getName()+".png");
			Util.transcode2png(svg, png);
			return new ProtFile(png, fileName+".png");
		} else if (format.toLowerCase().equals("pptx")) {
			Log.info("transcoding '"+svg.getName()+".pptx'");
			File png = new File(svg.getParentFile(),svg.getName()+".png");
			Util.transcode2png(svg, png);
			File pptx = new File(svg.getParentFile(),svg.getName()+".pptx");
			PowerpointExporter.createPPTX(pptx, title, png);
			return new ProtFile(pptx, fileName+".pptx");
		} else if (format.toLowerCase().equals("pdf")) {
			Log.info("transcoding and returning '"+svg.getName()+".pdf'");
			File pdf = new File(svg.getParentFile(),svg.getName()+".pdf");
			Util.transcode2pdf(svg, pdf);
			return new ProtFile(pdf, fileName+".pdf"); 
		} else if (format.toLowerCase().equals("svg")) {
			Log.info("returning '"+svg.getName()+"'");
			return new ProtFile(svg, fileName+".svg");
		} else if (format.toLowerCase().equals("svgz")) {
			File svgz = new File(svg.getParentFile(),svg.getName()+".svgz");
			Log.info("compressing '"+svg.getName()+"' and returning '"+svgz.getName()+"'");
			Util.zip(svg, svgz);
			return new ProtFile(svgz, fileName+".svgz");
		} else
			Log.errorThrow("unkown format: "+format);
		return null;
	}
	
	public static List<Object> generateKey(Prot prot) {
		return Arrays.asList(
				prot.seq,
				prot.tmLabel,
				prot.nonTMprotein,
				prot.nterm,
				prot.tmRegions,
				prot.imRegions,
				prot.anchorRegions,
				prot.arrTextopo
		);
	}
	
	public static class ProtFile {
		File file;
		String fileName;
		
		public ProtFile(File file, String fileName) {
			this.file = file;
			this.fileName = fileName;
		}
		
		public String getPath() {
			return this.file.getPath();
		}
	}
}
