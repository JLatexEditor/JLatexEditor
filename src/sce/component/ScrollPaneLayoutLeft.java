package sce.component;

import javax.swing.*;
import java.awt.*;

/**
 * ScrollPaneLayout with scroll bar on the left.
 *
 * @author Alex, http://forums.sun.com/thread.jspa?threadID=600107
 */
public class ScrollPaneLayoutLeft extends ScrollPaneLayout {
  public void layoutContainer(Container container) {
    super.layoutContainer(container);
    
    Rectangle viewport = this.viewport.getBounds();
		
    Insets insets = container.getInsets();
    int expectedWidth = container.getBounds().width - insets.left - insets.right;

    if (viewport.width != expectedWidth) {
      Rectangle verticalScrollBarBounds = vsb.getBounds();
      verticalScrollBarBounds.x = 0;
      vsb.setBounds(verticalScrollBarBounds);

      if (this.viewport != null) {
        viewport.x = viewport.x + verticalScrollBarBounds.width;
        this.viewport.setBounds(viewport);
      }

      Rectangle horizontalScrollBarBounds = hsb.getBounds();
      horizontalScrollBarBounds.x = horizontalScrollBarBounds.x + verticalScrollBarBounds.width;
      hsb.setBounds(horizontalScrollBarBounds);
    }
  }
}
