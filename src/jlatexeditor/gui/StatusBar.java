package jlatexeditor.gui;

import jlatexeditor.JLatexEditorJFrame;
import jlatexeditor.gproperties.GProperties;
import jlatexeditor.tools.SVN;
import util.ColorUtil;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;

/**
 * StatusBar.
 */
public class StatusBar extends JPanel implements MouseListener {
  private JLatexEditorJFrame jLatexEditor;

  private JPanel right = new JPanel();
  private JPanel center = new JPanel();
  private JPanel left = new JPanel();

  private JLabel updatesAvailable = new JLabel("SVN updates are available.");
  private ArrayList<File> filesNotInSVN = null;
  private JLabel notInSVN = new JLabel("Add files to SVN.");

  private ArrayList<String> messages = new ArrayList<String>();

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
    updatesAvailable.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(1, 3, 1, 3)));
    updatesAvailable.setVisible(false);
    updatesAvailable.addMouseListener(this);
    center.add(updatesAvailable);

    notInSVN.setOpaque(true);
    notInSVN.setBackground(new Color(255, 192, 172)); //131, 255, 131
    notInSVN.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(1, 3, 1, 3)));
    notInSVN.setVisible(false);
    notInSVN.addMouseListener(this);
    center.add(notInSVN);

    right.add(new MemoryUsage());
    setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
  }

  public synchronized void showMessage(String shortMessage, String message) {
    messages.add(message);
    new MessagePopup(new Color(0, 192, 0), message, jLatexEditor);
  }

  public void setUpdatesAvailableVisible(boolean v) {
    updatesAvailable.setVisible(v);
  }

  public void setNotInSVNVisible(boolean v, ArrayList<File> files) {
    filesNotInSVN = files;
    notInSVN.setVisible(v);
  }

  public void mouseClicked(MouseEvent e) {
  }

  public void mousePressed(MouseEvent e) {
    if(e.getSource() == updatesAvailable) {
      setUpdatesAvailableVisible(false);
      jLatexEditor.actionPerformed(new ActionEvent(this, 0, "svn update"));
    }
    if(e.getSource() == notInSVN && filesNotInSVN != null) {
      setNotInSVNVisible(false, filesNotInSVN);
      for(File file : filesNotInSVN) {
        // todo: show window that allows to select the files to add
        SVN.getInstance().add(file);
      }
    }
  }

  public void mouseReleased(MouseEvent e) {
  }

  public void mouseEntered(MouseEvent e) {
  }

  public void mouseExited(MouseEvent e) {
  }

	public void showTextError(String shortMessage, String textMessage) {
		String message = "<html>" + textMessage.replaceAll("\\n", "<br>") + "</html>";
		messages.add(message);
		new MessagePopup(new Color(192, 0, 0), message, jLatexEditor);
	}

	public static class MemoryUsage extends JPanel implements ActionListener {
    public static Color COLOR_BAR = new Color(163, 201, 247);
    public static Color COLOR_BACKGROUND = ColorUtil.mix(COLOR_BAR, 0.5, Color.WHITE);
    private Timer timer;

    public MemoryUsage() {
      timer = new Timer(2000, this);
      timer.start();

      setBackground(COLOR_BACKGROUND);
      setPreferredSize(new Dimension(100, 20));

	    addMouseListener(new MouseAdapter(){
		    @Override
		    public void mouseClicked(MouseEvent e) {
			    System.gc();
		    }
	    });
    }

    public void paint(Graphics g) {
      super.paint(g);

      Runtime runtime = Runtime.getRuntime();
      long total = runtime.totalMemory();
      long used = total - runtime.freeMemory();

      g.setColor(COLOR_BAR);
      g.fillRect(0, 0, (int) (getWidth() * used / total), getHeight());

      String string = (used / (1024 * 1024)) + "M of " + (total / (1024 * 1024)) + "M";
      Rectangle2D stringRect = g.getFontMetrics().getStringBounds(string, g);
      g.setColor(Color.BLACK);
      g.drawString(string, (int) (getWidth() - stringRect.getWidth()) / 2, (int) (getHeight() + stringRect.getHeight()) / 2 - 2);
    }

    public void actionPerformed(ActionEvent e) {
      repaint();
    }
  }
}
