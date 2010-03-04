/**
 * @author Jörg Endrullis
 */

package sce.syntaxhighlighting;

public abstract class SyntaxHighlighting extends Thread {
	protected SyntaxHighlighting() {
	}

	protected SyntaxHighlighting(String name) {
		super(name);
	}

	protected SyntaxHighlighting(Runnable target) {
		super(target);
	}
}
