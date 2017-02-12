package at.omasits.proteomics.protter;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import javax.swing.UIManager;

import at.omasits.proteomics.protter.ProtProvider.ProtFile;
import at.omasits.proteomics.protter.ranges.RangeParserProvider;
import at.omasits.util.Config;
import at.omasits.util.Log;
import at.omasits.util.Util;
import at.omasits.util.httpServer.FileServer;
import at.omasits.util.httpServer.NanoHTTPD;
import at.omasits.util.httpServer.NanoHTTPD.Response.Status;


public class ProtterServer extends FileServer {
	public static final int port = Integer.valueOf(Config.get("port","81"));
	public static final String server = Config.get("server","localhost");
	public static final String proxyPrefix = Config.get("proxyPrefix","");
	public static final int proxyPort = Config.get("proxyPort","").length()>0 ? Integer.valueOf(Config.get("proxyPort")) : port;
	public static final String baseUrl = "http://"+server + ((proxyPort!=80) ? ":"+proxyPort : "") + proxyPrefix;
	public static final String bitlyLogin = Config.get("bitly_login");
	public static final String bitlyApiKey = Config.get("bitly_apikey","");
	public static File wwwRoot;
	public static File uploadPath;
	public static String latexPath;
	public static File dvisvgm;

	public ProtterServer(int port) throws IOException {
		super("127.0.0.1", port, wwwRoot, proxyPrefix);
		MIME_TYPES.put("svg", "image/svg+xml");
		MIME_TYPES.put("svgz", "image/svg+xml");
	}
	
	public static void main(String[] args) {
		try	{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			// check for www root folder
			if (Config.get("wwwroot") != null) {
				wwwRoot = new File(Config.get("wwwroot"));
				if ( ! wwwRoot.exists())
					wwwRoot = null;
				uploadPath = new File(wwwRoot, "upload");
			}
			
			// check for dvisvgm
			dvisvgm = new File(Config.get("dvisvgmPath","dvisvgm.exe"));
			if (!dvisvgm.exists()) {
				Log.fatal("DviSvgM not found! Please download and install the latest version from http://dvisvgm.sourceforge.net and re-configure the protter.config file !", true);
				//Util.copyFileFromRessource(dvisvgm);
			}
			
			// check for latex
			latexPath = Config.get("latexPath","latex");
			try {
				Runtime.getRuntime().exec(latexPath + " -version");
			} catch (Exception e) {
				Log.fatal("LaTeX not found! Please download and install the latest MiKTeX version from http://miktex.org and re-configure the protter.config file !", true);
			}
			
			ProtterServer server = new ProtterServer(port);
			try {
				server.start();
			} catch (BindException e) {
				Log.fatal("There is another server running on port "+port+". Stop any running Apache or Skype and try again.", true);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// init rangeparsers
			try {
				RangeParserProvider.init();
			} catch (Exception e) {
				Log.fatal(e.getMessage(), true);
			}
			
			// keep the server running...
			try {
				while(!server.shutdown)
					Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Log.info("Recieved shutdown command - stopping the server...");
			server.stop();
			Log.info("The server is stopped. Goodbye.");
		} catch( Exception e ) {
			Log.fatal("Couldn't start server: " + e, true);
		}
		System.exit(0);
	}
	
	@Override
	public void start() throws IOException {
		super.start();
		String message = "Protter serving at port " + port + ".";
		if (wwwRoot != null)
			message += " Serving files from \"" + wwwRoot.getCanonicalPath() + "\".";
		Log.info(message);
		setupTrayIcon(message);
	}
	
	////////////////////////////
	
	public boolean shutdown = false;
	protected TrayIcon trayIcon;
	
	protected ActionListener openInBrowser = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent ae) {
			String url = "http://localhost:" + port;
			try {
				java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
			} catch (IOException e) {
				Log.error("Could not open Protter in your default web browser. Open up a browser window and navigate to '"+url+"'.");
				e.printStackTrace();
			}
		}
	};

