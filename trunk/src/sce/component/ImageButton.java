package sce.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

/**
 * Image button
 */
public class ImageButton extends JPanel implements MouseListener {
  private ImageIcon icon;
  private ImageIcon over;
  private ImageIcon press;

  private ImageIcon current;

  private ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();
  private String actionCommand = "clicked";

  public ImageButton(ImageIcon icon, ImageIcon over, ImageIcon press) {
    this.icon = icon;
    this.over = over;
    this.press = press;
    current = icon;

    setOpaque(false);
    addMouseListener(this);
  }

  public void paint(Graphics g) {
    g.drawImage(current.getImage(), 0, 0, null);
  }

  public Dimension getPreferredSize() {
    return new Dimension(current.getIconWidth(), current.getIconHeight());
  }

  public void addActionListener(ActionListener listener) {
    listeners.add(listener);
  }

  public void removeActionListener(ActionListener listener) {
    listeners.remove(listener);
  }

  public void mouseClicked(MouseEvent e) {
    for(ActionListener listener : (ArrayList<ActionListener>) listeners.clone()) {
      listener.actionPerformed(new ActionEvent(this, 0, actionCommand));
    }
  }

  public void mousePressed(MouseEvent e) {
    current = press;
    repaint();
  }

  public void mouseReleased(MouseEvent e) {
    current = icon;
    repaint();
  }

  public void mouseEntered(MouseEvent e) {
    current = over;
    repaint();
  }

  public void mouseExited(MouseEvent e) {
    current = icon;
    repaint();
  }
}
