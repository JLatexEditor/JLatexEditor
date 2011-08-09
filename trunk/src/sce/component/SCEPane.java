package sce.component;

import jlatexeditor.gproperties.GProperties;
import sce.codehelper.*;
import sce.quickhelp.QuickHelp;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.font.TextAttribute;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * SourceCodeEditor pane.
 *
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */
public class SCEPane extends JPanel implements SCEDocumentListener, SCECaretListener, FocusListener {
	/** Character type. */
	public enum CT { space, special, letter }
	
  SourceCodeEditor sourceCodeEditor;
  /** Document. */
  SCEDocument document = null;
  /** Code Helper (code completion). */
  CodeHelper codeHelper = null;
  CodeHelper tabCompletion = null;
  /** Quick help. */
  QuickHelp quickHelp = null;
	/** Listener for line breaks. */
  LineBreakListener lineBreakListener = null;
  /** Undo manager. */
  SCEUndoManager undoManager = null;
  /** General SCE Popup. */
  SCEPopup popup = null;

  // some text properties
  private int lineHeight = 0;
  private int lineAscent = 0;
  private int characterWidth = 0;
  /**
   * Spacer for line numbers.
   */
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

  /**
   * Caret in the document.
   */
  private SCECaret caret = null;
  private boolean freezeCaret = false;

  // UI properties
  private SCEPaneUI ui = null;
  public static Color currentLineHighlightColor = new Color(255, 255, 200); //new Color(255, 255, 220);

  public static Color selectionHighlightColor = new Color(82, 109, 165);
  public static Color nonSelectionHighlightColor = new Color(196, 196, 196);
  public static Color selectionHighlightColorLight = new Color(255 - (255 - 82) / 3, 255 - (255 - 109) / 3, 255);
  private SCETextHighlight selectionHighlight = new SCETextHighlight(this, null, null, selectionHighlightColor);

  public static Color editRangeHighlightColor = new Color(155, 0, 0);
  private SCETextHighlight editRangeHighlight = new SCEEditRangeHighlight(this, null, null, editRangeHighlightColor);

  // highlights
  private ArrayList<SCERowHighlight> rowHighlights = new ArrayList<SCERowHighlight>();
  private ArrayList<SCETextHighlight> textHighlights = new ArrayList<SCETextHighlight>();

  private boolean transparentTextBackground = false;

  // get focus back if lost
  private boolean focusBack = false;

  // the preferred size
  private Dimension addToPreferredSize = new Dimension(0,0);
  private Dimension preferredSize = new Dimension(240, 320);

  // some characters
  public static final String splitterCharsString = ",;.:!\"§$%&/()=?{[]}\\'´`+-*^°";
  public static final HashSet<Character> splitterChars = new HashSet<Character>() {{
    for(char c : splitterCharsString.toCharArray()) add(c);
  }};

  /**
   * Code assistants that are informed about alt+enter events.
   */
  private ArrayList<CodeAssistant> codeAssistants = new ArrayList<CodeAssistant>();
  private int columnsPerRow = 80;


