package jlatexeditor;

import java.awt.*;
import java.util.HashMap;

/**
 * Global properties for the editor.
 */
public class GProperties {
  public static HashMap<String,Object> textAntialiasignMap = new HashMap<String, Object>();
  public static String[] textAntialiasignMapKeys = new String[] {"On", "Off", "GASP", "LCD HBGR", "LCD HRGB", "LCD VBGR", "LCD VRGB"};
  public static Object textAntialiasign = RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;

  static {
    textAntialiasignMap.put("On", RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    textAntialiasignMap.put("Off", RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    textAntialiasignMap.put("GASP", RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
    textAntialiasignMap.put("LCD HBGR", RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR);
    textAntialiasignMap.put("LCD HRGB", RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
    textAntialiasignMap.put("LCD VBGR", RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR);
    textAntialiasignMap.put("LCD VRGB", RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB);
  }


}
