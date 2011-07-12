package jlatexeditor.tools

import scala.xml._

/**
 * Determine how often a documentclass is used via Google Code Search.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
object DocumentclassUsageCounter extends UsageCounter("docclassUsageCounts.properties") {
	val names = (XML.loadFile("docclasses.xml") \ "package" \ "environment").map(node => (node \ "@name").text).sortBy(x => x).distinct
	def getCode(name: String) = "\\\\documentclass(\\[[^\\]]*\\])?\\{" + name + "\\}"
}
