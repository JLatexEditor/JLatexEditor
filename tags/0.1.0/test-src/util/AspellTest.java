package util;

import junit.framework.TestCase;

import java.io.IOException;

/**
 * Test cases for the aspell wrapper.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class AspellTest extends TestCase {
	public void testAspell() throws IOException {
		Aspell aspell = new Aspell("en_GB");

		assertEquals("en", aspell.getLang());
		assertEquals("en_GB", aspell.getMasterLang());

		assertTrue  (aspell.check("the").isCorrect());
		assertFalse (aspell.check("bla").isCorrect());
		assertEquals("misspelled; suggestions: [line break, line-break, canebrake, linebacker, lawbreaker]", aspell.check("linebreak").toString());

		// add "teh" to personal dict
		aspell.addToPersonalDict("teh");
		assertTrue (aspell.check("teh").isCorrect());

		// remove "teh" from personal dict
		aspell.removeFromPersonalDict("teh");
		assertFalse (aspell.check("teh").isCorrect());

		aspell.shutdown();
	}
}
