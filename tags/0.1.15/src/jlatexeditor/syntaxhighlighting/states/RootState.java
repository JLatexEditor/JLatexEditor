package jlatexeditor.syntaxhighlighting.states;

import sce.syntaxhighlighting.ParserState;

/**
 * First state of a Latex document.
 *
 * @author Stefan Endrullis
 */
public class RootState implements ParserState {
	public ParserState copy() {
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof RootState;
	}
}
