package at.omasits.util;


import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;


public class Log {
	private static Logger log4jLogger;
	static {
		try {
			log4jLogger = Logger.getRootLogger();
			Layout layout = new PatternLayout("%d [%p] %m%n"); //[%-5p]
			ConsoleAppender consoleAppender = new ConsoleAppender( layout );
			log4jLogger.addAppender( consoleAppender );
			File logFile = new File("protterServer.log"); //Util.getUserFile("info.log");
			FileAppender fileAppender = new FileAppender( layout, logFile.getAbsolutePath(), true );
			log4jLogger.addAppender( fileAppender );
		} catch (IOException e) {
			e.printStackTrace();
		}
		log4jLogger.setLevel( Level.toLevel(Config.get("loglevel")) );
	}
    public static void debug(Object o) {
    	log4jLogger.debug(o);
    }
    public static void warn(Object o) {
    	log4jLogger.warn(o);
    }
    public static void info(Object o) {
    	log4jLogger.info(o);
    }
    public static void error(Object o) {
    	log4jLogger.error(o);
    }
    public static void errorThrow(Object o) throws Exception {
    	log4jLogger.error(o);
    	throw new Exception(o.toString());
    }
    public static void fatal(Object o) {
    	fatal(o, false);
    }
    public static void fatal(Object o, boolean messageBox) {
    	if (messageBox)
    		JOptionPane.showMessageDialog(null, o, "Protter Server", JOptionPane.ERROR_MESSAGE);
    	log4jLogger.fatal(o);
    	System.exit(1);
    }
    public static void setLevel(Level l) {
    	log4jLogger.setLevel(l);
    }
}
