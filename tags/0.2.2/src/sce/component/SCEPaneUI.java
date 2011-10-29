package sce.component;

import sce.codehelper.CodeHelper;
import sce.codehelper.CodeHelperPane;
import sce.codehelper.LineBreakListener;
import sce.quickhelp.QuickHelp;
import sce.quickhelp.QuickHelpPane;
import sun.swing.DefaultLookup;
import sun.swing.UIAction;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * SourceCodeEditor UI.
 *
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */
public class SCEPaneUI extends ComponentUI implements KeyListener, MouseListener, MouseMotionListener {
	// properties
  private SCEPane pane = null;
  private SCEDocument document = null;
  private SCECaret caret = null;
	private Clipboard selectionClipboard = Toolkit.getDefaultToolkit().getSystemSelection();

  // quick help
  QuickHelpPane quickHelpPane = null;

  // auto completion
  CodeHelperPane codeHelperPane = null;

	// line break listener
	LineBreakListener lineBreakListener = null;

  // last mouse click
  long lastMouseClick = 0;

  // some characters
  private String allowedChars = "abcdefghijklmnopqrstuvwxyz" +
          "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
          "äöüÄÖÜß" +
          "1234567890" +
          "^<>|+*~'#,;.:-_!\"§$%&/()=?`´\\{[]}@ ";

  public SCEPaneUI(SCEPane pane) {
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

	  installUI(pane);
  }

