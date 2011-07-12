package jlatexeditor.tools

import java.net.URL
import com.google.gdata.data.codesearch.CodeSearchFeed
import com.google.gdata.client.codesearch.CodeSearchService

/**
 * Small Google Code Search API.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
object GoogleCodeSearch {
	val CODE_SEARCH_FEEDS_URL = "https://www.google.com/codesearch/feeds/search?"
	val codeSearchService = new CodeSearchService("gdata-sample-codesearch")

	def determineUsageCountForLatexCode(code: String) = {
		val query: String = "lang:tex$%20" + code;
		val url = new URL(CODE_SEARCH_FEEDS_URL + "q=" + query)
		val feed: CodeSearchFeed = codeSearchService.getFeed(url, classOf[CodeSearchFeed])

		feed.getTotalResults
	}
}
