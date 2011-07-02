package jlatexeditor.syntaxhighlighting.states;

import jlatexeditor.syntaxhighlighting.LatexStyles;
import sce.syntaxhighlighting.ParserState;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class MathMode implements ParserState {
	public enum Type { simple, doubled, bracket, parenthesis }

  private static byte[] styles = new byte[256];

  static {
    for (int i = 0; i < styles.length; i++) styles[i] = (byte) i;
    styles[LatexStyles.TEXT] = LatexStyles.MATH;
    styles[LatexStyles.COMMAND] = LatexStyles.MATH_COMMAND;
    styles[LatexStyles.NUMBER] = LatexStyles.MATH;
  }

	private Type type;

	public MathMode(Type type) {
		this.type = type;
	}

	@Override
  public boolean equals(Object obj) {
    if (obj instanceof MathMode) {
      MathMode that = (MathMode) obj;
	    return this.type.equals(that.type);
    }
    return false;
  }

  public ParserState copy() {
    return new MathMode(type);
  }

  public byte[] getStyles() {
    return styles;
  }
}
