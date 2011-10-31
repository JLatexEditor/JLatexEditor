package jlatexeditor.syntaxhighlighting.states;

import jlatexeditor.syntaxhighlighting.LatexStyles;
import sce.syntaxhighlighting.ParserState;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class MathMode implements ParserState {
	public enum Type { simple, doubled, openingBracket, closingBracket, openingParenthesis, closingParenthesis, openingEnv, closingEnv }

  private static byte[] styles = new byte[256];

  static {
    for (int i = 0; i < styles.length; i++) styles[i] = (byte) i;
    styles[LatexStyles.TEXT] = LatexStyles.MATH;
    styles[LatexStyles.COMMAND] = LatexStyles.MATH_COMMAND;
    styles[LatexStyles.NUMBER] = LatexStyles.MATH;
  }

	private Type type;
	private boolean envBased;
	/** Name of the math environment if envBased. */
	private String envName;

	public MathMode(Type type) {
		this.type = type;
	}

	public MathMode(Type type, String envName) {
		this.type = type;
		this.envBased = true;
		this.envName = envName;
	}

	@Override
  public boolean equals(Object obj) {
    if (obj instanceof MathMode) {
      MathMode that = (MathMode) obj;
	    if (this.type.equals(that.type)) {
		    return !envBased || this.envName.equals(that.envName);
	    }
    }
    return false;
  }

	/**
	 * Returns true if this math mode closes that given one.
	 *
	 * @param that previous math mode
	 * @return true if this math mode closes that given one
	 */
	public boolean closes(MathMode that) {
		switch (that.type) {
			case simple:
				return this.type == Type.simple;
			case doubled:
				return this.type == Type.doubled;
			case openingBracket:
				return this.type == Type.closingBracket;
			case openingParenthesis:
				return this.type == Type.closingParenthesis;
			case openingEnv:
				return this.type == Type.closingEnv && this.envName.equals(that.envName);
			default:
				return false;
		}
	}

	public boolean mayBeOpening() {
		switch (type) {
			case closingBracket:
			case closingParenthesis:
			case closingEnv:
				return false;
			default:
				return true;
		}
	}

  public ParserState copy() {
    return new MathMode(type);
  }

  public byte[] getStyles() {
    return styles;
  }
}
