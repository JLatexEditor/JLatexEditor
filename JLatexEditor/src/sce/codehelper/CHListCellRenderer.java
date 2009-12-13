/**
 * @author Jörg Endrullis
 */

package sce.codehelper;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CHListCellRenderer extends DefaultListCellRenderer{
  protected static Border noFocusBorder;

  /**
   * Constructs a default renderer object for an item in a list.
   */
  public CHListCellRenderer(){
    super();
    if(noFocusBorder == null){
      noFocusBorder = new EmptyBorder(1, 1, 1, 1);
    }

    setOpaque(true);
    setBorder(noFocusBorder);
  }

  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus){
    setComponentOrientation(list.getComponentOrientation());
    if(isSelected){
      setBackground(list.getSelectionBackground());
      setForeground(list.getSelectionForeground());
    } else{
      setBackground(list.getBackground());
      setForeground(list.getForeground());
    }

    if(value instanceof Icon){
      setIcon((Icon) value);
      setText("");
    } else{
      setIcon(null);
      setText((value == null) ? "" : value.toString());
    }

    setEnabled(list.isEnabled());
    setFont(list.getFont());
    setBorder((cellHasFocus) ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);

    return this;
  }
}