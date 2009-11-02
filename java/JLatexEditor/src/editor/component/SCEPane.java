
/**
 * @author Jörg Endrullis
 */

package editor.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Iterator;

public class SCEPane extends JPanel implements SCEDocumentListener, SCECaretListener, FocusListener{
  // the document
  SCEDocument document = null;
  // undo manager
  SCEUndoManager undoManager = null;

  // some text properties
  private int lineHeight = 0;
  private int lineAscent = 0;
  private int characterWidth = 0;
  // spacer for line numbers
  private int lineNumberSpacer = 30;
  // margin constants
  public static final int MARGIN_TOP = 0;
  public static final int MARGIN_BOTTOM = 1;
  public static final int MARGIN_LEFT = 2;
  public static final int MARGIN_RIGHT = 3;

  // normal text font
  private Font fontText = null;
  private Font fontLineNumbers = null;

  // the caret
  private SCECaret caret = null;

  // UI properties
  private SCEPaneUI ui = null;
  private Color currentLineHighlightColor = new Color(255, 255, 220);

  private Color selectionHighlightColor = new Color(82, 109, 165);
  private SCETextHighlight selectionHighlight = new SCETextHighlight(this, null, null, selectionHighlightColor);

  private Color editRangeHighlightColor = new Color(155, 0,0);
  private SCETextHighlight editRangeHighlight = new SCEEditRangeHighlight(this, null, null, editRangeHighlightColor);

  // highlights
  private ArrayList rowHighlights = new ArrayList();
  private ArrayList textHighlights = new ArrayList();

  // is this text pane editable?
  private boolean editable = true;

  // get focus back if lost
  private boolean focusBack = false;

  // the preferred size
  Dimension preferredSize = new Dimension(240, 320);

  // some characters
  private String splitterChars = " ,;.:!\"§$%&/()=?{[]}\\'´`+-*^°";

  /**
   * Creates a SCEPane (a text pane for editing source code).
   */
  public SCEPane(){
    setBackground(Color.WHITE);

    // create the underlying document
    document = new SCEDocument();
    document.addSCEDocumentListener(this);

    // create undo manager
    undoManager = new SCEUndoManager(document);

    // create a caret
    caret = new SCECaret(this);
    caret.addSCECaretListener(this);

    // normal text font
    fontText = new Font("MonoSpaced", 0, 13);
    fontLineNumbers = new Font("SansSerif", 0, 11);
    setFont(fontText);


    addFocusListener(this);
  }

  /**
   * If this pane is added to a container.
   */
  public void addNotify(){
    super.addNotify();

    // create the UI
    ui = new SCEPaneUI(this);

    // get font properties
    Graphics g2D = getGraphics();
    FontMetrics fm = g2D.getFontMetrics(fontText);
    lineHeight = fm.getAscent() + fm.getDescent() + fm.getLeading();
    lineAscent = fm.getAscent();
    characterWidth = fm.charWidth(' ');
  }

