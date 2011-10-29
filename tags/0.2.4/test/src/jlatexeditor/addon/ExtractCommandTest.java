package jlatexeditor.addon;

import junit.framework.TestCase;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class ExtractCommandTest extends TestCase {
	public void testTemplateTransformation() {
		assertEquals("", new ExtractCommand.Template("foo", "").toString());
		assertEquals("123", new ExtractCommand.Template("foo", "123").toString());
		assertEquals("1#23", new ExtractCommand.Template("foo", "1#23").toString());
		assertEquals("#1", new ExtractCommand.Template("foo", "#1").toString());
		assertEquals("#1+#1", new ExtractCommand.Template("foo", "#1+#1").toString());
		assertEquals("#1+#2", new ExtractCommand.Template("foo", "#1+#2").toString());

		assertEquals("", new ExtractCommand.Template("foo", "").toInputRegEx());
		assertEquals("123", new ExtractCommand.Template("foo", "123").toInputRegEx());
		assertEquals("1(.*?)3", new ExtractCommand.Template("foo", "1#23").toInputRegEx());
		assertEquals("(.*?)", new ExtractCommand.Template("foo", "#1").toInputRegEx());
		assertEquals("(.{1-50}?)\\+\\1", new ExtractCommand.Template("foo", "#1+#1").toInputRegEx());
		assertEquals("(.*?)\\+(.*?)", new ExtractCommand.Template("foo", "#1+#2").toInputRegEx());

		assertEquals(true, new ExtractCommand.Template("foo", "#1+#1").isValid());
		assertEquals(false, new ExtractCommand.Template("foo", "#2+#2").isValid());
		assertEquals(true, new ExtractCommand.Template("foo", "#1+#2").isValid());
		assertEquals(false, new ExtractCommand.Template("foo", "#1+#3").isValid());

		assertEquals("\\\\foo{\\1}", new ExtractCommand.Template("foo", "#1+#1").toReplacement());
		assertEquals("\\\\foo{\\1}{\\2}", new ExtractCommand.Template("foo", "#1+#2").toReplacement());
		assertEquals("\\\\foo{\\2}{\\1}", new ExtractCommand.Template("foo", "#2+#1").toReplacement());

		assertEquals("\\DeclareRobustCommand{\\foo}{bar}", new ExtractCommand.Template("foo", "bar").toDeclaration());
		assertEquals("\\DeclareRobustCommand{\\foo}[1]{#1+#1}", new ExtractCommand.Template("foo", "#1+#1").toDeclaration());
		assertEquals("\\DeclareRobustCommand{\\foo}[2]{#1+#2}", new ExtractCommand.Template("foo", "#1+#2").toDeclaration());
		assertEquals("\\DeclareRobustCommand{\\foo}[2]{#2+#1}", new ExtractCommand.Template("foo", "#2+#1").toDeclaration());
	}
}
