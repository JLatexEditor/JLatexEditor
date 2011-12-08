package jlatexeditor.tools

import java.io.File
import utils.ic.File._

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
object SymbolsParser {
	def main(args: Array[String]) {
		val symbolsTxt = new File("../dev/symbols/symbols.txt")

		val lines = symbolsTxt.readLines.filter(!_.matches("\\s*%")).toList

		lines.foreach(println(_))
	}
}
