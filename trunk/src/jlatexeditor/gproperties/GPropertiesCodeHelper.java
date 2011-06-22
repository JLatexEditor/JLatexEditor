package jlatexeditor.gproperties;

import de.endrullis.utils.BetterProperties2.PSet;
import de.endrullis.utils.BetterProperties2.Range;
import jlatexeditor.codehelper.PatternHelper;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

public class GPropertiesCodeHelper extends PatternHelper {
  protected String key;
  protected WordWithPos value;
  protected Range range;

  public GPropertiesCodeHelper() {
    pattern = new PatternPair("^([^#=]+)=([^#]*)");
  }

  @Override
  public boolean matches() {
    if (super.matches()) {
      key = params.get(0).word.replaceAll("\\\\ ", " ");
      value = params.get(1);

      range = GProperties.getRange(key);

      if(range.description().equals("Java shortcut")) {
        params = new PatternPair("^([^#=]+)=([^#]*)", "([^#]*)").find(pane);
        key = params.get(0).word.replaceAll("\\\\ ", " ");
        value = params.get(1);

        KeyStrokeCreator keystrokeCreator = new KeyStrokeCreator();
        keystrokeCreator.popItUp();

        return false;
      }

      return true;
    }
    return false;
  }

  @Override
  public WordWithPos getWordToReplace() {
    return value;
  }

  @Override
  public Iterable<? extends CHCommand> getCompletions() {
    return getCompletions(key, value.word);
  }

  @Override
  public String getMaxCommonPrefix() {
    return getMaxCommonPrefix(key, value.word);
  }

  public Iterable<ValueCompletion> getCompletions(String key, final String prefix) {
    ArrayList<ValueCompletion> list = new ArrayList<ValueCompletion>();

    if (range instanceof PSet) {
      PSet set = (PSet) range;
      for (String value : set.content) {
        if (value.startsWith(prefix)) {
          list.add(new ValueCompletion(value));
        }
      }
    } else {
      list.add(new ValueCompletion("<" + range.description() + ">"));
    }

    return list;
  }

  /**
   * Searches for the best completion of the prefix.
   *
   * @param key    property name
   * @param prefix filename
   * @return the completion suggestion (without the prefix)
   */
  public String getMaxCommonPrefix(String key, String prefix) {
    if (range instanceof PSet) {
      int prefixLength = prefix.length();
      String completion = null;

      for (CHCommand command : getCompletions(key, prefix)) {
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
    } else {
      return prefix;
    }
  }

  public static class ValueCompletion extends CHCommand {
    /**
     * Creates a command with the given name.
     *
     * @param name the name
     */
    public ValueCompletion(String name) {
      super(name);
    }
	}

  public class KeyStrokeCreator extends JPopupMenu implements KeyListener {
    JTextField field = new JTextField("<perform your keystroke>");

    public KeyStrokeCreator() {
      field.setColumns(50);
      add(field);
    }

    public void keyTyped(KeyEvent e) {
      e.consume();
    }

    public void keyPressed(KeyEvent e) {
      if(e.getKeyCode() == KeyEvent.VK_ENTER) {
        WordWithPos wordPos = getWordToReplace();
        document.replace(wordPos.getStartPos(), wordPos.getEndPos(), field.getText());
        setVisible(false);
      } else
      if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
        setVisible(false);
      } else {
        String text = KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers()).toString();
        text = text.replaceAll("pressed ", "");
        field.setText(text);
      }
      e.consume();
    }

    public void keyReleased(KeyEvent e) {
    }

    public void popItUp() {
      WordWithPos wordPos = getWordToReplace();
      Point wordPoint = pane.modelToView(wordPos.getStartRow(), wordPos.getStartCol());

      show(pane, wordPoint.x, wordPoint.y + pane.getLineHeight());
      pack();

      field.addKeyListener(this);
      field.requestFocus();
    }
  }
}