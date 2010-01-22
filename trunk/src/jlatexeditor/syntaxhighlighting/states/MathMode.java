package jlatexeditor.syntaxhighlighting.states;

import jlatexeditor.syntaxhighlighting.LatexStyles;
import sce.syntaxhighlighting.ParserState;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class MathMode implements ParserState {
	private boolean doubleMath;
  private static byte[] styles = new byte[256];
  static {
    for(int i = 0; i < styles.length; i++) styles[i] = (byte) i;
    styles[LatexStyles.TEXT] = LatexStyles.MATH;
    styles[LatexStyles.COMMAND] = LatexStyles.MATH_COMMAND;
    styles[LatexStyles.NUMBER] = LatexStyles.MATH;
  }

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

  public byte[] getStyles() {
    return styles;
  }

  public boolean isDoubleMath() {
		return doubleMath;
	}
}
