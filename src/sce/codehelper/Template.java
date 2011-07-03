package sce.codehelper;

import sce.component.*;
import util.Tuple;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class Template {
	private SCEPane pane;
	private CodeHelperPane codeHelperPane;
	private SCECaret caret;
	private SCEDocument document;
	private String template = null;
	private SCEDocumentPosition templateCaretPosition = null;
	// template argument values
	private ArrayList<CHCommandArgument> templateArguments = null;
	private int templateArgumentNr = -1;

	/**
	 * Starts the template execution.
	 *
	 * @param templateWithAt the template String
	 * @param arguments      the arguments
	 * @param row            the row
	 * @param column         the column
	 */
	public static Template startTemplate(SCEPane pane, String templateWithAt, ArrayList<CHCommandArgument> arguments, int row, int column) {
		Template template = new Template();
		template.setup(pane);

	  String newTemplate = templateWithAt;

	  // remove the caret mark
	  int caretIndex = newTemplate.lastIndexOf("@|@");
	  if (caretIndex != -1) newTemplate = newTemplate.substring(0, caretIndex) + newTemplate.substring(caretIndex + 3);

	  // remember the template and arguments
	  newTemplate = newTemplate.replaceAll("@", "");
	  newTemplate = newTemplate.replaceAll("&at;", "@");
	  templateWithAt = templateWithAt.replaceAll("&at;", "A");

	  // insert the template in the document
	  template.document.insert(newTemplate, row, column);

		// set the caret position and remove it from template
		Tuple<String, SCEDocumentPosition> pair = template.getTransformedTemplate(templateWithAt, arguments, row, column);
		templateWithAt = pair.first;

		// if it's a simple template without arguments we just insert it and move the caret to the end position
		if (arguments.size() == 0 && !templateWithAt.contains("\n")) {
			template.caret.moveTo(pair.second, false);
			return null;
		}

		// ensure there's no other active template
		template.codeHelperPane.endTemplateEditing(false);

		template.template = newTemplate;
		template.templateArguments = arguments;
		template.templateCaretPosition = pair.second;

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

	      SCEDocumentPosition occurrenceStart = template.document.createDocumentPosition(occurrence_row, occurrence_column - 1 - rel, rel);
	      SCEDocumentPosition occurrenceEnd = template.document.createDocumentPosition(occurrence_row, occurrence_column + argument.getName().length() + rel, -rel);
	      occurrences.add(new SCEDocumentRange(occurrenceStart, occurrenceEnd));
	    }
	    argument.setOccurrences(occurrences);
	  }

		for (CHCommandArgument argument : arguments) {
			if (!argument.getName().equals(argument.getValue())) {
				SCEDocumentRange range = argument.getOccurrences().get(0);
				SCEPosition start = range.getStartPosition().relative(0, 1);
				SCEDocumentPosition end = range.getEndPosition();
				template.document.replace(start, end, argument.getValue());
			}
		}

	  // line breaks
	  String indentation = template.getSpaceString(column);
	  int lineBreakPos = -1;
	  while ((lineBreakPos = template.template.indexOf('\n', lineBreakPos + 1)) != -1) {
	    template.document.insert(indentation, ++row, 0);
	  }

	  // hide code helper
	  template.codeHelperPane.setVisible(false);

	  // start editing with argument number 0
	  template.editTemplate(0);

		return template;
	}

	private void setup(SCEPane pane) {
		this.pane = pane;
		codeHelperPane = pane.getPaneUI().getCodeHelperPane();
		document = pane.getDocument();
		caret = pane.getCaret();
	}

	/**
	 * Returns a string with the given number of space characters.
	 *
	 * @param spaceCount number of space characters
	 * @return string with the given number of space characters
	 */
	private String getSpaceString(int spaceCount) {
		if (spaceCount < CodeHelperPane.spaces.length())
			return CodeHelperPane.spaces.substring(0, spaceCount);
		else
			return CodeHelperPane.spaces;
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

	public boolean goToPreviousArgument() {
		return editTemplate(templateArgumentNr - 1);
	}

	public boolean goToNextArgument() {
		return editTemplate(templateArgumentNr + 1);
	}

	/**
	 * Edit the template argument with the given number.
	 *
	 * @param argument_nr the argument number
	 * @return true if argument has been successfully changed
	 */
	private boolean editTemplate(int argument_nr) {
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

				if (value.equals("") || value.equals(oldArgument.getInitialValue())) {
					for (SCEDocumentRange argumentRange : oldArgument.getOccurrences()) {
						// check if char before range and after range is [ or ], respectively
						int colBefore = argumentRange.getStartPosition().getColumn();
						int colAfter  = argumentRange.getEndPosition().getColumn();
						int rowNr = argumentRange.getStartPosition().getRow();
						SCEDocumentRow row = document.getRowsModel().getRow(rowNr);
						if (colBefore >= 0 && colAfter < row.length &&
								row.chars[colBefore].character == '[' && row.chars[colAfter].character == ']') {
							document.remove(rowNr, colBefore, rowNr, colAfter + 1, SCEDocumentEvent.EVENT_EDITRANGE, false);
						}
					}
				}
			}
		}

	  if (noArgument || argument_nr >= templateArguments.size()) {
	    // end template editing
		  codeHelperPane.endTemplateEditing(false);

	    // set the caret to the end position
	    caret.moveTo(templateCaretPosition, false);

	    return false;
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
				SCEDocumentRow row = document.getRowsModel().getRow(rowNr);
				if (colBefore >= 0 && colAfter < row.length &&
						row.chars[colBefore].character != '[' || row.chars[colAfter].character != ']') {
					document.insert("[]", rowNr, colBefore, SCEDocumentEvent.EVENT_EDITRANGE, false);
				}
			}
		}

	  SCEDocumentRange argumentRange = argument.getOccurrences().get(0);
	  SCEDocumentPosition start = new SCEDocumentPosition(argumentRange.getStartPosition().getRow(), argumentRange.getStartPosition().getColumn() + 1);
	  SCEDocumentPosition end = argumentRange.getEndPosition();
	  document.setEditRange(start, end, false);

	  // select the argument value
	  caret.moveTo(start, false);
	  caret.moveTo(end, true);

		if (argument.isCompletion()) {
			codeHelperPane.callCodeHelperWithCompletion();
		}

		return true;
	}

	public boolean hasArguments() {
		return templateArguments != null && templateArguments.size() > 0;
	}

	public void documentChanged(SCEDocument sender, SCEDocumentEvent event) {
		if (templateArguments != null && templateArguments.size() > 0 && templateArgumentNr >= 0) {
			if (document.hasEditRange() &&
							event.getRangeStart().compareTo(document.getEditRangeStart()) >= 0 &&
							event.getRangeStart().compareTo(document.getEditRangeEnd()) <= 0 &&
							(event.isInsert() || event.isRemove())) {
				// get the argument value
				String argumentValue = document.getEditRangeText();

				// update all occurrences of the argument
				CHCommandArgument argument = templateArguments.get(templateArgumentNr);

				Iterator<SCEDocumentRange> occurrencesIterator = argument.getOccurrences().iterator();
				occurrencesIterator.next(); // jump over the first occurrence
				while (occurrencesIterator.hasNext()) {
					setArgumentValue(occurrencesIterator.next(), argumentValue);
				}

				// update all generated arguments
				for (CHArgumentGenerator generator : argument.getGenerators()) {
					String generatedValue = generator.getFunction().apply(argumentValue);
					for (SCEDocumentRange argumentRange : generator.getArgument().getOccurrences()) {
						setArgumentValue(argumentRange, generatedValue);
					}
				}
			}
		}
	}

	private void setArgumentValue(SCEDocumentRange argumentRange, String argumentValue) {
		SCEDocumentPosition start = new SCEDocumentPosition(argumentRange.getStartPosition().getRow(), argumentRange.getStartPosition().getColumn() + 1);
		SCEDocumentPosition end = argumentRange.getEndPosition();

		if (!document.getText(start, end).equals(argumentValue)) {
			pane.setFreezeCaret(true);
			document.remove(start.getRow(), start.getColumn(), end.getRow(), end.getColumn(), 0, false);
			document.insert(argumentValue, start.getRow(), start.getColumn(), 0, false);
			pane.setFreezeCaret(false);
		}
	}

	public static Template editAsTemplate(ArrayList<CHCommandArgument> arguments, SCEDocumentPosition caretEndPosition) {
		Template template = new Template();
		template.templateArguments = arguments;
		template.templateCaretPosition = caretEndPosition;
		template.editTemplate(0);

		return template;
	}
}
