
/**
 * @author Jörg Endrullis
 */

package qaqua;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicListUI;

public class QaquaListUI extends BasicListUI{
  public static ComponentUI createUI(JComponent x){
    return new QaquaListUI();
  }
}
