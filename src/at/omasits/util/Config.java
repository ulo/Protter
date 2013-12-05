package at.omasits.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;


public class Config {
	private static Properties properties;
	public static Properties mailProperties;
	static {
		try {
			properties = new Properties();
			File cfg = new File("protter.config");
//			if (!cfg.exists()) {
//				// load default config file from jar-resources
//				Util.copyFileFromRessource(cfg);
//			}
			InputStream in = new FileInputStream(cfg);
			properties.load(in);
			in.close();
			
			mailProperties = new Properties();
			for (String prop : properties.stringPropertyNames()) {
				if (prop.startsWith("mail"))
					mailProperties.setProperty(prop, properties.getProperty(prop));
			}
		} catch (Exception e) {
			Log.fatal("Could not load configuration file 'protter.config'.");
		}
	}
	
	public static String get(String key) {
		return get(key, null);
	}
	public static String get(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}
}