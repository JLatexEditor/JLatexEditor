/**
 * @author Jörg Endrullis
 */

package jlatexeditor.errorhighlighting;

import jlatexeditor.ErrorView;
import sce.component.*;

import javax.xml.transform.Source;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class LatexErrorHighlighting implements LatexCompileListener {
  // editor and document
  private SourceCodeEditor editor = null;
  private SCEPane pane = null;
  private SCEDocument document = null;

  private ErrorView errorView = null;

  // error highlights
  private ArrayList<SCEErrorHighlight> errorHighlights = new ArrayList<SCEErrorHighlight>();


  public LatexErrorHighlighting(){
  }

  public void attach(SourceCodeEditor editor, ErrorView errorView) {
    this.editor = editor;
    pane = editor.getTextPane();
    document = pane.getDocument();
    this.errorView = errorView;

    update();
  }

  public void detach() {
    if(pane != null) clear();
    editor = null;
    pane = null;
    document = null;
  }

  public void clear() {
    // remove old error highlights
    Iterator highlightsIterator = errorHighlights.iterator();
    while(highlightsIterator.hasNext()){
      pane.removeTextHighlight((SCEErrorHighlight) highlightsIterator.next());
      highlightsIterator.remove();
    }
  }

  public void update() {
    if(errorView == null || editor == null || editor.getFile() == null) return;

    try {
      String canonicalFile = editor.getFile().getCanonicalPath();
      for(LatexCompileError error : errorView.getErrors()) {
        if(error.getType() != LatexCompileError.TYPE_ERROR) continue;
        if(!canonicalFile.equals(error.getFile().getCanonicalPath())) continue;

        // add error highlights
        int errorRow = Math.max(0, error.getLineStart() - 1);
        String row = document.getRow(errorRow);

        int errorColumn = 0;
        String before = error.getTextBefore();
        if(before != null) errorColumn = row.indexOf(before) + before.length();

        SCEDocumentPosition start, end;
        if(error.getCommand() != null) {
          errorColumn = row.lastIndexOf(error.getCommand(), errorColumn);
          start = document.createDocumentPosition(errorRow, errorColumn);
          end = document.createDocumentPosition(errorRow, errorColumn + error.getCommand().length());
        } else {
          start = document.createDocumentPosition(errorRow, Math.max(0, errorColumn - 5));
          end = document.createDocumentPosition(errorRow, errorColumn);
        }

        SCEErrorHighlight errorHighlight = new SCEErrorHighlight(pane, start, end, new Color(255, 0, 0));

        pane.addTextHighlight(errorHighlight);
        errorHighlights.add(errorHighlight);
      }
    } catch (IOException e) { }
  }

  // LatexCompileListener
  public void compileStarted(){
  }

  public void compileEnd(){
    update();
  }

  public void latexError(LatexCompileError error){
  }
}
