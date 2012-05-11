package jlatexeditor.bib;

import sce.codehelper.WordWithPos;
import sce.component.SCEDocumentPosition;
import sce.component.SCEDocumentRange;
import sce.component.SCEPosition;

import java.util.HashMap;

public class BibEntry {
  private SCEDocumentRange range = new SCEDocumentRange(0,0,0,0);

  private String type = null;
  private WordWithPos name = new WordWithPos("", 0,0);
  private HashMap<String,BibKeyValuePair> parameters = new HashMap<String, BibKeyValuePair>();
  private HashMap<String,BibKeyValuePair> allParameters = new HashMap<String, BibKeyValuePair>();

  public BibEntry copy() {
    BibEntry copy = new BibEntry();
    copy.range = range;
    copy.type = type;
    copy.name = name;
    copy.parameters = new HashMap<String, BibKeyValuePair>(parameters);
    copy.allParameters = allParameters;
    return copy;
  }

  public SCEPosition getStartPos() {
    return range.getStartPos();
  }

  public void setStartPos(SCEDocumentPosition startPos) {
    range.setStartPos(startPos);
  }

  public SCEPosition getEndPos() {
    return range.getEndPos();
  }

  public void setEndPos(SCEDocumentPosition endPos) {
    range.setEndPos(endPos);
  }

  public String getType(boolean allowNull) {
    return type != null || allowNull ? type : "";
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getName() {
    return name.word;
  }

  public WordWithPos getNameWithPos() {
    return name;
  }

  public void setName(WordWithPos name) {
    this.name = name;
  }

  public HashMap<String,BibKeyValuePair> getParameters() {
    return parameters;
  }

  public void setParameters(HashMap<String,BibKeyValuePair> parameters) {
    this.parameters = parameters;
  }

  public HashMap<String,BibKeyValuePair> getAllParameters() {
    return allParameters;
  }

  public void setAllParameters(HashMap<String,BibKeyValuePair> allParameters) {
    this.allParameters = allParameters;
  }
}
