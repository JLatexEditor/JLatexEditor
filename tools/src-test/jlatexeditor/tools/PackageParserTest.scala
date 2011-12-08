package jlatexeditor.tools

import junit.framework.Assert._
import jlatexeditor.tools.PackageParser._
import junit.framework.{TestCase}

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
class PackageParserTest extends TestCase {
	def testRegExes() {
		"""    \def\chaptermark##1{%""" match {
			case Def(name, args) =>
				assertEquals("chaptermark", name)
				assertEquals(1, DefArgCount.findAllIn(args).size)
		}

		"""\newcommand\listoffigures{%""" match {
			case NewCommand(name, args, optArg) =>
				assertEquals("listoffigures", name)
				assertEquals(null, args)
				assertEquals(null, optArg)
		}

		"""\let\endtabularx\relax""" match {
			case Def(name, args) =>
				assertEquals("endtabularx", name)
				assertEquals("", args)
		}

		"""\newlength{\blalen}\x""" match {
			case NewLength(len) =>
				assertEquals("blalen", len)
		}

		"""\newlength \blalen\x""" match {
			case NewLength(len) =>
				assertEquals("blalen", len)
		}
	}
}
