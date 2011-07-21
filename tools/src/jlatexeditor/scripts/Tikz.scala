package jlatexeditor.scripts

import collection.mutable.{MutableList}

/**
 * Tikz scripting in JLE.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
object Tikz {
	/**
	 * Example code:
	 *
	 * node("root") {
	 *   --("1")--> node("sub1")
	 *   edge("1") to node("sub2")
	 *   node("sub3")
	 * }
	 * "node" {
	 *   "sub1"
	 *   "sub2"
	 * }
   */
	object Trees {
		private var nodesStack = List[Node]()
		implicit def string2node(s: String) = new Node(s)

		def node (label: String) = new Node(label)
		def --(edgeLabel: String) = new {
			def -->(n: Node) = {n.edgeFromParent = new EdgeFromParent(edgeLabel); n}
		}

		class Node(val label: String, val subs: MutableList[Node] = new MutableList[Node], var edgeFromParent: EdgeFromParent = null) {
			// add this node to parent
			nodesStack match {
				case parent :: _ => parent.subs += this
				case _ =>
			}

			def apply(block: => Unit) = {
				// put this node on stack
				nodesStack = this :: nodesStack
				// execute block for inner nodes
				block
				// remove this node from stack
				nodesStack = nodesStack.tail
				// return this node
				this
			}

			def print() {
				val sb = new StringBuilder
				printTo(sb, "")
				println(sb.toString())
			}

			def printTo(sb: StringBuilder, indent: String) {
				sb.append(indent).append(label)
				if (!subs.isEmpty) {
					sb.append("[\n")
					for (sub <- subs) {
						sub.printTo(sb, indent + "  ")
					}
					sb.append(indent).append("]")
				}
				sb.append("\n")
			}
		}

		case class EdgeFromParent (label: String)
	}
}

object Test {
	def main(args: Array[String]) {
		import jlatexeditor.scripts.Tikz.Trees._

		node("asdf") {
			--("a")--> node("1") {
				node("b")
			}
			node("2")
		}.print()
	}
}
