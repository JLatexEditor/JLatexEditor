package de.endrullis.utils.collections;

import jlatexeditor.addon.EnvironmentUtils;

/**
 * Collection of standard equals methods.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public interface Equals<T> {
	public static final Equals NATURAL_EQUALITY = new Equals() {
		public boolean equals(Object a1, Object a2) {
			return a1.equals(a2);
		}
	};

	public boolean equals(T a1, T a2);
}
