/**
 * @author Jörg Endrullis
 */

package qaqua;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;

public class QaquaLookAndFeel extends MetalLookAndFeel {
  // the package name
  private String quaquaPackageName = "qaqua.";

  // a component (needed to use media tracker)
  private static JComponent component = new JPanel();

  public String getName() {
    return "Quaqua 1.0";
  }

  public String getID() {
    return "Quaqua";
  }

  public String getDescription() {
    return "Quaqua";
  }

  public UIDefaults getDefaults() {
    UIDefaults table = super.getDefaults();
    addCustomDefaults(table);
    return table;
  }

  /**
   * Installs the Quaqua specific UI.
   *
   * @param table the UIDefaults table
   */
  public void addCustomDefaults(UIDefaults table) {
    table.put("ScrollBarUI", quaquaPackageName + "QaquaScrollBarUI");
    table.put("ListUI", quaquaPackageName + "QaquaListUI");

    table.put("PopupMenuUI", quaquaPackageName + "QaquaPopupMenuUI");

    table.put("MenuBarUI", quaquaPackageName + "QaquaMenuBarUI");
    table.put("MenuUI", quaquaPackageName + "QaquaMenuUI");
    table.put("MenuItemUI", quaquaPackageName + "QaquaMenuItemUI");
    /*
 "CheckBoxMenuItemUI", basicPackageName + "BasicCheckBoxMenuItemUI",
    "RadioButtonMenuItemUI", basicPackageName + "BasicRadioButtonMenuItemUI",
    */
  }

  /**
   * Loads an image from the given file.
   *
   * @param name the name of the image
   * @return the image
   */
  public static Image loadImage(String name) {
    Image image = Toolkit.getDefaultToolkit().getImage(QaquaLookAndFeel.class.getResource(name));

    try {
      MediaTracker mediaTracker = new MediaTracker(component);
      mediaTracker.addImage(image, 0);
      mediaTracker.waitForAll();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    return image;
  }

  /**
   * Creates a pixel array from a image.
   *
   * @param img the image
   * @return the pixel array
   */
  public static int[] getPixelArrayFromImage(Image img) {
    int pix[];
    pix = new int[img.getWidth(null) * img.getHeight(null)];
    PixelGrabber pg = new PixelGrabber(img, 0, 0, img.getWidth(null), img.getHeight(null), pix, 0, img.getWidth(null));
    try {
      pg.grabPixels();
    } catch (InterruptedException e) {
      return null;
    }
    return pix;
  }

  /**
   * Creates an image from pixel array.
   *
   * @param array  the array
   * @param width  the width
   * @param height the height
   * @return the image
   */
  public static Image getImageFromPixelArray(int array[], int width, int height) {
    // aus dem Array wieder ein Bild machen
    MemoryImageSource producer = new MemoryImageSource(width, height, array, 0, width);
    producer.setAnimated(false); // nur ein Bild, nicht animiert

    // das neue Bild zurückliefern
    return Toolkit.getDefaultToolkit().createImage(producer);
  }

  public static Image rotateImage90(Image image) {
    int pixels[] = getPixelArrayFromImage(image);
    int rotatePixels[] = new int[pixels.length];

    // rotate the pixel array
    int width = image.getWidth(null);
    int height = image.getHeight(null);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        rotatePixels[y + x * height] = pixels[x + y * width];
      }
    }

    return getImageFromPixelArray(rotatePixels, height, width);
  }
}
