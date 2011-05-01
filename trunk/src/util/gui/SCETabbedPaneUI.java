package util.gui;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class SCETabbedPaneUI extends BasicTabbedPaneUI implements MouseListener, MouseMotionListener {
  public static Color BLUE = new Color(173,206,249); // new Color(163, 201, 247);
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

  private int closeMouseOver = -1;
  private boolean displayCloseIcons;
  private ImageIcon closeIconActive;
  private ImageIcon closeIconActiveRollover;
  private ImageIcon closeIconInactive;
  private ImageIcon closeIconInactiveRollover;

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

      closeIconActive = new ImageIcon(getClass().getResource("/images/tabbedPane/tab_close_active.png"));
      closeIconActiveRollover = new ImageIcon(getClass().getResource("/images/tabbedPane/tab_close_active_rollover.png"));
      closeIconInactive = new ImageIcon(getClass().getResource("/images/tabbedPane/tab_close_inactive.png"));
      closeIconInactiveRollover = new ImageIcon(getClass().getResource("/images/tabbedPane/tab_close_inactive_rollover.png"));
    } catch (Throwable e) {
      e.printStackTrace();
    }

    tabbedPane.addMouseListener(this);
    tabbedPane.addMouseMotionListener(this);

    displayCloseIcons = tabbedPane instanceof SCETabbedPane;
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
    return width + 8 + (displayCloseIcons ? 12 : 0);
  }

  protected int getTabLabelShiftX(int tabPlacement, int tabIndex, boolean selected) {
    return (tabIndex == tabbedPane.getTabCount() - 1 ? -2  : 1)
            - (displayCloseIcons ? 6 : 0);
  }

  protected int getTabLabelShiftY(int tabPlacement, int tabIndex, boolean selected) {
    return selected ? -2 : 0;
  }

  protected Rectangle getCloseIconBounds(int tabIndex) {
    if(tabIndex >= tabbedPane.getTabCount()) return new Rectangle(0,0,0,0);

    boolean right = tabIndex == tabbedPane.getTabCount() - 1;
    boolean selected = tabIndex == tabbedPane.getSelectedIndex();

    Rectangle bounds = getTabBounds(tabbedPane, tabIndex);
    int x = bounds.x + bounds.width - (right ? 8 : 3) - closeIconActive.getIconWidth();
    int y = bounds.y + (bounds.height - closeIconActive.getIconHeight())/2 + (selected ? 0 : 2) - 1;
    return new Rectangle(x,y,closeIconActive.getIconWidth(),closeIconActive.getIconHeight());
  }

  protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean selected) {
    Rectangle area = new Rectangle(x, y, w, h);

    // background
    {
      ImageIcon image = selected ? image_active : image_inactive;
      g.drawImage(image.getImage(), area.x, area.y, area.x + area.width, area.y + area.height-BORDER_HEIGHT, 0, 0, image.getIconWidth(), image.getIconHeight(), null);
    }
    // left
    if (tabIndex == 0) {
      g.drawImage((selected ? image_active_left : image_inactive_left).getImage(), area.x, area.y, null);
    } else {
      boolean left_isSelected = tabbedPane.getSelectedIndex() == tabIndex - 1;
      ImageIcon image = selected ? image_inactive_active : (left_isSelected ? image_active_inactive : image_inactive_inactive);
      int hw = image.getIconWidth() / 2;
      g.drawImage(image.getImage(), area.x, area.y, area.x + hw, area.y + image.getIconHeight(), hw, 0, 2 * hw, image.getIconHeight(), null);
    }
    // right
    if (tabIndex == tabbedPane.getTabCount() - 1) {
      ImageIcon image = selected ? image_active_right : image_inactive_right;
      g.drawImage(image.getImage(), area.x + area.width - image.getIconWidth(), area.y, null);
    } else {
      boolean right_isSelected = tabbedPane.getSelectedIndex() == tabIndex + 1;
      ImageIcon image = selected ? image_active_inactive : (right_isSelected ? image_inactive_active : image_inactive_inactive);
      int hw = image.getIconWidth() / 2;
      g.drawImage(image.getImage(), area.x + area.width - hw, area.y, area.x + area.width, area.y + image.getIconHeight(), 0, 0, hw, image.getIconHeight(), null);
    }

    if(displayCloseIcons) {
      boolean over = tabIndex == closeMouseOver;
      Rectangle closeBounds = getCloseIconBounds(tabIndex);
      ImageIcon closeIcon = selected ?
              (over ? closeIconActiveRollover : closeIconActive) :
              (over ? closeIconInactiveRollover : closeIconInactive);
      g.drawImage(closeIcon.getImage(), closeBounds.x, closeBounds.y, null);
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

  /**
   * MouseListener.
   */
  public void mouseClicked(MouseEvent mouseEvent) {
    Point mouse = mouseEvent.getPoint();

    int over = getCloseTab(mouse);
    if(over != -1) {
      ((SCETabbedPane) tabbedPane).informCloseListeners(over);
      mouseEvent.consume();
      closeMouseOver = -1;
      tabbedPane.repaint();
    }
  }

  public void mousePressed(MouseEvent mouseEvent) {
    Point mouse = mouseEvent.getPoint();

    int over = getCloseTab(mouse);
    // TODO: consume is insufficient, when closing a non-selected tab it still gets selected
    if(over != -1) mouseEvent.consume();
  }


  public void mouseReleased(MouseEvent mouseEvent) {
  }

  public void mouseEntered(MouseEvent mouseEvent) {
  }

  public void mouseExited(MouseEvent mouseEvent) {
    if(closeMouseOver != -1) {
      tabbedPane.repaint(getCloseIconBounds(closeMouseOver));
      closeMouseOver = -1;
    }
  }

  /**
   * MouseMotionListener.
   */
  public void mouseDragged(MouseEvent mouseEvent) {
    mouseMoved(mouseEvent);
  }

  public void mouseMoved(MouseEvent mouseEvent) {
    Point mouse = mouseEvent.getPoint();

    int over = getCloseTab(mouse);
    if(over != closeMouseOver) {
      if(over >= 0) tabbedPane.repaint(getCloseIconBounds(over));
      if(closeMouseOver >= 0) tabbedPane.repaint(getCloseIconBounds(closeMouseOver));
      closeMouseOver = over;
    }
  }

  public int getCloseTab(Point point) {
    if(!displayCloseIcons) return -1;

    for(int tabIndex = 0; tabIndex < tabbedPane.getTabCount(); tabIndex++) {
      Rectangle area = getCloseIconBounds(tabIndex);
      if(area.contains(point)) {
        return tabIndex;
      }
    }
    return -1;
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
