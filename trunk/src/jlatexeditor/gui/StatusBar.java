package jlatexeditor.gui;

import util.ColorUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;

/**
 * StatusBar.
 */
public class StatusBar extends JPanel {
  public StatusBar() {
    setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 0));

    add(new MemoryUsage());
    setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
  }

  public static class MemoryUsage extends JPanel implements ActionListener {
    public static Color COLOR_BAR = new Color(163, 201, 247);
    public static Color COLOR_BACKGROUND = ColorUtil.mix(COLOR_BAR, 0.5, Color.WHITE);
    private Timer timer;

    public MemoryUsage() {
      timer = new Timer(2000, this);
      timer.start();

      setBackground(COLOR_BACKGROUND);
      setPreferredSize(new Dimension(100,20));
    }

    public void paint(Graphics g) {
      super.paint(g);

      Runtime runtime = Runtime.getRuntime();
      long total = runtime.totalMemory();
      long used = total - runtime.freeMemory();

      g.setColor(COLOR_BAR);
      g.fillRect(0, 0, (int) (getWidth() * used / total), getHeight());

      String string = (used / (1024*1024)) + "M of " + (total / (1024*1024)) + "M";
      Rectangle2D stringRect = g.getFontMetrics().getStringBounds(string, g);
      g.setColor(Color.BLACK);
      g.drawString(string, (int) (getWidth() - stringRect.getWidth())/2, (int) (getHeight() + stringRect.getHeight())/2 - 2);
    }

    public void actionPerformed(ActionEvent e) {
      repaint();
    }
  }
}
