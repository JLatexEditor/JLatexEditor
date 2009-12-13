
/**
 * @author Jörg Endrullis
 */

package qaqua;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalScrollBarUI;
import java.awt.*;

public class QaquaScrollBarUI extends MetalScrollBarUI{
  // images for the scrollbar
  private Image thumbTop = null;
  private Image thumbMiddle = null;
  private Image thumbBottom = null;

  // images for the track
  private Image trackTop = null;
  private Image track = null;
  private Image trackBottom = null;

  // images for buttons
  private Image buttonIncrease = null;
  private Image buttonIncreaseDown = null;
  private Image buttonDecrease = null;
  private Image buttonDecreaseDown = null;

  public static ComponentUI createUI(JComponent c){
    return new QaquaScrollBarUI();
  }

  public void installUI(JComponent c){
    super.installUI(c);

    // load images
    thumbTop = loadImage("images/scrollbar/thumbTop.gif");
    thumbMiddle = loadImage("images/scrollbar/thumbMiddle.gif");
    thumbBottom = loadImage("images/scrollbar/thumbBottom.gif");

    trackTop = loadImage("images/scrollbar/trackTop.gif");
    track = loadImage("images/scrollbar/track.gif");
    trackBottom = loadImage("images/scrollbar/trackBottom.gif");
  }

  /**
   * Loads the images (and rotates them if orientation == horizontal).
   *
   * @param name the image name
   * @return the image
   */
  private Image loadImage(String name){
    return loadImage(name, scrollbar.getOrientation());
  }

  /**
   * Loads the images (and rotates them if orientation == horizontal).
   *
   * @param name the image name
   * @param orientation the orientation
   * @return the image
   */
  private Image loadImage(String name, int orientation){
    Image image = QaquaLookAndFeel.loadImage(name);
    return orientation == JScrollBar.VERTICAL ? image : QaquaLookAndFeel.rotateImage90(image);
  }

  protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds){
    if(scrollbar.getOrientation() == JScrollBar.VERTICAL){
      int x = thumbBounds.x + 1;
      int middleStart = thumbBounds.y + thumbTop.getHeight(null);
      int middleEnd = thumbBounds.y + thumbBounds.height - thumbBottom.getHeight(null);
      int middleIncrement = thumbMiddle.getHeight(null);

      int y = middleStart;
      for(; y < middleEnd - middleIncrement; y += middleIncrement){
        g.drawImage(thumbMiddle, x, y, null);
      }
      g.drawImage(thumbMiddle, x, y, thumbMiddle.getWidth(null), middleEnd - y, null);

      g.drawImage(thumbTop, x, thumbBounds.y, null);
      g.drawImage(thumbBottom, x, middleEnd, null);
    }else{
      int y = thumbBounds.y + 1;
      int middleStart = thumbBounds.x + thumbTop.getWidth(null);
      int middleEnd = thumbBounds.x + thumbBounds.width - thumbBottom.getWidth(null);
      int middleIncrement = thumbMiddle.getWidth(null);

      int x = middleStart;
      for(; x < middleEnd - middleIncrement; x += middleIncrement){
        g.drawImage(thumbMiddle, x, y, null);
      }
      g.drawImage(thumbMiddle, x, y, middleEnd - x, thumbMiddle.getHeight(null), null);

      g.drawImage(thumbTop, thumbBounds.x, y, null);
      g.drawImage(thumbBottom, middleEnd, y, null);
    }
  }

  protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds){
    if(scrollbar.getOrientation() == JScrollBar.VERTICAL){
      int start = trackBounds.y;
      int end = trackBounds.y + trackBounds.height;
      int increment = track.getHeight(null);

      for(int y = start; y < end; y += increment){
        g.drawImage(track, trackBounds.x, y, null);
      }

      g.drawImage(trackTop, trackBounds.x, start, null);
      g.drawImage(trackBottom, trackBounds.x, end - trackBottom.getHeight(null), null);
    }else{
      int start = trackBounds.x;
      int end = trackBounds.x + trackBounds.width;
      int increment = track.getWidth(null);

      for(int x = start; x < end; x += increment){
        g.drawImage(track, x, trackBounds.y, null);
      }

      g.drawImage(trackTop, start, trackBounds.y, null);
      g.drawImage(trackBottom, end - trackBottom.getWidth(null), trackBounds.y, null);
    }
  }

  protected JButton createIncreaseButton(int orientation){
    buttonIncrease = loadImage("images/scrollbar/buttonBottom.gif");
    buttonIncreaseDown = loadImage("images/scrollbar/buttonBottomDown.gif");

    return new QaquaButton(buttonIncrease, buttonIncreaseDown);
  }

  protected JButton createDecreaseButton(int orientation){
    buttonDecrease = loadImage("images/scrollbar/buttonTop.gif", orientation);
    buttonDecreaseDown = loadImage("images/scrollbar/buttonTopDown.gif", orientation);

    return new QaquaButton(buttonDecrease, buttonDecreaseDown);
  }
}
