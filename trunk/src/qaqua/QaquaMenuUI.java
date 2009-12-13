package qaqua;

import javax.swing.plaf.basic.BasicMenuUI;
import javax.swing.plaf.ComponentUI;
import javax.swing.*;
import java.awt.*;

/**
 * @author Jörg Endrullis
 */
public class QaquaMenuUI extends BasicMenuUI{
  public static ComponentUI createUI(JComponent x){
    x.setOpaque(false);
    return new QaquaMenuUI();
  }

  protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
    if(menuItem.getModel().isSelected()){
      Graphics2D g2D = (Graphics2D) g;
      g2D.setColor(new Color(82, 109, 165, 50));
      g2D.fillRect(0, 0, menuItem.getWidth(), menuItem.getHeight());
    }
  }
}
