package sce.component;

import sce.codehelper.WordWithPos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Search pane.
 */
public class SCESearch extends JPanel implements ActionListener, KeyListener, SCEDocumentListener, SCESelectionListener {
  private SourceCodeEditor editor;

  private JTextField input = new JTextField();
  private ImageButton buttonNext;
  private ImageButton buttonPrevious;
  private JCheckBox caseSensitive = new JCheckBox("Case sensitive", false);
  private JCheckBox regExp = new JCheckBox("Regexp", false);
  private JCheckBox selectionOnly = new JCheckBox("Selection only", true);
  private ImageButton buttonClose;
  private ImageButton buttonShowReplace;

  private boolean showReplace = false;
  private JTextField replace = new JTextField();
  private ImageButton buttonReplace;
  private ImageButton buttonReplaceAll;

  private GroupLayout layout;

  // search update thread
  private UpdateThread updateThread = new UpdateThread();

  // search results
  private ArrayList<SCEDocumentRange> results = new ArrayList<SCEDocumentRange>();

  // selection before searching
  private SCEDocumentRange selection = null;

  private GroupLayout.Group groupHorizontal;
  private GroupLayout.Group groupVertical;
  private static final Color ERROR_COLOR = new Color(255, 204, 204);

  public SCESearch(SourceCodeEditor editor) {
    this.editor = editor;
    setBackground(new Color(233, 244, 255));

    buttonNext = new ImageButton(
            new ImageIcon(getClass().getResource("/images/buttons/arrow_down.png")),
            new ImageIcon(getClass().getResource("/images/buttons/arrow_down_highlight.png")),
            new ImageIcon(getClass().getResource("/images/buttons/arrow_down_press.png")));

    buttonPrevious = new ImageButton(
            new ImageIcon(getClass().getResource("/images/buttons/arrow_up.png")),
            new ImageIcon(getClass().getResource("/images/buttons/arrow_up_highlight.png")),
            new ImageIcon(getClass().getResource("/images/buttons/arrow_up_press.png")));

    buttonClose = new ImageButton(
            new ImageIcon(getClass().getResource("/images/buttons/close.png")),
            new ImageIcon(getClass().getResource("/images/buttons/close_highlight.png")),
            new ImageIcon(getClass().getResource("/images/buttons/close_press.png")));

    buttonShowReplace = new ImageButton(
            new ImageIcon(getClass().getResource("/images/buttons/showReplace.png")),
            new ImageIcon(getClass().getResource("/images/buttons/showReplace_highlight.png")),
            new ImageIcon(getClass().getResource("/images/buttons/showReplace_press.png")));

    buttonReplace = new ImageButton(
            new ImageIcon(getClass().getResource("/images/buttons/replace.png")),
            new ImageIcon(getClass().getResource("/images/buttons/replace_highlight.png")),
            new ImageIcon(getClass().getResource("/images/buttons/replace_press.png")));

    buttonReplaceAll = new ImageButton(
            new ImageIcon(getClass().getResource("/images/buttons/replace_all.png")),
            new ImageIcon(getClass().getResource("/images/buttons/replace_all_highlight.png")),
            new ImageIcon(getClass().getResource("/images/buttons/replace_all_press.png")));

    layout = new GroupLayout(this);
    setLayout(layout);

    layout.setAutoCreateGaps(true);

    input.setColumns(40);
    add(input);
    input.addKeyListener(this);
    add(buttonNext);
    buttonNext.addActionListener(this);
    add(buttonPrevious);
    buttonPrevious.addActionListener(this);
    caseSensitive.setOpaque(false);
    add(caseSensitive);
    caseSensitive.addActionListener(this);
    regExp.setOpaque(false);
    add(regExp);
    regExp.addActionListener(this);
    selectionOnly.setOpaque(false);
    add(selectionOnly);
    selectionOnly.addActionListener(this);
    add(buttonShowReplace);
    buttonShowReplace.addActionListener(this);
    add(buttonClose);
    buttonClose.addActionListener(this);

    replace.setColumns(40);
    add(replace);
    replace.setToolTipText("For regexp groups use \\1, \\2, ...");
    replace.addKeyListener(this);
    add(buttonReplace);
    buttonReplace.addActionListener(this);
    add(buttonReplaceAll);
    buttonReplaceAll.addActionListener(this);

    groupHorizontal =
            layout.createSequentialGroup()
                    .addGap(5)
                    .addGroup(
                            layout.createParallelGroup()
                                    .addComponent(input)
                                    .addComponent(replace)
                    )
                    .addGroup(
                            layout.createParallelGroup()
                                    .addGroup(
                                            layout.createSequentialGroup()
                                                    .addComponent(buttonNext)
                                                    .addComponent(buttonPrevious)
                                                    .addComponent(caseSensitive)
                                                    .addComponent(regExp)
                                                    .addComponent(selectionOnly)
                                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 100, Short.MAX_VALUE)
                                                    .addComponent(buttonShowReplace)
                                                    .addComponent(buttonClose)
                                    )
                                    .addGroup(
                                    layout.createSequentialGroup()
                                            .addComponent(buttonReplace)
                                            .addComponent(buttonReplaceAll)
                            )
                    );

