package sce.syntaxhighlighting;

import sce.component.*;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Highlighting brackets.
 */
public class BracketHighlighting implements SCECaretListener, MouseListener {
  private SourceCodeEditor editor;

  private static final Color colors[] = new Color[]{
          new Color(153, 204, 255),
          new Color(255, 153, 204),
          new Color(150, 255, 133),
          new Color(100, 100, 215),
          new Color(215, 100, 100),
          new Color(100, 215, 100),
  };

  private ArrayList<SCETextHighlight> highlights = new ArrayList<SCETextHighlight>();

  private long last = System.nanoTime();
  private Timer timer = new Timer(true);
  private TimerTask task = null;

  public BracketHighlighting(SourceCodeEditor editor) {
    this.editor = editor;
    editor.getTextPane().getCaret().addSCECaretListener(this);
  }

  public void update() {
    if(last < System.nanoTime() - 100000000) {
      execute();
    } else {
      if(task != null) {
        task.cancel();
        timer.purge();
      }
      task = new TimerTask() {
        public void run() {
          execute();
        }
      };
      timer.schedule(task, 500);
    }
    last = System.nanoTime();
  }

  private void execute() {
    SCECaret caret = editor.getTextPane().getCaret();
    int row = caret.getRow();
    int column = caret.getColumn();

    SCEPane pane = editor.getTextPane();
    for(SCETextHighlight highlight : highlights) pane.removeTextHighlight(highlight);
    highlights.clear();

    int coloursCount = colors.length;

    SCEDocument document = pane.getDocument();
    String line = document.getRow(row);
    if(column > 0 && (line.charAt(column-1) == '}' || line.charAt(column-1) == ')')) {
      // search backwards
      char open = line.charAt(column-1);
      char close = line.charAt(column-1) == '}' ? '{' : '(';

      int level = 0;
      for(int srow = row; srow >= Math.max(0, row-40); srow--) {
        line = document.getRow(srow);
        int startColumn = srow == row ? column-1 : line.length()-1;
        for(int scolumn = startColumn ; scolumn >= 0; scolumn--) {
          char c = line.charAt(scolumn);
          if(c == open) {
            if(level < coloursCount) {
              highlights.add(new SCETextHighlight(
                      pane,
                      document.createDocumentPosition(srow,scolumn),
                      document.createDocumentPosition(srow,scolumn+1),
                      colors[level]));
            }
            level++;
          } else if(level == 0) { srow = -1; break; }
          if(c == close) {
            level--;
            if(level < coloursCount) {
              highlights.add(new SCETextHighlight(
                      pane,
                      document.createDocumentPosition(srow,scolumn),
                      document.createDocumentPosition(srow,scolumn+1),
                      colors[level]));
            }
          }
        }
      }
    }
    if(column < line.length() && (line.charAt(column) == '{' || line.charAt(column) == '(')) {
      // search backwards
      char open = line.charAt(column);
      char close = line.charAt(column) == '{' ? '}' : ')';

      int level = 0;
      for(int srow = row; srow <= Math.min(document.getRowsCount()-1, row+40); srow++) {
        line = document.getRow(srow);
        int startColumn = srow == row ? column : 0;
        for(int scolumn = startColumn ; scolumn < line.length(); scolumn++) {
          char c = line.charAt(scolumn);
          if(c == open) {
            if(level < coloursCount) {
              highlights.add(new SCETextHighlight(
                      pane,
                      document.createDocumentPosition(srow,scolumn),
                      document.createDocumentPosition(srow,scolumn+1),
                      colors[level]));
            }
            level++;
          } else if(level == 0) { srow = document.getRowsCount(); break; }
          if(c == close) {
            level--;
            if(level < coloursCount) {
              highlights.add(new SCETextHighlight(
                      pane,
                      document.createDocumentPosition(srow,scolumn),
                      document.createDocumentPosition(srow,scolumn+1),
                      colors[level]));
            }
          }
        }
      }
    }

    for(SCETextHighlight highlight : highlights) pane.addTextHighlight(highlight);
  }

  public void mouseClicked(MouseEvent e) {
  }

  public void mousePressed(MouseEvent e) {
    update();
  }

  public void mouseReleased(MouseEvent e) {
    update();
  }

  public void mouseEntered(MouseEvent e) {
  }

  public void mouseExited(MouseEvent e) {
  }

  public void caretMoved(int row, int column, int lastRow, int lastColumn) {
    update();
  }
}
