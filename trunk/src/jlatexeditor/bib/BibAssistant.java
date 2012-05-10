package jlatexeditor.bib;

import jlatexeditor.JLatexEditorJFrame;
import org.jetbrains.annotations.Nullable;
import sce.codehelper.CodeAssistant;
import sce.codehelper.PatternPair;
import sce.codehelper.SCEPopup;
import sce.codehelper.WordWithPos;
import sce.component.*;
import sce.syntaxhighlighting.ParserStateStack;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Assists with bibtex files.
 *
 * @author Joerg Endrullis &lt;joerg@endrullis.de&gt;
 */
public class BibAssistant implements CodeAssistant, SCEPopup.ItemHandler {
  private PatternPair authorPattern = new PatternPair("(?:^| and )(?:(?! and ) )*((?:(?! and ).)*?)", true, "(.*?)(?:$| and$| and )");
  private String[] valueOrder = new String[] {
          "author", "title",
          "booktitle",
          "journal", "volume", "number",
          "editor",
          "pages",
          "institution",
          "publisher", "series",
          "month", "year",
          "edition",
          "note",
          "address",
          "isbn","issn","doi","ee"
  };

  public BibAssistant() {
	}

	public boolean assistAt(SCEPane pane) {
    SCEDocument document = pane.getDocument();
    SCEDocumentRow[] rows = document.getRowsModel().getRows();
    int row = pane.getCaret().getRow();
    int column = pane.getCaret().getColumn();

    ParserStateStack stateStack = BibSyntaxHighlighting.parseRow(rows[row], column, document);
    BibParserState state = (BibParserState) stateStack.peek();

    if(column == 0 && state.getState() == BibParserState.STATE_NOTHING) {
      // refactor the whole bib
      return false;
    }

    if(state.getState() != BibParserState.STATE_NOTHING) {
      BibEntry entry = state.getEntry();

      for(BibKeyValuePair keyValue : entry.getAllParameters().values()) {
        for(WordWithPos value : keyValue.getValues()) {
          if(value.contains(row, column)) {
            String key = keyValue.getKey().word.toLowerCase();

            // selection
            SCEDocumentPosition selectionStart = document.getSelectionStart();
            SCEDocumentPosition selectionEnd = document.getSelectionEnd();
            if(document.hasSelection()
                    && !selectionStart.equals(selectionEnd)
                    && value.contains(selectionStart.getRow(), selectionStart.getColumn())
                    && value.contains(selectionEnd.getRow(), selectionEnd.getColumn())) {

              popup(pane, document, new WordWithPos(document.getSelectedText(), selectionStart, selectionEnd), "");
              return true;
            }

            // author
            if(key.equals("author")) {
              List<WordWithPos> params = authorPattern.find(pane, BibKeyValuePair.getInnerStart(value), BibKeyValuePair.getInnerEnd(value));
              if(params != null) {
                WordWithPos name = params.get(0);
                String nameKey = getAuthorString(name.word);

                popup(pane, document, name, nameKey);
                return true;
              }
            }

            // other values
            SCEDocumentPosition start;
            SCEDocumentPosition end;
            if(value.word.startsWith("\"") || value.word.startsWith("{")) {
              start = new SCEDocumentPosition(value.getStartPos().getRow(), value.getStartPos().getColumn()+1);
              end = new SCEDocumentPosition(value.getEndPos().getRow(), value.getEndPos().getColumn()-1);
            } else {
              start = new SCEDocumentPosition(value.getStartPos().getRow(), value.getStartPos().getColumn());
              end = new SCEDocumentPosition(value.getEndPos().getRow(), value.getEndPos().getColumn());
            }
            popup(pane, document, new WordWithPos(document.getText(start, end), start, end), "");
            return true;
          }
        }
      }
    }

    return false;
	}

  private void popup(SCEPane pane, SCEDocument document, WordWithPos word, String key) {
    document.setSelectionRange(word.getStartPos(), word.getEndPos(), true);
    pane.repaint();

    // find similar string
    ArrayList<WeightedElement<BibEntry>> weightedEntries = new ArrayList<WeightedElement<BibEntry>>();

    ArrayList<BibEntry> stringEntries = getEntries(document, "string");
    for(BibEntry stringEntry : stringEntries) {
      if(stringEntry.getName().isEmpty()) continue;
      String stringValue = stringEntry.getAllParameters().get(stringEntry.getName().toLowerCase()).getValuesString();

      int common = lcs(word.word, stringValue);
      weightedEntries.add(new WeightedElement<BibEntry>(common, stringEntry));
    }

    Collections.sort(weightedEntries);

    List<Object> list = new ArrayList<Object>();
    list.add(new CreateStringAction(word.word, key, pane));

    for(int itemNr = 0; itemNr < 3; itemNr++) {
      if(weightedEntries.isEmpty()) break;
      BibEntry entry = weightedEntries.remove(weightedEntries.size()-1).element;
      list.add(new ReplaceByAction(word.word, entry.getName(), entry.getAllParameters().get(entry.getName().toLowerCase()).getValuesString(), pane));
    }

    // open popup
    pane.getPopup().openPopup(list, this);
  }

