package jlatexeditor.gui;

import jlatexeditor.JLatexEditorJFrame;
import jlatexeditor.tools.SVN;
import util.ColorUtil;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;

/**
 * StatusBar.
 */
public class StatusBar extends JPanel implements ActionListener {
  private JLatexEditorJFrame jLatexEditor;

  private JPanel right = new JPanel();
  private JPanel center = new JPanel();
  private JPanel left = new JPanel();

  private boolean checkForUpdates = true;
  private JLabel updatesAvailable = new JLabel("SVN updates are available.");

  private ArrayList<String> messges = new ArrayList<String>();

  public StatusBar(JLatexEditorJFrame jLatexEditor) {
    this.jLatexEditor = jLatexEditor;

    right.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 0));
    center.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
    left.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));

    setLayout(new BorderLayout());
    add(right, BorderLayout.EAST);
    add(center, BorderLayout.CENTER);
    add(left, BorderLayout.WEST);

    updatesAvailable.setOpaque(true);
    updatesAvailable.setBackground(new Color(192, 255, 192)); //131, 255, 131
    updatesAvailable.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(1,3,1,3)));
    updatesAvailable.setVisible(false);
    center.add(updatesAvailable);

    right.add(new MemoryUsage());
    setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

    new CheckForUpdates().start();
  }

  public synchronized void showMessage(String shortMessage, String message) {
    messges.add(message);
    new MessagePopup(message, jLatexEditor);
  }

  public synchronized void checkForUpdates() {
    if(!checkForUpdates) return;

    boolean hasUpdates = false;

    File file = jLatexEditor.getActiveEditor().getFile();
    if(!file.exists()) return;
    File dir = file.getParentFile();
    try {
      ArrayList<SVN.StatusResult> results = SVN.getInstance().status(dir);
      for(SVN.StatusResult result : results) {
        if(result.getServerStatus() == SVN.StatusResult.SERVER_OUTDATED) hasUpdates = true;
      }
    } catch (Exception e) {
      e.printStackTrace();
      checkForUpdates = false;
    }

    System.out.println(hasUpdates);
    updatesAvailable.setVisible(hasUpdates);
  }

  public void setUpdatesAvailableVisible(boolean v) {
    updatesAvailable.setVisible(v);
  }

  public void actionPerformed(ActionEvent e) {
    checkForUpdates();
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

  private class CheckForUpdates extends Thread {
    private CheckForUpdates() {
      setPriority(Thread.MIN_PRIORITY);
    }

    public void run() {
      checkForUpdates();

      try {
        synchronized (this) { wait(120000); }
      } catch (InterruptedException e) { }
    }
  }
}
