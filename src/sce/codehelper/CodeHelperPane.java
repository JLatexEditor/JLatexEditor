package sce.codehelper;

import de.endrullis.utils.KeyUtils;
import sce.component.*;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Popup window to show options for code completion.
 *
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */
public class CodeHelperPane extends JPanel implements KeyListener, SCEDocumentListener, MouseListener, PopupMenuListener {
  // the source code pane
  protected SCEPane pane = null;
  protected SCEDocument document = null;
  protected SCECaret caret = null;
	protected IdleThread idleThread;

  // the popup
  protected JPopupMenu popup = null;
	protected JScrollPane scrollPane = new JScrollPane();
	protected JLabel status = new JLabel();

  // the model
  private JList list = null;
  private DefaultListModel model = null;

  // helpers
  private CodeCompletion codeCompletion = null;
  private CodeCompletion tabCompletion = null;

  // the position of the code helper
  private WordWithPos wordPos = null;
	private int level = 1;
  // the prefix

  private Template template;

  static final String spaces = "                                                                                          ";

  public CodeHelperPane(SCEPane pane) {
    this.pane = pane;
    document = pane.getDocument();
    caret = pane.getCaret();

	  status.setFont(new Font("Serif", Font.PLAIN, 9));
	  status.setText("Level X");
	  status.setEnabled(true);

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
    scrollPane.setViewport(viewPort);

	  setLayout(new BorderLayout());
	  add(status, BorderLayout.NORTH);
	  add(scrollPane, BorderLayout.CENTER);

    // popup menu
    popup = new JPopupMenu();
    popup.add(this);
    popup.setFocusable(false);
	  popup.addPopupMenuListener(this);

    // add listeners
    pane.addKeyListener(this);
    document.addSCEDocumentListener(this);
  }

  public void setVisible(boolean visible) {
    super.setVisible(visible);
    popup.setVisible(visible);
    if (!visible) {
	    level = 1;
      wordPos = null;
    }
  }

  public boolean isVisible() {
    return popup.isVisible();
  }

  public CodeCompletion getCodeCompletion() {
    return codeCompletion;
  }

  public void setCodeCompletion(CodeCompletion codeCompletion) {
    this.codeCompletion = codeCompletion;
    if (codeCompletion != null) {
      this.codeCompletion.setSCEPane(pane);

	    if (codeCompletion.autoCompletion) {
		    idleThread = new IdleThread(codeCompletion);
	      idleThread.start();
	    }
    }
  }

  public CodeCompletion getTabCompletion() {
    return tabCompletion;
  }

  public void setTabCompletion(CodeCompletion tabCompletion) {
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

    dimension.width = Math.min(480, Math.max(dimension.width + 30, status.getPreferredSize().width + 5));
	  int height = dimension.height + status.getPreferredSize().height + 4;
	  if (!model.isEmpty()) {
		  height += 4;
	  }
	  dimension.height = Math.min(320, height);

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
   * @param level completion level
   */
  private void updatePrefix(int level) {
    if (wordPos == null || codeCompletion == null) return;

    // get selection
    Object selectedValue = null;
    if (model.size() > 0) selectedValue = list.getSelectedValue();

    // ask code helper for command suggestions at this position
    model.removeAllElements();

    if (codeCompletion.documentChanged()) {
	    while (model.isEmpty()) {
		    for (CHCommand command : codeCompletion.getCompletions(level)) model.addElement(command);
		    if (model.isEmpty()) {
			    if (level >= 3) {
				    break;
			    }
			    level++;
		    }
	    }

	    status.setText("Level " + level);
	    if (model.isEmpty()) {
		    status.setText(status.getText() + " - no suggestions");
	    }
	    wordPos = codeCompletion.getWordToReplace();

      // restore selection
      list.setSelectedValue(selectedValue, true);
      if (selectedValue == null || !model.contains(selectedValue)) list.setSelectedIndex(0);

      Dimension size = getPreferredSize();
      //size = new Dimension((int) (size.width * 1.2 + 50), size.height + 3);
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
    // tab completion
    if (e.getKeyCode() == KeyEvent.VK_TAB) {
      if (tabCompletion.matches()) {
        WordWithPos wordToReplace = tabCompletion.getWordToReplace();

	      Iterator<? extends CHCommand> iterator = tabCompletion.getCompletions(level).iterator();
	      if (iterator.hasNext()) {
					CHCommand template = iterator.next();
					if (template != null && template.isEnabled() && wordToReplace.word.equals(template.getName())) {
						SCECaret caret = pane.getCaret();
						document.remove(wordToReplace.getStartPos(), caret);

						Template newTemplate = Template.startTemplate(pane, template.getUsage(), template.getArguments(), wordToReplace.getStartRow(), wordToReplace.getStartCol());
						if (newTemplate != null) {
							this.template = newTemplate;
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
		if (codeCompletion.matches()) {
		  wordPos = codeCompletion.getWordToReplace();
		  updatePrefix(level);
		  String replacement = codeCompletion.getMaxCommonPrefix();

		  if (replacement != null) {
			  if (wordPos.word.equals(replacement)) {
				  if (popup.isVisible()) {
						level = Math.min(level + 1, 3);
						updatePrefix(level);
				  }
			  } else {
				  replace(wordPos, replacement);
			  }

			  if (!popup.isVisible()) {
				  popItUp();
			  }
		  }
		}
	}

	private void callCodeHelperWithoutCompletion() {
		if (popup.isVisible()) return;

		if (codeCompletion.matches()) {
		  wordPos = codeCompletion.getWordToReplace();
		  updatePrefix(level);

			String replacement = codeCompletion.getMaxCommonPrefix();

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

    if (isVisible()) updatePrefix(level);
	  else {
	    if (idleThread != null) {
	      idleThread.documentChanged(sender, event);
	    }
    }
  }

	public void editAsTemplate(ArrayList<CHCommandArgument> arguments, SCEPosition caretEndPosition) {
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

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
	}

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		level = 1;
	}

	@Override
	public void popupMenuCanceled(PopupMenuEvent e) {
		level = 1;
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

		public IdleThread(CodeCompletion codeCompletion) {
			super("CodeHelperPane-IdleThread");
			setDaemon(true);
			delay = codeCompletion.autoCompletionDelay;
			minLetters = codeCompletion.autoCompletionMinLetters;
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
