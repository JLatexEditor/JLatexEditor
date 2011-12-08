package jlatexeditor.gui;

import jlatexeditor.SCEManager;
import jlatexeditor.translation.I18n;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * About dialog.
 *
 * @author Stefan Endrullis
 */
public class AboutDialog extends JFrame {
  private JPanel contentPane;
  private JLabel credits;

  public AboutDialog(String version) {
    super(I18n.getString("about_dialog.title"));
    setUndecorated(true);

	  SCEManager.setWindowIcon(this);

	  StringBuilder sb = new StringBuilder();
    sb.append(System.getProperty("java.vm.name")).append("<br>");
    sb.append(System.getProperty("java.vendor")).append("<br>");
    sb.append(System.getProperty("java.home"));
    String vmString = sb.toString();

    // setup GUI components
    contentPane = new JPanel();
    contentPane.setLayout(new GridBagLayout());
    contentPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));

    credits = new JLabel();
    credits.setHorizontalAlignment(2);
    credits.setHorizontalTextPosition(0);
    if (version != null) {
      credits.setText(I18n.getString("about_dialog.text", version, System.getProperty("java.version"), vmString));
    }

    /*
      Color creditsFg = new Color(0xf49514);
      Color creditsShadow = Color.BLACK;
      float creditsShadowAlpha = 0.30f;
      int shadowStart = 1;
      */

    /*
      Color creditsFg = Color.WHITE;
      Color creditsShadow = Color.BLACK;
      float creditsShadowAlpha = 0.30f;
      int shadowStart = 1;
      */

    /*
      Color creditsFg = Color.BLACK;
      Color creditsShadow = Color.WHITE;
      float creditsShadowAlpha = 0.20f;
      int shadowStart = 0;
      */

    Color creditsFg = Color.WHITE;
    Color creditsShadow = Color.BLACK;
    float creditsShadowAlpha = 0.30f;
    int shadowStart = 1;

    ImageIcon icon = new ImageIcon(getClass().getResource("/images/logo.png"));
    Image iconImage = icon.getImage();
    BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    g.drawImage(iconImage, 0, 0, null);
    credits.setSize(credits.getPreferredSize());
    int x = (image.getWidth() - credits.getWidth()) / 2;
    int y = (image.getHeight() - credits.getHeight()) / 2;
    g.translate(x, y);
    credits.setForeground(creditsShadow);
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, creditsShadowAlpha));
    for (int dx = shadowStart; dx <= 2; dx++) {
      for (int dy = shadowStart; dy <= 2; dy++) {
        g.translate(dx, dy);
        credits.paint(g);
        g.translate(-dx, -dy);
      }
    }
    credits.setForeground(creditsFg);
    g.setComposite(AlphaComposite.SrcAtop);
    credits.paint(g);

    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    contentPane.add(new JLabel(new ImageIcon(image)), gbc);

    // set window size and location
    setSize(icon.getIconWidth() + 4, icon.getIconHeight() + 4);
    // place window in the center of the screen
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension windowSize = getSize();
    setLocation(screenSize.width / 2 - (windowSize.width / 2), screenSize.height / 2 - (windowSize.height / 2));
    setContentPane(contentPane);
//		setModal(true);

    // add listeners
    addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        onClick();
      }
    });
    addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        onClick();
      }
    });
    addFocusListener(new FocusAdapter() {
      public void focusLost(FocusEvent e) {
        onClick();
      }
    });
  }

  private void onClick() {
    dispose();
  }

  public void showIt() {
    setVisible(true);
  }

  public void showAndAutoHideAfter(final int ms) {
    setAlwaysOnTop(true);
    setVisible(true);
    new Thread() {
      @Override
      public void run() {
        try {
          Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
        onClick();
      }
    }.start();
  }
}
