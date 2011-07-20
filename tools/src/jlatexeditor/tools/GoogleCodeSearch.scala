package jlatexeditor.tools

import java.net.URL
import com.google.gdata.data.codesearch.CodeSearchFeed
import com.google.gdata.client.codesearch.CodeSearchService
import sun.management.snmp.jvminstr.JvmThreadInstanceEntryImpl.ThreadStateMap

/**
 * Small Google Code Search API.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
object GoogleCodeSearch {
	val CODE_SEARCH_FEEDS_URL = "https://www.google.com/codesearch/feeds/search?"
	val codeSearchService = new CodeSearchService("gdata-sample-codesearch")

	def determineUsageCountForLatexCode(code: String): Int = {
		val query: String = "lang:tex$%20" + code;
		val url = new URL(CODE_SEARCH_FEEDS_URL + "q=" + query)

		var trials = 0
		while (true) {
			trials += 1
			try {
				val feed: CodeSearchFeed = codeSearchService.getFeed(url, classOf[CodeSearchFeed])
				return feed.getTotalResults
			} catch {
				case e =>
					if (trials > 1) {
						println("Failed to retrieve " + url)
						//throw e
						return -1
					} else {
						println("  waiting 10s")
						Thread.sleep(10000)
					}
			}
		}
		-1
	}
}