  /**
   * Creates a SCEPane (a text pane for editing source code).
   *
   * @param sourceCodeEditor source code editor
   */
  public SCEPane(SourceCodeEditor sourceCodeEditor) {
    this.sourceCodeEditor = sourceCodeEditor;

    setOpaque(true);
    setBackground(Color.WHITE);

    // create the underlying document
    document = new SCEDocument();
    document.addSCEDocumentListener(this);

    // create undo manager
    undoManager = new SCEUndoManager(this);

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
  public void addNotify() {
    super.addNotify();

    if (ui != null) return;

    // create the UI
    ui = new SCEPaneUI(this);
    ui.setCodeHelper(codeHelper);
    ui.setTabCompletion(tabCompletion);
    ui.setQuickHelp(quickHelp);
    ui.setLineBreakListener(lineBreakListener);

    // get font properties
    Graphics g2D = getGraphics();
    FontMetrics fm = g2D.getFontMetrics(fontText);
    lineHeight = fm.getAscent() + fm.getDescent() + fm.getLeading();
    lineAscent = fm.getAscent();
    characterWidth = fm.charWidth('W');

	  updateComponentSize();
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
  public void paint(Graphics g) {
    Rectangle r = g.getClipBounds();
    g.setColor(Color.WHITE);
    g.fillRect(r.x, r.y, r.width, r.height);
    //super.paint(g); // does not work for java version "1.6.0_06"

    Graphics2D g2D = (Graphics2D) g;
    g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, GProperties.getTextAntiAliasing());

    // clip bounds
    int x = g2D.getClipBounds().x;
    int y = g2D.getClipBounds().y;
    int width = g2D.getClipBounds().width;
    int height = g2D.getClipBounds().height;

    // calculate the start and end row
    SCEDocumentPosition upperLeft = viewToModel(x, y);
    SCEDocumentPosition lowerRight = viewToModel(x + width, y + height);
    int startRow = upperLeft.getRow();
    int endRow = lowerRight.getRow() + 1;
    int startCol = Math.max(0, upperLeft.getColumn() - 1);
    int endCol = lowerRight.getColumn() + 1;
    int charWidth = g2D.getFontMetrics(GProperties.getEditorFont()).charsWidth(new char[]{' '}, 0, 1);

    // highlight the current line
    g2D.setColor(currentLineHighlightColor);
    Point caretPos = modelToView(caret.getRow(), caret.getColumn());
    g2D.fillRect(lineNumberSpacer, caretPos.y, getWidth(), lineHeight);

    // draw row highlights
    synchronized (rowHighlights) {
      for (SCERowHighlight rowHighlight : rowHighlights) {
        int row = rowHighlight.getPosition().getRow();
        if (row < startRow || row > endRow) continue;
        rowHighlight.paint(g2D, this);
      }
    }
    g.setColor(Color.WHITE);

    // draw text highlights
    synchronized (textHighlights) {
      for (SCETextHighlight textHighlight : textHighlights) {
        if (textHighlight.getEndPosition().getRow() < startRow || textHighlight.getStartPosition().getRow() > endRow)
          continue;
        textHighlight.paint(g2D, this);
      }
    }

    // draw the selection
    if (document.hasSelection()) {
      selectionHighlight.paint(g2D, this, document.getSelectionStart(), document.getSelectionEnd());
    }

    // draw the text
    for (int line = startRow; line < endRow; line++) {
      AttributedString attributedString = document.getRowAttributed(line, startCol, endCol);
      if (attributedString == null) continue;
      int posx = lineNumberSpacer + SPACE_LEFT + startCol * charWidth;
      int posy = line * lineHeight + lineAscent - 1;
      if(transparentTextBackground) {
        attributedString.addAttribute(TextAttribute.BACKGROUND, null);
      }
      g2D.drawString(attributedString.getIterator(), posx, posy);
    }

	  // draw the edit range
	  if (document.hasEditRange()) {
	    editRangeHighlight.paint(g2D, this, document.getEditRangeStart(), document.getEditRangeEnd());
	  }

    // draw the border
    g2D.setColor(Color.lightGray);
    g2D.drawLine(lineNumberSpacer - 1, y, lineNumberSpacer - 1, y + height);
    if (columnsPerRow > 0)
      g2D.drawLine(lineNumberSpacer + 2 + columnsPerRow * characterWidth, y, lineNumberSpacer + 2 + columnsPerRow * characterWidth, y + height);

    // draw the line numbers
    g2D.setFont(fontLineNumbers);
    FontMetrics fm = g2D.getFontMetrics(fontLineNumbers);
    for (int line = startRow; line < endRow; line++) {
      String lineString = (line + 1) + "";
      int posx = lineNumberSpacer - fm.stringWidth(lineString) - 2;
      int posy = line * lineHeight + lineAscent - 1;
      g2D.drawString(lineString, posx, posy);
    }

    // draw the caret
    if (hasFocus()) caret.paint(g);

    // paint children
    paintChildren(g);
  }

  /**
   * Returns the preferred size of the component.
   *
   * @return preferred size of the component
   */
  public Dimension getPreferredSize() {
    return preferredSize;
  }

  public Dimension getAddToPreferredSize() {
    return addToPreferredSize;
  }

  public void setAddToPreferredSize(Dimension addToPreferredSize) {
    this.addToPreferredSize = addToPreferredSize;
    updateComponentSize();
  }

  /**
   * Returns the source code editor.
   *
   * @return source code editor
   */
  public SourceCodeEditor getSourceCodeEditor() {
    return sourceCodeEditor;
  }

  /**
   * Returns the underlying document.
   *
   * @return the document
   */
  public SCEDocument getDocument() {
    return document;
  }

  /**
   * Returns the undo manager for the document.
   *
   * @return the undo manager
   */
  public SCEUndoManager getUndoManager() {
    return undoManager;
  }

  /**
   * Returns the caret of this pane.
   *
   * @return the caret
   */
  public SCECaret getCaret() {
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
	 * Inserts the given text.
	 *
	 * @param text text to insert
	 */
	public void insert(String text) {
		if (document.hasSelection()) ui.removeSelection();
		document.insert(text, caret.getRow(), caret.getColumn());
	}

  /**
   * Insert text from clipboard.
   */
  public void paste() {
    Transferable content = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
    try {
      String string = (String) content.getTransferData(DataFlavor.stringFlavor);
	    insert(string.replaceAll("\t", "  "));
    } catch (Exception ignored) {
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
      document.setSelectionRange(new SCEDocumentPosition(caret.getRow(), 0), new SCEDocumentPosition(caret.getRow() + 1, 0), true);
      repaint();
    }
  }

  public void clearSelection() {
    document.setSelectionRange(null, null, true);
    caret.removeSelectionMark();
	  caret.setSelectionMark();
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
      caret.moveTo(row, col + commentPrefix.length(), false);
    } else {
      SCEDocumentPosition startSel = document.getSelectionStart();
      SCEDocumentPosition endSel = document.getSelectionEnd();

      int startRow = startSel.getRow();
      int endRow = endSel.getRow() - (endSel.getColumn() == 0 ? 1 : 0);
      for (int row = startRow; row <= endRow; row++) {
        document.insert(commentPrefix, row, 0);
      }

      int endCol = endSel.getColumn();
      endCol = endCol == 0 ? endCol : endCol + 2;
      endSel = new SCEDocumentPosition(endSel.getRow(), endCol);
      caret.moveTo(endSel.getRow(), endCol, false);
      document.setSelectionRange(startSel, endSel, true);
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
        caret.moveTo(row, Math.max(col - commentPrefix.length(), 0), false);
      }
    } else {
      SCEDocumentPosition startSel = document.getSelectionStart();
      SCEDocumentPosition endSel = document.getSelectionEnd();

      int startRow = startSel.getRow();
      int endRow = endSel.getRow() - (endSel.getColumn() == 0 ? 1 : 0);
      boolean moveCaret = false;
      for (int row = startRow; row <= endRow; row++) {
        if (removeComment(commentPrefix, row) && row == caret.getRow()) {
          moveCaret = true;
        }
      }

      int endCol = endSel.getColumn();
      endCol = Math.max(endCol, 0);
      endSel = new SCEDocumentPosition(endSel.getRow(), endCol);
      if (moveCaret) {
        caret.moveTo(endSel.getRow(), endCol, false);
      }
      document.setSelectionRange(startSel, endSel, true);
    }
  }

