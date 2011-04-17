package util.gui;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;

public class SCETabbedPaneUI extends BasicTabbedPaneUI {
  public static Color BLUE = new Color(144,170,212); // new Color(163, 201, 247);
  public static Color BORDER = new Color(90, 110, 133);

  private int BORDER_HEIGHT = 4;

  private ImageIcon image_active_left;
  private ImageIcon image_active;
  private ImageIcon image_active_right;
  private ImageIcon image_inactive_left;
  private ImageIcon image_inactive;
  private ImageIcon image_inactive_right;
  private ImageIcon image_active_inactive;
  private ImageIcon image_inactive_active;
  private ImageIcon image_inactive_inactive;

  private JTabbedPane tabbedPane;

  public SCETabbedPaneUI(JTabbedPane tabbedPane) {
    this.tabbedPane = tabbedPane;
    UIManager.put("TabbedPane.contentAreaColor", Color.WHITE);
    UIManager.put("TabbedPane.background", new Color(238, 238, 238));
    UIManager.put("TabbedPane.tabAreaBackground", new Color(238, 238, 238));
    UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
    UIManager.put("TabbedPane.selectedTabPadInsets", new Insets(0, 0, 0, 0));
    UIManager.put("TabbedPane.tabAreaInsets", new Insets(0, 0, 0, 0));
    UIManager.put("TabbedPane.tabInsets", new Insets(0, 0, 0, 0));
    tabbedPane.setOpaque(true);

    try {
      image_active_left = new ImageIcon(getClass().getResource("/images/tabbedPane/tab_active_left.png"));
      image_active = new ImageIcon(getClass().getResource("/images/tabbedPane/tab_active.png"));
      image_active_right = new ImageIcon(getClass().getResource("/images/tabbedPane/tab_active_right.png"));
      image_inactive_left = new ImageIcon(getClass().getResource("/images/tabbedPane/tab_inactive_left.png"));
      image_inactive = new ImageIcon(getClass().getResource("/images/tabbedPane/tab_inactive.png"));
      image_inactive_right = new ImageIcon(getClass().getResource("/images/tabbedPane/tab_inactive_right.png"));
      image_active_inactive = new ImageIcon(getClass().getResource("/images/tabbedPane/tab_active_inactive.png"));
      image_inactive_active = new ImageIcon(getClass().getResource("/images/tabbedPane/tab_inactive_active.png"));
      image_inactive_inactive = new ImageIcon(getClass().getResource("/images/tabbedPane/tab_inactive_inactive.png"));
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  public static ComponentUI createUI(JComponent c) {
    return new SCETabbedPaneUI((JTabbedPane) c);
  }

  protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
    return image_active.getIconHeight() + BORDER_HEIGHT;
  }

  protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
    int width = super.calculateTabWidth(tabPlacement, tabIndex, metrics);
    if(tabIndex == 0) width += 3;
    if(tabIndex == tabbedPane.getTabCount() - 1) width += 5;
    return width + 10;
  }

  protected int getTabLabelShiftX(int tabPlacement, int tabIndex, boolean b) {
    return tabIndex == tabbedPane.getTabCount() - 1 ? -1  : 2;
  }

  protected int getTabLabelShiftY(int tabPlacement, int tabIndex, boolean b) {
    return b ? -2 : 0;
  }

  protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
    Rectangle area = new Rectangle(x, y, w, h);

    // background
    {
      ImageIcon image = isSelected ? image_active : image_inactive;
      g.drawImage(image.getImage(), area.x, area.y, area.x + area.width, area.y + area.height-BORDER_HEIGHT, 0, 0, image.getIconWidth(), image.getIconHeight(), null);
    }
    // left
    if (tabIndex == 0) {
      g.drawImage((isSelected ? image_active_left : image_inactive_left).getImage(), area.x, area.y, null);
    } else {
      boolean left_isSelected = tabbedPane.getSelectedIndex() == tabIndex - 1;
      ImageIcon image = isSelected ? image_inactive_active : (left_isSelected ? image_active_inactive : image_inactive_inactive);
      int hw = image.getIconWidth() / 2;
      g.drawImage(image.getImage(), area.x, area.y, area.x + hw, area.y + image.getIconHeight(), hw, 0, 2 * hw, image.getIconHeight(), null);
    }
    // right
    if (tabIndex == tabbedPane.getTabCount() - 1) {
      ImageIcon image = isSelected ? image_active_right : image_inactive_right;
      g.drawImage(image.getImage(), area.x + area.width - image.getIconWidth(), area.y, null);
    } else {
      boolean right_isSelected = tabbedPane.getSelectedIndex() == tabIndex + 1;
      ImageIcon image = isSelected ? image_active_inactive : (right_isSelected ? image_inactive_active : image_inactive_inactive);
      int hw = image.getIconWidth() / 2;
      g.drawImage(image.getImage(), area.x + area.width - hw, area.y, area.x + area.width, area.y + image.getIconHeight(), 0, 0, hw, image.getIconHeight(), null);
    }
  }

  protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
  }

  protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
  }

  protected void paintContentBorderBottomEdge(Graphics g, int tabPlacement, int selectedIndex, int x, int y, int w, int h) {
    Rectangle bounds  = getTabBounds(tabbedPane, selectedIndex);

    g.setColor(BLUE);
    g.fillRect(0, y - BORDER_HEIGHT, 2000, BORDER_HEIGHT);
    g.setColor(BORDER);
    g.drawLine(0, y - BORDER_HEIGHT, 2000, y - BORDER_HEIGHT);

    int sx = bounds.x;
    int swidth = bounds.width;
    if(selectedIndex == 0) { sx += 3; swidth -= 3; }
    if(selectedIndex == tabbedPane.getTabCount()-1) { swidth -= 5; }

    g.setColor(BLUE);
    g.drawLine(sx, y - BORDER_HEIGHT, sx + swidth, y - BORDER_HEIGHT);
  }

  protected void paintContentBorderTopEdge(Graphics g, int tabPlacement, int selectedIndex, int x, int y, int w, int h) {
  }

  protected void paintContentBorderLeftEdge(Graphics g, int tabPlacement, int selectedIndex, int x, int y, int w, int h) {
  }

  protected void paintContentBorderRightEdge(Graphics g, int tabPlacement, int selectedIndex, int x, int y, int w, int h) {
  }

  protected LayoutManager createLayoutManager() {
    return new TabbedPaneLayout();
  }

  public class TabbedPaneLayout extends BasicTabbedPaneUI.TabbedPaneLayout {
    public TabbedPaneLayout() {
      SCETabbedPaneUI.this.super();
    }

    protected void normalizeTabRuns(int tabPlacement, int tabCount,
                                    int start, int max) {
      if (tabPlacement == TOP || tabPlacement == BOTTOM) {
        super.normalizeTabRuns(tabPlacement, tabCount, start, max);
      }
    }

    protected void rotateTabRuns(int tabPlacement, int selectedRun) {
    }

    protected void padSelectedTab(int tabPlacement, int selectedIndex) {
    }
  }
}
