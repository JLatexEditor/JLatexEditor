/**
 * @author Jörg Endrullis
 */

package jlatexeditor.errorhighlighting;

import jlatexeditor.ErrorView;
import sce.component.*;

import javax.xml.transform.Source;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;

public class LatexErrorHighlighting implements LatexCompileListener {
  // editor and document
  private SourceCodeEditor editor = null;
  private SCEPane pane = null;
  private SCEDocument document = null;

  // error highlights
  private ArrayList<SCEErrorHighlight> errorHighlights = new ArrayList<SCEErrorHighlight>();

  public LatexErrorHighlighting(SourceCodeEditor editor, ErrorView errorView){
    this.editor = editor;
    pane = editor.getTextPane();
    document = pane.getDocument();
  }

  // LatexCompileListener
  public void compileStarted(){
    // remove old error highlights
    Iterator highlightsIterator = errorHighlights.iterator();
    while(highlightsIterator.hasNext()){
      pane.removeTextHighlight((SCEErrorHighlight) highlightsIterator.next());
      highlightsIterator.remove();
    }
  }

  public void compileEnd(){
  }

  public void latexError(LatexCompileError error){
    if(error.getType() != LatexCompileError.TYPE_ERROR) return;
    if(!error.getFile().equals(editor.getFile())) return;

    // add error highlights
    int errorRow = error.getLineStart() - 1;
    int errorColumn = document.getRow(errorRow).indexOf(error.getTextBefore()) + error.getTextBefore().length();
    SCEDocumentPosition start = document.createDocumentPosition(errorRow, errorColumn - 5);
    SCEDocumentPosition end = document.createDocumentPosition(errorRow, errorColumn);
    SCEErrorHighlight errorHighlight = new SCEErrorHighlight(pane, start, end, new Color(255, 0, 0));

    pane.addTextHighlight(errorHighlight);
    errorHighlights.add(errorHighlight);
  }
}