  /**
   * Removes the selected text from the document.
   */
  public void removeSelection() {
	  if (document.removeSelection()) {
		  caret.setSelectionMark();
	  }
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
   * Sets the tab completion for the source code editor.
   *
   * @param tabCompletion tab completion
   */
  public void setTabCompletion(CodeHelper tabCompletion) {
    if (codeHelperPane == null) return;
    codeHelperPane.setTabCompletion(tabCompletion);
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
	 * Sets the listener for line breaks.
	 *
	 * @param lineBreakListener listener for line breaks
	 */
	public void setLineBreakListener(LineBreakListener lineBreakListener) {
		this.lineBreakListener = lineBreakListener;
	}

	/**
   * Scrolls the visible rectangle.
   *
   * @param rows the number of rows to scroll
   * @return visible rectangle
   */
  public Rectangle scrollVisibleRect(int rows) {
    Rectangle visibleRect = pane.getVisibleRect();
    visibleRect.translate(0, rows * pane.getLineHeight());
    pane.scrollRectToVisible(visibleRect);

    return visibleRect;
  }

  // KeyListener Methods

  public void keyTyped(KeyEvent e) {
    if (e.isConsumed()) return;

    //if (allowedChars.indexOf(e.getKeyChar()) == -1) return;
    if (e.isControlDown() || e.isAltDown() || e.isMetaDown()) return;
	  if (Character.isISOControl(e.getKeyChar())) return;

    if (document.hasSelection()) removeSelection();
    document.insert(e.getKeyChar() + "", caret.getRow(), caret.getColumn());
  }

  public void keyPressed(KeyEvent e) {
    if (e.isConsumed()) return;

	  int keyCode = e.getKeyCode();

    // ignore meta key
    if (e.isMetaDown()) return;

	  // is control down?
    if (e.isControlDown() && !e.isAltDown()) {
      if (!isModifierKey(keyCode)) return;
    }

    // is alt down?
    if (e.isAltDown()) {
      keyAltDown(e);
      return;
    }

    // delete and back space
    if (keyCode == KeyEvent.VK_BACK_SPACE) {
      if (document.hasSelection()) {
	      removeSelection();
      } else {
	      if (caret.getRow() == 0 && caret.getColumn() == 0) return;
	      caret.move(SCECaret.DIRECTION_LEFT, false);
	      keyCode = KeyEvent.VK_DELETE;
      }
      e.consume();
    }
    if (keyCode == KeyEvent.VK_DELETE) {
      // selection to remove?
      if (document.hasSelection()) {
        removeSelection();
	      e.consume();
        return;
      }

      // is the caret at the last column of the line?
      if (caret.getColumn() == document.getRowsModel().getRowLength(caret.getRow())) {
        document.remove(caret.getRow(), caret.getColumn(), caret.getRow() + 1, 0);
      } else {
        document.remove(caret.getRow(), caret.getColumn(), caret.getRow(), caret.getColumn() + 1);
      }
      e.consume();
      return;
    }

	  // navigation keys
	  if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN ||
		    keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT ||
		    keyCode == KeyEvent.VK_PAGE_UP || keyCode == KeyEvent.VK_PAGE_DOWN ||
		    keyCode == KeyEvent.VK_HOME || keyCode == KeyEvent.VK_END) {

			// remove selection if shift not pressed
		  boolean keepSelection = e.isShiftDown();

			// caret movement
			if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN) {
				int direction = keyCode == KeyEvent.VK_UP ? SCECaret.DIRECTION_UP : SCECaret.DIRECTION_DOWN;
				caret.move(direction, keepSelection);
				e.consume();
			}
			if (keyCode == KeyEvent.VK_LEFT) {
				caret.move(SCECaret.DIRECTION_LEFT, keepSelection);
				e.consume();
			}
			if (keyCode == KeyEvent.VK_RIGHT) {
				caret.move(SCECaret.DIRECTION_RIGHT, keepSelection);
				e.consume();
			}
			if (keyCode == KeyEvent.VK_PAGE_UP || keyCode == KeyEvent.VK_PAGE_DOWN) {
				int jump = keyCode == KeyEvent.VK_PAGE_UP ? -pane.getVisibleRowsCount() : pane.getVisibleRowsCount();
				scrollVisibleRect(jump);
				caret.moveTo(caret.getRow() + jump, caret.getColumn(), keepSelection);
				e.consume();
			}
			if (keyCode == KeyEvent.VK_HOME) {
				if (caret.getColumn() != 0) {
					caret.moveTo(caret.getRow(), 0, keepSelection);
				} else {
					// move caret to the first non-space character
					char[] chars = document.getRowsModel().getRow(caret.getRow()).toCharArray();
					for (int i = 0; i < chars.length; i++) {
						if (chars[i] != ' ' && chars[i] != '\t') {
							caret.moveTo(caret.getRow(), i, keepSelection);
							break;
						}
					}
				}
				e.consume();
			}
			if (keyCode == KeyEvent.VK_END) {
				int rowLength = document.getRowsModel().getRowLength(caret.getRow());
				if (caret.getColumn() != rowLength) {
					caret.moveTo(caret.getRow(), rowLength, keepSelection);
				} else {
					// move caret to first non-space character
					char[] chars = document.getRowsModel().getRow(caret.getRow()).toCharArray();
					for (int i = rowLength; i > 0; i--) {
						if (chars[i - 1] != ' ' && chars[i - 1] != '\t') {
							caret.moveTo(caret.getRow(), i, keepSelection);
							break;
						}
					}
				}
				e.consume();
			}
	  }

    // enter
    if (keyCode == KeyEvent.VK_ENTER) {
      if (document.hasSelection()) removeSelection();
      int row = caret.getRow();
      int column = caret.getColumn();

      String line = document.getRowsModel().getRowAsString(row, 0, column);
      document.insert("\n", row, column);
	    // take over indentation of last line
	    int indentCount = 0;
	    while (indentCount < line.length() && line.charAt(indentCount) == ' ') indentCount++;
			document.insert(line.substring(0, indentCount), row + 1, 0);

	    if (lineBreakListener != null) {
		    lineBreakListener.linedWrapped(pane);
	    }
      e.consume();
    }

    // tab
    if (keyCode == KeyEvent.VK_TAB) {
      if (!document.hasSelection()) {
        if (!e.isShiftDown()) {
          document.insert("  ", caret.getRow(), caret.getColumn());
        } else {
          int row = caret.getRow();
          int col = caret.getColumn();
          removeIndentation(caret.getRow());
          caret.moveTo(row, Math.max(col - 2, 0), false);
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
        for (int row = startRow; row <= endRow; row++) {
          if (e.isShiftDown()) {
            removeIndentation(row);
          } else {
            document.insert("  ", row, 0);
          }
        }

        int endCol = endSel.getColumn();
        if (!e.isShiftDown()) {
          endCol = endCol == 0 ? endCol : endCol + 2;
        } else {
          endCol = Math.max(endCol, 0);
        }
        endSel = new SCEDocumentPosition(endSel.getRow(), endCol);
        caret.moveTo(endSel.getRow(), endCol, false);
        document.setSelectionRange(startSel, endSel);
      }
      e.consume();
      return;
    }

	  // TODO: remove
	  /*
    // selection
    if (e.isShiftDown()) {
      if (caret.getSelectionMark() == null) caret.setSelectionMark();
    } else {
      if (caret.getSelectionMark() != null && e.isActionKey()) clearSelection();
    }
    */
  }

  private void keyAltDown(KeyEvent e) {
    // alt+enter -> code assistant
    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
      pane.callCodeAssistants();
      e.consume();
    }

    // alt+backspace -> undo the last edit
    if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
      pane.getUndoManager().undo(false);
      e.consume();
    }
  }

  private void clearSelection() {
    caret.removeSelectionMark();
	  caret.setSelectionMark();
    //pane.repaint();
  }

  private void removeIndentation(int row) {
    String rowString = document.getRowsModel().getRowAsString(row);
    if (rowString.startsWith("  ")) {
      document.remove(row, 0, row, 2);
    } else if (rowString.startsWith(" ")) {
      document.remove(row, 0, row, 1);
    }
  }

  private boolean isModifierKey(int keyCode) {
    return KeyEvent.VK_SHIFT <= keyCode && keyCode <= KeyEvent.VK_ALT;
  }

  public void keyReleased(KeyEvent e) {
  }

