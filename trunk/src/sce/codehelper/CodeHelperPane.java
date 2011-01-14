package sce.codehelper;

import sce.component.*;
import util.Tuple;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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
public class CodeHelperPane extends JScrollPane implements KeyListener, SCEDocumentListener {
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

  // for working with templates
  private String template = null;
  private SCEDocumentPosition templateCaretPosition = null;
  // template argument values
  private ArrayList<CHCommandArgument> templateArguments = null;
  private int templateArgumentNr = -1;

  private static final String spaces = "                                                                                          ";

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
    SCEDocumentRow documentRow = document.getRows()[row];

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

  /**
   * Starts the template execution.
   *
   * @param templateWithAt the template String
   * @param arguments      the arguments
   * @param row            the row
   * @param column         the column
   */
  private void startTemplate(String templateWithAt, ArrayList<CHCommandArgument> arguments, int row, int column) {
    String newTemplate = templateWithAt;

    // remove the caret mark
    int caretIndex = newTemplate.lastIndexOf("@|@");
    if (caretIndex != -1) newTemplate = newTemplate.substring(0, caretIndex) + newTemplate.substring(caretIndex + 3);

    // remember the template and arguments
    newTemplate = newTemplate.replaceAll("@", "");
    newTemplate = newTemplate.replaceAll("&at;", "@");
    templateWithAt = templateWithAt.replaceAll("&at;", "A");

    // insert the template in the document
    document.insert(newTemplate, row, column);

		// set the caret position and remove it from template
	  Tuple<String, SCEDocumentPosition> pair = getTransformedTemplate(templateWithAt, arguments, row, column);
		templateWithAt = pair.first;

	  if (arguments.size() == 0 && !templateWithAt.contains("\n")) {
		  caret.moveTo(pair.second);
		  document.clearSelection();
		  return;
	  }

	  template = newTemplate;
	  templateArguments = arguments;
	  templateCaretPosition = pair.second;

    // initialize the argument values and occurrences
    for (CHCommandArgument argument : arguments) {
      // find occurrences
      ArrayList<SCEDocumentRange> occurrences = new ArrayList<SCEDocumentRange>();
      int index = -1;
      while ((index = templateWithAt.indexOf("@" + argument.getName() + "@", index + 1)) != -1) {
        // get the position in the document
        int occurrence_row = row;
        int occurrence_column = column;

        for (int char_nr = 0; char_nr < index; char_nr++) {
          char character = templateWithAt.charAt(char_nr);

          if (character != '@') occurrence_column++;
          if (character == '\n') {
            occurrence_row++;
            occurrence_column = 0;
          }
        }

	      int rel = 0;
	      if (argument.isOptional()) rel = 1;

        SCEDocumentPosition occurrenceStart = document.createDocumentPosition(occurrence_row, occurrence_column - 1 - rel, rel);
        SCEDocumentPosition occurrenceEnd = document.createDocumentPosition(occurrence_row, occurrence_column + argument.getName().length() + rel, -rel);
        occurrences.add(new SCEDocumentRange(occurrenceStart, occurrenceEnd));
      }
      argument.setOccurrences(occurrences);
    }

	  for (CHCommandArgument argument : arguments) {
		  if (!argument.getName().equals(argument.getValue())) {
			  SCEDocumentRange range = argument.getOccurrences().get(0);
			  SCEPosition start = range.getStartPosition().relative(0, 1);
			  SCEDocumentPosition end = range.getEndPosition();
			  document.replace(start, end, argument.getValue());
		  }
	  }

    // line breaks
    String indentation = getSpaceString(column);
    int lineBreakPos = -1;
    while ((lineBreakPos = template.indexOf('\n', lineBreakPos + 1)) != -1) {
      document.insert(indentation, ++row, 0);
    }

    // hide code helper
    setVisible(false);

    // start editing with argument number 0
    editTemplate(0);
  }

	/**
	 * Returns a string with the given number of space characters.
	 *
	 * @param spaceCount number of space characters
	 * @return string with the given number of space characters
	 */
	private String getSpaceString(int spaceCount) {
		if (spaceCount < spaces.length())
			return spaces.substring(0, spaceCount);
		else
			return spaces;
	}

