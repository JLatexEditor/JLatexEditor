
/**
 * @author Jörg Endrullis
 */

package qaqua;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class QaquaFrame extends JWindow implements FocusListener, ComponentListener{
  // the title
  private String title = null;
  private JMenuBar menuBar = null;

  // bug fix - we need a frame
  JFrame owner = null;
  JWindow window = this;

  // the content pane
  private JPanel contentPane = null;
  private QaquaFrameTitle titlePane = null;

  // maximize/ minimize the window
  private boolean isMaximized = false;
  private Rectangle normalWindowBounds = null;

  // close operation
  private int defaultCloseOperation = NOTHING_ON_CLOSE;
  public static final int NOTHING_ON_CLOSE = 0;
  public static final int EXIT_ON_CLOSE = 1;

  public QaquaFrame(JFrame owner){
    super(owner);

    this.owner = owner;

    // set the frame to ouside of the screen
    owner.setBounds(-1000, -1000, 0,0);
    owner.setVisible(true);

    // set the layout
    Container content = super.getContentPane();
    content.setLayout(null);

    titlePane = new QaquaFrameTitle(this);
    content.add(titlePane);
    contentPane= new JPanel();
    content.add(contentPane);

    // add Listener
    addFocusListener(this);
    addComponentListener(this);
  }

  public void addNotify(){
    super.addNotify();

    componentResized(null);
  }

  public Container getContentPane(){
    return contentPane;
  }

  /**
   * Returns the owner of this window (JFrame).
   *
   * @return the owner
   */
  public JFrame getJFrameOwner(){
    return owner;
  }

  /**
   * Returns the title of the window.
   *
   * @return the title
   */
  public String getTitle(){
    return owner.getTitle();
  }

  /**
   * Returns the default close operation.
   *
   * @return close operation
   */
  public int getDefaultCloseOperation(){
    return defaultCloseOperation;
  }

  public void setDefaultCloseOperation(int operation){
    defaultCloseOperation = operation;
  }

  /**
   * Returns true if the window is in maximized state.
   *
   * @return true if maximized
   */
  public boolean isMaximized(){
    return isMaximized;
  }

  public void setMaximized(boolean maximized){
    if(maximized == isMaximized) return;

    if(maximized){
      normalWindowBounds = window.getBounds();

      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      // real fullsceen window:
      // GraphicsDevice gd = ge.getDefaultScreenDevice();
      // GraphicsConfiguration gc = gd.getDefaultConfiguration();
      window.setBounds(ge.getMaximumWindowBounds());
    }else{
      window.setBounds(normalWindowBounds);
    }

    isMaximized = maximized;
  }

  /**
   * Returns the normal window bounds (if the window is not maximized).
   *
   * @return normal window bounds
   */
  public Rectangle getNormalWindowBounds(){
    return normalWindowBounds;
  }

  public void setNormalWindowBounds(Rectangle bounds){
    normalWindowBounds = bounds;
  }

  /**
   * Sets the menu bar for this window.
   *
   * @param mb menu bar
   */
  public void setJMenuBar(JMenuBar mb){
    this.menuBar = mb;

    getLayeredPane().add(menuBar, JLayeredPane.DEFAULT_LAYER);
  }

  // FocusListener methods
  public void focusGained(FocusEvent e){
    dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_ACTIVATED));
  }

  public void focusLost(FocusEvent e){
  }

  // ComponentListener methods
  public void componentResized(ComponentEvent e){
    // layout title
    titlePane.setBounds(0, 0, getWidth(), 23);
    // layout contentPane
    contentPane.setBounds(0, 23, getWidth(), getHeight() - 23);
    contentPane.updateUI();
    // layout menu bar
    if(menuBar != null){
      int x = titlePane.getTitleWidth();
      menuBar.setBounds(x, 0, getWidth() - x, 23);
    }
  }

  public void componentMoved(ComponentEvent e){
  }

  public void componentShown(ComponentEvent e){
  }

  public void componentHidden(ComponentEvent e){
  }
}
