package at.omasits.proteomics.protter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import at.omasits.util.Log;


public class Style {
	public static class Color {
		private static Map<String,String> namedColorsByName = new HashMap<String, String>() { private static final long serialVersionUID = 5782550602012523376L; {
			put("aliceblue","f0f8ff");
			put("antiquewhite","faebd7");
			put("aqua","00ffff");
			put("aquamarine","7fffd4");
			put("azure","f0ffff");
			put("beige","f5f5dc");
			put("bisque","ffe4c4");
			put("black","000000");
			put("blanchedalmond","ffebcd");
			put("blue","0000ff");
			put("blueviolet","8a2be2");
			put("brown","a52a2a");
			put("burlywood","deb887");
			put("cadetblue","5f9ea0");
			put("chartreuse","7fff00");
			put("chocolate","d2691e");
			put("coral","ff7f50");
			put("cornflowerblue","6495ed");
			put("cornsilk","fff8dc");
			put("crimson","dc143c");
			put("cyan","00ffff");
			put("darkblue","00008b");
			put("darkcyan","008b8b");
			put("darkgoldenrod","b8860b");
			put("darkgray","a9a9a9");
			put("darkgreen","006400");
			put("darkgrey","a9a9a9");
			put("darkkhaki","bdb76b");
			put("darkmagenta","8b008b");
			put("darkolivegreen","556b2f");
			put("darkorange","ff8c00");
			put("darkorchid","9932cc");
			put("darkred","8b0000");
			put("darksalmon","e9967a");
			put("darkseagreen","8fbc8f");
			put("darkslateblue","483d8b");
			put("darkslategray","2f4f4f");
			put("darkslategrey","2f4f4f");
			put("darkturquoise","00ced1");
			put("darkviolet","9400d3");
			put("deeppink","ff1493");
			put("deepskyblue","00bfff");
			put("dimgray","696969");
			put("dimgrey","696969");
			put("dodgerblue","1e90ff");
			put("firebrick","b22222");
			put("floralwhite","fffaf0");
			put("forestgreen","228b22");
			put("fuchsia","ff00ff");
			put("gainsboro","dcdcdc");
			put("ghostwhite","f8f8ff");
			put("gold","ffd700");
			put("goldenrod","daa520");
			put("gray","808080");
			put("green","008000");
			put("greenyellow","adff2f");
			put("grey","808080");
			put("honeydew","f0fff0");
			put("hotpink","ff69b4");
			put("indianred","cd5c5c");
			put("indigo","4b0082");
			put("ivory","fffff0");
			put("khaki","f0e68c");
			put("lavender","e6e6fa");
			put("lavenderblush","fff0f5");
			put("lawngreen","7cfc00");
			put("lemonchiffon","fffacd");
			put("lightblue","add8e6");
			put("lightcoral","f08080");
			put("lightcyan","e0ffff");
			put("lightgoldenrodyellow","fafad2");
			put("lightgray","d3d3d3");
			put("lightgreen","90ee90");
			put("lightgrey","d3d3d3");
			put("lightpink","ffb6c1");
			put("lightsalmon","ffa07a");
			put("lightseagreen","20b2aa");
			put("lightskyblue","87cefa");
			put("lightslategray","778899");
			put("lightslategrey","778899");
			put("lightsteelblue","b0c4de");
			put("lightyellow","ffffe0");
			put("lime","00ff00");
			put("limegreen","32cd32");
			put("linen","faf0e6");
			put("magenta","ff00ff");
			put("maroon","800000");
			put("mediumaquamarine","66cdaa");
			put("mediumblue","0000cd");
			put("mediumorchid","ba55d3");
			put("mediumpurple","9370db");
			put("mediumseagreen","3cb371");
			put("mediumslateblue","7b68ee");
			put("mediumspringgreen","00fa9a");
			put("mediumturquoise","48d1cc");
			put("mediumvioletred","c71585");
			put("midnightblue","191970");
			put("mintcream","f5fffa");
			put("mistyrose","ffe4e1");
			put("moccasin","ffe4b5");
			put("navajowhite","ffdead");
			put("navy","000080");
			put("oldlace","fdf5e6");
			put("olive","808000");
			put("olivedrab","6b8e23");
			put("orange","ffa500");
			put("orangered","ff4500");
			put("orchid","da70d6");
			put("palegoldenrod","eee8aa");
			put("palegreen","98fb98");
			put("paleturquoise","afeeee");
			put("palevioletred","db7093");
			put("papayawhip","ffefd5");
			put("peachpuff","ffdab9");
			put("peru","cd853f");
			put("pink","ffc0cb");
			put("plum","dda0dd");
			put("powderblue","b0e0e6");
			put("purple","800080");
			put("red","ff0000");
			put("rosybrown","bc8f8f");
			put("royalblue","4169e1");
			put("saddlebrown","8b4513");
			put("salmon","fa8072");
			put("sandybrown","f4a460");
			put("seagreen","2e8b57");
			put("seashell","fff5ee");
			put("sienna","a0522d");
			put("silver","c0c0c0");
			put("skyblue","87ceeb");
			put("slateblue","6a5acd");
			put("slategray","708090");
			put("slategrey","708090");
			put("snow","fffafa");
			put("springgreen","00ff7f");
			put("steelblue","4682b4");
			put("tan","d2b48c");
			put("teal","008080");
			put("thistle","d8bfd8");
			put("tomato","ff6347");
			put("turquoise","40e0d0");
			put("violet","ee82ee");
			put("wheat","f5deb3");
			put("white","ffffff");
			put("whitesmoke","f5f5f5");
			put("yellow","ffff00");
			put("yellowgreen","9acd32");
		}};
//		private static Map<String,String> namedColorsByColor = new HashMap<String, String>() {{
//			for (java.util.Map.Entry<String, String> e : namedColorsByName.entrySet()) {
//				put(e.getValue(), e.getKey());
//			}
//		}};
		
