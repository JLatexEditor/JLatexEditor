package sce.component;

import sce.codehelper.CodeHelper;
import sce.codehelper.CodeHelperPane;
import sce.quickhelp.QuickHelp;
import sce.quickhelp.QuickHelpPane;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;

/**
 * SourceCodeEditor UI.
 *
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */
public class SCEPaneUI implements KeyListener, MouseListener, MouseMotionListener{
  // properties
  private SCEPane pane = null;
  private SCEDocument document = null;
  private SCECaret caret = null;

  // quick help
  QuickHelpPane quickHelpPane = null;

  // auto completion
  CodeHelperPane codeHelperPane = null;

  // last mouse click
  long lastMouseClick = 0;

  // some characters
  private String allowedChars = "abcdefghijklmnopqrstuvwxyz" +
          "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
          "äöüÄÖÜß" +
          "1234567890" +
          "<>|+*~'#,;.:-_!\"§$%&/()=?`´\\{[]} ";

	public SCEPaneUI(SCEPane pane){
    this.pane = pane;
    this.document = pane.getDocument();
    this.caret = pane.getCaret();

    pane.setLayout(null);

    // KeyListener
    pane.addKeyListener(this);
    pane.setFocusable(true);
    pane.setFocusCycleRoot(false);
    pane.setFocusTraversalKeysEnabled(false);

    // MouseListener
    pane.addMouseListener(this);
    pane.addMouseMotionListener(this);
  }

  /**
   * Removes the selected text from the document.
   */
  public void removeSelection(){
    SCEDocumentPosition start = document.getSelectionStart();
    SCEDocumentPosition end = document.getSelectionEnd();

    document.remove(start.getRow(), start.getColumn(), end.getRow(), end.getColumn());
    caret.removeSelectionMark();
  }

	/**
	 * Sets the code helper for the source code editor.
	 *
	 * @param codeHelper code helper
	 */
	public void setCodeHelper(CodeHelper codeHelper) {
		// remove old code helper
		if (codeHelperPane != null) codeHelperPane.destroy();

		// add new code helper
		if (codeHelper != null) {
			// UI key listener shall be at the end of the processor queue
			pane.removeKeyListener(this);

			// create auto completion list
			codeHelperPane = new CodeHelperPane(pane);
			codeHelperPane.setVisible(false);
			codeHelperPane.setCodeHelper(codeHelper);

			// UI key listener shall be at the end of the processor queue
			pane.addKeyListener(this);
		}
	}

	/**
	 * Sets the code helper for the source code editor.
	 *
	 * @param quickHelp quick help
	 */
	public void setQuickHelp(QuickHelp quickHelp) {
		// remove old quick help
		if (quickHelpPane != null) quickHelpPane.destroy();

		// add new quick help
		if (quickHelp != null) {
			quickHelpPane = new QuickHelpPane(pane);
			quickHelpPane.setVisible(false);
			quickHelpPane.setQuickHelp(quickHelp);
		}
	}

  /**
   * Scrolls the visible rectangle.
   *
   * @param rows the number of rows to scroll
   * @return visible rectangle
   */
  public Rectangle scrollVisibleRect(int rows){
    Rectangle visibleRect = pane.getVisibleRect();
    visibleRect.translate(0, rows * pane.getLineHeight());
    pane.scrollRectToVisible(visibleRect);

    return visibleRect;
  }

  // KeyListener Methods
  public void keyTyped(KeyEvent e){
    if(e.isConsumed()) return;

    if(allowedChars.indexOf(e.getKeyChar()) == -1) return;
    if(e.isControlDown() || e.isAltDown()) return;

    if(document.hasSelection()) removeSelection();
    document.insert(e.getKeyChar() + "", caret.getRow(), caret.getColumn());
  }

