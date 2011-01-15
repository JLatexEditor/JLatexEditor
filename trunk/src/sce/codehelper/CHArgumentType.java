package sce.codehelper;

import java.util.HashMap;
import java.util.Properties;

/**
 * Argument type of a CHCommandArgument.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class CHArgumentType {
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
