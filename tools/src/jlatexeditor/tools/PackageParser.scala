package jlatexeditor.tools

import java.io.{FileFilter, File}
import utils.ic.File._

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
object PackageParser {
	val Def = ".*\\\\def\\\\(\\w+)\\{.*".r
	val DefParam = ".*\\\\def\\\\(\\w+)#(\\d+).*".r
	val NewCommand = ".*\\\\newcommand\\{\\\\(\\w+)\\}(?:\\[(\\d+)\\])?(?:\\[([^\\]]+)\\])?.*".r
	val NewEnvironment = ".*\\\\newenvironment\\{(\\w+)\\}(?:\\[(\\d+)\\])?(?:\\[([^\\]]+)\\])?.*".r

	def main(args: Array[String]) {
		parseFile(new File("/usr/share/texmf-texlive/tex/latex/algorithms/algorithmic.sty"))

		if (true) return

		val texmfDirs = new File("/usr/share").listFiles(new FileFilter {
			def accept(file: File) = file.isDirectory && file.getName.startsWith("texmf")
		})

		texmfDirs.foreach(parse(_))
	}

	def parse (dir: File) {
		for (file <- dir.listFiles()) {
			if (file.isDirectory) {
				parse(file)
			} else
			if (file.isFile && file.getName.endsWith(".sty")) {
				val packageName = file.getName.substring(0, file.getName.length() - 4)
				println(packageName)
			}
		}
	}

	def parseFile (file: File) {
		for(line <- file.readLines) {
			line match {
				case Def(cmd) =>
					println(cmd)
				case DefParam(cmd, params) =>
					println(cmd + " with " + params + " arguments")
				case NewCommand(cmd, params, optParam) =>
					println(cmd + " with " + params + " arguments and " + optParam)
				case NewEnvironment(cmd, params, optParam) =>
					println("  environment " + cmd + " with " + params + " arguments and " + optParam)
				case _ =>
			}
		}
	}
}
