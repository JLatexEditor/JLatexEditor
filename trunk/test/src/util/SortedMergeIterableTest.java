package util;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class SortedMergeIterableTest extends TestCase {
	public void testClass() {
		List<String> list1 = Arrays.asList("01", "04", "05", "12", "17");
		List<String> list2 = Arrays.asList("02", "06", "07", "13", "15");
		List<String> list3 = Arrays.asList("03", "08", "09", "10", "11", "14", "16");

		Comparator<String> comparator = new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			}
		};
		SortedMergeIterable<String> sortedMergeIterable = new SortedMergeIterable<String>(comparator, list1, list2, list3);

		StringBuilder sb = new StringBuilder();
		for (String s : sortedMergeIterable) {
			sb.append(s).append(", ");
		}

		assertEquals("01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11, 12, 13, 14, 15, 16, 17, ", sb.toString());
	}
}
