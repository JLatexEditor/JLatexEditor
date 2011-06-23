package jlatexeditor.tools

import utils.ic.File._
import utils.ic.InputStream._
import java.io._
import collection.mutable._

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
object PackageParser {
	val Def = ".*\\\\def\\s*\\\\(\\w+)([#\\[\\]\\d]*)\\s*\\{.*".r
	val NewCommand = ".*\\\\newcommand\\{\\\\(\\w+)\\}(?:\\[(\\d+)\\])?(?:\\[([^\\]]+)\\])?.*".r
	val NewEnvironment = ".*\\\\newenvironment\\{(\\w+)\\}(?:\\[(\\d+)\\])?(?:\\[([^\\]]+)\\])?.*".r
	val Input = ".*\\\\input\\{([^}]+)\\}".r
	val DpkgResult = "([\\w\\.-]+): .*".r

	val processedFiles = new HashSet[String]
	val files2debPackage = new HashMap[String, DebPackage]

	def main(args: Array[String]) {
		//parseFile(new File("/usr/share/texmf-texlive/tex/latex/algorithms/algorithmic.sty"))
		/*
		val p = findDebPackage(findFile("tikz.sty").getAbsolutePath)
		println(p.name)
		for (f <- p.files) {
			println(f)
		}
		*/

		//parseFile(new Package("asdf"), new File("/usr/share/texmf/tex/latex/pgf/frontendlayer/tikz.sty"))

		val texmfDirs = new File("/usr/share").listFiles(new FileFilter {
			def accept(file: File) = file.isDirectory && file.getName.startsWith("texmf")
		})

		val packs = new MutableList[Package]

		texmfDirs.foreach(dir => parse(packs, dir))

		writeXmlFile(packs)

		/*
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
		*/
	}

	def writeXmlFile(packages: MutableList[Package]) {
		println("<packages>")
		for (pack <- packages) {
			val debPackageString = pack.debPackage.map( pack => " debPackage=\"" + pack.name + "\"").getOrElse("")
			println("  <package name=\"" + pack.name + "\"" + debPackageString + ">")
			for (command <- pack.commands.values) {
				val optArgString = if (command.optionalArgs.isEmpty) "" else " " + command.optionalArgs(0)
				println("    <command name=\"" + command.name + "\" argCount=\"" + command.argCount + "\" optionalArg=\"" + optArgString + " />")
			}
			for (env <- pack.environments.values) {
				val optArgString = if (env.optionalArgs.isEmpty) "" else " " + env.optionalArgs(0)
				println("    <environment name=\"" + env.name + "\" argCount=\"" + env.argCount + "\" optionalArg=\"" + optArgString + " />")
			}
			println("  </package>")
		}
		println("</packages>")
	}

	def parse (packs: MutableList[Package], dir: File) {
		for (file <- dir.listFiles()) {
			if (file.isDirectory) {
				parse(packs, file)
			} else
			if (file.isFile && file.getName.endsWith(".sty")) {
				val packageName = file.getName.substring(0, file.getName.length() - 4)
				//println(packageName)
				val pack = new Package(file, packageName)
				packs += pack
				print(packs.size + "\r")
				parseFile(pack, file);
			}
		}
	}

	def parseFile (pack: Package, file: File) {
		for(line <- file.readLines) {
			line match {
				case Def(cmd, args) =>
					pack.commands += cmd -> Command(pack, cmd, args.count(_ == '#'))
				case NewCommand(cmd, args, optArg) =>
					val argCount = if (args == null) 0 else args.toInt
					pack.commands += cmd -> Command(pack, cmd, argCount, if (optArg == null) List() else List(optArg))
//					println(cmd + " with " + args + " arguments and " + optArgs)
				case NewEnvironment(cmd, args, optArg) =>
					val argCount = if (args == null) 0 else args.toInt
					pack.environments += cmd -> Environment(pack, cmd, argCount, if (optArg == null) List() else List(optArg))
//					println("  environment " + cmd + " with " + args + " arguments and " + optArgs)
				case Input(fileName) =>
					try {
						if (!processedFiles.contains(fileName)) {
							processedFiles += fileName
							//println("  input ." + fileName + ".")
							parseFile(pack, findFile(fileName))
						}
					} catch {
						case e: FileNotFoundException =>
					}
//					parseFile(pack, )
				case _ =>
			}
		}
	}

	def findFile(fileName: String) = {
		val userDir = System.getProperty("user.dir");
		val process = Runtime.getRuntime.exec(Array(userDir + "/scripts/findTexFile", fileName))
		val iter = process.getInputStream.readLines
		if (iter.hasNext) new File(iter.next())
		else throw new FileNotFoundException(fileName)
	}

	def findDebPackage(path: String): DebPackage = {
		files2debPackage.get(path) match {
			case Some(debPackage) => debPackage
			case None =>
				val process = Runtime.getRuntime.exec(Array("dpkg", "-S", path))
				val iter = process.getInputStream.readLines
				if (iter.hasNext) iter.next() match {
					case DpkgResult(name) =>
						val debPackage = getDebPackage(name)
						for (file <- debPackage.files) {
							files2debPackage += file -> debPackage
						}
						debPackage
				}
				else throw new FileNotFoundException(path)
		}
	}

	def getDebPackage(name: String) = {
		val files = new MutableList[String]
		val process = Runtime.getRuntime.exec(Array("dpkg", "-L", name))

		for (file <- process.getInputStream.readLines) {
			files += file
		}

		DebPackage(name, files)
	}

	case class Command(pack: Package, name: String, argCount: Int, optionalArgs: List[String] = List())
	case class Environment(pack: Package, name: String, argCount: Int, optionalArgs: List[String] = List())
	case class Package(file: File, name: String, commands: LinkedHashMap[String, Command] = new LinkedHashMap[String, Command],
	                   environments: LinkedHashMap[String, Environment] = new LinkedHashMap[String, Environment]) {
		val debPackage = try {
			Some(findDebPackage(file.getAbsolutePath))
		} catch {
			case e: FileNotFoundException => None
		}
	}
	case class DebPackage(name: String, files: MutableList[String])
}
