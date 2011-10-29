package jlatexeditor.syntaxhighlighting.states;

import sce.syntaxhighlighting.ParserState;

public class ScriptMode implements ParserState {
  private static byte[] styles = new byte[256];

  static {
    for (int i = 0; i < styles.length; i++) styles[i] = (byte) i;
  }

  public ParserState copy() {
    return new ScriptMode();
  }

  public byte[] getStyles() {
    return styles;
  }
}
