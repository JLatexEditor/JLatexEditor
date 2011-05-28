/**
 * @author Jörg Endrullis
 */

package sce.syntaxhighlighting;

public abstract class SyntaxHighlighting extends Thread {
	protected SyntaxHighlighting() {
		setDaemon(true);
	}

	protected SyntaxHighlighting(String name) {
		super(name);
		setDaemon(true);
	}

	protected SyntaxHighlighting(Runnable target) {
		super(target);
		setDaemon(true);
	}
}