// UI Installation/De-installation

	public void installUI(JComponent c) {
		this.pane = (SCEPane) c;

		installInputMap(pane);
		installKeyboardActions();
	}

	public void uninstallUI(JComponent c) {
		uninstallKeyboardActions();
		uninstallInputMap(pane);

		this.pane = null;
	}

	protected void installKeyboardActions() {
		//InputMap km = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		InputMap km = new ComponentInputMap(pane);
		km.put(KeyStroke.getKeyStroke("control shift L"), Actions.JUMP_LEFT);

		SwingUtilities.replaceUIInputMap(pane, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, km);
		km = getInputMap(JComponent.WHEN_FOCUSED);
		SwingUtilities.replaceUIInputMap(pane, JComponent.WHEN_FOCUSED, km);

		pane.setActionMap(Actions.getActionMap());

		//LazyActionMap.installLazyActionMap(pane, BasicTabbedPaneUI.class, "SCEPane.actionMap");
		//updateMnemonics();
	}

	protected InputMap getInputMap(int condition) {
		if (condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) {
			return (InputMap) DefaultLookup.get(pane, this, "SCEPane.ancestorInputMap");
		}
		else if (condition == JComponent.WHEN_FOCUSED) {
			return (InputMap)DefaultLookup.get(pane, this, "SCEPane.focusInputMap");
		}
		return null;
	}

	protected void uninstallKeyboardActions() {
		SwingUtilities.replaceUIActionMap(pane, null);
		SwingUtilities.replaceUIInputMap(pane, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, null);
		SwingUtilities.replaceUIInputMap(pane, JComponent.WHEN_FOCUSED, null);
		SwingUtilities.replaceUIInputMap(pane, JComponent.WHEN_IN_FOCUSED_WINDOW, null);
	}

	public QuickHelpPane getQuickHelpPane() {
		return quickHelpPane;
	}

	public static class Actions extends UIAction {
		public static final String MOVE_VIEW_UP             = "move view up";
		public static final String MOVE_VIEW_UP_SHIFT       = "move view up shift";
		public static final String MOVE_VIEW_DOWN           = "move view down";
		public static final String MOVE_VIEW_DOWN_SHIFT     = "move view down shift";
		public static final String JUMP_RIGHT               = "jump right";
		public static final String JUMP_RIGHT_SHIFT         = "jump right shift";
		public static final String JUMP_LEFT                = "jump left";
		public static final String JUMP_LEFT_SHIFT          = "jump left shift";
		public static final String JUMP_TO_FRONT            = "jump to front";
		public static final String JUMP_TO_FRONT_SHIFT      = "jump to front shift";
		public static final String JUMP_TO_END              = "jump to end";
		public static final String JUMP_TO_END_SHIFT        = "jump to end shift";
		public static final String REMOVE_LINE              = "remove line";
		public static final String REMOVE_LINE_BEFORE_CARET = "remove line before caret";
		public static final String REMOVE_LINE_BEHIND_CARET = "remove line behind caret";
		public static final String REMOVE_WORD_BEFORE_CARET = "remove word before caret";
		public static final String REMOVE_WORD_BEHIND_CARET = "remove word behind caret";
		public static final String COMPLETE                 = "complete";

		Actions(String key) {
			super(key);
		}

		public void actionPerformed(ActionEvent e) {
			String key = getName();
			SCEPane pane = (SCEPane) e.getSource();
			SCEPaneUI ui = pane.getPaneUI();

			if (ui == null) return;

			if (key.startsWith(MOVE_VIEW_UP)) {
				ui.moveViewVertically(-1, key.endsWith(" shift"));
			} else
			if (key.startsWith(MOVE_VIEW_DOWN)) {
				ui.moveViewVertically(1, key.endsWith(" shift"));
			} else
			if (key.startsWith(JUMP_LEFT)) {
				ui.jumpSideways(-1, key.endsWith(" shift"));
			} else
			if (key.startsWith(JUMP_RIGHT)) {
				ui.jumpSideways(1, key.endsWith(" shift"));
			} else
			if (key.startsWith(JUMP_TO_FRONT)) {
				ui.jumpToFront(key.endsWith(" shift"));
			} else
			if (key.startsWith(JUMP_TO_END)) {
				ui.jumpToEnd(key.endsWith(" shift"));
			} else
			if (key.equals(REMOVE_LINE)) {
				ui.removeLine();
			} else
			if (key.equals(REMOVE_LINE_BEFORE_CARET)) {
				ui.removeLineBeforeCaret();
			} else
			if (key.equals(REMOVE_LINE_BEHIND_CARET)) {
				ui.removeLineBehindCaret();
			} else
			if (key.equals(REMOVE_WORD_BEFORE_CARET)) {
				ui.removeWordBeforeCaret();
			} else
			if (key.equals(REMOVE_WORD_BEHIND_CARET)) {
				ui.removeWordBehindCaret();
			} else
			if (key.equals(COMPLETE)) {
				ui.complete();
			}
		}

		public static ActionMap getActionMap() {
			ActionMap am = new ActionMap();
			add(am, MOVE_VIEW_UP, MOVE_VIEW_UP_SHIFT, MOVE_VIEW_DOWN, MOVE_VIEW_DOWN_SHIFT,
				JUMP_LEFT, JUMP_LEFT_SHIFT, JUMP_RIGHT, JUMP_RIGHT_SHIFT, JUMP_TO_FRONT, JUMP_TO_FRONT_SHIFT, JUMP_TO_END, JUMP_TO_END_SHIFT,
				REMOVE_LINE, REMOVE_LINE_BEFORE_CARET, REMOVE_LINE_BEHIND_CARET, REMOVE_WORD_BEFORE_CARET, REMOVE_WORD_BEHIND_CARET,
				COMPLETE);
			return am;
		}

		public static void add(ActionMap am, String... commands) {
			for (String command : commands) {
				am.put(command, new Actions(command));
			}
		}
	}

	public void moveViewVertically(int direction, boolean isShiftDown) {
		Rectangle visibleRect = scrollVisibleRect(direction);

		int minRow = visibleRect.y / pane.getLineHeight();
		int maxRow = (visibleRect.y + visibleRect.height) / pane.getLineHeight();

		if (caret.getRow() < minRow + 2) caret.move(SCECaret.DIRECTION_DOWN, isShiftDown);
		if (caret.getRow() > maxRow - 2) caret.move(SCECaret.DIRECTION_UP, isShiftDown);
	}

	/**
	 * Jumps one word to the left or right.
	 * Use direction -1 for left and 1 for right.
	 *
	 * @param direction -1 for left and 1 for right
	 * @param isShiftDown is shift pressed
	 */
	public void jumpSideways(int direction, boolean isShiftDown) {
		caret.moveTo(pane.findSplitterPosition(caret.getRow(), caret.getColumn(), direction), isShiftDown);
	}

	public void jumpToFront(boolean isShiftDown) {
		caret.moveTo(0, 0, isShiftDown);
	}

	public void jumpToEnd(boolean isShiftDown) {
		SCEDocumentRows rowsModel = document.getRowsModel();

		int row = rowsModel.getRowsCount() - 1;
		int column = rowsModel.getRowLength(row);
		caret.moveTo(row, column, isShiftDown);
	}

	public void removeLine() {
		SCEDocumentRows rowsModel = document.getRowsModel();

		if (caret.getRow() < rowsModel.getRowsCount() - 1) {
		  document.remove(caret.getRow(), 0, caret.getRow() + 1, 0);
		} else if (caret.getRow() > 0) {
		  document.remove(caret.getRow() - 1, rowsModel.getRowLength(caret.getRow() - 1), caret.getRow(), rowsModel.getRowLength(caret.getRow()));
		  caret.moveTo(caret.getRow() - 1, caret.getColumn(), false);
		} else {
		  document.remove(0, 0, 0, rowsModel.getRowLength(0));
		  caret.moveTo(0, 0, false);
		}
	}

	public void removeLineBeforeCaret() {
		document.remove(caret.getRow(), 0, caret.getRow(), caret.getColumn());
	}

	public void removeLineBehindCaret() {
		document.remove(caret.getRow(), caret.getColumn(), caret.getRow(), document.getRowsModel().getRowLength(caret.getRow()));
	}

	public void removeWordBeforeCaret() {
		document.remove(pane.findSplitterPosition(caret.getRow(), caret.getColumn(), -1), caret);
		clearSelection();
	}

	public void removeWordBehindCaret() {
		document.remove(caret, pane.findSplitterPosition(caret.getRow(), caret.getColumn(), 1));
		clearSelection();
	}

	public void complete() {
		if (codeHelperPane != null) {
		  codeHelperPane.callCodeHelperWithCompletion();
		}
	}

  // MouseListener methods

  public void mouseClicked(MouseEvent e) {
  }

  public void mousePressed(MouseEvent e) {
    SCEDocumentPosition position = pane.viewToModel(e.getX(), e.getY());
    SCEDocumentRows rowsModel = document.getRowsModel();

    // hide quick help
    if (quickHelpPane != null && quickHelpPane.isVisible()) {
      quickHelpPane.setVisible(false);
    }

    // request focus
    if (!pane.hasFocus()) pane.requestFocus();

    // mouse button 1
    if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
      if (caret.getRow() != position.getRow() || caret.getColumn() != position.getColumn()) {
        caret.moveTo(position.getRow(), position.getColumn(), e.isShiftDown());
      } else {
        // select the word or line
        int left = pane.findSplitterInRow(caret.getRow(), caret.getColumn(), -1);
        int right = pane.findSplitterInRow(caret.getRow(), caret.getColumn(), 1);
        SCEDocumentPosition leftPosition = document.createDocumentPosition(caret.getRow(), left);
        SCEDocumentPosition rightPosition = document.createDocumentPosition(caret.getRow(), right);

        boolean select_line = document.hasSelection() && left == document.getSelectionStart().getColumn() && right == document.getSelectionEnd().getColumn();

        if (select_line && lastMouseClick + 500 > System.currentTimeMillis()) {
          SCEDocumentPosition startPosition = document.createDocumentPosition(caret.getRow(), 0);
          SCEDocumentPosition endPosition = document.createDocumentPosition(caret.getRow() + 1, 0);
          if (caret.getRow() >= rowsModel.getRowsCount() - 1) {
            endPosition = document.createDocumentPosition(caret.getRow(), rowsModel.getRowLength(caret.getRow()));
          }
          document.setSelectionRange(startPosition, endPosition);
        } else {
          document.setSelectionRange(leftPosition, rightPosition);
        }
        pane.repaint();

        lastMouseClick = System.currentTimeMillis();
        return;
      }
    } else

    // mouse button 2
	  if ((e.getModifiers() & MouseEvent.BUTTON2_MASK) != 0) {
		  // insert content of select clipboard (middle mouse button under X11)
		  try {
			  caret.moveTo(position.getRow(), position.getColumn(), false);
			  Transferable transfer = selectionClipboard.getContents( null );
			  String data = (String) transfer.getTransferData( DataFlavor.stringFlavor );
		    document.insert(data, caret.getRow(), caret.getColumn());
		  } catch (Exception ignored) {}
	  } else

    // mouse button 3
    if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) {
      pane.addTextHighlight(new SCETextHighlight(pane, position, position, Color.red));
    }

    lastMouseClick = System.currentTimeMillis();
  }

  public void mouseReleased(MouseEvent e) {
  }

  public void mouseEntered(MouseEvent e) {
  }

  public void mouseExited(MouseEvent e) {
  }

  // MouseMotionListener

  public void mouseDragged(MouseEvent e) {
    if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
      if (caret.getSelectionMark() == null) caret.setSelectionMark();

      SCEDocumentPosition position = pane.viewToModel(e.getX(), e.getY());
      caret.moveTo(position.getRow(), position.getColumn(), true);
    }
  }

  public void mouseMoved(MouseEvent e) {
  }

	public CodeHelperPane getCodeHelperPane() {
		return codeHelperPane;
	}


	private static final ArrayList<WeakReference<SCEPane>> allPanes = new ArrayList<WeakReference<SCEPane>>();
	private static final HashMap<KeyStroke, String> keyStrokeMap = new HashMap<KeyStroke, String>();
	private static final HashMap<String, KeyStroke> actionMap = new HashMap<String, KeyStroke>();

	private static void installInputMap(SCEPane pane) {
		allPanes.add(new WeakReference<SCEPane>(pane));

		for (Map.Entry<KeyStroke, String> keyStrokeStringEntry : keyStrokeMap.entrySet()) {
			pane.getInputMap().put(keyStrokeStringEntry.getKey(), keyStrokeStringEntry.getValue());
		}
	}

	private static void uninstallInputMap(SCEPane pane) {
		allPanes.remove(new WeakReference<SCEPane>(pane));

		for (KeyStroke keyStroke : keyStrokeMap.keySet()) {
			pane.getInputMap().remove(keyStroke);
		}
	}

	public static void replaceKeyStrokeAndAction(KeyStroke keyStroke, String action) {
		// remove action
		if (actionMap.containsKey(action)) {
			removeKeyStroke(actionMap.get(action));
		}
		// remove keystroke
		if (keyStrokeMap.containsKey(keyStroke)) {
			removeKeyStroke(keyStroke);
		}
		// add keystroke
		addKeyStroke(keyStroke, action);
	}

	public static void addKeyStroke(KeyStroke keyStroke, String action) {
		// overwrite keystroke
		for (WeakReference<SCEPane> paneRef : allPanes) {
			SCEPane pane = paneRef.get();
			pane.getInputMap().put(keyStroke, action);
		}
		keyStrokeMap.put(keyStroke, action);
		actionMap.put(action, keyStroke);
	}

	public static void removeKeyStroke(KeyStroke keyStroke) {
		if (keyStrokeMap.containsKey(keyStroke)) {
			for (WeakReference<SCEPane> paneRef : allPanes) {
				SCEPane pane = paneRef.get();
				pane.getInputMap().remove(keyStroke);
			}
		}
		actionMap.remove(keyStrokeMap.get(keyStroke));
		keyStrokeMap.remove(keyStroke);
	}
}