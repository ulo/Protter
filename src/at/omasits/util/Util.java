package at.omasits.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.fop.svg.PDFTranscoder;
import org.apache.pdfbox.io.IOUtils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class Util {
	public final static String nl = System.getProperty("line.separator");
	
	public final static Splitter tabSplitter = Splitter.on('\t');
	public final static Joiner tabJoiner = Joiner.on('\t');
	
	public static File getUserFile(String filename) {
		File userHome = new File(System.getProperty("user.home"));
		File protterFolder = new File(userHome,".protter");
		if (!protterFolder.exists())
			protterFolder.mkdir();
		return new File(protterFolder, filename);
	}
	public static File getTempFile(String filename) {
		// load or create the tmp folder
		File tmpDir = Util.getUserFile("tmp");
		if (!tmpDir.exists())
			tmpDir.mkdir();
		// create the file
		File tmpFile = new File(tmpDir,filename);
		// mark it as temporary
		tmpFile.deleteOnExit();
		return tmpFile;
	}
	public static void copyFileFromRessource(File file) throws IOException {
		// load default config file from jar-resources
		InputStream streamIn = Config.class.getResourceAsStream("/"+file.getName());
		BufferedInputStream in = new BufferedInputStream(streamIn);
		FileOutputStream streamOut = new FileOutputStream(file);
		BufferedOutputStream out = new BufferedOutputStream(streamOut);
		int i;
		while ((i = in.read()) != -1) {
		    out.write(i);
		}
		out.flush();
		out.close();
	}
	
	public static String substringBetweenStrings(String within, String pre, String post) {
		int from = within.indexOf(pre);
		if (from<0) return null;
		from += pre.length();
		int to = within.indexOf(post, from);
		if (to<0) return null;
		return within.substring(from, to);
	}
	
	public static void transcode2png(File svg, File png) throws IOException, TranscoderException {
        PNGTranscoder t = new PNGTranscoder();
        //t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, 2000.0f);
        //t.addTranscodingHint(PNGTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, 0.5f);
        t.addTranscodingHint(PNGTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, 0.05f);
        //t.addTranscodingHint(PNGTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, 0.02f);
        TranscoderInput input = new TranscoderInput(svg.toURI().toString());
        TranscoderOutput output = new TranscoderOutput(new FileOutputStream(png));
        t.transcode(input, output);
        output.getOutputStream().flush();
        output.getOutputStream().close();
	}

	public static void transcode2pdf(File svg, File pdf) throws Exception {
		// Convert the SVG into PDF
		Transcoder t = new PDFTranscoder();
		TranscoderInput input = new TranscoderInput(svg.toURI().toString()); 
		TranscoderOutput output = new TranscoderOutput(new FileOutputStream(pdf)); 
		t.transcode(input, output);
		output.getOutputStream().flush();
        output.getOutputStream().close();
	}
	
	public static void zip(File in, File out) throws FileNotFoundException, IOException {
		byte[] byteArr =  IOUtils.toByteArray(new FileInputStream(in));
    	FileOutputStream os = new FileOutputStream(out);
        GZIPOutputStream gzip = new GZIPOutputStream(os);
        gzip.write(byteArr);
        gzip.close();
        os.close();
	}
	
	public static <E> String join(Iterable<E> iterable, String separator) {
		if (iterable == null) return null;
		Iterator<E> iterator = iterable.iterator();
		StringBuffer buf = new StringBuffer(256);
		while (iterator.hasNext()) {
			E elem = iterator.next();
			if (elem != null)
				buf.append(elem);
			if (iterator.hasNext())
				buf.append(separator);
		}
		return buf.toString();
	}
	
	public static BufferedReader reader(File file) throws FileNotFoundException, IOException {
		if (file.getName().endsWith(".gz") || file.getName().endsWith(".tgz"))
			return new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
		else
			return new BufferedReader(new FileReader(file));
	}
	
	public static void sendMail(String to, String subject, String body) throws AddressException, MessagingException, UnsupportedEncodingException {
		sendMail(to, subject, body, null);
	}
	
	public static void sendMail(String to, String subject, String body, File attachment) throws AddressException, MessagingException, UnsupportedEncodingException {
		Session session = Session.getDefaultInstance(Config.mailProperties, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(Config.get("mail_username"),Config.get("mail_password"));
				}
			});
 
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(Config.get("mail_from"), "Protter"));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
		message.setSubject(subject);
		
		Multipart multipart = new MimeMultipart();
		
		BodyPart bodypartText = new MimeBodyPart();
		bodypartText.setText(body);
		multipart.addBodyPart(bodypartText);
		
		if (attachment!=null) {
			BodyPart bodypartFile = new MimeBodyPart();
			bodypartFile.setDataHandler(new DataHandler(new FileDataSource(attachment)));
			bodypartFile.setFileName(attachment.getName());
			multipart.addBodyPart(bodypartFile);
		}
		
		message.setContent(multipart);
		
		Log.info("sending mail '"+subject+"' to '"+to+"'");
		Transport.send(message);
	}
	
	public static List<String> tabSplit2List(CharSequence line) {
		return Lists.newArrayList(tabSplitter.split(line));
	}
	
	public static List<String> split2List(CharSequence line, char separator) {
		return Lists.newArrayList(Splitter.on(separator).split(line));
	}
	
	public static String substringUpTo(String str, CharMatcher matcher) {
		int i = matcher.indexIn(Preconditions.checkNotNull(str));
		if (i<0)
			return str;
		else
			return str.substring(0, i);
	}
	
	public static File saveToTempFile(File folder, String content, String fileType) throws IOException {
		String uuid = UUID.randomUUID().toString();
		File f = new File(folder, uuid+"."+fileType);
		BufferedWriter out = new BufferedWriter(new FileWriter(f));
		out.write(content);
		out.close();
		return f;
	}
	
	public static String toDataTableJSON(List<List<? extends Object>> tbl) {
		return "{ \"aaData\": [" +
			Joiner.on(',').join( Lists.transform(tbl, new Function<List<? extends Object>, String> () {
				@Override
				public String apply(List<? extends Object> arg) {
					return "[\"" + Joiner.on("\",\"").join(arg) + "\"]";
				}
		}) ) + "] }";
	}
	
	public static String decodePercent(String str) throws InterruptedException {
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                switch (c) {
                    case '+':
                        sb.append(' ');
                        break;
                    case '%':
                        sb.append((char) Integer.parseInt(str.substring(i + 1, i + 3), 16));
                        i += 2;
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            }
            return sb.toString();
        } catch (Exception e) {
            throw new InterruptedException();
        }
    }
}
