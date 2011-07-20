package sce.codehelper;

import de.endrullis.utils.KeyUtils;
import sce.component.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Popup window to show options for code completion.
 *
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */
public class CodeHelperPane extends JScrollPane implements KeyListener, SCEDocumentListener, MouseListener {
  // the source code pane
  protected SCEPane pane = null;
  protected SCEDocument document = null;
  protected SCECaret caret = null;
	protected IdleThread idleThread;

  // the popup
  protected JPopupMenu popup = null;

  // the model
  private JList list = null;
  private DefaultListModel model = null;

  // helpers
  private CodeHelper codeHelper = null;
  private CodeHelper tabCompletion = null;

  // the position of the code helper
  private WordWithPos wordPos = null;
  // the prefix

  private Template template;

  static final String spaces = "                                                                                          ";

  public CodeHelperPane(SCEPane pane) {
    this.pane = pane;
    document = pane.getDocument();
    caret = pane.getCaret();

    // create the list
    list = new JList();
    list.setCellRenderer(new SCEListCellRenderer()); //list.setBackground(new Color(235, 244, 254));
    model = new DefaultListModel();
    list.setModel(model);

    // set colors
    list.setBackground(new Color(235, 244, 254));
    list.setForeground(Color.BLACK);
    list.setSelectionBackground(new Color(0, 82, 164));
    list.setSelectionForeground(Color.WHITE);
	  //list.setFont(pane.getFont().deriveFont(pane.getFont().getStyle() | Font.BOLD));
	  list.setFont(pane.getFont());

    // put us first in the listner list
    MouseListener[] listeners = list.getMouseListeners();
    for(MouseListener listener : listeners) list.removeMouseListener(listener);
    list.addMouseListener(this);
    for(MouseListener listener : listeners) list.addMouseListener(listener);

    // add the component to the viewport
    JViewport viewPort = new JViewport();
    viewPort.add(list);
    setViewport(viewPort);

    // popup menu
    popup = new JPopupMenu();
    popup.add(this);
    popup.setFocusable(false);

    // add listeners
    pane.addKeyListener(this);
    document.addSCEDocumentListener(this);
  }

  public void setVisible(boolean visible) {
    super.setVisible(visible);
    popup.setVisible(visible);
    if (!visible) {
      wordPos = null;
    }
  }

  public boolean isVisible() {
    return popup.isVisible();
  }

  public CodeHelper getCodeHelper() {
    return codeHelper;
  }

  public void setCodeHelper(CodeHelper codeHelper) {
    this.codeHelper = codeHelper;
    if (codeHelper != null) {
      this.codeHelper.setSCEPane(pane);

	    if (codeHelper.autoCompletion) {
		    idleThread = new IdleThread(codeHelper);
	      idleThread.start();
	    }
    }
  }

  public CodeHelper getTabCompletion() {
    return tabCompletion;
  }

  public void setTabCompletion(CodeHelper tabCompletion) {
    this.tabCompletion = tabCompletion;
    if (tabCompletion != null) {
      this.tabCompletion.setSCEPane(pane);
    }
  }

  public JList getList() {
    return list;
  }

  public Dimension getPreferredSize() {
    Dimension dimension = list.getPreferredSize();

    dimension.width = Math.min(480, dimension.width + 30);
    dimension.height = Math.min(320, dimension.height + 5);

    return dimension;
  }

  /**
   * Searches for a command before the given position.
   *
   * @param row    the row
   * @param column the column
   * @return the command start
   */
  private int findPrefixStart(int row, int column) {
    // get the command name
    SCEDocumentRow documentRow = document.getRowsModel().getRow(row);

    int commandStart = pane.findSplitterInRow(row, column, -1);
    if (column > 0 && documentRow.chars[column - 1].character == ' ') commandStart = column;
    if (column > 0 && documentRow.chars[column - 1].character == ' ') commandStart = column;
    if (commandStart > 0 && documentRow.chars[commandStart - 1].character == '\\') commandStart--;

    return commandStart;
  }

  /**
   * Updates the search prefix for the code helper.
   */
  private void updatePrefix() {
    if (wordPos == null || codeHelper == null) return;

    // get selection
    Object selectedValue = null;
    if (model.size() > 0) selectedValue = list.getSelectedValue();

    // ask code helper for command suggestions at this position
    model.removeAllElements();

    if (codeHelper.documentChanged()) {
      for (CHCommand command : codeHelper.getCompletions()) model.addElement(command);
	    wordPos = codeHelper.getWordToReplace();

      // restore selection
      list.setSelectedValue(selectedValue, true);
      if (selectedValue == null || !model.contains(selectedValue)) list.setSelectedIndex(0);

      Dimension size = getPreferredSize();
      size = new Dimension((int) (size.width * 1.2 + 50), size.height + 3);
      setPreferredSize(size);
      popup.setPreferredSize(size);
      popup.pack();

	    if (isVisible()) {
		    // update popup position
		    Point wordPoint = pane.modelToView(wordPos.getStartRow(), wordPos.getStartCol());
		    popup.show(pane, wordPoint.x, wordPoint.y + pane.getLineHeight());
	    }
    } else {
      setVisible(false);
    }
  }

