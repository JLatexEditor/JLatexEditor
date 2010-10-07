package de.endrullis.utils;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class StringUtilsTest extends TestCase {
	public void testTokenize() {
		assertEquals(Arrays.asList(new String[]{"this", "is", "a", "test"}), StringUtils.tokenize("this is a test"));
		assertEquals(Arrays.asList(new String[]{"this", "is", "a", "test"}), StringUtils.tokenize("  this is   a test  "));
		assertEquals(Arrays.asList(new String[]{"this is", "a", "test"}), StringUtils.tokenize("\"this is\" a test"));
		assertEquals(Arrays.asList(new String[]{"this", "\"is", "a\"", "test"}), StringUtils.tokenize("this \\\"is a\\\" test"));
	}
}
