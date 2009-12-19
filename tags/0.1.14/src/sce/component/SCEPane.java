package sce.component;

import jlatexeditor.GProperties;
import sce.codehelper.CodeAssistant;
import sce.codehelper.CodeHelper;
import sce.codehelper.SCEPopup;
import sce.quickhelp.QuickHelp;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.text.AttributedString;
import java.util.ArrayList;

/**
 * SourceCodeEditor pane.
 *
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */
public class SCEPane extends JPanel implements SCEDocumentListener, SCECaretListener, FocusListener{
  /** Document. */
  SCEDocument document = null;
	/** Code Helper (code completion). */
  CodeHelper codeHelper = null;
  CodeHelper tabCompletion = null;
	/** Quick help. */
	QuickHelp quickHelp = null;
  /** Undo manager. */
  SCEUndoManager undoManager = null;
	/** General SCE Popup. */
	SCEPopup popup = null;

  // some text properties
  private int lineHeight = 0;
  private int lineAscent = 0;
  private int characterWidth = 0;
  /** Spacer for line numbers. */
  private int lineNumberSpacer = 30;
  private final int SPACE_LEFT = 3;

  // margin constants
  public static final int MARGIN_TOP = 0;
  public static final int MARGIN_BOTTOM = 1;
  public static final int MARGIN_LEFT = 2;
  public static final int MARGIN_RIGHT = 3;

  // normal text font
  private Font fontText = null;
  private Font fontLineNumbers = null;

  /** Caret in the document. */
  private SCECaret caret = null;
  private boolean freezeCaret = false;

  // UI properties
  private SCEPaneUI ui = null;
  private Color currentLineHighlightColor = new Color(255, 255, 200); //new Color(255, 255, 220);

  private Color selectionHighlightColor = new Color(82, 109, 165);
  private SCETextHighlight selectionHighlight = new SCETextHighlight(this, null, null, selectionHighlightColor);

  private Color editRangeHighlightColor = new Color(155, 0,0);
  private SCETextHighlight editRangeHighlight = new SCEEditRangeHighlight(this, null, null, editRangeHighlightColor);

  // highlights
  private ArrayList<SCERowHighlight> rowHighlights = new ArrayList<SCERowHighlight>();
  private ArrayList<SCETextHighlight> textHighlights = new ArrayList<SCETextHighlight>();

  // get focus back if lost
  private boolean focusBack = false;

  // the preferred size
  Dimension preferredSize = new Dimension(240, 320);

  // some characters
  private String splitterChars = " ,;.:!\"§$%&/()=?{[]}\\'´`+-*^°";

	/** Code assistants that are informed about alt+enter events. */
	private ArrayList<CodeAssistant> codeAssistants = new ArrayList<CodeAssistant>();


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

	  // create popup
	  popup = new SCEPopup(this);

    // normal text font
    fontText = GProperties.getEditorFont();
    fontLineNumbers = new Font("SansSerif", 0, 11);
    setFont(fontText);

