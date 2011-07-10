package jlatexeditor.tools

import scala.xml._
import java.net.URL
import com.google.gdata.client.codesearch.CodeSearchService
import com.google.gdata.data.codesearch.CodeSearchFeed
import com.google.gdata.util.ServiceException
import java.io.{FileInputStream, PrintStream}
import java.util.{Properties}
import collection.mutable.HashMap
import scala.collection.JavaConversions._

/**
 * Determine how often a package is used via Google Code Search.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
object  PackageUsageCounter {
	val CODE_SEARCH_FEEDS_URL = "https://www.google.com/codesearch/feeds/search?"
	val codeSearchService = new CodeSearchService("gdata-sample-codesearch")

	def main(args: Array[String]) {
		val packages = (XML.loadFile("packages.xml") \ "package").map(node => (node \ "@name").text).sortBy(x => x).distinct

		val properties = new Properties()
		properties.load(new FileInputStream("packageUsageCounts.properties"))
		val lastValue = new HashMap[String, String]()
		for (entry <- properties.entrySet()) {
			lastValue += entry.getKey.toString -> entry.getValue.toString
		}

		val out = new PrintStream("packageUsageCounts.properties");

		try {
			for (pack <- packages) {
				println(pack)
				//try {
				val usageCountFor = lastValue.getOrElse(pack, {
					Thread.sleep(1000);
					"" + determineUsageCountFor(pack)
				})
				out.println(pack + "=" + usageCountFor)
/*				} catch {
					case e: ServiceException =>
						out.println(pack + "=error")
				}*/
			}
		} catch {
			case e: Exception => e.printStackTrace()
		}
		out.flush()
		out.close()
	}

	def determineUsageCountFor(pack: String) = {
		val query: String = "lang:tex$%20\\\\usepackage\\{" + pack + "\\}"
		val url = new URL(CODE_SEARCH_FEEDS_URL + "q=" + query)
		val feed: CodeSearchFeed = codeSearchService.getFeed(url, classOf[CodeSearchFeed])

		feed.getTotalResults
	}
}