  /**
   * Paint the component.
   *
   * @param g the graphics to paint on
   */
  public void paint(Graphics g){
    Graphics2D g2D = (Graphics2D) g;
    super.paint(g2D);

    // clip bounds
    int y = g2D.getClipBounds().y;
    int height = g2D.getClipBounds().height;

    // calculate the start and end row
    int startRow = viewToModel(0, y).getRow();
    int endRow = viewToModel(0, y + height).getRow() + 1;

    // highlight the current line
    g2D.setColor(currentLineHighlightColor);
    Point caretPos = modelToView(caret.getRow(), caret.getColumn());
    g2D.fillRect(lineNumberSpacer, caretPos.y, getWidth(), lineHeight);

    // draw row highlights
    Iterator rowHighlightIterator = rowHighlights.iterator();
    while(rowHighlightIterator.hasNext()){
      SCERowHighlight highlight = (SCERowHighlight) rowHighlightIterator.next();
      highlight.paint(g2D, this);
    }

    // draw text highlights
    Iterator textHighlightIterator = textHighlights.iterator();
    while(textHighlightIterator.hasNext()){
      SCETextHighlight highlight = (SCETextHighlight) textHighlightIterator.next();
      highlight.paint(g2D, this);
    }

    // draw the selection
    if(document.hasSelection()){
      selectionHighlight.paint(g2D, this, document.getSelectionStart(),  document.getSelectionEnd());
    }

    // draw the selection
    if(document.hasEditRange()){
      editRangeHighlight.paint(g2D, this, document.getEditRangeStart(),  document.getEditRangeEnd());
    }

    // draw the test
    for(int line = startRow; line < endRow; line++){
      AttributedString attributedString = document.getRowAttributed(line);
      if(attributedString == null) continue;
      int posx = lineNumberSpacer;
      int posy = line * lineHeight + lineAscent - 1;
      g2D.drawString(attributedString.getIterator(), posx, posy);
    }

    // draw the border
    g2D.setColor(Color.lightGray);
    g2D.drawLine(lineNumberSpacer - 1, y, lineNumberSpacer - 1, y + height);
    g2D.drawLine(lineNumberSpacer + 80 * characterWidth, y, lineNumberSpacer + 80 * characterWidth, y + height);

    // draw the line numbers
    g2D.setFont(fontLineNumbers);
    FontMetrics fm = g2D.getFontMetrics(fontLineNumbers);
    for(int line = startRow; line < endRow; line++){
      String lineString = (line + 1) + "";
      int posx = lineNumberSpacer - fm.stringWidth(lineString) - 2;
      int posy = line * lineHeight + lineAscent - 1;
      g2D.drawString(lineString, posx, posy);
    }

    // draw the caret
    if(hasFocus()) caret.paint(g);
  }

  /**
   * Returns the preferred size of the component.
   *
   * @return
   */
  public Dimension getPreferredSize(){
    return preferredSize;
  }

  /**
   * Returns the underlying document.
   *
   * @return the document
   */
  public SCEDocument getDocument(){
    return document;
  }

  /**
   * Returns the undo manager for the document.
   *
   * @return the undo manager
   */
  public SCEUndoManager getUndoManager(){
    return undoManager;
  }

  /**
   * Returns the caret of this pane.
   *
   * @return the caret
   */
  public SCECaret getCaret(){
    return caret;
  }

  /**
   * Calculates the position on the screen.
   *
   * @param row the row
   * @param column the column
   * @return
   */
  public Point modelToView(int row, int column){
    return new Point(lineNumberSpacer + column * characterWidth, row * lineHeight);
  }

  /**
   * Converst a position on the screen into model coordinates.
   *
   * @param x the x coordinate
   * @param y the y coordinate
   * @return the model coordinates
   */
  public SCEDocumentPosition viewToModel(int x, int y){
    int row = y / lineHeight;
    int column = Math.max((x + characterWidth / 2 - lineNumberSpacer) / characterWidth, 0);
    return new SCEDocumentPosition(row, column);
  }

  /**
   * Returns the character width.
   *
   * @return character width
   */
  public int getCharacterWidth(){
    return characterWidth;
  }

  /**
   * Returns the line height.
   *
   * @return line height
   */
  public int getLineHeight(){
    return lineHeight;
  }

  /**
   * Returns the margin of the text pane.
   *
   * @param margin MARGIN_TOP/ MARGIN_BOTTOM/ MARGIN_LEFT/ MARGIN_RIGHT
   * @return
   */
  public int getMargin(int margin){
    if(margin == MARGIN_LEFT) return lineNumberSpacer;
    return 0;
  }

  /**
   * Returns the number of visible rows.
   *
   * @return visible rows
   */
  public int getVisibleRowsCount(){
    return (int) ((getVisibleRect().getHeight() / lineHeight) - 1);
  }

  /**
   * Sets the text in this pane.
   *
   * @param text the text
   */
  public void setText(String text){
    document.setText(text);
  }

  /**
   * Returns the text in this pane.
   *
   * @return the text
   */
  public String getText(){
    return document.getText();
  }