	private Tuple<String, SCEDocumentPosition> getTransformedTemplate(String templateWithAt, ArrayList<CHCommandArgument> arguments, int row, int column) {
		int cursorIndex = templateWithAt.lastIndexOf("@|@");
		if (cursorIndex != -1) {
		  templateWithAt = templateWithAt.substring(0, cursorIndex) + templateWithAt.substring(cursorIndex + 1);
		} else {
		  cursorIndex = templateWithAt.length();
		}

		// get the position in the document
		int caret_row = row;
		int caret_column = column;

		for (int char_nr = 0; char_nr < cursorIndex; char_nr++) {
		  char character = templateWithAt.charAt(char_nr);
		  if (character != '@') caret_column++;
		  if (character == '\n') {
		    caret_row++;
		    caret_column = 0;
		  }
		}

		return new Tuple<String,SCEDocumentPosition>(templateWithAt, document.createDocumentPosition(caret_row, caret_column));
	}

	/**
   * Edit the template argument with the given number.
   *
   * @param argument_nr the argument number
   */
  private void editTemplate(int argument_nr) {
	  boolean noArgument = false;
	  if (templateArguments == null || templateArguments.size() == 0) noArgument = true;

	  if (!noArgument && templateArgumentNr >= 0 && templateArgumentNr < templateArguments.size()) {
		  // leave current template argument
		  CHCommandArgument oldArgument = templateArguments.get(templateArgumentNr);

		  if (oldArgument.isOptional()) {
			  SCEDocumentRange range = oldArgument.getOccurrences().get(0);
			  SCEPosition start = range.getStartPosition().relative(0, 1);
			  SCEDocumentPosition end = range.getEndPosition();
			  String value = document.getText(start, end);

			  if (value.equals("") || value.equals(oldArgument.getName())) {
				  for (SCEDocumentRange argumentRange : oldArgument.getOccurrences()) {
					  // check if char before range and after range is [ or ], respectively
					  int colBefore = argumentRange.getStartPosition().getColumn();
					  int colAfter  = argumentRange.getEndPosition().getColumn();
					  int rowNr = argumentRange.getStartPosition().getRow();
					  SCEDocumentRow row = document.getRows()[rowNr];
					  if (colBefore >= 0 && colAfter < row.length &&
							  row.chars[colBefore].character == '[' && row.chars[colAfter].character == ']') {
						  document.remove(rowNr, colBefore, rowNr, colAfter + 1, SCEDocumentEvent.EVENT_EDITRANGE);
					  }
				  }
			  }
		  }
	  }

    if (noArgument || argument_nr >= templateArguments.size()) {
      // end template editing
	    endTemplateEditing(false);

      // set the caret to the end position
      caret.removeSelectionMark();
      caret.moveTo(templateCaretPosition);

      return;
    }
    if (argument_nr < 0) argument_nr = templateArguments.size() - 1;

    templateArgumentNr = argument_nr;

    // set the document edit range
    CHCommandArgument argument = templateArguments.get(argument_nr);

	  if (argument.isOptional()) {
		  for (SCEDocumentRange argumentRange : argument.getOccurrences()) {
			  // check if char before range and after range is [ or ], respectively
			  int colBefore = argumentRange.getStartPosition().getColumn();
			  int colAfter  = argumentRange.getEndPosition().getColumn();
			  int rowNr = argumentRange.getStartPosition().getRow();
			  SCEDocumentRow row = document.getRows()[rowNr];
			  if (colBefore >= 0 && colAfter < row.length &&
					  row.chars[colBefore].character != '[' || row.chars[colAfter].character != ']') {
				  document.insert("[]", rowNr, colBefore, SCEDocumentEvent.EVENT_EDITRANGE);
			  }
		  }
	  }

    SCEDocumentRange argumentRange = argument.getOccurrences().get(0);
    SCEDocumentPosition start = new SCEDocumentPosition(argumentRange.getStartPosition().getRow(), argumentRange.getStartPosition().getColumn() + 1);
    SCEDocumentPosition end = argumentRange.getEndPosition();
    document.setEditRange(start, end, false);

    // select the argument value
    caret.moveTo(start);
    caret.setSelectionMark();
    caret.moveTo(end);

	  if (argument.isCompletion()) {
		  callCodeHelperWithCompletion();
	  }
  }

