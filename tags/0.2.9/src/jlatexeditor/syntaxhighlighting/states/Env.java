package jlatexeditor.syntaxhighlighting.states;

import sce.syntaxhighlighting.ParserState;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class Env implements ParserState {
	private String name;
	private boolean closing;
	private byte[] styles;

	public Env(String name, boolean closing, ParserState previousState) {
		this.name = name;
		this.closing = closing;
		this.styles = previousState.getStyles();
	}

	public String getName() {
		return name;
	}

	public boolean isClosing() {
		return closing;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Env) {
			Env that = (Env) obj;
			return this.name.equals(that.name) && this.closing == that.closing;
		}
		return false;
	}

	public boolean closes(Env that) {
		return this.closing && !that.closing && this.name.equals(that.name);
	}

	@Override
	public ParserState copy() {
		return new Env(name, closing, this);
	}

	@Override
	public byte[] getStyles() {
		return styles;
	}
}