  /**
   * Replaces a word with another word.
   *
   * @param word        word to replace
   * @param replacement replacement
   */
  private void replace(WordWithPos word, String replacement) {
    document.replace(word.getStartPos(), word.getEndPos(), replacement);
  }

	public void endTemplateEditing(boolean undo) {
		if (template != null) {
			document.setEditRange(null, null, undo);
			template = null;
		}
	}

	public void destroy() {
    pane.removeKeyListener(this);
    document.removeSCEDocumentListener(this);
  }

  // KeyListener methods

  public void keyTyped(KeyEvent e) {
  }

  public void keyPressed(KeyEvent e) {
    //int row = caret.getRowAsString();
    //int column = caret.getColumn();

    // TODO: move somewhere else
    /*
    // begin... end completion
    if(e.getKeyChar() == '}') {
      String begin = "\\begin";

      int index = column-1;
      String rowString = document.getRowAsString(row);
      while(index > 0) {
        char c = rowString.charAt(index);
        if(c == '{') break;
        if(!Character.isLetterOrDigit(c)) return;
        index--;
      }
      String environment = rowString.substring(index+1, column);

      int beginIndex = index - begin.length();
      if(beginIndex < 0) return;
      if(rowString.substring(beginIndex, index).equals(begin)) {
        String indentation = spaces.substring(0, beginIndex);
        pane.setFreezeCaret(true);
        document.insert("\n" + indentation + "\\end{" + environment + "}", row, column);
        pane.setFreezeCaret(false);
      }
      e.consume();
      return;
    }
    */

    // tab completion
    if (e.getKeyCode() == KeyEvent.VK_TAB) {
      if (tabCompletion.matches()) {
        WordWithPos wordToReplace = tabCompletion.getWordToReplace();

        for (CHCommand command : tabCompletion.getCompletions()) {
          if (wordToReplace.word.equals(command.getName())) {
            SCECaret caret = pane.getCaret();
            document.remove(wordToReplace.getStartPos(), caret);

            Template newTemplate = Template.startTemplate(pane, command.getUsage(), command.getArguments(), wordToReplace.getStartRow(), wordToReplace.getStartCol());
	          if (newTemplate != null) {
		          template = newTemplate;
	          }

            e.consume();
            return;
          }
        }
      }
    }

    // if the code helper is visible
    if (isVisible()) {
	    if (list.getModel().getSize() == 0) {
		    setVisible(false);
		    return;
	    }

			// hide on cursor movement
			if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_TAB) {
				setVisible(false);
			}

			// up and down
			if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
				int direction = e.getKeyCode() == KeyEvent.VK_UP ? -1 : 1;
				int index = list.getSelectedIndex() + direction;
				select(index);

				e.consume();
			}
			// page up and down
			if (e.getKeyCode() == KeyEvent.VK_PAGE_UP || e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
				int direction = e.getKeyCode() == KeyEvent.VK_PAGE_UP ? -1 : 1;
				direction *= list.getLastVisibleIndex() - list.getFirstVisibleIndex();
				int index = list.getSelectedIndex() + direction;
				select(index);

				e.consume();
			}
			// home
			if (e.getKeyCode() == KeyEvent.VK_HOME) {
				select(0);
				e.consume();
			}
			// end
			if (e.getKeyCode() == KeyEvent.VK_END) {
				select(list.getModel().getSize() - 1);
				e.consume();
			}

