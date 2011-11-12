package util;

import java.awt.*;

public class GraphicsUtil {
  public static Color COLOR_SELECTION_TOP = new Color(99, 136, 248);
  public static Color COLOR_SELECTION_BOTTOM = new Color(13, 83, 236);

  public static void paintSelectionBackground(Component component, Graphics2D g) {
    GradientPaint gp = new GradientPaint(0, 0, COLOR_SELECTION_TOP, 0, component.getHeight()-1, COLOR_SELECTION_BOTTOM);
    g.setPaint(gp);
    g.fillRect(0, 0, component.getWidth(), component.getHeight());
    g.setPaint(Color.BLACK);
  }
}
