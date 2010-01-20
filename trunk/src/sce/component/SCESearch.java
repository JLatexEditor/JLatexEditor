package sce.component;

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
public class SCESearch extends JPanel implements ActionListener, KeyListener, SCEDocumentListener {
  private SourceCodeEditor editor;

  private JTextField input = new JTextField();
  private ImageButton buttonNext;
  private ImageButton buttonPrevious;
  private JCheckBox caseSensitive = new JCheckBox("Case sensitive", false);
  private JCheckBox regExp = new JCheckBox("Regexp", false);
  private ImageButton buttonClose;
  private ImageButton buttonShowReplace;

  private boolean showReplace = false;
  private JTextField replace = new JTextField();
  private ImageButton buttonReplace;

  private GroupLayout layout;

  // search update thread
  private UpdateThread updateThread = new UpdateThread();

  // search results
  private ArrayList<SCEDocumentRange> results = new ArrayList<SCEDocumentRange>();

  private GroupLayout.Group groupHorizontal;
  private GroupLayout.Group groupVertical;

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

    layout = new GroupLayout(this);
    setLayout(layout);

    layout.setAutoCreateGaps(true);

    input.setColumns(40);
    add(input);
    input.addKeyListener(this);
    editor.getTextPane().addKeyListener(this);
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
    add(buttonClose);
    buttonClose.addActionListener(this);
    add(buttonShowReplace);
    buttonShowReplace.addActionListener(this);

