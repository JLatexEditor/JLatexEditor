package jlatexeditor.codehelper;

import jlatexeditor.PackagesExtractor;
import sce.codehelper.CHCommand;
import util.Function1;
import util.TrieSet;

import java.util.Comparator;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public abstract class ExtPatternHelper extends PatternHelper {
	protected static final Comparator<String> STRING_COMPARATOR = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			return o1.compareTo(o2);
		}
	};
	protected static final Function1<TrieSet<PackagesExtractor.Environment>,String> TRIE_SET_2_STRING_FUNCTION = new Function1<TrieSet<PackagesExtractor.Environment>, String>() {
		@Override
		public String apply(TrieSet<PackagesExtractor.Environment> trieSet) {
			return trieSet.getObjects().iterator().next().getName();
		}
	};
	protected  static final Function1<String,CHCommand> STRING_2_CHCOMMAND = new Function1<String, CHCommand>() {
		public CHCommand apply(String packageName) {
			return new ValueCompletion(packageName);
		}
	};
	protected  static final Function1<Environment,String> ENVIRONMENT_2_STRING_FUNCTION = new Function1<Environment, String>() {
		public String apply(Environment env) {
			return env.getName();
		}
	};
	private static final Function1<Command,String> COMMAND_2_STRING_FUNCTION = new Function1<Command, String>() {
		public String apply(Command env) {
			return env.getName();
		}
	};
}
