package at.omasits.util;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.apache.pdfbox.io.IOUtils;

public class PowerpointExporter {
	public static File referenceDirectory = new File("pptxTemplate");
	
	public static void createPPTX(File destFile, String title, File image) throws IOException {
		FileOutputStream dest = new FileOutputStream(destFile);
		ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
		byte data[] = new byte[2048];
		// add all entries from reference directory structure
		Queue<String> files = new LinkedList<String>(Arrays.asList(referenceDirectory.list()));
		while (!files.isEmpty()) {
			String fileName = files.remove();
			File f = new File(referenceDirectory, fileName);
			if(f.isDirectory()) {
				for (String sub : f.list())
					files.add(fileName+'/'+sub);
			} else {
				BufferedInputStream origin = new BufferedInputStream(new FileInputStream(f), 2048);
				out.putNextEntry(new ZipEntry(fileName));
				int count;
				while ((count = origin.read(data, 0, 2048)) != -1) {
					out.write(data, 0, count);
					out.flush();
				}
				origin.close();
			}
        }
		
		// add entry 'ppt/media/image1.png'
		out.putNextEntry(new ZipEntry("ppt/media/image1.png"));
		out.write(IOUtils.toByteArray(new FileInputStream(image)));
		out.flush();
		
		BufferedImage png = ImageIO.read(image);
		int pngHeight = png.getHeight();
		int pngWidth = png.getWidth();
		long h = 5361720, w = 12192000, x = 0, y = 1273542, cx = 6096000, cy = 3954402;
		if (1.0*pngWidth/pngHeight > 1.0*12192000/5361720) { // wider -> reduce height & adjust y pos
			h = Math.round( 1.0 * 12192000 / pngWidth * pngHeight );
			y = cy - h/2;
		} else { // higher -> reduce width & adjust x pos
			w = Math.round( 1.0 * 5361720 / pngHeight * pngWidth );
			x = cx - w/2;
		}
		
		// add entry 'ppt/slides/slide1.xml' with adapted layout and title
		out.putNextEntry(new ZipEntry("ppt/slides/slide1.xml"));
		out.write(part1.getBytes());
		out.write(title.getBytes());
		out.write(part2.getBytes());
		out.write(("<a:off x='"+x+"' y='"+y+"'/><a:ext cx='"+w+"' cy='"+h+"'/>").getBytes());
		out.write(part3.getBytes());
		out.flush();
        out.close();
	}
	
	public static String part1 = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>\r\n<p:sld xmlns:a='http://schemas.openxmlformats.org/drawingml/2006/main' xmlns:r='http://schemas.openxmlformats.org/officeDocument/2006/relationships' xmlns:p='http://schemas.openxmlformats.org/presentationml/2006/main'><p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id='1' name=''/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x='0' y='0'/><a:ext cx='0' cy='0'/><a:chOff x='0' y='0'/><a:chExt cx='0' cy='0'/></a:xfrm></p:grpSpPr><p:sp><p:nvSpPr><p:cNvPr id='2' name='Title 1'/><p:cNvSpPr><a:spLocks noGrp='1'/></p:cNvSpPr><p:nvPr><p:ph type='title'/></p:nvPr></p:nvSpPr><p:spPr/><p:txBody><a:bodyPr/><a:lstStyle/><a:p><a:r><a:rPr lang='en-GB' dirty='0' smtClean='0'/><a:t>";
	public static String part2 = "</a:t></a:r><a:endParaRPr lang='en-GB' dirty='0'/></a:p></p:txBody></p:sp><p:pic><p:nvPicPr><p:cNvPr id='4' name='Content Placeholder 3'/><p:cNvPicPr><a:picLocks noGrp='1' noChangeAspect='1'/></p:cNvPicPr><p:nvPr><p:ph idx='1'/></p:nvPr></p:nvPicPr><p:blipFill><a:blip r:embed='rId2' cstate='print'><a:extLst><a:ext uri='{28A0092B-C50C-407E-A947-70E740481C1C}'><a14:useLocalDpi xmlns:a14='http://schemas.microsoft.com/office/drawing/2010/main' val='0'/></a:ext></a:extLst></a:blip><a:stretch><a:fillRect/></a:stretch></p:blipFill><p:spPr><a:xfrm>";
	public static String part3 = "</a:xfrm></p:spPr></p:pic><p:sp><p:nvSpPr><p:cNvPr id='5' name='TextBox 4'/><p:cNvSpPr txBox='1'/><p:nvPr/></p:nvSpPr><p:spPr><a:xfrm><a:off x='0' y='6497488'/><a:ext cx='12191999' cy='369332'/></a:xfrm><a:prstGeom prst='rect'><a:avLst/></a:prstGeom><a:noFill/></p:spPr><p:txBody><a:bodyPr wrap='square' rtlCol='0'><a:spAutoFit/></a:bodyPr><a:lstStyle/><a:p><a:pPr algn='r'/><a:r><a:rPr lang='en-GB' i='1' dirty='0'/><a:t>Protter - </a:t></a:r><a:r><a:rPr lang='en-GB' i='1' dirty='0' smtClean='0'/><a:t>visualize proteoforms</a:t></a:r><a:r><a:rPr lang='en-GB' i='1' dirty='0'/><a:t> </a:t></a:r><a:r><a:rPr lang='en-GB' i='1' dirty='0' smtClean='0'/><a:t>  </a:t></a:r><a:r><a:rPr lang='en-GB' sz='1400' i='1' dirty='0' smtClean='0'/><a:t>Omasits </a:t></a:r><a:r><a:rPr lang='en-GB' sz='1400' i='1' dirty='0'/><a:t>et al</a:t></a:r><a:r><a:rPr lang='en-GB' sz='1400' i='1' dirty='0' smtClean='0'/><a:t>., Bioinformatics</a:t></a:r><a:r><a:rPr lang='en-GB' sz='1400' i='1' dirty='0'/><a:t>. 2013 Nov 21.</a:t></a:r><a:endParaRPr lang='en-GB' i='1' dirty='0'/></a:p></p:txBody></p:sp></p:spTree><p:extLst><p:ext uri='{BB962C8B-B14F-4D97-AF65-F5344CB8AC3E}'><p14:creationId xmlns:p14='http://schemas.microsoft.com/office/powerpoint/2010/main' val='475242705'/></p:ext></p:extLst></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr><p:timing><p:tnLst><p:par><p:cTn id='1' dur='indefinite' restart='never' nodeType='tmRoot'/></p:par></p:tnLst></p:timing></p:sld>";
}