	    public final String code;
	    public final String name;
	    public Color(String code) { this(code, code); }
	    public Color(String code, String name) { this.code = "#"+code; this.name = name; }
	    public static Color fromString(String col) throws Exception {
	    	if (namedColorsByName.containsKey(col.toLowerCase()))
	    		return new Color(namedColorsByName.get(col.toLowerCase()), col.toLowerCase());
	    	else if (col.toLowerCase().matches("[a-f0-9]{6}"))
	    		return new Color(col.toLowerCase());
	    	else
	    		Log.errorThrow("Unkown color: "+col);
	    	return null;
			//try { return namedColors.get(col.toLowerCase()); }
			//catch (IllegalArgumentException e) { throw new Exception("Unkown color: "+col);	}
	    }
	}
	public static enum Shape {
		circ, 
		box, 
		diamond;
		public static Shape fromString(String shape) throws Exception {
			try { return Shape.valueOf(shape.toLowerCase()); }
			catch (IllegalArgumentException e) { Log.errorThrow("Unkown shape: "+shape);	}
			return null;
    	}
	};
	
	public Shape shape;
	public Color frameColor;
	public Color backgroundColor;
	public Color charColor;
	public String name;

	public Style(Shape shape, Color frameColor, Color backgroundColor, Color charColor, String name) {
		this.shape = shape;
		this.frameColor = frameColor;
		this.backgroundColor = backgroundColor;
		this.charColor = charColor;
		this.name = name;
	}
	
	public void overlayWithNewStyle(Style newStyle) {
		if (newStyle.shape!=null) this.shape = newStyle.shape;
		if (newStyle.frameColor!=null) this.frameColor = newStyle.frameColor;
		if (newStyle.backgroundColor!=null) this.backgroundColor = newStyle.backgroundColor;
		if (newStyle.charColor!=null) this.charColor = newStyle.charColor;
		if (newStyle.name!=null) this.name = (this.name!=null ? this.name+" &amp; " : "") + newStyle.name;
	}
	
	@Override
	public String toString() {
		List<String> str = new ArrayList<String>(4);
		if (shape!=null) str.add("s:"+shape.name());
		if (frameColor!=null) str.add("fc:"+frameColor.name);
		if (backgroundColor!=null) str.add("bc:"+backgroundColor.name);
		if (charColor!=null) str.add("cc:"+charColor.name);
		if (name!=null) str.add("n:"+name);
		return StringUtils.join(str,",");
	}
	@Override
	public boolean equals(Object other) {
		if (other instanceof Style) {
			Style that = (Style) other;
			return that.backgroundColor.equals(this.backgroundColor)
					&& that.frameColor.equals(this.frameColor)
					&& that.charColor.equals(this.charColor)
					&& that.shape.equals(this.shape)
					&& that.name.equals(this.name);
		} else {
			return false;
		}
	}
	@Override
	public int hashCode() {
		int hashCode = 31;
		if (shape != null)
			hashCode = (hashCode + shape.ordinal()) * 31;
		if (frameColor != null)
			//hashCode = (hashCode + frameColor.ordinal()) * 31;
			hashCode = (hashCode + frameColor.code.hashCode()) * 31;
		if (backgroundColor != null)
			//hashCode = (hashCode + backgroundColor.ordinal()) * 31;
			hashCode = (hashCode + backgroundColor.code.hashCode()) * 31;
		if (charColor != null)
			//hashCode = (hashCode + charColor.ordinal()) * 31;
			hashCode = (hashCode + charColor.code.hashCode()) * 31;
		if (name != null)
			hashCode = (hashCode + name.hashCode()) * 31;
		return hashCode;
	}
	
	
	public static Style fromString(String styleString) throws Exception {
		// defaults
		Shape s = null;
		Color fc = null;
		Color bc = null;
		Color cc = null;
		String n = null;
		
		String[] arr = styleString.split(",");
		for(String styleElem : arr) {
			if (styleElem.equalsIgnoreCase("inactive"))
				return null;
			String param = styleElem.substring(0,styleElem.indexOf(':'));
			String value = styleElem.substring(styleElem.indexOf(':')+1);
			if (param.equals("s")) {
				s = Shape.fromString(value);
			} else if (param.equals("fc")) {
				fc = Color.fromString(value);
			} else if (param.equals("bc")) {
				bc = Color.fromString(value);
			} else if (param.equals("cc")) {
				cc = Color.fromString(value);
			} else if (param.equals("n")) {
				n = value;
			} else {
				Log.errorThrow("invalid style: "+styleString);
			}
		}
		return new Style(s, fc, bc, cc, n);
	}
}
