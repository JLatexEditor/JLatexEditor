/**
 * @author Jörg Endrullis
 */

package qaqua;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextAttribute;
import java.text.AttributedString;

public class QaquaFrameTitle extends JPanel implements ActionListener, MouseListener, MouseMotionListener {
  // the owner frame
  QaquaFrame window = null;
  AttributedString titleString = null;

  // some images
  private Image imgTitleLeft = null;
  private Image imgTitleBackground = null;
  private Image imgTitleRight = null;

  private Image imgClose = null;
  private Image imgCloseOver = null;
  private Image imgMinimize = null;
  private Image imgMinimizeOver = null;
  private Image imgMaximize = null;
  private Image imgMaximizeOver = null;
  private Image imgGrayBall = null;

  /**
   * Creates a title bar for a QaquaFrame.
   *
   * @param window the frame
   */
  public QaquaFrameTitle(QaquaFrame window) {
    this.window = window;

    // set the layout
    setLayout(null);

    // load images
    imgTitleLeft = QaquaLookAndFeel.loadImage("images/frame/titleLeft.gif");
    imgTitleBackground = QaquaLookAndFeel.loadImage("images/frame/titleBackground.gif");
    imgTitleRight = QaquaLookAndFeel.loadImage("images/frame/titleRight.gif");

    imgClose = QaquaLookAndFeel.loadImage("images/frame/close.gif");
    imgCloseOver = QaquaLookAndFeel.loadImage("images/frame/closeOver.gif");
    imgMinimize = QaquaLookAndFeel.loadImage("images/frame/minimize.gif");
    imgMinimizeOver = QaquaLookAndFeel.loadImage("images/frame/minimizeOver.gif");
    imgMaximize = QaquaLookAndFeel.loadImage("images/frame/maximize.gif");
    imgMaximizeOver = QaquaLookAndFeel.loadImage("images/frame/maximizeOver.gif");
    imgGrayBall = QaquaLookAndFeel.loadImage("images/frame/grayBall.gif");

    // add frame buttons
    QaquaButton closeButton = new QaquaButton(imgClose, imgCloseOver);
    closeButton.setRolloverImage(imgCloseOver);
    closeButton.setDeactivatedImage(imgGrayBall, window);
    closeButton.setBounds(10, 3, closeButton.getPreferredSize().width, closeButton.getPreferredSize().height);
    closeButton.setRolloverEnabled(true);
    closeButton.setActionCommand("close");
    closeButton.addActionListener(this);
    this.add(closeButton);
    QaquaButton minimizeButton = new QaquaButton(imgMinimize, imgMinimizeOver);
    minimizeButton.setRolloverImage(imgMinimizeOver);
    minimizeButton.setDeactivatedImage(imgGrayBall, window);
    minimizeButton.setBounds(30, 3, minimizeButton.getPreferredSize().width, minimizeButton.getPreferredSize().height);
    minimizeButton.setRolloverEnabled(true);
    minimizeButton.setActionCommand("minimize");
    minimizeButton.addActionListener(this);
    this.add(minimizeButton);
    QaquaButton maximizeButton = new QaquaButton(imgMaximize, imgMaximizeOver);
    maximizeButton.setRolloverImage(imgMaximizeOver);
    maximizeButton.setDeactivatedImage(imgGrayBall, window);
    maximizeButton.setBounds(50, 3, maximizeButton.getPreferredSize().width, maximizeButton.getPreferredSize().height);
    maximizeButton.setRolloverEnabled(true);
    maximizeButton.setActionCommand("maximize");
    maximizeButton.addActionListener(this);
    this.add(maximizeButton);

    // title string
    titleString = new AttributedString(window.getTitle());
    titleString.addAttribute(TextAttribute.FAMILY, "Dialog");
    titleString.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);

    // MouseListener
    addMouseListener(this);
    addMouseMotionListener(this);
  }

  public void paint(Graphics g) {
    Graphics2D g2D = (Graphics2D) g;

    int increment = imgTitleBackground.getWidth(null);
    int width = getWidth();
    for (int x = 0; x < width; x += increment) {
      g2D.drawImage(imgTitleBackground, x, 0, null);
    }

    g2D.drawImage(imgTitleLeft, 0, 0, null);
    g2D.drawImage(imgTitleRight, width - imgTitleRight.getWidth(null), 0, null);

    // draw the title of the window
    g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2D.setColor(new Color(128, 128, 128, 128));
    g2D.drawString(titleString.getIterator(), 81, 17);
    g2D.setColor(Color.BLACK);
    g2D.drawString(titleString.getIterator(), 80, 16);

    paintChildren(g2D);
  }

  /**
   * Returns the minimum width of the title.
   *
   * @return the with of the title
   */
  public int getTitleWidth() {
    Graphics2D g2D = (Graphics2D) getGraphics();
    if (g2D == null) return 100;
    FontMetrics fm = g2D.getFontMetrics(g2D.getFont());

    return 81 + fm.stringWidth(window.getTitle()) + 15;
  }

  // ActionListener methods

  public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand().equals("close")) {
      if (window.getDefaultCloseOperation() == QaquaFrame.EXIT_ON_CLOSE) System.exit(0);
      setVisible(false);
    }
    if (e.getActionCommand().equals("minimize")) {
      window.getJFrameOwner().setExtendedState(JFrame.ICONIFIED);
    }
    if (e.getActionCommand().equals("maximize")) {
      window.setMaximized(!window.isMaximized());
    }
  }

  // MouseListener methods
  private Point dragStartMouse = new Point();
  private Point dragStartWindow = new Point();
  private Point currentMousePos = new Point();

  public void mouseClicked(MouseEvent e) {
    if (e.getClickCount() == 2) window.setMaximized(!window.isMaximized());
  }

  public void mousePressed(MouseEvent e) {
    dragStartWindow = window.getLocationOnScreen();
    dragStartMouse.x = dragStartWindow.x + e.getX();
    dragStartMouse.y = dragStartWindow.y + e.getY();
  }

  public void mouseReleased(MouseEvent e) {
  }

  public void mouseEntered(MouseEvent e) {
  }

  public void mouseExited(MouseEvent e) {
  }

  // MouseMotionListener methods

  public void mouseDragged(MouseEvent e) {
    if (window.isMaximized()) return;

    currentMousePos.x = window.getLocationOnScreen().x + e.getX();
    currentMousePos.y = window.getLocationOnScreen().y + e.getY();

    window.setLocation(dragStartWindow.x + currentMousePos.x - dragStartMouse.x, dragStartWindow.y + currentMousePos.y - dragStartMouse.y);
  }

  public void mouseMoved(MouseEvent e) {
  }
}
