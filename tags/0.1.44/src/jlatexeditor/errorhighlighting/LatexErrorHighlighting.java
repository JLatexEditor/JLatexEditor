/**
 * @author Jörg Endrullis
 */

package jlatexeditor.errorhighlighting;

import jlatexeditor.Doc;
import jlatexeditor.ErrorView;
import sce.component.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class LatexErrorHighlighting implements LatexCompileListener {
  // editor
  private SourceCodeEditor editor = null;
  private ErrorView errorView = null;

  // error highlights
  private ArrayList<SCEErrorHighlight> errorHighlights = new ArrayList<SCEErrorHighlight>();


  public LatexErrorHighlighting() {
  }

  public void attach(SourceCodeEditor editor, ErrorView errorView) {
    this.editor = editor;
    this.errorView = errorView;

    update();
  }

  public void detach() {
    if (editor != null) clear();
  }

  public void clear() {
    editor.getMarkerBar().clear();

    // remove old error highlights
    SCEPane pane = editor.getTextPane();
    Iterator highlightsIterator = errorHighlights.iterator();
    while (highlightsIterator.hasNext()) {
      pane.removeTextHighlight((SCEErrorHighlight) highlightsIterator.next());
      highlightsIterator.remove();
    }
  }

  public void update() {
    if (errorView == null || editor == null) return;

    File file;
    if (editor.getResource() instanceof Doc.FileDoc) {
      file = ((Doc.FileDoc) editor.getResource()).getFile();
    } else {
      return;
    }

    SCEPane pane = editor.getTextPane();
    SCEDocument document = pane.getDocument();
    SCEMarkerBar markerBar = editor.getMarkerBar();
    try {
      String canonicalFile = file.getCanonicalPath();
      for (LatexCompileError error : errorView.getErrors()) {
        if (!canonicalFile.equals(error.getFile().getCanonicalPath())) continue;

        // add error highlights
        int errorRow = Math.max(0, error.getLineStart() - 1);

        int markerType = LatexCompileError.errorType2markerType(error.getType());
        markerBar.addMarker(new SCEMarkerBar.Marker(markerType, errorRow, error));

        if (error.getType() != LatexCompileError.TYPE_ERROR) continue;

        String row = document.getRow(errorRow);

        int errorColumn = 0;
        String before = error.getTextBefore();
        if (before != null) errorColumn = row.indexOf(before) + before.length();

        SCEDocumentPosition start, end;
        if (error.getCommand() != null) {
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
      markerBar.repaint();
    } catch (IOException ignored) {
    }
  }

  // LatexCompileListener
  public void compileStarted() {
  }

  public void compileEnd() {
    update();
  }

  public void latexError(LatexCompileError error) {
  }
}
