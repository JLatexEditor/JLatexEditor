/**
 * @author Jörg Endrullis
 */

package jlatexeditor.errorhighlighting;

import jlatexeditor.ErrorView;
import sce.component.SCEDocument;
import sce.component.SCEPane;
import sce.component.SCEErrorHighlight;
import sce.component.SCEDocumentPosition;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;

public class LatexErrorHighlighting implements LatexCompileListener {
  // The TextPane and document
  SCEPane pane = null;
  SCEDocument document = null;

  // The errors
  ArrayList errors = new ArrayList();
  ArrayList errorHighlights = new ArrayList();
  // TextArea for error messages
  ErrorView errorView = null;

  public LatexErrorHighlighting(SCEPane pane, ErrorView errorView){
    this.pane = pane;
    document = pane.getDocument();
    this.errorView = errorView;
  }

  // LatexCompileListener
  public void compileStarted(){
    errors.clear();
  }

  public void compileEnd(){
    // remove old error highlights
    Iterator highlightsIterator = errorHighlights.iterator();
    while(highlightsIterator.hasNext()){
      pane.removeTextHighlight((SCEErrorHighlight) highlightsIterator.next());
      highlightsIterator.remove();
    }

    // add highlights
    Iterator iterator = errors.iterator();
    while(iterator.hasNext()){
      LatexCompileError error = (LatexCompileError) iterator.next();
      if(error.getType() != LatexCompileError.TYPE_ERROR) continue;

      // add error highlights
      int errorRow = error.getLineStart() - 1;
      int errorColunm = document.getRow(errorRow).indexOf(error.getTextBefore()) + error.getTextBefore().length();
      SCEDocumentPosition start = document.createDocumentPosition(errorRow, errorColunm - 5);
      SCEDocumentPosition end = document.createDocumentPosition(errorRow, errorColunm);
      SCEErrorHighlight errorHighlight = new SCEErrorHighlight(pane, start, end, new Color(255, 0, 0));

      pane.addTextHighlight(errorHighlight);
      errorHighlights.add(errorHighlight);
    }
  }

  public void latexError(LatexCompileError error){
    errors.add(error);
  }
}
