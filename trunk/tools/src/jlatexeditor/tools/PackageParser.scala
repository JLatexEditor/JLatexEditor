package jlatexeditor.tools

import utils.ic.File._
import java.lang.Package
import java.io._
import collection.mutable.{HashMap, HashSet, MutableList}

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
object PackageParser {
	val Def = ".*\\\\def\\s*\\\\(\\w+)([#\\[\\]\\d]*)\\s*\\{.*".r
	val NewCommand = ".*\\\\newcommand\\{\\\\(\\w+)\\}(?:\\[(\\d+)\\])?(?:\\[([^\\]]+)\\])?.*".r
	val NewEnvironment = ".*\\\\newenvironment\\{(\\w+)\\}(?:\\[(\\d+)\\])?(?:\\[([^\\]]+)\\])?.*".r
	val Input = ".*\\\\input\\{([^}]+)\\}".r

	val processedFiles = new HashSet[String]

	def main(args: Array[String]) {
		//parseFile(new File("/usr/share/texmf-texlive/tex/latex/algorithms/algorithmic.sty"))
		parseFile(new Package("asdf"), new File("/usr/share/texmf/tex/latex/pgf/frontendlayer/tikz.sty"))

		val texmfDirs = new File("/usr/share").listFiles(new FileFilter {
			def accept(file: File) = file.isDirectory && file.getName.startsWith("texmf")
		})

		val packs = new MutableList[Package]

		texmfDirs.foreach(dir => parse(packs, dir))

		val commandName2command = new HashMap[String, MutableList[Command]]()
		for (pack <- packs; command <- pack.commands) {
			commandName2command.getOrElseUpdate(command.name, new MutableList[Command]) += command
		}

		for (key <- commandName2command.keys) {
			println(key)
			for (command <- commandName2command(key)) {
				println("  " + command.name + " : " + command.argCount + " : " + command.pack.name)
			}
		}
	}

	def parse (packs: MutableList[Package], dir: File) {
		for (file <- dir.listFiles()) {
			if (file.isDirectory) {
				parse(packs, file)
			} else
			if (file.isFile && file.getName.endsWith(".sty")) {
				val packageName = file.getName.substring(0, file.getName.length() - 4)
				//println(packageName)
				val pack = new Package(packageName)
				packs += pack
				parseFile(pack, file);
			}
		}
	}

	def parseFile (pack: Package, file: File) {
		for(line <- file.readLines) {
			line match {
				case Def(cmd, args) =>
					pack.commands += Command(pack, cmd, args.count(_ == '#'))
				case NewCommand(cmd, args, optArg) =>
					val argCount = if (args == null) 0 else args.toInt
					pack.commands += Command(pack, cmd, argCount, if (optArg == null) List() else List(optArg))
//					println(cmd + " with " + args + " arguments and " + optArgs)
				case NewEnvironment(cmd, args, optArg) =>
//					println("  environment " + cmd + " with " + args + " arguments and " + optArgs)
				case Input(fileName) =>
					try {
						if (!processedFiles.contains(fileName)) {
							processedFiles += fileName
							//println("  input ." + fileName + ".")
							parseFile(pack, find(fileName))
						}
					} catch {
						case e: FileNotFoundException =>
					}
//					parseFile(pack, )
				case _ =>
			}
		}
	}

	def find(fileName: String) = {
		val userDir = System.getProperty("user.dir");
		val process = Runtime.getRuntime.exec(Array(userDir + "/scripts/findTexFile", fileName))
		val r = new BufferedReader(new InputStreamReader(process.getInputStream))
		val s = r.readLine()
		if (s == null) throw new FileNotFoundException(fileName)
		else new File(s)
	}

	case class Command(pack: Package, name: String, argCount: Int, optionalArgs: List[String] = List())
	case class Environment(pack: Package, name: String, argCount: Int, optionalArgs: List[String] = List())
	case class Package(name: String, commands: MutableList[Command] = new MutableList[Command],
	                   environments: MutableList[Environment] = new MutableList[Environment])
}