  public void keyPressed(KeyEvent e){
    if(e.isConsumed()) return;

    // is control down?
    if(e.isControlDown()){
      keyControlDown(e);
	    if (!isModifierKey(e.getKeyCode())) return;
    }

    // undo the last edit
    if(e.isAltDown() && e.getKeyCode() == KeyEvent.VK_BACK_SPACE){
      pane.getUndoManager().undo();
      return;
    }

    // delete and back space
    if(e.getKeyCode() == KeyEvent.VK_BACK_SPACE){
      if(caret.getRow() == 0 && caret.getColumn() == 0) return;
      if(!document.hasSelection()) caret.move(SCECaret.DIRECTION_LEFT);
      e.setKeyCode(KeyEvent.VK_DELETE);
      e.consume();
    }
    if(e.getKeyCode() == KeyEvent.VK_DELETE){
      // selection to remove?
      if(document.hasSelection()){
        removeSelection();
        return;
      }

      // is the caret at the last column of the line?
      if(caret.getColumn() == document.getRowLength(caret.getRow())){
        document.remove(caret.getRow(), caret.getColumn(), caret.getRow() + 1, 0);
      } else{
        document.remove(caret.getRow(), caret.getColumn(), caret.getRow(), caret.getColumn() + 1);
      }
      e.consume();
      return;
    }

    // caret movement
    if(e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN){
      int direction = e.getKeyCode() == KeyEvent.VK_UP ? SCECaret.DIRECTION_UP : SCECaret.DIRECTION_DOWN;
      caret.move(direction);
      e.consume();
    }
    if(e.getKeyCode() == KeyEvent.VK_LEFT){
      caret.move(SCECaret.DIRECTION_LEFT);
      e.consume();
    }
    if(e.getKeyCode() == KeyEvent.VK_RIGHT){
      caret.move(SCECaret.DIRECTION_RIGHT);
      e.consume();
    }
    if(e.getKeyCode() == KeyEvent.VK_PAGE_UP || e.getKeyCode() == KeyEvent.VK_PAGE_DOWN){
      int jump = e.getKeyCode() == KeyEvent.VK_PAGE_UP ? -pane.getVisibleRowsCount() : pane.getVisibleRowsCount();
      scrollVisibleRect(jump);
      caret.moveTo(caret.getRow() + jump, caret.getColumn());
      e.consume();
    }
    if(e.getKeyCode() == KeyEvent.VK_HOME){
      caret.moveTo(caret.getRow(), 0);
      e.consume();
    }
    if(e.getKeyCode() == KeyEvent.VK_END){
      caret.moveTo(caret.getRow(), document.getRowLength(caret.getRow()));
      e.consume();
    }

    // enter
    if(e.getKeyCode() == KeyEvent.VK_ENTER){
      if(document.hasSelection()) removeSelection();
      document.insert("\n", caret.getRow(), caret.getColumn());
      e.consume();
    }

    // tab
    if(e.getKeyCode() == KeyEvent.VK_TAB){
      if(!document.hasSelection()){
	      if(!e.isShiftDown()){
	        document.insert("  ", caret.getRow(), caret.getColumn());
	      } else {
		      int row = caret.getRow();
		      int col = caret.getColumn();
		      removeIndentation(caret.getRow());
		      caret.moveTo(row, Math.max(col-2, 0));
		      caret.removeSelectionMark();
		      pane.repaint();
	      }
	      e.consume();
	      return;
      } else {
	      SCEDocumentPosition startSel = document.getSelectionStart();
	      SCEDocumentPosition endSel = document.getSelectionEnd();

        int startRow = startSel.getRow();
        int endRow = endSel.getRow() - (endSel.getColumn() == 0 ? 1 : 0);
        for(int row = startRow; row <= endRow; row++){
          if(e.isShiftDown()){
	          removeIndentation(row);
          } else{
            document.insert("  ", row, 0);
          }
        }

	      int endCol = endSel.getColumn();
				if (!e.isShiftDown()) {
					endCol = endCol == 0 ? endCol : endCol+2;
				} else {
					endCol = Math.max(endCol, 0);
				}
	      endSel = new SCEDocumentPosition(endSel.getRow(), endCol);
	      caret.moveTo(endSel.getRow(), endCol);
	      document.setSelectionRange(startSel, endSel);
      }
      e.consume();
	    return;
    }

    // selection
    if(e.isShiftDown()){
      if(caret.getSelectionMark() == null) caret.setSelectionMark();
    } else {
      if(caret.getSelectionMark() != null && e.isActionKey()) clearSelection();
    }
  }

