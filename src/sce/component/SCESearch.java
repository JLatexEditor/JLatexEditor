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

  // search update thread
  private UpdateThread updateThread = new UpdateThread();

  // search results
  private ArrayList<Position> positions = new ArrayList<Position>();

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

    GroupLayout layout = new GroupLayout(this);
    setLayout(layout);

    layout.setAutoCreateGaps(true);
    //layout.setAutoCreateContainerGaps(true);

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

    layout.setHorizontalGroup(
       layout.createSequentialGroup()
               .addGap(5)
               .addComponent(input)
               .addComponent(buttonNext)
               .addComponent(buttonPrevious)
               .addComponent(caseSensitive)
               .addComponent(regExp)
               .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                                100, Short.MAX_VALUE)
               .addComponent(buttonShowReplace)
    );
    layout.setVerticalGroup(
       layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
               .addComponent(input)
               .addComponent(buttonNext)
               .addComponent(buttonPrevious)
               .addComponent(caseSensitive)
               .addComponent(regExp)
               .addComponent(buttonShowReplace)
    );

    
    updateThread = new UpdateThread();
    updateThread.start();
  }

  public void focus() {
    input.grabFocus();
  }

  private void clearHighlights() {
    SCEMarkerBar markerBar = editor.getMarkerBar();
    markerBar.clear(SCEMarkerBar.TYPE_SEARCH);
    positions.clear();
    SCEPane pane = editor.getTextPane();
    pane.removeAllTextHighlights();
  }

  public void close() {
    setVisible(false);
    clearHighlights();
  }

  private void moveTo(Position position) {
    editor.moveTo(position.getRow(), position.getColumn());
    SCEDocument document = editor.getTextPane().getDocument();
    SCEDocumentPosition start = document.createDocumentPosition(position.getRow(), position.getColumn());
    SCEDocumentPosition end = document.createDocumentPosition(position.getRow(), position.getColumn() + input.getText().length());
    document.setSelectionRange(start, end);
    editor.getTextPane().repaint();
  }

  public void previous() {
    SCECaret caret = editor.getTextPane().getCaret();
    Position last = null;
    for(Position position : positions) {
      if(position.getRow() > caret.getRow() || (position.getRow() == caret.getRow() && position.getColumn() >= caret.getColumn())) break;
      last = position;
    }
    if(last != null) moveTo(last); else first();
  }

  public void next() {
    SCECaret caret = editor.getTextPane().getCaret();
    for(Position position : positions) {
      if(position.getRow() < caret.getRow() || (position.getRow() == caret.getRow() && position.getColumn() <= caret.getColumn())) continue;
      moveTo(position);
      return;
    }
    last();
  }

  private void first() {
    if(positions.size() > 0) moveTo(positions.get(0));
  }

  private void last() {
    if(positions.size() > 0) moveTo(positions.get(positions.size()-1));
  }

  public void actionPerformed(ActionEvent e) {
    if(e.getSource() == buttonClose) close();
    if(e.getSource() == caseSensitive) updateThread.documentChanged();
    if(e.getSource() == buttonNext) next();
    if(e.getSource() == buttonPrevious) previous();
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

            if(caret.getRow() == rowNr && caret.getColumn() == columnNr) moveCaret = false;
            positions.add(new Position(rowNr, columnNr));
            pane.addTextHighlight(new SCETextHighlight(pane, document.createDocumentPosition(rowNr, columnNr), document.createDocumentPosition(rowNr, columnNr+length), Color.YELLOW));
            markerBar.addMarker(new SCEMarkerBar.Marker(SCEMarkerBar.TYPE_SEARCH, rowNr, columnNr, ""));
          }
        } else {
          // regexp search
          Pattern pattern = Pattern.compile(search, Pattern.MULTILINE | (caseSensitive.isSelected() ? Pattern.CASE_INSENSITIVE : 0));
          Matcher matcher = pattern.matcher(text);
          while(matcher.find()) {
            int start = matcher.start();
            int rowStart = text2row[start];
            int columnStart = text2column[start];

            int end = matcher.end();
            int rowEnd = text2row[end];
            int columnEnd = text2column[end];

            if(caret.getRow() == rowStart && caret.getColumn() == columnStart) moveCaret = false;
            positions.add(new Position(rowStart, columnStart));
            pane.addTextHighlight(new SCETextHighlight(pane, document.createDocumentPosition(rowStart, columnStart), document.createDocumentPosition(rowEnd, columnEnd), Color.YELLOW));
            markerBar.addMarker(new SCEMarkerBar.Marker(SCEMarkerBar.TYPE_SEARCH, rowStart, columnEnd, ""));
          }
        }
      }

      markerBar.repaint();
      pane.repaint();

      if(moveCaret) next();
      if(length > 0 && positions.size() == 0) input.setBackground(new Color(255, 204, 204));
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
