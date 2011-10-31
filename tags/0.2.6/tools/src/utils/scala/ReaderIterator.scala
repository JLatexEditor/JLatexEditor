package utils.scala

import java.io.{InputStream, BufferedReader, InputStreamReader}

/**
 * Transforms an InputStream into an Iterator that reads the stream line by line.
 *
 * @author Stefan Endrullis <stefan@endrullis.de>
 */
class ReaderIterator(val r: BufferedReader) extends Iterator[String] {
	def this(in: InputStream) = this(new BufferedReader(new InputStreamReader(in)))

	private var lastLine: String = null
	def hasNext: Boolean = {
		if (lastLine != null) return true
		lastLine = r.readLine
		return lastLine != null
	}
	def next: String = {
		if (lastLine != null) {
			var ret = lastLine
			lastLine = null
			ret
		} else {
			r.readLine
		}
	}
}