    addFocusListener(this);
  }

  public void setSize(int width, int height) {
    super.setSize(Math.max(preferredSize.width, width), Math.max(preferredSize.height, height));
  }

  public void setSize(Dimension size) {
    setSize(size.width, size.height);
  }

  /**
   * If this pane is added to a container.
   */
  public void addNotify(){
    super.addNotify();

    if(ui != null) return;
    
    // create the UI
    ui = new SCEPaneUI(this);
	  ui.setCodeHelper(codeHelper);
	  ui.setTabCompletion(tabCompletion);
	  ui.setQuickHelp(quickHelp);

    // get font properties
    Graphics g2D = getGraphics();
    FontMetrics fm = g2D.getFontMetrics(fontText);
    lineHeight = fm.getAscent() + fm.getDescent() + fm.getLeading();
    lineAscent = fm.getAscent();
    characterWidth = fm.charWidth(' ');
  }


  public void update(Graphics g) {
    //super.update(g);
  }

  boolean o = false;
  /**
   * Paint the component.
   *
   * @param g the graphics to paint on
   */
  public void paint(Graphics g){
    Rectangle r = g.getClipBounds();
    g.clearRect(r.x, r.y, r.width, r.height);
    //super.paint(g); // does not work for java version "1.6.0_06"

    Graphics2D g2D = (Graphics2D) g;
    g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, GProperties.getTextAntialiasign());
    
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
	  for (SCERowHighlight rowHighlight : rowHighlights) {
      int row = rowHighlight.getPosition().getRow();
      if(row < startRow || row > endRow) continue;
		  rowHighlight.paint(g2D, this);
	  }

    // draw text highlights
	  for (SCETextHighlight textHighlight : textHighlights) {
      if(textHighlight.getEndPosition().getRow() < startRow || textHighlight.getStartPosition().getRow() > endRow) continue;
			textHighlight.paint(g2D, this);
	  }

    // draw the selection
    if(document.hasSelection()){
      selectionHighlight.paint(g2D, this, document.getSelectionStart(),  document.getSelectionEnd());
    }

    // draw the selection
    if(document.hasEditRange()){
      editRangeHighlight.paint(g2D, this, document.getEditRangeStart(),  document.getEditRangeEnd());
    }

    // draw the text
    for(int line = startRow; line < endRow; line++){
      AttributedString attributedString = document.getRowAttributed(line);
      if(attributedString == null) continue;
      int posx = lineNumberSpacer + SPACE_LEFT;
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
   * @return preferred size of the component
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
   * Do not move the caret with document changes?
   */
  public boolean isFreezeCaret() {
    return freezeCaret;
  }

  /**
   * Do not move the caret with document changes?
   */
  public void setFreezeCaret(boolean freezeCaret) {
    this.freezeCaret = freezeCaret;
  }

  /**
   * Insert text from clipboard.
   */
  public void paste() {
    Transferable content = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
    try{
      String string = (String) content.getTransferData(DataFlavor.stringFlavor);
	    string = string.replaceAll("\t", "  ");
      if(document.hasSelection()) ui.removeSelection();
      document.insert(string, caret.getRow(), caret.getColumn());
    } catch(Exception ignored){
    }
  }

  /**
   * Copy text to clipboard.
   */
  public void copy() {
	  ensureSelection();
	  StringSelection s = new StringSelection(document.getSelectedText());
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(s, null);
  }

	private void ensureSelection() {
		if (document.getSelectedText() == null) {
			document.setSelectionRange(new SCEDocumentPosition(caret.getRow(), 0), new SCEDocumentPosition(caret.getRow() + 1, 0));
			repaint();
		}
	}

	/**
   * Cut text to clipboard.
   */
  public void cut() {
		ensureSelection();
    StringSelection s = new StringSelection(document.getSelectedText());
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(s, null);
    ui.removeSelection();
  }

	/**
	 * Comment the selected lines.
	 * 
	 * @param commentPrefix prefix used to mark a line as lineComment
	 */
	public void lineComment(String commentPrefix) {
		if (!document.hasSelection()) {
			int row = caret.getRow();
			int col = caret.getColumn();
			document.insert(commentPrefix, caret.getRow(), 0);
			caret.moveTo(row, col+commentPrefix.length());
			caret.removeSelectionMark();
		} else {
			SCEDocumentPosition startSel = document.getSelectionStart();
			SCEDocumentPosition endSel = document.getSelectionEnd();

		  int startRow = startSel.getRow();
		  int endRow = endSel.getRow() - (endSel.getColumn() == 0 ? 1 : 0);
		  for(int row = startRow; row <= endRow; row++){
				document.insert("% ", row, 0);
		  }

			int endCol = endSel.getColumn();
			endCol = endCol == 0 ? endCol : endCol+2;
			endSel = new SCEDocumentPosition(endSel.getRow(), endCol);
			caret.moveTo(endSel.getRow(), endCol);
			document.setSelectionRange(startSel, endSel);
		}
	}

	/**
	 * Uncomment the selected lines.
	 *
	 * @param commentPrefix prefix used to mark a line as lineComment
	 */
	public void lineUncomment(String commentPrefix) {
		if (!document.hasSelection()) {
			int row = caret.getRow();
			int col = caret.getColumn();
			if (removeComment(commentPrefix, caret.getRow())) {
				caret.moveTo(row, Math.max(col-commentPrefix.length(), 0));
			}
			caret.removeSelectionMark();
			repaint();
		} else {
			SCEDocumentPosition startSel = document.getSelectionStart();
			SCEDocumentPosition endSel = document.getSelectionEnd();

		  int startRow = startSel.getRow();
		  int endRow = endSel.getRow() - (endSel.getColumn() == 0 ? 1 : 0);
			boolean moveCaret = false;
		  for(int row = startRow; row <= endRow; row++){
			  if (removeComment(commentPrefix, row) && row == caret.getRow()) {
				  moveCaret = true;
			  }
		  }

			int endCol = endSel.getColumn();
			endCol = Math.max(endCol, 0);
			endSel = new SCEDocumentPosition(endSel.getRow(), endCol);
			if (moveCaret) {
				caret.moveTo(endSel.getRow(), endCol);
			}
			document.setSelectionRange(startSel, endSel);
		}
	}

	private boolean removeComment(String commentPrefix, int row) {
		String rowString = document.getRow(row);
		if(rowString.startsWith(commentPrefix)){
		  document.remove(row, 0, row, commentPrefix.length());
			return true;
		}
		return false;
	}

  /**
	 * Returns the popup for code assistants.
	 *
	 * @return popup for code assistants
	 */
	public SCEPopup getPopup() {
		return popup;
	}

	/**
   * Calculates the position on the screen.
   *
   * @param row the row
   * @param column the column
   * @return position on the screen
   */
  public Point modelToView(int row, int column){
    return new Point(lineNumberSpacer + SPACE_LEFT + column * characterWidth, row * lineHeight);
  }

  /**
   * Converts a position on the screen into model coordinates.
   *
   * @param x the x coordinate
   * @param y the y coordinate
   * @return the model coordinates
   */
  public SCEDocumentPosition viewToModel(int x, int y){
    int row = y / lineHeight;
    int column = Math.max((x + characterWidth / 2 - lineNumberSpacer - SPACE_LEFT) / characterWidth, 0);
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
   * @return margin of the text pane
   */
  public int getMargin(int margin){
    if(margin == MARGIN_LEFT) return lineNumberSpacer + SPACE_LEFT;
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
    freezeCaret = true;
    document.setText(text);
    freezeCaret = false;
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
	 * Sets the code helper for the source code editor.
	 *
	 * @param codeHelper code helper
	 */
	public void setCodeHelper(CodeHelper codeHelper) {
		this.codeHelper = codeHelper;
		if (ui != null) {
			ui.codeHelperPane.setCodeHelper(codeHelper);
		}
	}

  /**
   * Sets the tab completion for the source code editor.
   *
   * @param tabCompletion tab completion
   */
  public void setTabCompletion(CodeHelper tabCompletion) {
    this.tabCompletion = tabCompletion;
    if (ui != null) {
      ui.codeHelperPane.setTabCompletion(tabCompletion);
    }
  }

	/**
	 * Sets the code helper for the source code editor.
	 *
	 * @param quickHelp quick help
	 */
	public void setQuickHelp(QuickHelp quickHelp) {
		this.quickHelp = quickHelp;
		if (ui != null) {
			ui.codeHelperPane.setCodeHelper(codeHelper);
		}
	}

	/**
	 * Registers a code assistant in order to be informed about alt+enter events.
	 *
	 * @param codeAssistant code assistant which wants to be informed about alt+enter events
	 */
	public void addCodeAssistantListener(CodeAssistant codeAssistant){
	  if(codeAssistants.contains(codeAssistant)) return;
	  codeAssistants.add(codeAssistant);
	}

	/**
	 * Unregisters a code assistant.
	 *
	 * @param codeAssistant code assistant to unregister
	 */
	public void removeCodeAssistantListener(CodeAssistant codeAssistant){
	  codeAssistants.remove(codeAssistant);
	}

	/**
	 * Informs the code assistants about alt+enter events.
	 */
	void callCodeAssistants() {
	  for (CodeAssistant codeAssistant : codeAssistants) {
	    if (codeAssistant.assistAt(this)) return;
	  }
	}
	
  /**
   * Finds the next splitter in the given row and direction.
   *
   * @param row row
   * @param column column
   * @param direction -1 / 1
   * @return splitter column
   */
  public int findSplitterInRow(int row, int column, int direction){
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
	 * Finds the next splitter in the given direction.
	 *
	 * @param row row
	 * @param column column
	 * @param direction direction -1 / 1
	 * @return splitter position
	 */
	public SCEPosition findSplitterPosition(int row, int column, int direction) {
		switch (direction) {
			case -1:
				if (column == 0 && row > 0) {
					return new SCEDocumentPosition(row-1, document.getRowLength(row-1));
				} else {
					return new SCEDocumentPosition(row, findSplitterInRow(row, column, -1));
				}
			case 1:
				if (column == document.getRowLength(row) && row < document.getRowsCount()-1) {
					return new SCEDocumentPosition(row+1, 0);
				} else {
					return new SCEDocumentPosition(row, findSplitterInRow(row, column, 1));
				}
		}
		return null;
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

  public void removeAllRowHighlights(){
    rowHighlights.clear();
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

  public void removeAllTextHighlights() {
    textHighlights.clear();
  }

  // sce.component.SCEDocumentListener methods
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
    if(event.isInsert() && !freezeCaret){
      caret.moveTo(end.getRow(), end.getColumn());
      if(caret.getSelectionMark() != null){
        caret.removeSelectionMark();
        caret.setSelectionMark();
      }
    }
    if(event.isRemove() && !freezeCaret){
      caret.moveTo(start.getRow(), start.getColumn());
    }

    repaint();
  }

  // sce.component.SCECaretListener methods
  public void caretMoved(int row, int column, int lastRow, int lastColumn){
    if(row != lastRow || document.hasSelection()){
      Point ul = modelToView(row, 0);
      repaint(0, ul.y, getWidth(), lineHeight);
      ul = modelToView(lastRow, 0);
      repaint(0, ul.y, getWidth(), lineHeight);
    }

    // scroll to caret
    Point caretPos = modelToView(row, column);
    scrollRectToVisible(new Rectangle(Math.max(0,caretPos.x - 30), Math.max(0, caretPos.y - 20), 60, lineHeight + 40));
  }

  // FocusListener methods
  public void focusGained(FocusEvent e){
  }

  public void focusLost(FocusEvent e){
    if(!focusBack) return;

    synchronized(this){
      try{ wait(100); } catch(InterruptedException ignored){ }
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