	public void endTemplateEditing(boolean undo) {
		templateArgumentNr = -1;
		document.setEditRange(null, null, undo);
		template = null;
	}

	public void destroy() {
    pane.removeKeyListener(this);
    document.removeSCEDocumentListener(this);
  }

  // KeyListener methods

  public void keyTyped(KeyEvent e) {
  }

  public void keyPressed(KeyEvent e) {
    //int row = caret.getRow();
    //int column = caret.getColumn();

    // control+space
    if (e.getKeyCode() == KeyEvent.VK_SPACE && e.isControlDown() && !isVisible()) {
	    callCodeHelperWithCompletion();

	    e.consume();
    }

    // TODO: move somewhere else
    /*
    // begin... end completion
    if(e.getKeyChar() == '}') {
      String begin = "\\begin";

      int index = column-1;
      String rowString = document.getRow(row);
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
            startTemplate(command.getUsage(), command.getArguments(), wordToReplace.getStartRow(), wordToReplace.getStartCol());

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
				WordWithPos oldWord = wordPos;
				setVisible(false);

				// remove the current text and then start the template
				CHCommand command = (CHCommand) list.getSelectedValue();

				SCECaret caret = pane.getCaret();
				document.remove(oldWord.getStartPos(), caret);
				startTemplate(command.getUsage(), command.getArguments(), oldWord.getStartRow(), oldWord.getStartCol());

				e.consume();
				return;
			}
    }

    // hide code helper on escape or close template
    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
	    if (isVisible()) {
				// hide code helper
				setVisible(false);
	    }
	    else {
				// end template editing
				if (template != null) {
					document.setEditRange(null, null, false);
					template = null;
				}
	    }
    }

    // tab - template editing
    if ((e.getKeyCode() == KeyEvent.VK_TAB || e.getKeyCode() == KeyEvent.VK_ENTER) && template != null) {
      if (e.isShiftDown()) {
        editTemplate(templateArgumentNr - 1);
      } else {
        editTemplate(templateArgumentNr + 1);
      }

      e.consume();
    }
  }

	private void callCodeHelperWithCompletion() {
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
    if (template != null && templateArguments != null && templateArguments.size() > 0 && templateArgumentNr >= 0) {
      if (document.hasEditRange() &&
              event.getRangeStart().compareTo(document.getEditRangeStart()) >= 0 &&
              event.getRangeStart().compareTo(document.getEditRangeEnd()) <= 0 &&
              (event.isInsert() || event.isRemove())) {
        // get the argument value
        String argumentValue = document.getEditRangeText();

        // update all occurrences of the argument
        CHCommandArgument argument = templateArguments.get(templateArgumentNr);

        Iterator occurrencesIterator = argument.getOccurrences().iterator();
        occurrencesIterator.next(); // jump over the first occurrence
        while (occurrencesIterator.hasNext()) {
          SCEDocumentRange argumentRange = (SCEDocumentRange) occurrencesIterator.next();
          SCEDocumentPosition start = new SCEDocumentPosition(argumentRange.getStartPosition().getRow(), argumentRange.getStartPosition().getColumn() + 1);
          SCEDocumentPosition end = argumentRange.getEndPosition();

          pane.setFreezeCaret(true);
          document.remove(start.getRow(), start.getColumn(), end.getRow(), end.getColumn(), 0);
          document.insert(argumentValue, start.getRow(), start.getColumn(), 0);
          pane.setFreezeCaret(false);
        }
      }
    }

    if (isVisible()) updatePrefix();
	  else {
	    if (idleThread != null) {
	      idleThread.documentChanged(sender, event);
	    }
    }
  }

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

					String word = document.getRow(caret.getRow(), 0, caret.getColumn());
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
