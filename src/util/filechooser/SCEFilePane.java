package util.filechooser;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.Timer;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import javax.swing.text.*;

import sun.awt.shell.*;
import sun.swing.FilePane;
import sun.swing.SwingUtilities2;

public class SCEFilePane extends FilePane {
  public SCEFilePane(FileChooserUIAccessor fileChooserUIAccessor) {
    super(fileChooserUIAccessor);

    final Container component = this;
    fileChooserUIAccessor.getFileChooser().addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        new Timer().schedule(
                new TimerTask() {
                  public void run() {
                    focus(component, 4);
                  }
                }, 100
        );
      }
    });
  }

  private void focus(Container component, int depth) {
    if(depth == 0) return;

    for(Component c : component.getComponents()) {
      if(c instanceof JList) {
        JList list = (JList) c;
        list.requestFocus();
        if(list.getSelectedIndex() < 0) list.setSelectedIndex(0);
      }
      if(c instanceof Container) focus((Container) c, depth-1);
    }
  }
}
