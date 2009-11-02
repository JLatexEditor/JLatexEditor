
/**
 * @author Jörg Endrullis
 */

package editor.component;

public interface SCECaretListener{
  public void caretMoved(int row, int column, int lastRow, int lastColumn);
}