  private void clearSelection() {
    caret.removeSelectionMark();
    pane.repaint();
  }

	private void removeIndentation(int row) {
		String rowString = document.getRow(row);
		if(rowString.startsWith("  ")){
		  document.remove(row, 0, row, 2);
		} else if(rowString.startsWith(" ")){
		  document.remove(row, 0, row, 1);
		}
	}

	private boolean isModifierKey(int keyCode) {
		return KeyEvent.VK_SHIFT <= keyCode && keyCode <= KeyEvent.VK_ALT;
	}

	public void keyReleased(KeyEvent e){
  }

  private void keyControlDown(KeyEvent e){
    // caret movement
    if(e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN){
      int jump = e.getKeyCode() == KeyEvent.VK_UP ? -1 : 1;
      Rectangle visibleRect = scrollVisibleRect(jump);

      int minRow = visibleRect.y / pane.getLineHeight();
      int maxRow = (visibleRect.y + visibleRect.height) / pane.getLineHeight();

      if(caret.getRow() < minRow + 2) caret.move(SCECaret.DIRECTION_DOWN);
      if(caret.getRow() > maxRow - 2) caret.move(SCECaret.DIRECTION_UP);
      e.consume();
    }
    if(e.getKeyCode() == KeyEvent.VK_LEFT){
      caret.moveTo(pane.findSplitterPosition(caret.getRow(), caret.getColumn(), -1));
      e.consume();
      if(!e.isShiftDown()) clearSelection();
    }
    if(e.getKeyCode() == KeyEvent.VK_RIGHT){
      caret.moveTo(pane.findSplitterPosition(caret.getRow(), caret.getColumn(), 1));
      e.consume();
      if(!e.isShiftDown()) clearSelection();
    }
    if(e.getKeyCode() == KeyEvent.VK_HOME){
      caret.moveTo(0, 0);
      e.consume();
      if(!e.isShiftDown()) clearSelection();
    }
    if(e.getKeyCode() == KeyEvent.VK_END){
      int row = document.getRowsCount() - 1;
      int column = document.getRowLength(row);
      caret.moveTo(row, column);
      e.consume();
      if(!e.isShiftDown()) clearSelection();
    }

	  // control+(shift)+Z -> undo/redo the last edit
	  if(e.getKeyCode() == KeyEvent.VK_Z){
		  if (e.isShiftDown()) {
				pane.getUndoManager().redo();
		  } else {
			  pane.getUndoManager().undo();
		  }
		  e.consume();
			return;
	  }

    // control+Y
    if(e.getKeyCode() == KeyEvent.VK_Y){
      if(caret.getRow() < document.getRowsCount() - 1){
        document.remove(caret.getRow(), 0, caret.getRow() + 1, 0);
      } else if(caret.getRow() > 0){
        document.remove(caret.getRow() - 1, document.getRowLength(caret.getRow() - 1), caret.getRow(), document.getRowLength(caret.getRow()));
        caret.moveTo(caret.getRow() - 1, caret.getColumn());
      } else{
        document.remove(0, 0, 0, document.getRowLength(0));
        caret.moveTo(0, 0);
      }
      e.consume();
    }

	  // control+U
	  if(e.getKeyCode() == KeyEvent.VK_U){
			document.remove(caret.getRow(), 0, caret.getRow(), caret.getColumn());
	    e.consume();
	  }
	  // control+K
	  if(e.getKeyCode() == KeyEvent.VK_K){
			document.remove(caret.getRow(), caret.getColumn(), caret.getRow(), document.getRowLength(caret.getRow()));
	    e.consume();
	  }
	  // control+backspace
	  if(e.getKeyCode() == KeyEvent.VK_BACK_SPACE){
			document.remove(pane.findSplitterPosition(caret.getRow(), caret.getColumn(), -1), caret);
	    e.consume();
	  }
	  // control+delete
	  if(e.getKeyCode() == KeyEvent.VK_DELETE){
			document.remove(caret, pane.findSplitterPosition(caret.getRow(), caret.getColumn(), 1));
	    e.consume();
	  }

    // insert text
    if(e.getKeyCode() == KeyEvent.VK_V && e.isControlDown()){
      Transferable content = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
      try{
        String string = (String) content.getTransferData(DataFlavor.stringFlavor);
        if(document.hasSelection()) removeSelection();
        document.insert(string, caret.getRow(), caret.getColumn());
      } catch(Exception ignored){
      }
      e.consume();
    }
    // copy text
    if(e.getKeyCode() == KeyEvent.VK_C && e.isControlDown() && document.hasSelection()){
      StringSelection s = new StringSelection(document.getSelectedText());
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(s, null);
      e.consume();
    }
    // cut text
    if(e.getKeyCode() == KeyEvent.VK_X && e.isControlDown() && document.hasSelection()){
      StringSelection s = new StringSelection(document.getSelectedText());
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(s, null);
      removeSelection();
      e.consume();
    }
  }

