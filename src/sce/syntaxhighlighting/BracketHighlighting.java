package sce.syntaxhighlighting;

import jlatexeditor.gproperties.GProperties;
import sce.component.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Highlighting brackets.
 */
public class BracketHighlighting implements SCECaretListener {
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
    if (last < System.nanoTime() - 100000000) {
      execute();
    } else {
      if (task != null) {
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
    for (SCETextHighlight highlight : highlights) pane.removeTextHighlight(highlight);
    highlights.clear();

    SCEDocument document = pane.getDocument();
    String line = document.getRowsModel().getRowAsString(row);

    if (column > 0) {
      // search backwards
      char open = line.charAt(column - 1);
      if(getDirection(open) == -1) {
        char close = getClosingChar(open);
        hightlight(document, pane, row, column, open, close, -1, Math.max(-1, row - 40));
      }
    }

    if (column < line.length() && (line.charAt(column) == '{' || line.charAt(column) == '(')) {
      // search backwards
      char open = line.charAt(column);
      if(getDirection(open) == 1) {
        char close = getClosingChar(open);
        hightlight(document, pane, row, column, open, close, 1, Math.min(document.getRowsModel().getRowsCount(), row + 40));
      }
    }

    for (SCETextHighlight highlight : highlights) pane.addTextHighlight(highlight);
  }

  private void hightlight(SCEDocument document, SCEPane pane, final int row, final int column, final char open, final char close, final int direction, final int endRow) {
    final int coloursCount = Math.min(colors.length, GProperties.getInt("editor.bracket_matching.depth"));

    int level = 0;
    for (int srow = row; srow != endRow; srow += direction) {
      String line = document.getRowsModel().getRowAsString(srow);

      final int startColumn, endColumn;
      if(direction == -1) {
        startColumn = srow == row ? column - 1 : line.length() - 1;
        endColumn = -1;
      } else {
        startColumn = srow == row ? column : 0;
        endColumn = line.length();
      }

      for (int scolumn = startColumn; scolumn != endColumn; scolumn += direction) {
        char c = line.charAt(scolumn);
        if (c == open) {
          if (level < coloursCount) {
            highlights.add(new SCETextHighlight(
                    pane,
                    document.createDocumentPosition(srow, scolumn),
                    document.createDocumentPosition(srow, scolumn + 1),
                    colors[level]));
          }
          level++;
        } else if (level == 0) {
          srow = endRow - direction;
          break;
        }
        if (c == close) {
          level--;
          if (level < coloursCount) {
            highlights.add(new SCETextHighlight(
                    pane,
                    document.createDocumentPosition(srow, scolumn),
                    document.createDocumentPosition(srow, scolumn + 1),
                    colors[level]));
          }
        }
      }
    }
  }

  public static int getDirection(char open) {
    switch (open) {
      case '}' : return -1;
      case ')' : return -1;
      case ']' : return -1;
      case '{' : return 1;
      case '(' : return 1;
      case '[' : return 1;
      default: return 0;
    }
  }

  public static char getClosingChar(char open) {
    switch (open) {
      case '}' : return '{';
      case ')' : return '(';
      case ']' : return '[';
      case '{' : return '}';
      case '(' : return ')';
      case '[' : return ']';
      default: return ' ';
    }
  }

  /**
   * Returns the position of the matching closing bracket or null.
   */
  public static SCEDocumentPosition getClosingBracket(SCEDocument document, final int row, final int column, final char open, final char close, final int direction) {
    final int endRow = direction == -1 ? -1 : document.getRowsModel().getRowsCount();

    int level = 0;
    for (int srow = row; srow != endRow; srow += direction) {
      String line = document.getRowsModel().getRowAsString(srow);

      final int startColumn, endColumn;
      if(direction == -1) {
        startColumn = srow == row ? column : line.length() - 1;
        endColumn = -1;
      } else {
        startColumn = srow == row ? column : 0;
        endColumn = line.length();
      }

      for (int scolumn = startColumn; scolumn != endColumn; scolumn += direction) {
        char c = line.charAt(scolumn);
        if (c == open) {
          level++;
        } else
        if (c == close) {
          level--;
          if(level == 0) return new SCEDocumentPosition(srow, scolumn);
        }
      }
    }

    return null;
  }

  public void caretMoved(int row, int column, int lastRow, int lastColumn) {
    update();
  }
}
