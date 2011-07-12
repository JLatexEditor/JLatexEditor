package jlatexeditor.tools

import java.util.Properties
import collection.mutable.HashMap
import scala.collection.JavaConversions._
import java.io.{File, PrintStream, FileInputStream}

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
abstract class UsageCounter(val fileName: String) {
	val names: Seq[String]
	def getCode(name: String): String

	lazy val usageCounts = {
		val lastValue = new HashMap[String, String]()
		if (new File(fileName).exists()) {
			val properties = new Properties()
			properties.load(new FileInputStream(fileName))
			for (entry <- properties.entrySet()) {
				lastValue += entry.getKey.toString -> entry.getValue.toString
			}
		}
		lastValue
	}

	def main(args: Array[String]) {
		val lastValue = usageCounts

		val out = new PrintStream(fileName);

		try {
			for (name <- names) {
				println(name)
				val usageCountFor = lastValue.getOrElse(name, {
					Thread.sleep(100);
					"" + GoogleCodeSearch.determineUsageCountForLatexCode(getCode(name))
				})
				out.println(name + "=" + usageCountFor)
			}
		} catch {
			case e: Exception => e.printStackTrace()
		}
		out.flush()
		out.close()
	}
}