  // MouseListener methods
  public void mouseClicked(MouseEvent e){
  }

  public void mousePressed(MouseEvent e){
    SCEDocumentPosition position = pane.viewToModel(e.getX(), e.getY());

    // hide quick help
    if(quickHelpPane != null && quickHelpPane.isVisible()){
      quickHelpPane.setVisible(false);
    }

    // request focus
    if(!pane.hasFocus()) pane.requestFocus();

    // mouse button 1
    if((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0){
      if(caret.getRow() != position.getRow() || caret.getColumn() != position.getColumn()){
        caret.moveTo(position.getRow(), position.getColumn());
      } else{
        // select the word or line
        int left = pane.findSplitterInRow(caret.getRow(), caret.getColumn(), -1);
        int right = pane.findSplitterInRow(caret.getRow(), caret.getColumn(), 1);
        SCEDocumentPosition leftPosition = new SCEDocumentPosition(caret.getRow(), left);
        SCEDocumentPosition rightPosition = new SCEDocumentPosition(caret.getRow(), right);

        boolean select_line = document.hasSelection() && left == document.getSelectionStart().getColumn() && right == document.getSelectionEnd().getColumn();
        document.setSelectionRange(leftPosition, rightPosition);

        if(select_line && lastMouseClick + 500 > System.currentTimeMillis()){
          SCEDocumentPosition startPosition = new SCEDocumentPosition(caret.getRow(), 0);
          SCEDocumentPosition endPosition = new SCEDocumentPosition(caret.getRow() + 1, 0);
          if(caret.getRow() >= document.getRowsCount() - 1){
            endPosition = new SCEDocumentPosition(caret.getRow(), document.getRowLength(caret.getRow()));
          }
          document.setSelectionRange(startPosition, endPosition);
        }
        pane.repaint();

        lastMouseClick = System.currentTimeMillis();
        return;
      }

      if(!e.isShiftDown() && document.hasSelection()){
        caret.removeSelectionMark();
        pane.repaint();
      }
    }
    if((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0){
      pane.addTextHighlight(new SCETextHighlight(pane, position, position, Color.red));
    }

    lastMouseClick = System.currentTimeMillis();
  }

  public void mouseReleased(MouseEvent e){
  }

  public void mouseEntered(MouseEvent e){
  }

  public void mouseExited(MouseEvent e){
  }

  // MouseMotionListener
  public void mouseDragged(MouseEvent e){
    if((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0){
      if(caret.getSelectionMark() == null) caret.setSelectionMark();

      SCEDocumentPosition position = pane.viewToModel(e.getX(), e.getY());
      caret.moveTo(position.getRow(), position.getColumn());
    }
  }

  public void mouseMoved(MouseEvent e){
  }
}