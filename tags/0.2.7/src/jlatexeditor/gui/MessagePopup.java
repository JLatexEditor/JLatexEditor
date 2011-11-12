package jlatexeditor.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Display messages.
 */
public class MessagePopup extends JPopupMenu implements MouseListener, KeyListener {
  private long startTime = -1;
  private long alphaTime = 1000000000;

  private JLabel label;
	private Color backgroundColor = new Color(192, 239, 192);
	private Color strokeColor = new Color(0, 128, 0);

	public MessagePopup(Color color, String message, JFrame invoker) {
    super(message);

    label = new JLabel(getLabel());
    label.setMaximumSize(new Dimension(800, 600));

		backgroundColor = mix(Color.WHITE, color, 0.75);
		strokeColor = mix(Color.BLACK, color, 0.33);

    Dimension preferred = label.getPreferredSize();
    int width = preferred.width;
    int height = preferred.height;
    label.setSize(preferred);

    setOpaque(false);
    setInvoker(invoker);
    setLocation((invoker.getWidth() - width) / 2, (invoker.getHeight() - height) / 2);
    setPopupSize(width + 40, height + 40);
    setVisible(true);

    addMouseListener(this);
		addKeyListener(this);
  }

	private Color mix(Color c1, Color c2, double c1ratio) {
		return new Color(
			(int) (c1.getRed()*c1ratio + c2.getRed()*(1-c1ratio)),
			(int) (c1.getGreen()*c1ratio + c2.getGreen()*(1-c1ratio)),
			(int) (c1.getBlue()*c1ratio + c2.getBlue()*(1-c1ratio)));
	}

  public void setVisible(boolean b) {
    if (!b) return;
    super.setVisible(b);
	  requestFocus();
  }

  public void update(Graphics g) {
  }

  public void paint(Graphics g) {
    if (startTime < 0) startTime = System.nanoTime();
    Graphics2D g2D = (Graphics2D) g;

    float alpha = Math.min(1f, 0.1f + 0.9f * (System.nanoTime() - startTime) / alphaTime);
    g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));

    g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // green rectangle
    g2D.clearRect(0, 0, getWidth(), getHeight());
    g2D.setColor(backgroundColor); // 217, 231, 194
    g2D.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 15, 15);
    g2D.setColor(strokeColor);
    g2D.setStroke(new BasicStroke(2));
    g2D.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 15, 15);

    // message
    g2D.setColor(Color.BLACK);
    g2D.translate((getWidth() - label.getWidth()) / 2, (getHeight() - label.getHeight()) / 2);
    label.paint(g);

    // continue repainting
    if (alpha < 1) {
      repaint();
    }
  }

  public void mouseClicked(MouseEvent e) {
    super.setVisible(false);
  }

  public void mousePressed(MouseEvent e) {
  }

  public void mouseReleased(MouseEvent e) {
  }

  public void mouseEntered(MouseEvent e) {
  }

  public void mouseExited(MouseEvent e) {
  }

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		super.setVisible(false);
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}
}
