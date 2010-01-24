
/**
 * @author Jörg Endrullis
 */

package qaqua;

import javax.swing.plaf.basic.BasicListUI;
import javax.swing.plaf.ComponentUI;
import javax.swing.*;
import java.awt.*;

public class QaquaListUI extends BasicListUI{
  public static ComponentUI createUI(JComponent x){
    return new QaquaListUI();
  }
}