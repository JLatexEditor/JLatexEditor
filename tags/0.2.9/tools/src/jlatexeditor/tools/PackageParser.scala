package jlatexeditor.tools

import utils.ic.File._
import utils.ic.InputStream._
import java.io._
import collection.mutable._
import org.apache.commons.lang.StringEscapeUtils

/**
 * Parser for latex cls and sty files.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
object PackageParser {
	val DeclareOption = """[^%]*\\DeclareOption\s*\{([^\}]+)}.*""".r
	val DeclareOptionBeamer = """[^%]*\\DeclareOptionBeamer\s*\{([^\}]+)}.*""".r
	val RequirePackage = """[^%]*\\RequirePackage\s*\{([^\}]+)}.*""".r
	val Def = """[^%]*\\(?:def|let)\s*\\(\w+)([#\[\]\d]*)\s*[\{\\].*""".r
	val DefArgCount = """#+""".r
	val NewCommand = """[^%]*\\newcommand\{?\\(\w+)\}?(?:\[(\d+)\])?(?:\[([^\]]+)\])?.*""".r
	val DeclareSymbol = """[^%]*\\Declare(?:\w*)Symbol\s*\{?\\(\w+)\}?.*""".r
	val NewEnvironment = """[^%]*\\newenvironment\{(\w+)\}(?:\[(\d+)\])?(?:\[([^\]]+)\])?.*""".r
	val NewLength = """[^%]*\\newlength\s*\{?\\(\w+)\}?.*""".r
	val NewCounter = """[^%]*\\newcounter\s*\{(\w+)\}.*""".r
	val Input = """[^%]*\\input\s*\{([^}]+)\}.*""".r
	val DpkgResult = """([\w\.-]+): .*""".r
	val CtanPackSplit = """([^=]+)=(.*)""".r

	val processedFiles = new HashSet[String]
	val files2debPackage = new HashMap[String, DebPackage]
	val ctanPackInfos = {
		val map = new HashMap[String, CtanPackInfo]

		new File("ctan-packages.txt").readLines.foreach( _ match {
			case CtanPackSplit(title, desc) => map += title.toLowerCase -> new CtanPackInfo(title, desc.trim().replace("\\.$", ""))
		})

		map
	}

	def main(args: Array[String]) {
		/*
		val pack = new Package(new File("."), true, "report")
		parseFile(pack, new File("/usr/share/texmf-texlive/tex/latex/base/report.cls"))
		for (opt <- pack.options) {
			println(opt)
		}

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
		val classes = new MutableList[Package]

		texmfDirs.foreach(dir => parse(packs, classes, dir))

		writeXmlFile("packages.xml", null, packs)
		writeXmlFile("docclasses.xml", null, classes)

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

	def writeXmlFile(file: String, templateFile: String, packages: MutableList[Package]) {
		val out = new PrintStream(file)
		out.println("<packages>")
		if (templateFile != null) {
			out.println(new File(templateFile).getContent)
		}
		for (pack <- packages) {
			val optionsPackString = if(pack.options.isEmpty) "" else " options=\"" + pack.options.map(escape(_)).mkString(",") + "\"";
			val requiresPackagesString = if(pack.requiresPackages.isEmpty) "" else " requiresPackages=\"" + pack.requiresPackages.mkString(",") + "\"";
			val ctanPackString = pack.ctanPackInfo.map( pack => " title=\"" + escape(pack.title) + "\" description=\"" + escape(pack.desc) + "\"").getOrElse("")
			val debPackageString = pack.debPackage.map( pack => " debPackage=\"" + pack.name + "\"").getOrElse("")
			val usageCountString = pack.usageCount.map( count => " usageCount=\"" + count + "\"").getOrElse("")
			out.println("  <package name=\"" + pack.name + "\"" + optionsPackString + requiresPackagesString + ctanPackString + debPackageString + usageCountString + ">")
			for (command <- pack.commands.values) {
				val usageCountString = command.usageCount.map( count => " usageCount=\"" + count + "\"").getOrElse("")
				val optArgString = if (command.optionalArgs.isEmpty) "" else " optionalArg=\"" + escape(command.optionalArgs(0)) + "\""
				out.println("    <command name=\"" + command.name + "\" argCount=\"" + command.argCount + "\"" + optArgString + usageCountString + " />")
			}
			for (env <- pack.environments.values) {
				val usageCountString = env.usageCount.map( count => " usageCount=\"" + count + "\"").getOrElse("")
				val optArgString = if (env.optionalArgs.isEmpty) "" else " optionalArg=\"" + escape(env.optionalArgs(0)) + "\""
				out.println("    <environment name=\"" + env.name + "\" argCount=\"" + env.argCount + "\"" + optArgString + usageCountString + " />")
			}
			for (len <- pack.lengths) {
				out.println("    <length name=\"" + len + "\" />")
			}
			for (counter <- pack.counters) {
				out.println("    <counter name=\"" + counter + "\" />")
			}
			out.println("  </package>")
		}
		out.println("</packages>")
		out.flush()
		out.close()
	}

	def escape(text: String) = StringEscapeUtils.escapeHtml(text)

	def parse (packs: MutableList[Package], classes: MutableList[Package], dir: File) {
		for (file <- dir.listFiles()) {
			if (file.isDirectory) {
				parse(packs, classes, file)
			} else
			if (file.isFile) {
				if (file.getName.endsWith(".sty") || file.getName.endsWith(".cls")) {
					val packageName = file.getName.substring(0, file.getName.length() - 4)
					val pack = new Package(file, file.getName.endsWith(".cls"), packageName)
					pack.cls match {
						case false => packs   += pack
						case true  => classes += pack
					}
					print(packs.size + " | " + classes.size + "\r")
					parseFile(pack, file);
				}
			}
		}
	}

	def parseFile (pack: Package, file: File) {
		for(line <- file.readLines) {
			parseLine1(pack, line)
		}
	}

	def parseLine1(pack: Package, line: String) = line match {
		case DeclareOption(option) =>
			pack.options += option
		case DeclareOptionBeamer(option) =>
			pack.options += option
		case RequirePackage(packList) =>
			pack.requiresPackages ++= packList.split(",").toList.map(_.trim()).filterNot(_.contains("<"))
		case Def(cmd, args) =>
			pack.commands += cmd -> new Command(pack, cmd, DefArgCount.findAllIn(args).size)
			if (cmd.startsWith("end")) {
				val envName = cmd.substring(3)
				for (startCmd <- pack.commands.get(envName)) {
					pack.environments += envName -> new Environment(pack, envName, startCmd.argCount, startCmd.optionalArgs)
				}
			}
		case NewCommand(cmd, args, optArg) =>
			val argCount = if (args == null) 0 else args.toInt
			pack.commands += cmd -> new Command(pack, cmd, argCount, if (optArg == null) List() else List(optArg))
		case DeclareSymbol(cmd) =>
			pack.commands += cmd -> new Command(pack, cmd, 0, List())
		case NewEnvironment(cmd, args, optArg) =>
			val argCount = if (args == null) 0 else args.toInt
			pack.environments += cmd -> new Environment(pack, cmd, argCount, if (optArg == null) List() else List(optArg))
		case _ => parseLine2(pack, line)
	}

	/** parseLine is splitted into parseLine1 and parseLine2 because of https://issues.scala-lang.org/browse/SI-1133 */
	def parseLine2(pack: Package, line: String) = line match {
		case NewLength(len) =>
			pack.lengths += len
		case NewCounter(counter) =>
			pack.counters += counter
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
		case _ =>
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

		new DebPackage(name, files)
	}

	class Command(val pack: Package, val name: String, val argCount: Int, val optionalArgs: List[String] = List()) {
		val usageCount = CommandUsageCounter.usageCounts.get(name)
	}
	class Environment(val pack: Package, val name: String, val argCount: Int, val optionalArgs: List[String] = List()) {
		val usageCount = EnvironmentUsageCounter.usageCounts.get(name)
	}
	class Package(val file: File, val cls: Boolean, val name: String, val options: LinkedHashSet[String] = new LinkedHashSet[String],
								val requiresPackages: HashSet[String] = new HashSet[String],
	              val commands: LinkedHashMap[String, Command] = new LinkedHashMap[String, Command],
	              val environments: LinkedHashMap[String, Environment] = new LinkedHashMap[String, Environment],
			          val lengths: HashSet[String] = new HashSet[String],
			          val counters: HashSet[String] = new HashSet[String]) {
		val debPackage = try {
			Some(findDebPackage(file.getAbsolutePath))
		} catch {
			case e: FileNotFoundException => None
		}
		val ctanPackInfo = ctanPackInfos.get(name.toLowerCase)
		val usageCount = if (cls) {
			DocumentclassUsageCounter.usageCounts.get(name)
		} else {
			PackageUsageCounter.usageCounts.get(name)
		}
	}
	class CtanPackInfo(val title: String, val desc: String)
	class DebPackage(val name: String, val files: MutableList[String])
}
