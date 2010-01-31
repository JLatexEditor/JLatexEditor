package sce.codehelper;

/**
 * Word and its position in the document and its replacement.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class WordReplacement {
	WordWithPos wordWithPos;
	String replacement;

	public WordReplacement(WordWithPos wordWithPos, String replacement) {
		this.wordWithPos = wordWithPos;
		this.replacement = replacement;
	}
}
