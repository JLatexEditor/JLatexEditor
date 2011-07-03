import scala.xml.XML

val xml = XML.loadFile("data/codehelper/liveTemplates.xml")

val commands = xml \ "command"
for (command <- commands) {
	val name = (command \ "@name").text
	var usage = (command \ "@usage").text
	val hint = (command \ "@hint").text
	val arguments = (command \ "argument").map(x => (x \ "@name").text)
	
	usage = usage.replaceAll("@newline@", "\n").replaceAll("@\\|@", "|")
	for (arg <- arguments) {
		usage = usage.replaceAll("@" + arg + "@", "<i>" + arg + "</i>")
	}

	println("<tr><td>" + name + """</td><td><pre class="wiki">""" + usage + "</pre></td></tr>")
}
