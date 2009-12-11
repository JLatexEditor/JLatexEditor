package sce.component;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Image button
 */
public class ImageButton extends JButton implements MouseListener {
  private Icon icon;
  private Icon over;
  private Icon press;

  public ImageButton(Icon icon, Icon over, Icon press) {
    super(icon);
    this.icon = icon;
    this.over = over;
    this.press = press;
  }

  public void mouseClicked(MouseEvent e) {
  }

  public void mousePressed(MouseEvent e) {
    setIcon(press);
  }

  public void mouseReleased(MouseEvent e) {
    setIcon(icon);
  }

  public void mouseEntered(MouseEvent e) {
    setIcon(over);
  }

  public void mouseExited(MouseEvent e) {
    setIcon(icon);
  }
}
