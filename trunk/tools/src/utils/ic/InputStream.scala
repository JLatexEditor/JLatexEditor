package utils.ic

import utils.StreamUtils
import java.io
import io._
import utils.scala.ReaderIterator

/**
 * Implicit casts for {@link InputStream}
 *
 * @author Stefan Endrullis <stefan@endrullis.de>
 */
object InputStream {
	implicit def inputStream2InputStreamExt (in: io.InputStream) = new InputStreamExt(in)

	class InputStreamExt (in: io.InputStream) {
		/**
		 * Writes the input stream to another stream.
		 */
		def writeTo (out: OutputStream) {
			StreamUtils.copyStream(in, out)
		}

		/**
		 * Writes the input stream line by line to another stream. Therefore newlines in the destination
	   * file are based on the operating system.
		 */
		def writeLinewiseTo (out: OutputStream) {
			StreamUtils.copyStreamLinewise(in, out)
		}

	  /**
	   * Writes the input stream to a file.
	   */
	  def writeTo (file: io.File) {
	    val out = new FileOutputStream(file)
	    writeTo(out)
	    out.close
	  }

	  /**
	   * Copies this file line by line to another. Therefore newlines in the destination
	   * file are based on the operating system.
	   */
	  def copyLinewiseTo (file: io.File) {
		  val out = new FileOutputStream(file)
		  writeLinewiseTo(out)
		  out.close
	  }

		/**
		 * Returns the content of the input stream as String.
		 */
		def getContent () = {
			val out = new ByteArrayOutputStream()

			StreamUtils.copyStream(in, out)

			in.close
			out.close

			new String(out.toByteArray)
		}

		/**
		 * Returns an iterator with the lines of this input stream.
		 */
		def readLines: Iterator[String] = new ReaderIterator(new BufferedReader(new InputStreamReader(in)))
	}
}
