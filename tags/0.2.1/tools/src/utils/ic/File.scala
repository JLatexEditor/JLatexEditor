package utils.ic

import utils.StreamUtils
import java.io
import io._
import utils.scala.ReaderIterator
import InputStream._

/**
 * Implicit casts for {@link java.io.File}.
 *
 * @author Stefan Endrullis <stefan@endrullis.de>
 */
object File {
  implicit def file2FileExt (file: io.File) = new FileExt(file)

  class FileExt (file: io.File) {
    /**
     * Copies this file binary to another.
     */
    def copyTo (dest: io.File) {
      if (dest.exists && dest.isDirectory) {
        copyToFile(new io.File(dest, file.getName))
      } else {
        copyToFile(dest)
      }
    }

    /**
     * Copies this file binary to another.
     */
    def copyToFile (dest: io.File) {
      val in = new FileInputStream(file)
      val out = new FileOutputStream(dest)

      StreamUtils.copyStream(in, out)
      
      in.close
      out.close
    }

    /**
     * Copies this file line by line to another. Therefore newlines in the destination
     * file are based on the operating system.
     */
    def copyLinewiseTo (dest: io.File) {
      if (dest.exists && dest.isDirectory) {
        copyLinewiseToFile(new io.File(dest, file.getName))
      } else {
        copyLinewiseToFile(dest)
      }
    }

    /**
     * Copies this file line by line to another. Therefore newlines in the destination
     * file are based on the operating system.
     */
    def copyLinewiseToFile (dest: io.File) {
      StreamUtils.copyFileLinewise(file, dest)
    }

    /**
     * Reads the file and returns the content as String.
     */
    def getContent: String = {
      new FileInputStream(file).getContent()
    }

	  /**
	   * Returns an iterator for the lines of this file.
	   */
	  def readLines: Iterator[String] = new ReaderIterator(new BufferedReader(new FileReader(file)))
  }
}
