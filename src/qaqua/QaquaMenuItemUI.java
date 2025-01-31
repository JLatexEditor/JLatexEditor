/**
 * @author Jörg Endrullis
 */

package qaqua;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuItemUI;
import java.awt.*;

public class QaquaMenuItemUI extends BasicMenuItemUI {
  // some images
  private Image imgBackground = null;

  public QaquaMenuItemUI() {
    // load images
    imgBackground = QaquaLookAndFeel.loadImage("images/menuitem/background.gif");
  }

  public static ComponentUI createUI(JComponent c) {
    return new QaquaMenuItemUI();
  }

  protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
    Graphics2D g2D = (Graphics2D) g;

    int increment = imgBackground.getWidth(null);
    int width = menuItem.getWidth();
    for (int x = 0; x < width; x += increment) {
      g2D.drawImage(imgBackground, x, 0, null);
    }

    if (menuItem.getModel().isArmed()) {
      g2D.setColor(new Color(82, 109, 165, 50));
      g2D.fillRect(0, 0, menuItem.getWidth(), menuItem.getHeight());
      g2D.setColor(Color.BLACK);
    }
  }
}
