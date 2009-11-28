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
  private JButton buttonNext = new JButton("Next");
  private JButton buttonPrevious = new JButton("Previous");
  private JButton buttonClose = new JButton("Close");

  // search results
  private ArrayList<Position> positions = new ArrayList<Position>();

  public SCESearch(SourceCodeEditor editor) {
    this.editor = editor;
    
    setLayout(new FlowLayout(FlowLayout.LEFT, 15, 5));
    input.setColumns(30);
    add(input);
    input.addKeyListener(this);
    add(buttonNext);
    buttonNext.addActionListener(this);
    add(buttonPrevious);
    buttonPrevious.addActionListener(this);
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

  public void actionPerformed(ActionEvent e) {
    if(e.getSource() == buttonClose) {
      setVisible(false);
      clearHighlights();
    }
  }

  public void keyTyped(KeyEvent e) {
  }

  public void keyPressed(KeyEvent e) {
	  if(e.getKeyCode() == KeyEvent.VK_ESCAPE){
		  setVisible(false);
		  e.consume();
	  }
  }

  public void keyReleased(KeyEvent e) {
    clearHighlights();
    SCEMarkerBar markerBar = editor.getMarkerBar();
    SCEPane pane = editor.getTextPane();

    input.setBackground(Color.WHITE);

    String search = input.getText();
    int length = search.length();
    if(length != 0) {
      SCEDocument document = editor.getTextPane().getDocument();
      for(int row = 0; row < document.getRowsCount(); row++) {
        String rowText = document.getRow(row);
        int column = -1;
        while((column = rowText.indexOf(search, column+1)) != -1) {
          positions.add(new Position(row, column));
          pane.addTextHighlight(new SCETextHighlight(pane, new SCEDocumentPosition(row, column), new SCEDocumentPosition(row, column+length), Color.YELLOW));
          markerBar.addMarker(new SCEMarkerBar.Marker(SCEMarkerBar.TYPE_SEARCH, row, column, ""));
        }
      }
    }
    markerBar.repaint();

    SCECaret caret = editor.getTextPane().getCaret();
    for(Position position : positions) {
      if(position.getRow() < caret.getRow()) continue;
      editor.moveTo(position.getRow(), position.getColumn());
      return;
    }
    if(positions.size() > 0) {
      Position position = positions.get(positions.size()-1);
      editor.moveTo(position.getRow(), position.getColumn());
      return;
    }
    if(length > 0) input.setBackground(new Color(255, 204, 204));
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
