package jlatexeditor.syntaxhighlighting.states;

import sce.syntaxhighlighting.ParserState;

/**
 * State for skipping syntax highlighting.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class DontParse implements ParserState {
	private byte[] styles;

	public DontParse(ParserState previousState) {
		this.styles = previousState.getStyles();
	}

	@Override
	public ParserState copy() {
		return new DontParse(this);
	}

	@Override
	public byte[] getStyles() {
		return styles;
	}
}
