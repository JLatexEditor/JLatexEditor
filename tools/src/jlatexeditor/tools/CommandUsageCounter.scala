package jlatexeditor.tools

import scala.xml._
import java.io.PrintStream

/**
 * Determine how often a command is used via Google Code Search.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
object CommandUsageCounter extends UsageCounter("commandUsageCounts.properties") {
	lazy val names = ((XML.loadFile("packages.xml") ++ XML.loadFile("docclasses.xml")) \ "package" \ "command").map(node => (node \ "@name").text).sortBy(x => x).distinct
	def getCode(name: String) = "\\\\" + name + "\\b"
}
