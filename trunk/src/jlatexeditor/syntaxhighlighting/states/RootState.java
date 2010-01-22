package jlatexeditor.syntaxhighlighting.states;

import jlatexeditor.syntaxhighlighting.LatexStyles;
import sce.syntaxhighlighting.ParserState;

/**
 * First state of a Latex document.
 *
 * @author Stefan Endrullis
 */
public class RootState implements ParserState {
  private static byte[] styles = new byte[256];
  static {
    for(int i = 0; i < styles.length; i++) styles[i] = (byte) i;
  }

	public ParserState copy() {
		return this;
	}

  public byte[] getStyles() {
    return styles;
  }

  @Override
	public boolean equals(Object obj) {
		return obj instanceof RootState;
	}
}
