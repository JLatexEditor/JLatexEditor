
/**
 * @author Jörg Endrullis
 */

package qaqua;

import javax.swing.plaf.basic.BasicMenuBarUI;
import javax.swing.plaf.ComponentUI;
import javax.swing.*;
import java.awt.*;

public class QaquaMenuBarUI extends BasicMenuBarUI{
  public static ComponentUI createUI(JComponent x){
    return new QaquaMenuBarUI();
  }

  /**
   * Do nothing. The title bar should paint the background.
   */
  public void update(Graphics g, JComponent c){
  }

  public void paint(Graphics g, JComponent c){
  }
}
