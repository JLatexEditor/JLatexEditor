package jlatexeditor.bib;

import sce.component.SCEDocumentPosition;
import sce.syntaxhighlighting.ParserState;

import java.util.HashMap;

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
  public static final int STATE_VALUE_BASIC  = 9;
  public static final int STATE_EXPECT_CLOSE = 10;

  private int state = STATE_NOTHING;
  private int bracketLevel = 0;

  // current entry
  private BibEntry entry = new BibEntry();
  private BibKeyValuePair value = new BibKeyValuePair();
  private SCEDocumentPosition valueOpening = new SCEDocumentPosition(0,0);

  // list of all entries
  private int entryNr = 0;
  private HashMap<Integer,BibEntry> entryByNr = new HashMap<Integer, BibEntry>();

  static {
    for (int i = 0; i < styles.length; i++) styles[i] = (byte) i;
  }

  public BibParserState(int state) {
    this.state = state;
  }

  public ParserState copy() {
    BibParserState copy = new BibParserState(state);
    copy.bracketLevel = bracketLevel;
    copy.entry = entry != null ? entry.copy() : null;
    copy.value = value.copy();
    copy.valueOpening = valueOpening;
    copy.entryNr = entryNr;
    copy.entryByNr = entryByNr;
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

  public BibEntry getEntry() {
    return entry;
  }

  public void setEntry(BibEntry entry) {
    this.entry = entry;

    entryByNr.put(entryNr, entry);
    entryNr++;
  }

  public void resetEntry() {
    entry = new BibEntry();
  }

  public BibKeyValuePair getValue() {
    return value;
  }

  public void setValue(BibKeyValuePair value) {
    this.value = value;
  }

  public SCEDocumentPosition getValueOpening() {
    return valueOpening;
  }

  public void setValueOpening(SCEDocumentPosition valueOpening) {
    this.valueOpening = valueOpening;
  }

  public int getEntryNr() {
    return entryNr;
  }

  public HashMap<Integer, BibEntry> getEntryByNr() {
    return entryByNr;
  }

  public boolean equals(Object obj) {
    if(!(obj instanceof BibParserState)) return false;
    BibParserState b = (BibParserState) obj;
    return state == b.state && bracketLevel == b.bracketLevel
            && equals(entry.getType(false), b.getEntry().getType(false))
            && equals(entry.getParameters(), b.getEntry().getParameters())
            && entry.getAllParameters() == b.getEntry().getAllParameters()
            && entryNr == b.entryNr;
  }

  private boolean equals(Object o1, Object o2) {
    if(o1 == null && o2 == null) return true;
    if(o1 != null && o2 != null) return o1.equals(o2);
    return false;
  }
}
