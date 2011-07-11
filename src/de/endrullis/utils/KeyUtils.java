package de.endrullis.utils;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class KeyUtils {
	public static final KeyStroke[] stopKeyStrokes = new KeyStroke[]{
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, KeyEvent.META_MASK)
	};

  public static boolean isStopKey(KeyEvent e) {
    return e.getKeyCode() == KeyEvent.VK_ESCAPE
            || (e.isMetaDown() && e.getKeyCode() == KeyEvent.VK_PERIOD);
  }
}
