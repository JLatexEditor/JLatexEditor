package util;

import java.awt.*;

/**
 * Color util.
 */
public class ColorUtil {
  public static Color mix(Color c1, double interpolation, Color c2) {
    return new Color(
            mix(c1.getRed(), interpolation, c2.getRed()),
            mix(c1.getGreen(), interpolation, c2.getGreen()),
            mix(c1.getBlue(), interpolation, c2.getBlue())
    );
  }

  public static int mix(int c1, double interpolation, int c2) {
    return (int) (c1 * (1 - interpolation) + c2 * interpolation);
  }
}