    groupVertical =
            layout.createSequentialGroup()
                    .addGap(2)
                    .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                    .addComponent(input)
                                    .addComponent(buttonNext)
                                    .addComponent(buttonPrevious)
                                    .addComponent(caseSensitive)
                                    .addComponent(regExp)
                                    .addComponent(selectionOnly)
                                    .addComponent(buttonShowReplace)
                                    .addComponent(buttonClose)
                    )
                    .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addComponent(replace)
                                    .addComponent(buttonReplace)
                                    .addComponent(buttonReplaceAll)
                    )
                    .addGap(2);

    layout.setHorizontalGroup(groupHorizontal);
    layout.setVerticalGroup(groupVertical);

    SCEPane pane = editor.getTextPane();
    SCEDocument document = pane.getDocument();
    pane.addKeyListener(this);
    document.addSCEDocumentListener(this);
    document.addSCESelectionListener(this);

    updateThread = new UpdateThread();
    updateThread.start();

    setShowReplace(false);
    setVisible(false);
  }

  public boolean isShowReplace() {
    return showReplace;
  }

  public void setShowReplace(boolean showReplace) {
    this.showReplace = showReplace;

    replace.setVisible(showReplace);
    buttonReplace.setVisible(showReplace);
    buttonReplaceAll.setVisible(showReplace);
  }

  public void focus() {
    input.requestFocusInWindow();
  }

  private void clearHighlights(boolean highlightSelection) {
    SCEMarkerBar markerBar = editor.getMarkerBar();
    markerBar.clear(SCEMarkerBar.TYPE_SEARCH);
    SCEPane pane = editor.getTextPane();
    pane.removeAllTextHighlights();
    if (highlightSelection && selection != null) {
      int rows = pane.getDocument().getRowsCount();
      SCEDocumentPosition endDocPos = new SCEDocumentPosition(rows, 0);
      pane.addTextHighlight(new SCETextHighlight(pane, new SCEDocumentPosition(0, 0), selection.getStartPosition(), SCEPane.nonSelectionHighlightColor));
      //pane.addTextHighlight(new SCETextHighlight(pane, selection.getStartPosition(), selection.getEndPosition(), SCEPane.selectionHighlightColorLight));
      pane.addTextHighlight(new SCETextHighlight(pane, selection.getEndPosition(), endDocPos, SCEPane.nonSelectionHighlightColor));
    }
  }

  public void setVisible(boolean visibility) {
    if (visibility == isVisible()) return;

    SCEPane pane = editor.getTextPane();
    SCEDocument document = pane.getDocument();
    if (visibility) {
      input.setText("");

      SCEDocumentPosition selectionStart = document.getSelectionStart();
      SCEDocumentPosition selectionEnd = document.getSelectionEnd();
      if (selectionStart == null || selectionEnd == null) {
        selection = null;
      } else {
        selection = new SCEDocumentRange(selectionStart, selectionEnd);
        document.clearSelection();
      }
    } else {
      results.clear();
    }
    clearHighlights(visibility);
    super.setVisible(visibility);
  }

  private void moveTo(SCEDocumentRange range) {
    SCEDocumentPosition start = range.getStartPosition();
    SCEDocumentPosition end = range.getEndPosition();

    editor.moveTo(start.getRow(), start.getColumn());
    SCEDocument document = editor.getTextPane().getDocument();
    document.setSelectionRange(start, end);
    editor.getTextPane().repaint();
  }

  public void previous() {
    SCECaret caret = editor.getTextPane().getCaret();
    SCEDocumentRange last = null;
    for (SCEDocumentRange result : results) {
      SCEDocumentPosition start = result.getStartPosition();
      if (start.getRow() > caret.getRow() || (start.getRow() == caret.getRow() && start.getColumn() >= caret.getColumn()))
        break;
      last = result;
    }
    if (last != null) moveTo(last);
    else first();
  }

  public void next(boolean includeCurrentPos) {
    SCECaret caret = editor.getTextPane().getCaret();
    for (SCEDocumentRange result : results) {
      SCEDocumentPosition start = result.getStartPosition();
      if (start.getRow() < caret.getRow()) continue;
      if (start.getRow() == caret.getRow()) {
        if (start.getColumn() < caret.getColumn()) continue;
        if ((start.getColumn() == caret.getColumn()) && !includeCurrentPos) continue;
      }
      moveTo(result);
      return;
    }
    last();
  }

  private void first() {
    if (results.size() > 0) moveTo(results.get(0));
  }

  private void last() {
    if (results.size() > 0) moveTo(results.get(results.size() - 1));
  }

  private void replace(SCEDocumentPosition start, SCEDocumentPosition end) {
    SCEDocument document = editor.getTextPane().getDocument();
    String text = document.getText(start, end);

    if (!regExp.isSelected()) {
      document.replace(start, end, replace.getText());
    } else {
      Pattern pattern = Pattern.compile(input.getText(), Pattern.MULTILINE | (caseSensitive.isSelected() ? Pattern.CASE_INSENSITIVE : 0));
      Matcher matcher = pattern.matcher(text);
      matcher.find();

      String replaceBy = replace.getText();
      StringBuilder builder = new StringBuilder();
      // handle escaped characters and groups
      boolean escape = false;
      for (char c : replaceBy.toCharArray()) {
        if (escape) {
          if ('1' <= c && c <= '9') {
            int groupNr = c - '0';
            if (groupNr <= matcher.groupCount()) {
              builder.append(matcher.group(groupNr));
            }
          } else if (c == 'n') {
            builder.append('\n');
          } else if (c == 'r') {
          } else if (c == 't') {
            builder.append("  ");
          } else {
            builder.append(c);
          }
          escape = false;
        } else {
          if (c == '\\') {
            escape = true;
          } else {
            builder.append(c);
          }
        }
      }

      document.replace(start, end, builder.toString());
    }
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == buttonClose) setVisible(false);
    if (e.getSource() == caseSensitive) {
      updateThread.documentChanged();
      updateThread.searchChanged();
    }
    if (e.getSource() == buttonNext) next(false);
    if (e.getSource() == buttonPrevious) previous();
    if (e.getSource() == regExp) updateThread.searchChanged();
    if (e.getSource() == selectionOnly) updateThread.searchChanged();
    if (e.getSource() == buttonShowReplace) setShowReplace(!isShowReplace());

    if (e.getSource() == buttonReplace) {
      SCEDocument document = editor.getTextPane().getDocument();
      replace(document.getSelectionStart(), document.getSelectionEnd());
      next(true);
    }

    if (e.getSource() == buttonReplaceAll) {
      ArrayList<SCEDocumentRange> matches = results;
      for (int matchNr = matches.size() - 1; matchNr >= 0; matchNr--) {
        SCEDocumentRange match = matches.get(matchNr);
        replace(match.getStartPosition(), match.getEndPosition());
      }
    }
  }

  public void keyTyped(KeyEvent e) {
  }

  public void keyPressed(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
      setVisible(false);
      e.consume();
    }
  }

  public void keyReleased(KeyEvent e) {
    if (e.getSource() == input) updateThread.searchChanged();
  }

  public void documentChanged(SCEDocument sender, SCEDocumentEvent event) {
    updateThread.documentChanged();
  }

  public void selectionChanged(SCEDocument sender, SCEDocumentPosition start, SCEDocumentPosition end) {
    if (start == null || end == null) {
      replaceEnabled(false);
      return;
    }

    for (SCEDocumentRange result : results) {
      SCEDocumentPosition rstart = result.getStartPosition();
      SCEDocumentPosition rend = result.getEndPosition();

      if (start.equals(rstart) && end.equals(rend)) {
        replaceEnabled(true);
        return;
      }
    }

    replaceEnabled(false);
  }

  private void replaceEnabled(boolean enabled) {
    buttonReplace.setEnabled(enabled);
    buttonReplaceAll.setEnabled(enabled);
  }

  private class UpdateThread extends Thread {
    private boolean searchChanged = true;
    private boolean documentChanged = true;

    private String text = "";
    private int text2row[] = new int[0];
    private int text2column[] = new int[0];

    private UpdateThread() {
	    super("SCESearch-UpdateThread");
      setPriority(Thread.NORM_PRIORITY);
    }

    public synchronized void searchChanged() {
      searchChanged = true;
      if (!isVisible()) return;
      notify();
    }

    public synchronized void documentChanged() {
      documentChanged = true;
      if (!isVisible()) return;
      notify();
    }

    private void updateDocument() {
      documentChanged = false;

      SCEDocument document = editor.getTextPane().getDocument();
      SCEDocumentRow[] documentRows = document.getRows();

      StringBuilder builder = new StringBuilder(100000);
      for (SCEDocumentRow row : documentRows) {
        builder.append(row.toString());
        builder.append('\n');

        if (documentChanged) return;
      }
      text = builder.toString();
      if (!caseSensitive.isSelected()) text = text.toLowerCase();

      // update position map
      if (text2row.length < text.length()) {
        text2row = new int[text.length()];
        text2column = new int[text.length()];
      }
      int rowNr = 0;
      int columnNr = 0;
      for (int charNr = 0; charNr < text.length(); charNr++) {
        text2row[charNr] = rowNr;
        text2column[charNr] = columnNr;

        columnNr++;
        char c = text.charAt(charNr);
        if (c == '\n') {
          rowNr++;
          columnNr = 0;
        }
      }
    }

    private void search(boolean move) {
      ArrayList<SCEDocumentRange> resultsTemp = new ArrayList<SCEDocumentRange>();

      searchChanged = false;
      SCEDocument document = editor.getTextPane().getDocument();

      clearHighlights(true);
      SCEMarkerBar markerBar = editor.getMarkerBar();
      SCEPane pane = editor.getTextPane();

      input.setBackground(Color.WHITE);

      SCECaret caret = editor.getTextPane().getCaret();
      boolean moveCaret = true;

      String search = input.getText();
      int length = search.length();

      SCERange selectionRange = null;

      if (length != 0) {
        if (!regExp.isSelected()) {
          // normal search
          if (!caseSensitive.isSelected()) search = search.toLowerCase();

          int index = -1;
          while ((index = text.indexOf(search, index + 1)) != -1) {
            int rowNr = text2row[index];
            int columnNr = text2column[index];

            SCEDocumentPosition start = document.createDocumentPosition(rowNr, columnNr);
            SCEDocumentPosition end = document.createDocumentPosition(rowNr, columnNr + length);
            if (!filter(start, end)) continue;

            if (caret.getRow() == rowNr && caret.getColumn() == columnNr) {
              moveCaret = false;
              selectionRange = new SCERange(rowNr, columnNr, rowNr, columnNr + length);
            }
            resultsTemp.add(new SCEDocumentRange(start, end));
            pane.addTextHighlight(new SCETextHighlight(pane, start, end, Color.YELLOW));
            markerBar.addMarker(new SCEMarkerBar.Marker(SCEMarkerBar.TYPE_SEARCH, rowNr, columnNr, ""));
          }
        } else {
          // regexp search
          Pattern pattern = Pattern.compile(search, Pattern.MULTILINE | (caseSensitive.isSelected() ? 0 : Pattern.CASE_INSENSITIVE));
          Matcher matcher = pattern.matcher(text);
          while (matcher.find()) {
            int startIndex = matcher.start();
            int endIndex = matcher.end();
            // skip maches of length 0
            if (startIndex == endIndex) continue;

            int rowStart = text2row[startIndex];
            int columnStart = text2column[startIndex];

            int rowEnd = text2row[endIndex];
            int columnEnd = text2column[endIndex];

            SCEDocumentPosition start = document.createDocumentPosition(rowStart, columnStart);
            SCEDocumentPosition end = document.createDocumentPosition(rowEnd, columnEnd);
            if (!filter(start, end)) continue;

            if (caret.getRow() == rowStart && caret.getColumn() == columnStart) {
              moveCaret = false;
              document.setSelectionRange(start, end);
            }
            resultsTemp.add(new SCEDocumentRange(start, end));
            pane.addTextHighlight(new SCETextHighlight(pane, start, end, Color.YELLOW));
            markerBar.addMarker(new SCEMarkerBar.Marker(SCEMarkerBar.TYPE_SEARCH, rowStart, columnEnd, ""));
          }
        }
      } else {
        document.clearSelection();
      }

      results = resultsTemp;

      if (length > 0 && resultsTemp.size() == 0) {
        input.setBackground(ERROR_COLOR);
        document.clearSelection();
      } else {
        // set the selection
        if (selectionRange != null) {
          document.setSelectionRange(selectionRange);
        }

        if (move && moveCaret) next(false);
      }

      markerBar.repaint();
      pane.repaint();
    }

    private boolean filter(SCEDocumentPosition start, SCEDocumentPosition end) {
      if (!selectionOnly.isSelected() || selection == null) return true;
      if (start.compareTo(selection.getStartPosition()) < 0) return false;
      if (end.compareTo(selection.getEndPosition()) > 0) return false;
      return true;
    }

    public void run() {
      try {
        while (!Thread.currentThread().isInterrupted()) {
          synchronized (this) {
            if ((!documentChanged && !searchChanged) || !isVisible()) {
              wait();
            }
          }
          if (!searchChanged && !documentChanged) continue;
          if (!isVisible()) continue;

          // update document information
          if (documentChanged) updateDocument();
          if (documentChanged) continue;

          // search
          try {
            search(searchChanged);
          } catch (Throwable ignored) {
          }
        }
      } catch (InterruptedException ignored) {
      }
    }
  }
}
