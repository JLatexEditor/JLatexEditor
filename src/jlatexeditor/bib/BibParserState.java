package jlatexeditor.bib;

import sce.syntaxhighlighting.ParserState;

import java.util.ArrayList;

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
  private String entryType = null;
  private ArrayList<String> keys = new ArrayList<String>();
  private ArrayList<String> allKeys = null;

  static {
    for (int i = 0; i < styles.length; i++) styles[i] = (byte) i;
  }

  public BibParserState(int state) {
    this.state = state;
  }

  public ParserState copy() {
    BibParserState copy = new BibParserState(state);
    copy.bracketLevel = bracketLevel;
    copy.entryType = entryType;
    copy.keys = new ArrayList<String>(keys);
    copy.allKeys = allKeys;
    return copy;
  }

  public byte[] getStyles() {
    return styles;
  }

  public int getState() {
    return state;
  }

  public void setState(int state) {
    this.state = state;
  }

  public int getBracketLevel() {
    return bracketLevel;
  }

  public void setBracketLevel(int bracketLevel) {
    this.bracketLevel = bracketLevel;
  }

  public String getEntryType() {
    return entryType;
  }

  public void setEntryType(String entryType) {
    this.entryType = entryType;
  }

  public ArrayList<String> getKeys() {
    return keys;
  }

  public void setKeys(ArrayList<String> keys) {
    this.keys = keys;
  }

  public ArrayList<String> getAllKeys() {
    return allKeys;
  }

  public void setAllKeys(ArrayList<String> allKeys) {
    this.allKeys = allKeys;
  }

  public boolean equals(Object obj) {
    if(!(obj instanceof BibParserState)) return false;
    BibParserState b = (BibParserState) obj;
    return state == b.state && bracketLevel == b.bracketLevel && equals(entryType, b.entryType) && equals(keys, b.keys);
  }

  private boolean equals(Object o1, Object o2) {
    if(o1 == null && o2 == null) return true;
    if(o1 != null && o2 != null) return o1.equals(o2);
    return false;
  }
}
