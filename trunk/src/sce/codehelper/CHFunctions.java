package sce.codehelper;

import sun.org.mozilla.javascript.internal.Function;
import util.Function1;

import java.util.HashMap;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class CHFunctions {
	private static HashMap<String, Function1<String, String>> functions = new HashMap<String, Function1<String, String>>() {{
		put("label", new LabelGenerator());
	}};

	public static Function1<String, String> get(String name) {
		return functions.get(name);
	}


// inner classes
	public static class LabelGenerator implements Function1<String, String> {
		@Override
		public String apply(String label) {
			return label.toLowerCase().replaceAll("\\s+", "_").replaceAll("[^\\p{L}0-9-_]", "");
		}
	}
}