  private ArrayList<BibEntry> getEntries(SCEDocument document, @Nullable String type) {
    ArrayList<BibEntry> entries = new ArrayList<BibEntry>();

    SCEDocumentRow[] rows = document.getRowsModel().getRows();

    BibParserState state = (BibParserState) rows[document.getRowsModel().getRowsCount()-1].parserStateStack.peek();
    int entriesCount = state.getEntryNr();
    for(int entryNr = 0; entryNr < entriesCount; entryNr++) {
      BibEntry entry = state.getEntryByNr().get(entryNr);
      if(type != null && !entry.getType().equalsIgnoreCase(type)) continue;
      entries.add(entry);
    }

    return entries;
  }

  private String getAuthorString(String name) {
    name = name.trim().toLowerCase();
    name = name.replaceAll("[^\\w\\d\\., ]", "");
    name = name.replaceAll(" +"," ");

    String firstName = "", lastName = "";

    int comma = name.indexOf(',');
    if(comma >= 0) {
      lastName = name.substring(0, comma);
      firstName = name.substring(comma+1);
    } else {
      String split[] = name.split("[ ](?=[^ ]*$)");
      if(split.length == 1) {
        lastName = split[0];
      } else {
        firstName = split[0];
        lastName = split[1];
      }
    }

    lastName = lastName.replaceAll(" ","");
    lastName = lastName.replaceAll("\\W","");
    firstName = firstName.replaceAll("(\\w)\\w*", "$1");
    firstName = firstName.replaceAll("\\W","");

    return lastName + "." + firstName;
  }

  /**
   * Longest common subsequence: http://introcs.cs.princeton.edu/java/96optimization/.
   */
  private int lcs(String x, String y) {
    int xlength = x.length();
    int ylength = y.length();

    int[][] opt = new int[xlength+1][ylength+1];

    for (int i = xlength-1; i >= 0; i--) {
      for (int j = ylength-1; j >= 0; j--) {
        if (x.charAt(i) == y.charAt(j))
          opt[i][j] = opt[i+1][j+1] + 1;
        else
          opt[i][j] = Math.max(opt[i+1][j], opt[i][j+1]);
      }
    }

    int length = 0;
    int i = 0, j = 0;
    while(i < xlength && j < ylength) {
      if (x.charAt(i) == y.charAt(j)) {
        length++;
        i++;
        j++;
      }
      else if (opt[i+1][j] >= opt[i][j+1]) i++;
      else                                 j++;
    }

    return length;
  }

  private String oneLine(String text) {
    text = text.replace('\n', ' ');
    text = text.replaceAll(" +", " ");
    return text;
  }

  private void replaceAll(SCEDocument document, String search, String replaceKey) {
    ArrayList<BibEntry> entries = getEntries(document, null);
    for(BibEntry entry : entries) {
      for(BibKeyValuePair keyValues : entry.getAllParameters().values()) {
        boolean oneLine = keyValues.getKey().word.equalsIgnoreCase("author");
        for(WordWithPos value : keyValues.getValues()) {
          if(!value.word.startsWith("\"") && !value.word.startsWith("{")) continue;

          replaceIn(document, value, oneLine, search, replaceKey);
        }
      }
    }
  }

  private void replaceIn(SCEDocument document, WordWithPos value, boolean oneLine, String search, String replaceKey) {
    String text = value.word;
    if(oneLine) text = oneLine(text);

    if(!text.contains(search)) return;

    if(text.startsWith("\"") || text.startsWith("{")) {
      text = text.substring(1,text.length()-1);
    }

    StringBuilder builder = new StringBuilder();
    int index;
    while((index = text.indexOf(search)) >= 0) {
      String between = text.substring(0, index);
      if(!between.isEmpty()) {
        if(builder.length() != 0) builder.append(" # ");
        builder.append("{").append(between).append("}");
      }
      if(builder.length() != 0) builder.append(" # ");
      builder.append(replaceKey);
      text = text.substring(index + search.length());
    }
    if(!text.isEmpty()) {
      if(builder.length() != 0) builder.append(" # ");
      builder.append("{").append(text).append("}");
    }


    document.replace(value.getStartPos(), value.getEndPos(), builder.toString());
  }