	protected void setupTrayIcon(String message) {
		if (SystemTray.isSupported()) {
			SystemTray tray = SystemTray.getSystemTray();
			PopupMenu popup = new PopupMenu();
			MenuItem openItem = new MenuItem("open Protter in browser");
			openItem.addActionListener(openInBrowser);
			popup.add(openItem);
		    MenuItem quitItem = new MenuItem("stop ProtterServer");
		    quitItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					ProtterServer.this.shutdown = true;
				}
			});
		    popup.add(quitItem);
		    Image image = Toolkit.getDefaultToolkit().getImage(
		    		ProtterServer.class.getResource("/p.png"));
		    trayIcon = new TrayIcon(image, message, popup);
		    trayIcon.setImageAutoSize(true);
		    trayIcon.addActionListener(openInBrowser); // open in browser on click on message
		    try {
		    	tray.add(trayIcon);
		    	trayIcon.displayMessage("Protter Server", 
		    			message,
			            TrayIcon.MessageType.INFO);
		    } catch (AWTException e) {
		        Log.warn("TrayIcon could not be added.");
		    }
		}
	}
	
	@Override
	public Response serve(final String uri, Method method, Map<String,String> header, Map<String,String> parms, Map<String, String> files, Socket socket) {
		long t = System.currentTimeMillis();
		Log.debug("REQUEST "+socket.getInetAddress().getHostAddress()+" "+socket.getInetAddress().getCanonicalHostName()+" "+socket.getPort()+" "+method+" '"+uri+"' '"+header.get("user-agent")+"'");
		Log.info("REQUEST "+method+ ((method==Method.POST)?(""):(" '"+uri+"' '"+parms+"'")) + " '" + header.get("user-agent")+"'");
		// debug the parameters
		//Log.debug("Parameters:");
		//for (Entry<String, String> e : parms.entrySet()) {
		//	Log.debug(e.getKey() + "=" + e.getValue());
		//}
		Response res;
		try {
			if (method==Method.POST) {
				//Log.info("REQUEST "+method+" "+header.get("user-agent")+"'");
				if (uri.equals("/batch")) {
					res = BatchProcess.processBatchRequest(parms, socket);
				} else if (parms.containsKey("SkylineReport")) {
					File tmpFile = Util.saveToTempFile(uploadPath, parms.get("SkylineReport"), "csv");
					Log.info("uploaded SkylineReport "+tmpFile.getName());
					String uriNew = proxyPrefix + uri + "#" + "file=" + tmpFile.getName();   
					res = new Response(Response.Status.REDIRECT, NanoHTTPD.MIME_HTML, "<html><body>Redirected: <a href=\"" + uriNew + "\">" + uriNew + "</a></body></html>");
	                res.addHeader("Location", uriNew);
				} else {
					res = new Response(Status.NOT_FOUND, "", "");					
				}
			} else {
				//Log.info("REQUEST "+method+" '"+uri+"' '"+parms+"' '"+header.get("user-agent")+"'");
				if (uri.equals("/create")) {
					try {
						Log.info(socket.getInetAddress().getHostAddress()+" "+socket.getInetAddress().getCanonicalHostName() + " creating and serving protter file ("+parms+")");
						ProtFile f = ProtProvider.getFile(parms);
						res = serveFile(f.getPath(), header, new File("."), f.fileName);
					} catch (Exception e) {
						e.printStackTrace();
						res = new Response(Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: " + e.getMessage());
					}
				} else if (uri.equals("/shutdown")) {
					shutdown = true;
					res = new Response(Status.OK, MIME_PLAINTEXT, "shutting down Protter server...");
				} else if (uri.equals("/download")) {
					String file = parms.get("file");
					if (file==null || file.length()==0) {
						res = new Response(Status.BAD_REQUEST, MIME_PLAINTEXT, "No file specified!");
					} else {
						Log.info(socket.getInetAddress().getHostAddress()+" "+socket.getInetAddress().getCanonicalHostName() + " downloading file: "+file);
						res = serveFile(file, header, ProtProvider.protsDir, file);
					}
				} else if (uri.equals("/galleries")) {
					//List<List<String>> ll = new ArrayList<List<String>>();
					//ll.add(Arrays.asList("Gecko","Firefox 2.0","Win 98+ / OSX.2+","1.8","A"));
					//ll.add(Arrays.asList("Webkit","Safari 1.3","OSX.3","312.8","A"));
					//ll.add(Arrays.asList("Presto","Opera 9.5","Win 88+ / OSX.3+","-","A"));
					// Util.toDataTableJSON(ll)
					res = new Response(Status.OK, MIME_PLAINTEXT, "");
				} else if (uri.equals("/link")) {
					String url = parms.get("url");
					if (bitlyLogin==null || bitlyApiKey==null) {
						res = new Response(Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Link shortening not configured!");
					} else if (url==null || url.length()==0) {
						res = new Response(Status.BAD_REQUEST, MIME_PLAINTEXT, "No url specified!");
					} else {
						Log.info(socket.getInetAddress().getHostAddress()+" "+socket.getInetAddress().getCanonicalHostName() + " linking url: "+url);
						try {
							BufferedReader br = new BufferedReader(new InputStreamReader(new URL("https://api-ssl.bitly.com/v3/shorten?format=txt&login="+bitlyLogin+"&apiKey="+bitlyApiKey+"&longUrl="+URLEncoder.encode(url,"UTF-8")).openStream()));
							res = new Response(Status.OK, MIME_PLAINTEXT, br.readLine());
							br.close();
						} catch (IOException e) {
							res = new Response(Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Link shortening failed!");
						}
					}
				} else {
					if (wwwRoot != null)
						res = serveFile(uri, header, wwwRoot); // serve file from www root directory
					else
						res = new Response(Status.NOT_FOUND, "", "");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.error(e.getMessage());
			res = new Response(Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
		}
		Log.info("RESPOND "+method+ ((method==Method.POST)?(""):(" '"+uri+"' '"+parms+"'")) + " '" + header.get("user-agent")+"': "+(System.currentTimeMillis()-t)+"ms");
		return res;
	}
}
