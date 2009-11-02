/**
 * @author Jörg Endrullis
 */

package editor.errorhighlighting;

import editor.component.SCEDocument;
import editor.component.SCEPane;
import editor.component.SCEErrorHighlight;
import editor.component.SCEDocumentPosition;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;

public class LatexErrorHighlighting implements LatexCompileListener{
  // The TextPane and document
  SCEPane pane = null;
  SCEDocument document = null;

  // The errors
  ArrayList errors = new ArrayList();
  ArrayList errorHighlights = new ArrayList();
  // TextArea for error messages
  JTextArea errorMessages = null;

  public LatexErrorHighlighting(SCEPane pane, JTextArea errorMessages){
    this.pane = pane;
    document = pane.getDocument();
    this.errorMessages = errorMessages;
  }

  // LatexCompileListener
  public void compileStarted(){
    errors.clear();
  }

  public void compileEnd(){
    String errorText = "";

    // remove old error highlights
    Iterator highlightsIterator = errorHighlights.iterator();
    while(highlightsIterator.hasNext()){
      pane.removeTextHighlight((SCEErrorHighlight) highlightsIterator.next());
      highlightsIterator.remove();
    }

    // Calculate error text
    Iterator iterator = errors.iterator();
    while(iterator.hasNext()){
      LatexCompileError error = (LatexCompileError) iterator.next();
      errorText += error.toString() + "\n\n";

      // add error highlights
      int errorRow = error.getLine() - 1;
      int errorColunm = document.getRow(errorRow).indexOf(error.getTextBefore()) + error.getTextBefore().length();
      SCEDocumentPosition start = document.createDocumentPosition(errorRow, errorColunm - 5);
      SCEDocumentPosition end = document.createDocumentPosition(errorRow, errorColunm);
      SCEErrorHighlight errorHighlight = new SCEErrorHighlight(pane, start, end, new Color(255, 0, 0));

      pane.addTextHighlight(errorHighlight);
      errorHighlights.add(errorHighlight);
    }

    // Update Error text
    if(errorMessages.getText() == null || !errorText.equals(errorMessages.getText())){
      errorMessages.setText(errorText);
    }
  }

  public void latexError(LatexCompileError error){
    errors.add(error);
  }
}
