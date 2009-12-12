package sce.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

/**
 * Search pane.
 */
public class SCESearch extends JPanel implements ActionListener, KeyListener {
  private SourceCodeEditor editor;

  private JTextField input = new JTextField();
  private ImageButton buttonNext;
  private ImageButton buttonPrevious;
  private JCheckBox caseSensitive = new JCheckBox("Case sensitive", false);
  private ImageButton buttonClose;

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

    setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
    input.setColumns(30);
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
    buttonClose.addActionListener(this);
    add(buttonClose);
    buttonClose.addActionListener(this);

    setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
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

  private void update() {
    clearHighlights();
    SCEMarkerBar markerBar = editor.getMarkerBar();
    SCEPane pane = editor.getTextPane();

    input.setBackground(Color.WHITE);

    String search = input.getText();
    if(!caseSensitive.isSelected()) search = search.toLowerCase();

    SCECaret caret = editor.getTextPane().getCaret();
    boolean moveCaret = true;

    int length = search.length();
    if(length != 0) {
      SCEDocument document = editor.getTextPane().getDocument();
      for(int row = 0; row < document.getRowsCount(); row++) {
        String rowText = document.getRow(row);
        int column = -1;
        while((column = rowText.indexOf(search, column+1)) != -1) {
          if(caret.getRow() == row && caret.getColumn() == column) moveCaret = false;
          positions.add(new Position(row, column));
          pane.addTextHighlight(new SCETextHighlight(pane, new SCEDocumentPosition(row, column), new SCEDocumentPosition(row, column+length), Color.YELLOW));
          markerBar.addMarker(new SCEMarkerBar.Marker(SCEMarkerBar.TYPE_SEARCH, row, column, ""));
        }
      }
    }
    markerBar.repaint();

    if(moveCaret) next();
    if(length > 0 && positions.size() == 0) input.setBackground(new Color(255, 204, 204));
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
    if(e.getSource() == caseSensitive) update();
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
    if(e.getSource() == input) update();
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
}