  private boolean removeComment(String commentPrefix, int row) {
    String rowString = document.getRowsModel().getRowAsString(row);
    if (rowString.startsWith(commentPrefix)) {
      document.remove(row, 0, row, commentPrefix.length());
      return true;
    }
    return false;
  }

	/**
	 * Removes the edit range without logging it in the undo manager.
	 */
	public void removeEditRangeSilently() {
		ui.codeHelperPane.endTemplateEditing(true);
	}

	public void editAsTemplate(List<List<WordWithPos>> wordGroups) {
		ArrayList<CHCommandArgument> arguments = new ArrayList<CHCommandArgument>(wordGroups.size());

		for (List<WordWithPos> wordGroup : wordGroups) {
			CHCommandArgument argument = new CHCommandArgument("", "", false);
			ArrayList<SCEDocumentRange> occurrences = new ArrayList<SCEDocumentRange>();
			for (WordWithPos word : wordGroup) {
				SCEPosition occurrenceStart = document.createDocumentPosition(word.startPos.getRow(), word.startPos.getColumn() - 1);
				SCEPosition occurrenceEnd = document.createDocumentPosition(word.endPos.getRow(), word.endPos.getColumn(), 0);
				occurrences.add(new SCEDocumentRange(occurrenceStart, occurrenceEnd));
			}
			argument.setOccurrences(occurrences);
			arguments.add(argument);
		}

		SCEPosition caretEndPosition = arguments.get(arguments.size() - 1).getOccurrences().get(0).getEndPos();
		ui.codeHelperPane.editAsTemplate(arguments, caretEndPosition);
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
   * @param row    the row
   * @param column the column
   * @return position on the screen
   */
  public Point modelToView(int row, int column) {
    return new Point(lineNumberSpacer + SPACE_LEFT + column * characterWidth, row * lineHeight);
  }

  /**
   * Converts a position on the screen into model coordinates.
   *
   * @param x the x coordinate
   * @param y the y coordinate
   * @return the model coordinates
   */
  public SCEDocumentPosition viewToModel(int x, int y) {
    int row = y / lineHeight;
    int column = Math.max((x + characterWidth / 2 - lineNumberSpacer - SPACE_LEFT) / characterWidth, 0);
    return new SCEDocumentPosition(row, column);
  }

  /**
   * Converts a position on the screen into model coordinates.
   *
   * @param x the x coordinate
   * @param y the y coordinate
   * @return the model coordinates
   */
  public SCEDocumentPosition viewToModelRoundOff(int x, int y) {
    int row = y / lineHeight;
    int column = Math.max((x - lineNumberSpacer - SPACE_LEFT) / characterWidth, 0);
    return new SCEDocumentPosition(row, column);
  }

  /**
   * Returns the character width.
   *
   * @return character width
   */
  public int getCharacterWidth() {
    return characterWidth;
  }

  /**
   * Returns the line height.
   *
   * @return line height
   */
  public int getLineHeight() {
    return lineHeight;
  }

  /**
   * Returns the margin of the text pane.
   *
   * @param margin MARGIN_TOP/ MARGIN_BOTTOM/ MARGIN_LEFT/ MARGIN_RIGHT
   * @return margin of the text pane
   */
  public int getMargin(int margin) {
    if (margin == MARGIN_LEFT) return lineNumberSpacer + SPACE_LEFT;
    return 0;
  }

  /**
   * Returns the number of visible rows.
   *
   * @return visible rows
   */
  public int getVisibleRowsCount() {
    return (int) ((getVisibleRect().getHeight() / lineHeight) - 1);
  }

	/**
	 * Returns the pane UI.
	 *
	 * @return pane UI
	 */
	public SCEPaneUI getPaneUI() {
		return ui;
	}

  /**
   * Sets the text in this pane.
   *
   * @param text the text
   */
  public void setText(String text) {
    freezeCaret = true;
    document.setText(text);
    freezeCaret = false;
  }

  /**
   * Returns the text in this pane.
   *
   * @return the text
   */
  public String getText() {
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
    if (ui != null) ui.setTabCompletion(tabCompletion);
  }

  /**
   * Sets the code helper for the source code editor.
   *
   * @param quickHelp quick help
   */
  public void setQuickHelp(QuickHelp quickHelp) {
    this.quickHelp = quickHelp;
    if (ui != null) ui.setCodeHelper(codeHelper);
  }

  /**
   * Registers a code assistant in order to be informed about alt+enter events.
   *
   * @param codeAssistant code assistant which wants to be informed about alt+enter events
   */
  public void addCodeAssistantListener(CodeAssistant codeAssistant) {
    if (codeAssistants.contains(codeAssistant)) return;
    codeAssistants.add(codeAssistant);
  }

  /**
   * Unregisters a code assistant.
   *
   * @param codeAssistant code assistant to unregister
   */
  public void removeCodeAssistantListener(CodeAssistant codeAssistant) {
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

	public void setLineBreakListener(LineBreakListener lineBreakListener) {
		this.lineBreakListener = lineBreakListener;
		if (ui != null) ui.setLineBreakListener(lineBreakListener);
	}

	/**
   * Finds the next splitter in the given row and direction.
   *
   * @param row_nr       row
   * @param column    column
   * @param direction -1 / 1
   * @return splitter column
   */
  public int findSplitterInRow(int row_nr, int column, int direction) {
    SCEDocumentRow row = document.getRowsModel().getRow(row_nr);
    String text = row.toString();

    int position = direction == -1 ? column - 1 : column;
    if (position < 0) return 0;
    if (position >= row.length) return row.length;

	  CT ct = getCharTypeAt(text, position);

	  while (position >= 0 && position < row.length) {
	    if (ct != getCharTypeAt(text, position)) break;
	    position += direction;
	  }
    if (direction == -1) position++;

    return position;
  }

	/**
	 * Returns the character type in a given string at a given position.
	 *
	 * @param text input string
	 * @param position position
	 * @return character type
	 */
	private CT getCharTypeAt(String text, int position) {
		return getCharType(text.charAt(position));
	}

  public static CT getCharType(char c) {
    if (c == ' ') return CT.space;
    if ((splitterChars.contains(c))) return CT.special;
    return CT.letter;
  }

  /**
   * Finds the next splitter in the given direction.
   *
   * @param row       row
   * @param column    column
   * @param direction direction -1 / 1
   * @return splitter position
   */
  public SCEPosition findSplitterPosition(int row, int column, int direction) {
    SCEDocumentRows rowsModel = document.getRowsModel();
    switch (direction) {
      case -1:
        if (column == 0 && row > 0) {
          return new SCEDocumentPosition(row - 1, rowsModel.getRowLength(row - 1));
        } else {
          return new SCEDocumentPosition(row, findSplitterInRow(row, column, -1));
        }
      case 1:
        if (column == rowsModel.getRowLength(row) && row < rowsModel.getRowsCount() - 1) {
          return new SCEDocumentPosition(row + 1, 0);
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
  public void addRowHighlight(SCERowHighlight highlight) {
    synchronized (rowHighlights) {
      rowHighlights.add(highlight);
    }
    repaint();
  }

  /**
   * Removes a row highlight.
   *
   * @param highlight the highlight
   */
  public void removeRowHighlight(SCERowHighlight highlight) {
    synchronized (rowHighlights) {
      rowHighlights.remove(highlight);
    }
    repaint();
  }

  public void removeAllRowHighlights() {
    synchronized (rowHighlights) {
      rowHighlights.clear();
    }
    repaint();
  }

  /**
   * Adds a text highlight.
   *
   * @param highlight the highlight
   */
  public void addTextHighlight(SCETextHighlight highlight) {
    synchronized (textHighlights) {
      textHighlights.add(highlight);
    }
    repaint();
  }

  /**
   * Removes a text highlight.
   *
   * @param highlight the highlight
   */
  public void removeTextHighlight(SCETextHighlight highlight) {
    synchronized (textHighlights) {
      textHighlights.remove(highlight);
    }
    repaint();
  }

  public void removeAllTextHighlights() {
    synchronized (textHighlights) {
      textHighlights.clear();
    }
    removeAll();
  }

  public ArrayList<SCETextHighlight> getTextHighlights() {
    return textHighlights;
  }

  /**
   * Transparent background for the text.
   */
  public boolean isTransparentTextBackground() {
    return transparentTextBackground;
  }

  public void setTransparentTextBackground(boolean transparentTextBackground) {
    this.transparentTextBackground = transparentTextBackground;
  }

  // sce.component.SCEDocumentListener methods

  public void documentChanged(SCEDocument sender, SCEDocumentEvent event) {
    SCEDocumentPosition start = event.getRangeStart();
    SCEDocumentPosition end = event.getRangeEnd();

    // update the component size
	  updateComponentSize();

	  // update the caret position
    if (event.isInsert() && !freezeCaret) {
      caret.moveTo(end.getRow(), end.getColumn(), false);
	    repaint();
    }
    if (event.isRemove() && !freezeCaret) {
      caret.moveTo(start.getRow(), start.getColumn(), false);
	    repaint();
    }
  }

	private void updateComponentSize() {
		if (getParent() != null) {
		  SCEDocumentRow[] rows = document.getRowsModel().getRows();
		  Dimension dimension = new Dimension();
		  for (SCEDocumentRow row : rows) {
		    dimension.width = Math.max(row.length * characterWidth + lineNumberSpacer + 30, dimension.width);
		  }
		  dimension.height = rows.length * lineHeight + 30;

      dimension.width += addToPreferredSize.width;
      dimension.height += addToPreferredSize.height;

		  preferredSize = dimension;
		  getParent().doLayout();
		}
	}

	// sce.component.SCECaretListener methods

  public void caretMoved(int row, int column, int lastRow, int lastColumn) {
    if (row != lastRow || document.hasSelection()) {
      Point ul = modelToView(row, 0);
      repaint(0, ul.y, getWidth(), lineHeight);
      ul = modelToView(lastRow, 0);
      repaint(0, ul.y, getWidth(), lineHeight);
    }

    // scroll to caret
    Point caretPos = modelToView(row, column);
    scrollRectToVisible(new Rectangle(Math.max(0, caretPos.x - 30), Math.max(0, caretPos.y - 20), 60, lineHeight + 40));
  }

  // FocusListener methods

  public void focusGained(FocusEvent e) {
  }

  public void focusLost(FocusEvent e) {
    if (!focusBack) return;

    synchronized (this) {
      try {
        wait(100);
      } catch (InterruptedException ignored) {
      }
    }

    frameToFront(this);
    requestFocus();

    focusBack = false;
  }

  private void frameToFront(Container component) {
    if (component instanceof JFrame) {
      ((JFrame) component).toFront();
      return;
    }
    frameToFront(component.getParent());
  }

  public void getFocusBack() {
    focusBack = true;
  }

  public int getColumnsPerRow() {
    return columnsPerRow;
  }

  public void setColumnsPerRow(int columnsPerRow) {
    this.columnsPerRow = columnsPerRow;
	}

	public void dispose() {
		ui.uninstallUI(this);
	}
}
