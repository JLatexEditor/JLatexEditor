package editor.codehelper;

import editor.component.*;

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
public class CodeHelperPane extends JScrollPane implements KeyListener, SCEDocumentListener{
  // the source code pane
  SCEPane pane = null;
  SCEDocument document = null;
  SCECaret caret = null;

  // the popup
  JPopupMenu popup = null;

  // the model
  private JList list = null;
  private DefaultListModel model = null;
	private CodeHelper codeHelper = null;
  // the command reference
  private Iterable<CHCommand> commands = null;

  // the position of the code helper
  private SCEDocumentPosition commandPosition = null;
  // the prefix
  private String prefix = null;

  // for working with templates
  private String template = null;
  private SCEDocumentPosition templateCaretPosition = null;
  // template argument values
  private ArrayList templateArguments = null;
  private int templateArgumentNr = -1;

  public CodeHelperPane(SCEPane pane){
    this.pane = pane;
    document = pane.getDocument();
    caret = pane.getCaret();

    // create the list
    list = new JList();
    list.setBackground(new Color(235, 244, 254));
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

  public void setVisible(boolean visible){
    super.setVisible(visible);
    popup.setVisible(visible);
  }

  public boolean isVisible(){
    return popup.isVisible();
  }

	public CodeHelper getCodeHelper() {
		return codeHelper;
	}

	public void setCodeHelper(CodeHelper codeHelper) {
		this.codeHelper = codeHelper;
		if (codeHelper != null) {
			this.codeHelper.setDocument(document);
		}
	}

	/**
   * Sets the prefix of the commands.
   *
   * @param prefix the prefix
   */
  public void setPrefix(String prefix){
    this.prefix = prefix;

    Object selectedValue = null;
    if(model.size() > 0) selectedValue = list.getSelectedValue();

    model.removeAllElements();
	  for (CHCommand command : commands) {
		  if (command.getName().startsWith(prefix)) model.addElement(command);
	  }

    list.setSelectedValue(selectedValue, true);
    if(selectedValue == null || !model.contains(selectedValue)) list.setSelectedIndex(0);
  }

  /**
   * Searches for the best completion of the prefix.
   *
   * @param prefix the prefix
   * @return the completion suggestion (without the prefix)
   */
  public String getCompletion(String prefix){
    int prefixLength = prefix.length();
    String completion = null;

	  for (CHCommand command : commands) {
		  String commandName = command.getName();
		  if (commandName.startsWith(prefix)) {
			  if (completion == null) {
				  completion = commandName;
			  } else {
				  // find the common characters
				  int commonIndex = prefixLength;
				  int commonLength = Math.min(completion.length(), commandName.length());
				  while (commonIndex < commonLength) {
					  if (completion.charAt(commonIndex) != commandName.charAt(commonIndex)) break;
					  commonIndex++;
				  }
				  completion = completion.substring(0, commonIndex);
			  }
		  }
	  }

    return completion;
  }

  public JList getList(){
    return list;
  }

  public Dimension getPreferredSize(){
    Dimension dimension = list.getPreferredSize();

    dimension.width = Math.min(480, dimension.width + 30);
    dimension.height = Math.min(320, dimension.height + 5);

    return dimension;
  }

  /**
   * Searches for a command before the given position.
   *
   * @param row the row
   * @param column the column
   * @return the command start
   */
  private int findPrefixStart(int row, int column){
    // get the command name
    SCEDocumentRow documentRow = document.getRows()[row];

    int commandStart = pane.findSplitterInRow(row, column, -1);
    if(column > 0 && documentRow.chars[column - 1].character == ' ') commandStart = column;
    if(commandStart > 0 && documentRow.chars[commandStart - 1].character == '\\') commandStart--;

    return commandStart;
  }

  /**
   * Updates the search prefix for the code helper.
   */
  private void updatePrefix(){
    if(commandPosition == null) return;

    int row = caret.getRow();
    int column = caret.getColumn();

	  // ask code helper for command suggestions at this position
	  commands = codeHelper.getCommandsAt(row, column);

    if(column < commandPosition.getColumn()){
      setVisible(false);
      commandPosition = null;
      return;
    }

    // extract the command from start until caret column
    setPrefix(document.getRow(row).substring(commandPosition.getColumn(),column));

    setSize(getPreferredSize());
    popup.pack();
  }

  /**
   * Sets the text to insert at the current position.
   *
   * @param text the text to insert
   */
  private void setText(String text){
    int row = commandPosition.getRow();
    int column = commandPosition.getColumn();

    document.remove(row, column, row, caret.getColumn());
    document.insert(text, row, column);
  }

  /**
   * Starts the template execution.
   *
   * @param templateWithAt the template String
   * @param arguments the arguments
   * @param row the row
   * @param column the column
   */
  private void startTemplate(String templateWithAt, ArrayList<CHCommandArgument> arguments, int row, int column){
    // remember the template and arguments
    template = templateWithAt.replaceAll("@", "");
    templateArguments = arguments;

    // remove the caret mark
    int caretIndex = template.lastIndexOf('|');
    if(caretIndex != -1) template = template.substring(0, caretIndex) + template.substring(caretIndex + 1);

    // insert the template in the document
    document.insert(template, row, column);

    // where to put the caret?
    {
      int cursorIndex = templateWithAt.lastIndexOf('|');
      if(cursorIndex != -1){
        templateWithAt = templateWithAt.substring(0, cursorIndex) + templateWithAt.substring(cursorIndex + 1);
      }else{
        cursorIndex = templateWithAt.length();
      }

      // get the position in the document
      int caret_row = row;
      int caret_column = column;

      for(int char_nr = 0; char_nr < cursorIndex; char_nr++){
        char character = templateWithAt.charAt(char_nr);
        if(character != '@') caret_column++;
        if(character == '\n'){
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

    // start editing with argument number 0
    editTemplate(0);

    // hide code helper
    setVisible(false);
    commandPosition = null;
  }

  /**
   * Edit the template argument with the given number.
   *
   * @param argument_nr the argument number
   */
  private void editTemplate(int argument_nr){
    if(templateArguments == null || templateArguments.size() == 0 || argument_nr >= templateArguments.size()){
      templateArgumentNr = -1;
      // end template editing
      document.setEditRange(null, null);
      template = null;

      // set the caret to the end position
      caret.moveTo(templateCaretPosition.getRow(), templateCaretPosition.getColumn());

      return;
    }
    if(argument_nr < 0) argument_nr = templateArguments.size() - 1;

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
  public void keyTyped(KeyEvent e){
  }

  public void keyPressed(KeyEvent e){
    int row = caret.getRow();
    int column = caret.getColumn();

    // run code helper
    if(e.getKeyCode() == KeyEvent.VK_SPACE && e.isControlDown() && !isVisible()){
      Point caretPos = pane.modelToView(row, column);

      commandPosition = new SCEDocumentPosition(row, findPrefixStart(row, column));
      updatePrefix();

      String completion = getCompletion(prefix);
      if(completion != null) setText(completion);

      setVisible(true);
      popup.show(pane, caretPos.x, caretPos.y + pane.getLineHeight());

      setSize(getPreferredSize());
      popup.pack();

      e.consume();
    }

    // hide on escape
    if(e.getKeyCode() == KeyEvent.VK_ESCAPE){
      // hide code helper
      setVisible(false);
      commandPosition = null;
      // end template editing
      if(template != null){
        document.setEditRange(null, null);
        template = null;
      }
    }

    // tab - template editing
    if((e.getKeyCode() == KeyEvent.VK_TAB || e.getKeyCode() == KeyEvent.VK_ENTER) && template != null){
      if(e.isShiftDown()){
        editTemplate(templateArgumentNr - 1);
      }else{
        editTemplate(templateArgumentNr + 1);
      }

      e.consume();
    }

    // continue only if the code helper is visible
    if(!isVisible()) return;

    // hide on cursor movement
    if(e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT){
      setVisible(false);
      commandPosition = null;
    }

    // up and down
    if(e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN){
      int direction = e.getKeyCode() == KeyEvent.VK_UP ? -1 : 1;
      int size = model.getSize();
      int index = list.getSelectedIndex() + direction;
      if(index >= 0 && index < size){
        list.setSelectedIndex(index);
        Rectangle scrollRect = list.getCellBounds(Math.max(index-2, 0), Math.min(index+2, size-1));
        list.scrollRectToVisible(scrollRect);
      }

      e.consume();
    }
    // page up and down
    if(e.getKeyCode() == KeyEvent.VK_PAGE_UP || e.getKeyCode() == KeyEvent.VK_PAGE_DOWN){
      int direction = e.getKeyCode() == KeyEvent.VK_PAGE_UP ? -1 : 1;
      direction *= list.getLastVisibleIndex() - list.getFirstVisibleIndex();
      int size = model.getSize();
      int index = list.getSelectedIndex() + direction;
      if(index < 0) index = 0;
      if(index > size - 1) index = size - 1;
      list.setSelectedIndex(index);

      e.consume();
    }
    // enter
    if(e.getKeyCode() == KeyEvent.VK_ENTER || (e.getKeyCode() == KeyEvent.VK_SPACE && !e.isControlDown())){
      if(model.size() == 0) return;

      // remove the current text and then start the template
      CHCommand command = (CHCommand) list.getSelectedValue();
      int command_row = commandPosition.getRow();
      int command_column = commandPosition.getColumn();
      document.remove(command_row, command_column, row, caret.getColumn());
      startTemplate(command.getUsage(), command.getArguments(), command_row, command_column);

      e.consume();
    }
  }

  public void keyReleased(KeyEvent e){
  }

  // SCEDocumentListener methods
  public void documentChanged(SCEDocument sender, SCEDocumentEvent event){
    // if we edit a template -> update other positions
    if(template != null && templateArguments != null && templateArguments.size() > 0 && templateArgumentNr >= 0){
      if(event.isInsert() || event.isRemove()){
        // get the argument value
        String argumentValue = document.getEditRangeText();

        // update all occurrences of the argument
        CHCommandArgument argument = (CHCommandArgument) templateArguments.get(templateArgumentNr);

        Iterator occurrencesIterator = argument.getOccurrences().iterator();
        occurrencesIterator.next(); // jump over the first occurrence
        while(occurrencesIterator.hasNext()){
          SCEDocumentRange argumentRange = (SCEDocumentRange) occurrencesIterator.next();
          SCEDocumentPosition start = new SCEDocumentPosition(argumentRange.getStartPosition().getRow(), argumentRange.getStartPosition().getColumn() + 1);
          SCEDocumentPosition end = argumentRange.getEndPosition();

          document.remove(start.getRow(), start.getColumn(), end.getRow(), end.getColumn(), 0);
          document.insert(argumentValue, start.getRow(), start.getColumn(), 0);
        }
      }
    }

    if(isVisible()) updatePrefix();
  }
}
