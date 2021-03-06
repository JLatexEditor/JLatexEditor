/**
 * @author Jörg Endrullis
 */

package qaqua;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

public class QaquaButton extends JButton implements WindowFocusListener, ActionListener {
  // images for up and down
  private Image imgButton = null;
  private Image imgPressed = null;
  private Image imgRollover = null;

  // window for focus listener
  private Window window = null;
  private Image imgDeaktivated = null;

  private Timer timer = null;
  private int timerCounter = 0;

  public QaquaButton(Image imgButton, Image imgPressed) {
    this.imgButton = imgButton;
    this.imgPressed = imgPressed;

    timer = new Timer(25, this);
    timer.start();
  }

  public boolean isOpaque() {
    return false;
  }

  public void setRolloverImage(Image imgRollover) {
    this.imgRollover = imgRollover;
  }

  public void setDeactivatedImage(Image imgDeaktivated, Window window) {
    this.imgDeaktivated = imgDeaktivated;

    this.window = window;
    window.addWindowFocusListener(this);
  }

  public Dimension getPreferredSize() {
    return new Dimension(imgButton.getWidth(null), imgButton.getHeight(null));
  }

  public void paint(Graphics g) {
    Graphics2D g2D = (Graphics2D) g;

    ButtonModel model = getModel();

    //g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.5f));
    if (model.isPressed()) {
      g2D.drawImage(imgPressed, 0, 0, null);
    } else if (model.isRollover() && imgRollover != null) {
      g2D.drawImage(imgRollover, 0, 0, null);
/*
    }else if(window != null && !window.isFocused() && imgDeaktivated != null){
      g2D.drawImage(imgDeaktivated, 0, 0, null);
    }else{
      g2D.drawImage(imgButton, 0, 0, null);
    }
*/
    } else {
      if (imgDeaktivated == null) {
        g2D.drawImage(imgButton, 0, 0, null);
      } else {
        paintDeactivation(g2D);
      }
    }
  }

  // WindowFocusListener methods
  public void windowGainedFocus(WindowEvent e) {
  }

  public void windowLostFocus(WindowEvent e) {
  }

  public void paintDeactivation(Graphics2D g2D) {
    g2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1.f - timerCounter / 20.f));
    g2D.drawImage(imgDeaktivated, 0, 0, null);
    g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, timerCounter / 20.f));
    g2D.drawImage(imgButton, 0, 0, null);
  }

  // ActionListener methods
  public void actionPerformed(ActionEvent e) {
    if (window != null && window.isFocused() && timerCounter < 20) {
      timerCounter = Math.min(timerCounter + 2, 20);
      repaint();
    }
    if (window != null && !window.isFocused() && timerCounter > 0) {
      timerCounter = Math.max(timerCounter - 1, 0);
      repaint();
    }
  }
}
