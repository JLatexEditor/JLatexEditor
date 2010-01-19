package jlatexeditor.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Display messages.
 */
public class MessagePopup extends JPopupMenu {
  private long startTime;
  private long alphaTime = 2000000000;

  public MessagePopup(String label, JFrame invoker) {
    super(label);

    setOpaque(false);
    setInvoker(invoker);
    setLocation(200,200);
    setPopupSize(500,40);
    setVisible(true);
  }

  public void setVisible(boolean b) {
    super.setVisible(b);
    if(b) { startTime = System.nanoTime(); }
  }

  public void update(Graphics g) {
  }

  public void paint(Graphics g) {
    Graphics2D g2D = (Graphics2D) g;

    float alpha = Math.min(1f, 0.1f + 0.9f * (System.nanoTime() - startTime) / alphaTime);
    g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));

    g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // green rectangle
    g2D.clearRect(0,0,getWidth(),getHeight());
    g2D.setColor(new Color(192, 239, 192)); // 217, 231, 194
    g2D.fillRoundRect(2,2,getWidth()-4,getHeight()-4,15,15);
    g2D.setColor(new Color(0, 128, 0));
    g2D.setStroke(new BasicStroke(2));
    g2D.drawRoundRect(2,2,getWidth()-4,getHeight()-4,15,15);

    // message
    g2D.setColor(Color.BLACK);
    Rectangle2D bounds = g2D.getFontMetrics().getStringBounds(getLabel(), g2D);
    g2D.drawString(getLabel(), (getWidth() - (int) bounds.getWidth())/2, (getHeight() - (int) bounds.getHeight())/2);

    // continue repainting
    if(alpha < 1) { repaint(); }
  }
}
