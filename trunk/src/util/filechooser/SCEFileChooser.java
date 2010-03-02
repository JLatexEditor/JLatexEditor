package util.filechooser;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.TimerTask;

public class SCEFileChooser extends JFileChooser {
  private JList list = null;

  public SCEFileChooser() {
    super();

    final Container component = this;
    addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        new java.util.Timer().schedule(
                new TimerTask() {
                  public void run() {
                    if (list == null) findList(component, 5);
                    if (list != null && list.isVisible()) {
                      list.requestFocus();
                      if (list.getSelectedIndex() < 0) list.setSelectedIndex(0);
                    }
                  }
                }, 100
        );
      }
    });
  }

  private void findList(Container component, int depth) {
    if (depth == 0) return;
    if (!component.isVisible()) return;
    if (list != null) return;

    for (Component c : component.getComponents()) {
      if (c instanceof JList) {
        list = (JList) c;
        return;
      }
      if (c instanceof Container) findList((Container) c, depth - 1);
    }
  }
}
