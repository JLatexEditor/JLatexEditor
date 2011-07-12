package jlatexeditor.tools

import scala.xml._

/**
 * Determine how often an environment is used via Google Code Search.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
object EnvironmentUsageCounter extends UsageCounter("environmentUsageCounts.properties") {
	val names = ((XML.loadFile("packages.xml") ++ XML.loadFile("docclasses.xml")) \ "package" \ "environment").map(node => (node \ "@name").text).sortBy(x => x).distinct
	//val names2 = (XML.loadFile("packages.xml") \ "package" \ "environment").map(node => (node \ "@name").text).sortBy(x => x).distinct
	def getCode(name: String) = "\\\\begin\\{" + name + "\\}"
}
