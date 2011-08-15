package sce.codehelper;

import java.util.HashMap;

/**
 * Argument type of a CHCommandArgument.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class CHArgumentType {
	public static final String NO_TYPE = "<no type>";
	public static final String[] TYPES = {NO_TYPE, "file", "title", "italic", "bold", "label_def", "label_ref", "cite_key_list", "opening_env", "closing_env"};

	private String name;
	private HashMap<String, String> properties = new HashMap<String, String>();

	public CHArgumentType(String type) {
		if (type.indexOf("|") > 0) {
			String[] strings = type.split("\\|");
			name = strings[0];

			// extract properties
			for (int i = 1; i < strings.length; i++) {
				String[] parts = strings[i].split("=");
				properties.put(parts[0], parts[1]);
			}
		} else {
			name = type;
		}
	}

	public String getName() {
		return name;
	}

	public HashMap<String, String> getProperties() {
		return properties;
	}

	public String getProperty(String defaultExtension) {
		return properties.get(defaultExtension);
	}
}
