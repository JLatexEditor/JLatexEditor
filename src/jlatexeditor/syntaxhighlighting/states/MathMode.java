package jlatexeditor.syntaxhighlighting.states;

import sce.syntaxhighlighting.ParserState;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class MathMode implements ParserState {
	private boolean doubleMath;

	public MathMode(boolean doubleMath) {
		this.doubleMath = doubleMath;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MathMode) {
			MathMode mathMode = (MathMode) obj;

		}
		return false;
	}

	public ParserState copy() {
		return new MathMode(doubleMath);
	}
	
	public boolean isDoubleMath() {
		return doubleMath;
	}
}
