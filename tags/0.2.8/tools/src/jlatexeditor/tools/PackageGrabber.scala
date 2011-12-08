package jlatexeditor.tools

import utils.ic.File._
import collection.mutable.{MutableList, HashMap}
import java.io.{PrintStream, FileFilter, File}

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
object PackageGrabber extends App {
	// http://packages.debian.org/sid/texlive-latex-extra

	val listDir = new File("/var/lib/apt/lists");

	val files = listDir.listFiles(new FileFilter {
		def accept(file: File) = file.isFile && file.getName.matches(".*ubuntu.com.*_binary.*") &&
			file.readLines.exists(_.startsWith("Package: texlive"))
	})

	val KeyValue = "([\\w-]+): (.*)".r
	val FollowUp = " (.*)".r
	val EmptyLine = "".r
	val CtanPack = " (.*)--(.*)".r

	var currPackage: Package = null
	var lastKey: String = null
	var ctanPackages = new HashMap[String, CtanPackage]()

	for (file <- files) {
		println("Parsing " + file.getName + "...")

		val packages = new MutableList[Package]()

		file.readLines.foreach{_ match {
			case KeyValue(key, value) =>
				if (key == "Package") {
					currPackage = new Package(value)
					packages += currPackage
				}
				currPackage.attributes(key) = value
				lastKey = key
			case FollowUp(value) =>
				currPackage.attributes(lastKey) += "\n" + value
			case EmptyLine() =>
				currPackage = null
		}}

		val texPackages = packages.filter(_.name.startsWith("texlive"))

		for (p <- texPackages) {
			println("  " + p.name)
			val descLines = p.attributes("Description").split('\n')

			var readCtan = false

			for (line <- descLines) {
				if (readCtan) {
					line match {
						case CtanPack(name, desc) =>
							val pack = CtanPackage (name.trim, desc.trim)
							ctanPackages(pack.name) = pack
						case _ =>
							readCtan = false
					}
				} else
				if (line == "This package includes the following CTAN packages:") {
					readCtan = true
				}
			}
		}
	}

	val outFile = new File("ctan-packages.txt")
	val out = new PrintStream(outFile)

	for (pack <- ctanPackages.values.toArray.sortBy(_.name)) {
		out.println(pack.name + "=" + pack.desc)
	}
	out.flush()
	out.close()

	case class Package(name: String, attributes: HashMap[String, String] = new HashMap[String, String]())
	case class CtanPackage(name: String, desc: String)
}
