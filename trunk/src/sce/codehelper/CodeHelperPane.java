package sce.codehelper;

import sce.component.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Iterator;

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
  private ArrayList templateArguments = null;
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

      // restore selection
      list.setSelectedValue(selectedValue, true);
      if (selectedValue == null || !model.contains(selectedValue)) list.setSelectedIndex(0);

      Dimension size = getPreferredSize();
      size = new Dimension((int) (size.width * 1.2 + 50), size.height + 3);
      setPreferredSize(size);
      popup.setPreferredSize(size);
      popup.pack();
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
    template = templateWithAt;

    // remove the caret mark
    int caretIndex = template.lastIndexOf("@|@");
    if (caretIndex != -1) template = template.substring(0, caretIndex) + template.substring(caretIndex + 3);

    // remember the template and arguments
    template = template.replaceAll("@", "");
    templateArguments = arguments;

    // insert the template in the document
    document.insert(template, row, column);

    // where to put the caret?
    {
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

      // set the caret position
      templateCaretPosition = document.createDocumentPosition(caret_row, caret_column);
    }

    // initialize the argument values and occurrences
    for (CHCommandArgument argument : arguments) {
      argument.setValue(argument.getName());

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

        SCEDocumentPosition occurrenceStart = document.createDocumentPosition(occurrence_row, occurrence_column - 1);
        SCEDocumentPosition occurrenceEnd = document.createDocumentPosition(occurrence_row, occurrence_column + argument.getName().length());
        occurrences.add(new SCEDocumentRange(occurrenceStart, occurrenceEnd));
      }
      argument.setOccurrences(occurrences);
    }

    // line breaks
    String indentation = spaces.substring(0, column);
    int linebreakPos = -1;
    while ((linebreakPos = template.indexOf('\n', linebreakPos + 1)) != -1) {
      document.insert(indentation, ++row, 0);
    }

    // start editing with argument number 0
    editTemplate(0);

    // hide code helper
    setVisible(false);
  }

  /**
   * Edit the template argument with the given number.
   *
   * @param argument_nr the argument number
   */
  private void editTemplate(int argument_nr) {
    if (templateArguments == null || templateArguments.size() == 0 || argument_nr >= templateArguments.size()) {
      templateArgumentNr = -1;
      // end template editing
      document.setEditRange(null, null);
      template = null;

      // set the caret to the end position
      caret.removeSelectionMark();
      caret.moveTo(templateCaretPosition.getRow(), templateCaretPosition.getColumn());

      return;
    }
    if (argument_nr < 0) argument_nr = templateArguments.size() - 1;

    templateArgumentNr = argument_nr;

    // set the document edit range
    CHCommandArgument argument = (CHCommandArgument) templateArguments.get(argument_nr);
    SCEDocumentRange argumentRange = (SCEDocumentRange) argument.getOccurrences().get(0);
    SCEDocumentPosition start = new SCEDocumentPosition(argumentRange.getStartPosition().getRow(), argumentRange.getStartPosition().getColumn() + 1);
    SCEDocumentPosition end = argumentRange.getEndPosition();
    document.setEditRange(start, end);

    // select the argument value
    caret.moveTo(start.getRow(), start.getColumn());
    caret.setSelectionMark();
    caret.moveTo(end.getRow(), end.getColumn());
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

      if (codeHelper.matches()) {
        wordPos = codeHelper.getWordToReplace();
        updatePrefix();
        String replacement = codeHelper.getMaxCommonPrefix();

        if (replacement != null) {
          replace(wordPos, replacement);

          Point wordPoint = pane.modelToView(wordPos.getStartRow(), wordPos.getStartCol());

          setVisible(true);
          popup.show(pane, wordPoint.x, wordPoint.y + pane.getLineHeight());

          setSize(getPreferredSize());
          popup.pack();
        }
      }

      e.consume();
    }

    // hide on escape
    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
      // hide code helper
      setVisible(false);
      // end template editing
      if (template != null) {
        document.setEditRange(null, null);
        template = null;
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

    // continue only if the code helper is visible
    if (!isVisible()) return;

    // hide on cursor movement
    if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) {
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
      if (model.size() == 0) return;

      WordWithPos oldWord = wordPos;
      setVisible(false);

      // remove the current text and then start the template
      CHCommand command = (CHCommand) list.getSelectedValue();

      SCECaret caret = pane.getCaret();
      document.remove(oldWord.getStartPos(), caret);
      startTemplate(command.getUsage(), command.getArguments(), oldWord.getStartRow(), oldWord.getStartCol());

      e.consume();
    }
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
        CHCommandArgument argument = (CHCommandArgument) templateArguments.get(templateArgumentNr);

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
  }

  public static class SCEListCellRenderer extends DefaultListCellRenderer {
    public static final Color BACKGROUND = new Color(219, 224, 253);

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (!isSelected) component.setBackground(index % 2 == 0 ? BACKGROUND : Color.WHITE);
      return component;
    }
  }
}
