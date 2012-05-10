package jlatexeditor.bib;

import sce.codehelper.WordWithPos;
import sce.component.SCEDocumentPosition;
import sce.component.SCEPosition;

import java.util.ArrayList;

public class BibKeyValuePair {
  private WordWithPos key;
  private WordWithPos eq;
  private ArrayList<WordWithPos> values = new ArrayList<WordWithPos>();

  /**
   * Key.
   */
  public WordWithPos getKey() {
    return key;
  }

  public BibKeyValuePair copy() {
    BibKeyValuePair copy = new BibKeyValuePair();
    copy.key = key;
    copy.eq = eq;
    copy.values = new ArrayList<WordWithPos>(values);
    return copy;
  }

  public void setKey(WordWithPos key) {
    this.key = key;
  }

  /**
   * Equality sign.
   */
  public WordWithPos getEq() {
    return eq;
  }

  public void setEq(WordWithPos eq) {
    this.eq = eq;
  }

  /**
   * List of values.
   */
  public void addValue(WordWithPos value) {
    values.add(value);
  }

  public ArrayList<WordWithPos> getValues() {
    return values;
  }

  public void setValues(ArrayList<WordWithPos> values) {
    this.values = values;
  }

  public static SCEPosition getInnerStart(WordWithPos value) {
    SCEPosition start = value.getStartPos();
    if(value.word.startsWith("\"") || value.word.startsWith("{")) {
      return new SCEDocumentPosition(start.getRow(), start.getColumn()+1);
    } else {
      return start;
    }
  }

  public static SCEPosition getInnerEnd(WordWithPos value) {
    SCEPosition end = value.getEndPos();
    if(value.word.endsWith("\"") || value.word.endsWith("}")) {
      return new SCEDocumentPosition(end.getRow(), end.getColumn()-1);
    } else {
      return end;
    }
  }

  public String getValuesString() {
    StringBuilder builder = new StringBuilder();
    for(WordWithPos value : values) {
      String word = value.word;
      if(word.startsWith("\"") || word.startsWith("{")) {
        builder.append(word.substring(1,word.length()-1));
      } else {
        builder.append(word);
      }

    }
    return builder.toString();
  }
}