    replace.setColumns(40);
    add(replace);
    replace.addKeyListener(this);
    add(buttonReplace);
    buttonReplace.addActionListener(this);

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
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 100, Short.MAX_VALUE)
                .addComponent(buttonShowReplace)
            )
            .addGroup(
              layout.createSequentialGroup()
                .addComponent(buttonReplace)
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
            .addComponent(buttonShowReplace)
        )
        .addGroup(
          layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(replace)
            .addComponent(buttonReplace)
        )
        .addGap(2);

    layout.setHorizontalGroup(groupHorizontal);
    layout.setVerticalGroup(groupVertical);

    updateThread = new UpdateThread();
    updateThread.start();

    setShowReplace(false);
  }
  
  public boolean isShowReplace() {
    return showReplace;
  }

  public void setShowReplace(boolean showReplace) {
    this.showReplace = showReplace;

    replace.setVisible(showReplace);
    buttonReplace.setVisible(showReplace);
  }

  public void focus() {
    input.grabFocus();
  }

  private void clearHighlights() {
    SCEMarkerBar markerBar = editor.getMarkerBar();
    markerBar.clear(SCEMarkerBar.TYPE_SEARCH);
    results.clear();
    SCEPane pane = editor.getTextPane();
    pane.removeAllTextHighlights();
  }

  public void close() {
    setVisible(false);
    clearHighlights();
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
    for(SCEDocumentRange result : results) {
      SCEDocumentPosition start = result.getStartPosition();
      if(start.getRow() > caret.getRow() || (start.getRow() == caret.getRow() && start.getColumn() >= caret.getColumn())) break;
      last = result;
    }
    if(last != null) moveTo(last); else first();
  }

  public void next() {
    SCECaret caret = editor.getTextPane().getCaret();
    for(SCEDocumentRange result : results) {
      SCEDocumentPosition start = result.getStartPosition();
      if(start.getRow() < caret.getRow() || (start.getRow() == caret.getRow() && start.getColumn() <= caret.getColumn())) continue;
      moveTo(result);
      return;
    }
    last();
  }

  private void first() {
    if(results.size() > 0) moveTo(results.get(0));
  }

  private void last() {
    if(results.size() > 0) moveTo(results.get(results.size()-1));
  }

  public void actionPerformed(ActionEvent e) {
    if(e.getSource() == buttonClose) close();
    if(e.getSource() == caseSensitive) updateThread.documentChanged();
    if(e.getSource() == buttonNext) next();
    if(e.getSource() == buttonPrevious) previous();
    if(e.getSource() == buttonShowReplace) setShowReplace(!isShowReplace());
  }

  public void keyTyped(KeyEvent e) {
  }

  public void keyPressed(KeyEvent e) {
	  if(e.getKeyCode() == KeyEvent.VK_ESCAPE){
		  close();
		  e.consume();
	  }
  }

  public void keyReleased(KeyEvent e) {
    if(e.getSource() == input) updateThread.documentChanged();
  }

  public void documentChanged(SCEDocument sender, SCEDocumentEvent event) {
    updateThread.documentChanged();
  }

  public static class Position {
    private int row;
    private int column;

    public Position(int row, int column) {
      this.row = row;
      this.column = column;
    }

    public int getRow() {
      return row;
    }

    public int getColumn() {
      return column;
    }
  }

  private class UpdateThread extends Thread {
    private boolean searchChanged = true;
    private boolean documentChanged = true;

    private String text = "";
    private int text2row[] = new int[0];
    private int text2column[] = new int[0];

    private UpdateThread() {
      setPriority(Thread.NORM_PRIORITY);
    }

    public synchronized void searchChanged() {
      searchChanged = true;
      notify();
    }

    public synchronized void documentChanged() {
      documentChanged = true;
      notify();
    }

    private void updateDocument() {
      documentChanged = false;

      SCEDocument document = editor.getTextPane().getDocument();
      SCEDocumentRow[] documentRows = document.getRows();

      StringBuilder builder = new StringBuilder(100000);
      for(SCEDocumentRow row : documentRows) {
        builder.append(row.toString());
        builder.append('\n');

        if (documentChanged) return;
      }
      text = builder.toString();
      if(!caseSensitive.isSelected()) text = text.toLowerCase();

      // update position map
      if(text2row.length < text.length()) {
        text2row = new int[text.length()];
        text2column = new int[text.length()];
      }
      int rowNr = 0;
      int columnNr = 0;
      for(int charNr = 0; charNr < text.length(); charNr++) {
        text2row[charNr] = rowNr;
        text2column[charNr] = columnNr;

        columnNr++;
        char c = text.charAt(charNr);
        if(c == '\n') { rowNr++; columnNr = 0; }
      }
    }

    private void search() {
      searchChanged = false;

      SCEDocument document = editor.getTextPane().getDocument();

      clearHighlights();
      SCEMarkerBar markerBar = editor.getMarkerBar();
      SCEPane pane = editor.getTextPane();

      input.setBackground(Color.WHITE);

      SCECaret caret = editor.getTextPane().getCaret();
      boolean moveCaret = true;

      String search = input.getText();
      int length = search.length();

      if(length != 0) {
        if(!regExp.isSelected()) {
          // normal search
          if(!caseSensitive.isSelected()) search = search.toLowerCase();

          int index = -1;
          while((index = text.indexOf(search, index+1)) != -1) {
            int rowNr = text2row[index];
            int columnNr = text2column[index];

            SCEDocumentPosition start = document.createDocumentPosition(rowNr, columnNr);
            SCEDocumentPosition end = document.createDocumentPosition(rowNr, columnNr+length);

            if(caret.getRow() == rowNr && caret.getColumn() == columnNr) moveCaret = false;
            results.add(new SCEDocumentRange(start,end));
            pane.addTextHighlight(new SCETextHighlight(pane, start, end, Color.YELLOW));
            markerBar.addMarker(new SCEMarkerBar.Marker(SCEMarkerBar.TYPE_SEARCH, rowNr, columnNr, ""));
          }
        } else {
          // regexp search
          Pattern pattern = Pattern.compile(search, Pattern.MULTILINE | (caseSensitive.isSelected() ? Pattern.CASE_INSENSITIVE : 0));
          Matcher matcher = pattern.matcher(text);
          while(matcher.find()) {
            int startIndex = matcher.start();
            int rowStart = text2row[startIndex];
            int columnStart = text2column[startIndex];

            int endIndex = matcher.end();
            int rowEnd = text2row[endIndex];
            int columnEnd = text2column[endIndex];

            SCEDocumentPosition start = document.createDocumentPosition(rowStart, columnStart);
            SCEDocumentPosition end = document.createDocumentPosition(rowEnd, columnEnd);

            if(caret.getRow() == rowStart && caret.getColumn() == columnStart) moveCaret = false;
            results.add(new SCEDocumentRange(start,end));
            pane.addTextHighlight(new SCETextHighlight(pane, start, end, Color.YELLOW));
            markerBar.addMarker(new SCEMarkerBar.Marker(SCEMarkerBar.TYPE_SEARCH, rowStart, columnEnd, ""));
          }
        }
      }

      markerBar.repaint();
      pane.repaint();

      if(moveCaret) next();
      if(length > 0 && results.size() == 0) input.setBackground(new Color(255, 204, 204));
    }

    public void run() {
      while(true) {
        if(!documentChanged && !searchChanged) {
          try {
            synchronized(this) { wait(500); } 
          } catch (InterruptedException e) { }
        }
        if(!searchChanged && !documentChanged) continue;

        // update document information
        if(documentChanged) updateDocument();
        if(documentChanged) continue;

        // search
        try { search(); } catch(Throwable e) { }
      }
    }
  }
}
