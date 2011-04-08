/**
 * @author Jörg Endrullis
 */

package sce.syntaxhighlighting;

public interface ParserState {
  public ParserState copy();

  public byte[] getStyles();
}

/*
public class ParserState implements Cloneable{
  public boolean mode_verbatim = false;
  public boolean mode_math = false;

  public boolean text_bold = false;
  public boolean text_italic = false;

  / **
   * Compares the two states.
   *
   * @param state the state to compare with
   * @return true, if the states are equal
   * /
  public boolean equals(ParserState state){
    if(state == null) return false;

    if(mode_verbatim != state.mode_verbatim) return false;
    if(mode_math != state.mode_math) return false;

    if(text_bold != state.text_bold) return false;
    if(text_italic != state.text_italic) return false;

    return true;
  }

  / **
   * Clones this states state.
   *
   * @return the clone
   * /
  public ParserState copy(){
    ParserState clone = new ParserState();

    clone.mode_verbatim = mode_verbatim;
    clone.mode_math = mode_math;
    clone.text_bold = text_bold;
    clone.text_italic = text_italic;

    return clone;
  }
}
*/
