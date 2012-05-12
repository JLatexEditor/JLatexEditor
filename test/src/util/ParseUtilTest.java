package util;

import junit.framework.TestCase;

/**
 * Tests for ParserUtil.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class ParseUtilTest extends TestCase {
	public void testParseItem() throws Exception {
		assertEquals("\\bla", ParseUtil.parseItem("\\newcommand\\bla1[2]{#1 foo #2}", 11)._1);
		assertEquals("\\bla", ParseUtil.parseItem("\\newcommand   \\bla1[2]{#1 foo #2}", 11)._1);
		assertEquals("\\bla", ParseUtil.parseItem("\\newcommand\\bla[2]{#1 foo #2}", 11)._1);
		assertEquals("\\bla", ParseUtil.parseItem("\\newcommand\\bla{#1 foo #2}", 11)._1);
		assertEquals("\\bla", ParseUtil.parseItem("\\newcommand\\bla {#1 foo #2}", 11)._1);
		assertEquals("\\-", ParseUtil.parseItem("\\newcommand\\- {#1 foo #2}", 11)._1);
		assertEquals("\\ ", ParseUtil.parseItem("\\newcommand\\ {#1 foo #2}", 11)._1);
		assertEquals("\\bla1", ParseUtil.parseItem("\\newcommand{\\bla1}[2]{#1 foo #2}", 11)._1);
	}
}
