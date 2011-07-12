package jlatexeditor.tools

import scala.xml._
import java.io.{PrintStream}

/**
 * Determine how often a package is used via Google Code Search.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
object PackageUsageCounter extends UsageCounter("packageUsageCounts.properties") {
	val names = (XML.loadFile("packages.xml") \ "package").map(node => (node \ "@name").text).sortBy(x => x).distinct
	def getCode(name: String) = "\\\\usepackage\\{" + name + "\\}"
}