  /**
   * Sets if this pane should be editable.
   *
   * @param editable true, if editable
   */
  public void setEditable(boolean editable){
    this.editable = editable;
  }

  /**
   * Returns if the text pane is editable.
   *
   * @return treu, if editable
   */
  public boolean isEditbale(){
    return editable;
  }

  /**
   * Finds the next splitter in this direction.
   *
   * @param direction -1 / 1
   * @return splitter column
   */
  public int findSplitter(int row, int column, int direction){
    String text = document.getRow(row);

    int position = direction == -1 ? column - 1 : column;
    if(position < 0) return 0;
    if(position >= document.getRowLength(row)) return document.getRowLength(row);

    boolean splitter = splitterChars.indexOf(text.charAt(position)) != -1;
    while(position >= 0 && position < document.getRowLength(row)){
      if(splitter != (splitterChars.indexOf(text.charAt(position)) != -1)) break;
      position += direction;
    }
    if(direction == -1) position++;

    return position;
  }

  /**
   * Adds a row highlight.
   *
   * @param highlight the highlight
   */
  public void addRowHighlight(SCERowHighlight highlight){
    rowHighlights.add(highlight);
    repaint();
  }

  /**
   * Removes a row highlight.
   *
   * @param highlight the highlight
   */
  public void removeRowHighlight(SCERowHighlight highlight){
    rowHighlights.remove(highlight);
    repaint();
  }

  /**
   * Adds a text highlight.
   *
   * @param highlight the highlight
   */
  public void addTextHighlight(SCETextHighlight highlight){
    textHighlights.add(highlight);
    repaint();
  }

  /**
   * Removes a text highlight.
   *
   * @param highlight the highlight
   */
  public void removeTextHighlight(SCETextHighlight highlight){
    textHighlights.remove(highlight);
    repaint();
  }

  // uni.editor.component.SCEDocumentListener methods
  public void documentChanged(SCEDocument sender, SCEDocumentEvent event){
    SCEDocumentPosition start = event.getRangeStart();
    SCEDocumentPosition end = event.getRangeEnd();

    // update the component size
    if(getParent() != null){
      Dimension dimension = new Dimension();
      for(int row_nr = 0; row_nr < document.getRowsCount(); row_nr++){
        dimension.width = Math.max(document.getRowLength(row_nr) * characterWidth + lineNumberSpacer + 30, dimension.width);
      }
      dimension.height = document.getRowsCount() * lineHeight + 30;
      preferredSize = dimension;
      getParent().doLayout();
    }

    // update the caret position
    if(event.isInsert()){
      caret.moveTo(end.getRow(), end.getColumn());
      if(caret.getSelectionMark() != null){
        caret.removeSelectionMark();
        caret.setSelectionMark();
      }
    }
    if(event.isRemove()){
      caret.moveTo(start.getRow(), start.getColumn());
    }

    repaint();
  }

  // uni.editor.component.SCECaretListener methods
  public void caretMoved(int row, int column, int lastRow, int lastColumn){
    if(row != lastRow || document.hasSelection()){
      Point ul = modelToView(row, 0);
      repaint(0, ul.y, getWidth(), lineHeight);
      ul = modelToView(lastRow, 0);
      repaint(0, ul.y, getWidth(), lineHeight);
    }

    // scroll to caret
    Point caretPos = modelToView(row, column);
    scrollRectToVisible(new Rectangle(caretPos.x - 30, caretPos.y - 20, 60, lineHeight + 40));
  }

  // FocusListener methods
  public void focusGained(FocusEvent e){
  }

  public void focusLost(FocusEvent e){
    if(!focusBack) return;

    synchronized(this){
      try{ wait(100); } catch(InterruptedException e1){ }
    }

    frameToFront(this);
    requestFocus();

    focusBack = false;
  }

  private void frameToFront(Container component){
    if(component instanceof JFrame){
      ((JFrame) component).toFront();
      return;
    }
    frameToFront(component.getParent());
  }

  public void getFocusBack(){
    focusBack = true;
  }
}