			// enter
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
        doIt();
				e.consume();
				return;
			}
    }

    // hide code helper on escape or close template
    if (KeyUtils.isStopKey(e)) {
	    if (isVisible()) {
				// hide code helper
				setVisible(false);
	    }
	    else {
				// end template editing
				if (template != null) {
					endTemplateEditing(false);
				}
	    }
    }

    // tab - template editing
    if ((e.getKeyCode() == KeyEvent.VK_TAB || e.getKeyCode() == KeyEvent.VK_ENTER) && template != null) {
      if (e.isShiftDown()) {
        if (!template.goToPreviousArgument())
	        endTemplateEditing(false);
      } else {
        if (!template.goToNextArgument())
	        endTemplateEditing(false);
      }

      e.consume();
    }
  }

  private void doIt() {
    WordWithPos oldWord = wordPos;
    setVisible(false);

    // remove the current text and then start the template
    CHCommand command = (CHCommand) list.getSelectedValue();

    SCECaret caret = pane.getCaret();
    document.remove(oldWord.getStartPos(), caret);

    Template newTemplate = Template.startTemplate(pane, command.getUsage(), command.getArguments(), oldWord.getStartRow(), oldWord.getStartCol());
    if (newTemplate != null) {
      template = newTemplate;
    }
  }

	public void callCodeHelperWithCompletion() {
		if (popup.isVisible()) return;

		if (codeHelper.matches()) {
		  wordPos = codeHelper.getWordToReplace();
		  updatePrefix();
		  String replacement = codeHelper.getMaxCommonPrefix();

		  if (replacement != null) {
		    replace(wordPos, replacement);

			  popItUp();
		  }
		}
	}

	private void callCodeHelperWithoutCompletion() {
		if (popup.isVisible()) return;

		if (codeHelper.matches()) {
		  wordPos = codeHelper.getWordToReplace();
		  updatePrefix();

			String replacement = codeHelper.getMaxCommonPrefix();

			if (replacement != null) {
				popItUp();
			}
		}
	}

	private void popItUp() {
		Point wordPoint = pane.modelToView(wordPos.getStartRow(), wordPos.getStartCol());

		setVisible(true);
		popup.show(pane, wordPoint.x, wordPoint.y + pane.getLineHeight());

		setSize(getPreferredSize());
		popup.pack();
	}

	private void select(int index) {
    int size = model.getSize();
    if (index < 0) index = 0;
    if (index > size - 1) index = size - 1;

    list.setSelectedIndex(index);
    Rectangle scrollRect = list.getCellBounds(Math.max(index - 2, 0), Math.min(index + 2, size - 1));
    list.scrollRectToVisible(scrollRect);
  }

  public void keyReleased(KeyEvent e) {
  }

  // SCEDocumentListener methods

  public void documentChanged(SCEDocument sender, SCEDocumentEvent event) {
    // if we edit a template -> update other positions
    if (template != null) {
	    template.documentChanged(sender, event);
    }

    if (isVisible()) updatePrefix();
	  else {
	    if (idleThread != null) {
	      idleThread.documentChanged(sender, event);
	    }
    }
  }

	public void editAsTemplate(ArrayList<CHCommandArgument> arguments, SCEDocumentPosition caretEndPosition) {
		template = Template.editAsTemplate(pane, arguments, caretEndPosition);
	}

  /**
   * MouseListener.
   */
  private int selectedIndex = -1;
  public void mouseClicked(MouseEvent e) {
    if(selectedIndex == list.getSelectedIndex() || e.getClickCount() >= 2) {
      doIt();
    }
  }

  public void mousePressed(MouseEvent e) {
    selectedIndex = list.getSelectedIndex();
  }

  public void mouseReleased(MouseEvent e) {
  }

  public void mouseEntered(MouseEvent e) {
  }

  public void mouseExited(MouseEvent e) {
  }


  // inner classes

  public static class SCEListCellRenderer extends DefaultListCellRenderer {
    public static final Color BACKGROUND = new Color(219, 224, 253);

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (!isSelected) component.setBackground(index % 2 == 0 ? BACKGROUND : Color.WHITE);
      return component;
    }
  }

	private class IdleThread extends Thread implements SCEDocumentListener {
		private final Object sync = new Object();
		private int delay;
		private int minLetters;
		private long lastTime;
		private Pattern pattern = Pattern.compile("\\p{Alnum}*$");

		public IdleThread(CodeHelper codeHelper) {
			super("CodeHelperPane-IdleThread");
			setDaemon(true);
			delay = codeHelper.autoCompletionDelay;
			minLetters = codeHelper.autoCompletionMinLetters;
		}

		@Override
		public void run() {
			try {
				while (!isInterrupted()) {
					lastTime = System.nanoTime();
					long toWait = delay;
					while (toWait > 0) {
						sleep(toWait);
						toWait = delay - ((System.nanoTime() - lastTime) / 1000000);
					}

					String word = document.getRowsModel().getRowAsString(caret.getRow(), 0, caret.getColumn());
					Matcher matcher = pattern.matcher(word);
					if (matcher.find()) {
						word = matcher.group();
						if (word.length() >= minLetters) {
							callCodeHelperWithoutCompletion();
						}
					}

					synchronized (sync) {
						sync.wait();
					}
				}
			} catch (InterruptedException ignored) {
			}
		}

		public void activate() {
			synchronized (sync) {
				sync.notify();
			}
		}

		public void documentChanged(SCEDocument sender, SCEDocumentEvent event) {
			lastTime = System.nanoTime();
			activate();
		}
	}
}
