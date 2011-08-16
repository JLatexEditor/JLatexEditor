package de.endrullis.utils.collections;

import java.util.Comparator;

/**
 * Collection of standard comparators.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class Comparators {
	public static final Comparator<Comparable> NATURAL_COMPARATOR = new Comparator<Comparable>() {
		public int compare(Comparable o1, Comparable o2) {
			return o1.compareTo(o2);
		}
	};
}
