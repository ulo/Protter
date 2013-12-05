package at.omasits.proteomics.protter.ranges;

import java.util.ArrayList;
import java.util.List;

import at.omasits.util.Config;


public class RangeParserProvider {
	// a static, ordered, list of all available range parser classes
	private static List<IRangeParser> rangeProviders;
	
	public static void init() throws Exception {
		rangeProviders = new ArrayList<IRangeParser>();
		String classNames = Config.get("rangeparsers");
		try {
			for (String className : classNames.split(",")) {
				Class<?> rangeProviderClass = Class.forName(className);
				if (IRangeParser.class.isAssignableFrom(rangeProviderClass)) {
					rangeProviders.add((IRangeParser) rangeProviderClass.newInstance());
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new Exception("Could not find the extension '"+e.getMessage()+"'. Please check the protter.config file and try again.");
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new Exception("Could not load the extension '"+e.getMessage()+"'. Please check the protter.config file and try again.");
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new Exception("Could not load the extension '"+e.getMessage()+"'. Please check the protter.config file and try again.");
		}
	}
	
	public static IRangeParser getMatchingRangeProvider(String rangeString) throws Exception {
		if (rangeProviders == null) init();
		for (IRangeParser rangeProvider : rangeProviders) {
			if (rangeProvider.matches(rangeString))
				return rangeProvider;
		}
		return null;
	}
}
