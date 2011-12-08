package util.gui;

import javax.swing.*;

public class SCETabbedPane extends JTabbedPane {
  private CloseListener closeListener;

  public CloseListener getCloseListener() {
    return closeListener;
  }

  public void setCloseListener(CloseListener closeListener) {
    this.closeListener = closeListener;
  }

  public void informCloseListeners(int tabIndex) {
    if(closeListener == null) return;
    closeListener.close(tabIndex);
  }

  public static interface CloseListener {
    public void close(int tabIndex);
  }
}
