package jlatexeditor.translation;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;
import util.Utf8ResourceBundle;

import java.util.ResourceBundle;

/**
 * I18n support for JLatexEditor.
 *
 * @author Stefan Endrullis
 */
public class I18n {
  @NonNls
  public static final ResourceBundle bundle = Utf8ResourceBundle.getBundle("jlatexeditor.translation.jlatexeditor");

  public static String getString(@PropertyKey(resourceBundle = "jlatexeditor.translation.jlatexeditor") String key, Object... params) {
    String value = bundle.getString(key);
    if (params.length == 0) {
      return value;
    } else {
      return String.format(value, params);
    }
  }

  public static char getMnemonic(@PropertyKey(resourceBundle = "jlatexeditor.translation.jlatexeditor") String key) {
    String value = getString(key);
    if (value == null) {
      return '!';
    }
    return value.charAt(0);
  }
}
