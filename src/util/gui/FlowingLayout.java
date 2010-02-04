package util.gui;

import java.awt.*;
import java.util.ArrayList;

/**
 * FlowLayout is simply shit, it ignores the maximum size.
 */
public class FlowingLayout implements LayoutManager {
  private int hgap = 10;
  private int vgap = 10;

  private Dimension preferred = new Dimension(100,100);

  public void addLayoutComponent(String name, Component comp) {
  }

  public void removeLayoutComponent(Component comp) {
  }

  public Dimension preferredLayoutSize(Container parent) {
    return preferred;
  }

  public Dimension minimumLayoutSize(Container parent) {
    return new Dimension(10,10);
  }

  public void layoutContainer(Container parent) {
    int width = (int) parent.getMaximumSize().getWidth();

    int x = hgap;
    int y = vgap;
    int maxHeight = 0;
    for(Component component : parent.getComponents()) {
      Dimension preferred = component.getPreferredSize();
      component.setSize(preferred);

      if(x + preferred.width + hgap > width || component instanceof NewLine) {
        x = hgap;
        y = y + maxHeight + vgap;
        maxHeight = 0;
      }
      if(component instanceof NewLine) continue;
      component.setLocation(x,y);
      x = x + preferred.width + hgap;
      maxHeight = Math.max(maxHeight, (int) preferred.getHeight());
    }

    preferred = new Dimension(width, y + maxHeight + vgap);
  }

  public static class NewLine extends Component {
    public NewLine() {
      setVisible(false);
    }
  }
}
