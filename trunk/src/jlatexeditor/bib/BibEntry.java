package jlatexeditor.bib;

import sce.codehelper.WordWithPos;
import sce.component.SCEDocumentPosition;

import java.util.HashMap;

public class BibEntry {
  private SCEDocumentPosition startPos = null;
  private SCEDocumentPosition endPos = null;

  private String type = null;
  private WordWithPos name = new WordWithPos("", 0,0);
  private HashMap<String,BibKeyValuePair> parameters = new HashMap<String, BibKeyValuePair>();
  private HashMap<String,BibKeyValuePair> allParameters = new HashMap<String, BibKeyValuePair>();

  public BibEntry copy() {
    BibEntry copy = new BibEntry();
    copy.type = type;
    copy.parameters = new HashMap<String, BibKeyValuePair>(parameters);
    copy.allParameters = allParameters;
    return copy;
  }

  public SCEDocumentPosition getStartPos() {
    return startPos;
  }

  public void setStartPos(SCEDocumentPosition startPos) {
    this.startPos = startPos;
  }

  public SCEDocumentPosition getEndPos() {
    return endPos;
  }

  public void setEndPos(SCEDocumentPosition endPos) {
    this.endPos = endPos;
  }

  public String getType() {
    return type;
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
