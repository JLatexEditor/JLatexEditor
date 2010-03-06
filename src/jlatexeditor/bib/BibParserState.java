package jlatexeditor.bib;

import sce.syntaxhighlighting.ParserState;

public class BibParserState implements ParserState {
  private static byte[] styles = new byte[256];

  public static final int STATE_NOTHING      = 0;
  public static final int STATE_EXPECT_OPEN  = 1;
  public static final int STATE_EXPECT_NAME  = 2;
  public static final int STATE_EXPECT_COMMA = 3;
  public static final int STATE_EXPECT_KEY   = 4;
  public static final int STATE_EXPECT_EQ    = 5;
  public static final int STATE_EXPECT_VALUE = 6;
  public static final int STATE_VALUE_QUOTED = 7;
  public static final int STATE_VALUE_BRACED = 8;

  private int state = STATE_NOTHING;
  private int bracketLevel = 0;

  static {
    for (int i = 0; i < styles.length; i++) styles[i] = (byte) i;
  }

  public BibParserState(int state, int bracketLevel) {
    this.state = state;
    this.bracketLevel = bracketLevel;
  }

  public ParserState copy() {
    return new BibParserState(state, bracketLevel);
  }

  public byte[] getStyles() {
    return styles;
  }

  public int getState() {
    return state;
  }

  public int getBracketLevel() {
    return bracketLevel;
  }

  public boolean equals(Object obj) {
    if(!(obj instanceof BibParserState)) return false;
    BibParserState b = (BibParserState) obj;
    return state == b.state && bracketLevel == b.bracketLevel;
  }
}
