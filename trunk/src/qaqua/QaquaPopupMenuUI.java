
/**
 * @author Jörg Endrullis
 */

package qaqua;

import javax.swing.plaf.basic.BasicPopupMenuUI;
import javax.swing.plaf.ComponentUI;
import javax.swing.*;
import java.awt.*;

public class QaquaPopupMenuUI extends BasicPopupMenuUI{
  public static ComponentUI createUI(JComponent x){
    return new QaquaPopupMenuUI();
  }
}
