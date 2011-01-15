/**
 * @author Jörg Endrullis
 */

package qaqua;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPopupMenuUI;

public class QaquaPopupMenuUI extends BasicPopupMenuUI {
  public static ComponentUI createUI(JComponent x) {
    return new QaquaPopupMenuUI();
  }
}