  private HashSet<String> getEntryNames(SCEDocument document) {
    HashSet<String> names = new HashSet<String>();
    ArrayList<BibEntry> stringEntries = getEntries(document, null);
    for(BibEntry stringEntry : stringEntries) {
      names.add(stringEntry.getName());
    }
    return names;
  }

	public void perform(Object item) {
    final String forbiddenCharacters = "[ \\{\\}]";

    if(item instanceof CreateStringAction) {
      CreateStringAction action = (CreateStringAction) item;
      SCEPane pane = action.getPane();
      SCEDocument document = pane.getDocument();

      final HashSet<String> names = getEntryNames(document);
      final JTextField textField = new JTextField(action.getKey());
      textField.getDocument().addDocumentListener(new DocumentListener() {
        public void insertUpdate(DocumentEvent e) {
          changedUpdate(e);
        }

        public void removeUpdate(DocumentEvent e) {
          changedUpdate(e);
        }

        public void changedUpdate(DocumentEvent e) {
          String string = textField.getText();
          string = string.replaceAll(forbiddenCharacters,"");

          if(names.contains(string) || string.isEmpty()) {
            textField.setBackground(new Color(255, 204, 204));
          } else {
            textField.setBackground(Color.WHITE);
          }
        }
      });
      textField.setColumns(60);

      int ok = JOptionPane.showConfirmDialog(JLatexEditorJFrame.getFrames()[0], textField, "Enter Entry Name", JOptionPane.OK_CANCEL_OPTION);
      if(ok == JOptionPane.OK_OPTION && textField.getBackground().equals(Color.WHITE)) {
        String key = textField.getText().replaceAll(forbiddenCharacters,"");

        document.removeSelection();
        pane.repaint();
        pane.setFreezeCaret(true);
        replaceAll(document, action.getString(), key);
        document.insert("@string{" + key + " = {" + action.getString() + "}}\n", 0, 0);
        pane.setFreezeCaret(false);
      }
    }

    if(item instanceof ReplaceByAction) {
      ReplaceByAction action = (ReplaceByAction) item;
      SCEPane pane = action.getPane();
      SCEDocument document = pane.getDocument();

      document.removeSelection();
      pane.repaint();
      pane.setFreezeCaret(true);
      replaceAll(document, action.getString(), action.getReplaceKey());
      pane.setFreezeCaret(false);
    }
  }

// inner class

  class CreateStringAction {
    private String string;
    private String key;
    private SCEPane pane;

    CreateStringAction(String string, String key, SCEPane pane) {
      this.string = string;
      this.key = key;
      this.pane = pane;
    }

    public String getString() {
      return string;
    }

    public String getKey() {
      return key;
    }

    public SCEPane getPane() {
      return pane;
    }

    @Override
    public String toString() {
      return "Create Entry";
    }
  }

  class ReplaceByAction {
    private String string;
    private String replaceKey;
    private String replaceValue;
    private SCEPane pane;

    ReplaceByAction(String string, String replaceKey, String replaceValue, SCEPane pane) {
      this.string = string;
      this.replaceKey = replaceKey;
      this.replaceValue = replaceValue;
      this.pane = pane;
    }

    public String getString() {
      return string;
    }

    public String getReplaceKey() {
      return replaceKey;
    }

    public String getReplaceValue() {
      return replaceValue;
    }

    public SCEPane getPane() {
      return pane;
    }

    @Override
    public String toString() {
      return "Replace by: " + replaceValue;
    }
  }

  class WeightedElement<E> implements Comparable<WeightedElement<E>> {
    private double weight;
    private E element;

    WeightedElement(double weight, E element) {
      this.weight = weight;
      this.element = element;
    }

    public double getWeight() {
      return weight;
    }

    public void setWeight(double weight) {
      this.weight = weight;
    }

    public E getElement() {
      return element;
    }

    public void setElement(E element) {
      this.element = element;
    }

    public String toString() {
      return element.toString();
    }

    @Override
    public int compareTo(WeightedElement<E> o) {
      return new Double(weight).compareTo(o.weight);
    }
  }
}
