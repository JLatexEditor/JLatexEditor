package sce.codehelper;

import util.Function1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Functions for mapping attribute values to other attributes.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class CHFunctions {
	private static HashMap<String, Function1<String, String>> functions = new HashMap<String, Function1<String, String>>() {{
		put("id", new Function1<String, String>() {
			public String apply(String a1) {
				return a1;
			}
		});
		put("label", new LabelGenerator());
		put("lower_case", new Function1<String, String>() {
			public String apply(String a1) {
				return a1.toLowerCase();
			}
		});
		put("upper_case", new Function1<String, String>() {
			public String apply(String a1) {
				return a1.toUpperCase();
			}
		});
	}};
	private static HashMap<Function1<String, String>, String> functionNames = new HashMap<Function1<String, String>, String>() {{
		for (Map.Entry<String, Function1<String, String>> entry : functions.entrySet()) {
			put(entry.getValue(), entry.getKey());
		}
	}};

	public static Function1<String, String> get(String name) {
		return functions.get(name);
	}

	public static String getName(Function1<String, String> function) {
		return functionNames.get(function);
	}

	public static ArrayList<String> getFunctionNames() {
		ArrayList<String> functionNames = new ArrayList<String>();
		for (String s : functions.keySet()) {
			functionNames.add(s);
		}
		return functionNames;
	}


// inner classes
	public static class LabelGenerator implements Function1<String, String> {
		@Override
		public String apply(String label) {
			return label.toLowerCase().replaceAll("\\s+", "_").replaceAll("[^\\p{L}0-9-_]", "");
		}
	}
}
