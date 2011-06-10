package de.endrullis.utils;

import java.awt.event.KeyEvent;

public class KeyUtils {
  public static boolean isStopKey(KeyEvent e) {
    return e.getKeyCode() == KeyEvent.VK_ESCAPE
            || (e.isMetaDown() && e.getKeyCode() == KeyEvent.VK_PERIOD);
  }
}
