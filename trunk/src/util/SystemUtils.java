package util;

import java.io.File;
import java.util.regex.Pattern;

/**
 * SystemUtils includes some special system functions.
 */
public class SystemUtils {
  private static Pattern absoluteFile = Pattern.compile("(?:/)|(?:[A-Z]:)");

  /**
   * Return the name of the operating system.
   *
   * @return name of the operating system
   */
  public static String getOSName() {
    return System.getProperty("os.name");
  }

  /**
   * Returns if the operating system is a ms windows system.
   *
   * @return true if os is windows
   */
  public static boolean isWinOS() {
    return System.getProperty("os.name").toLowerCase().startsWith("win");
  }

  /**
   * Returns if the simple operating system type (windows/unix).
   *
   * @return windows or unix
   */
  public static String getSimpleOSType() {
    return isWinOS() ? "windows" : "unix";
  }

  public static File newFile(File parent, String fileName) {
    System.out.println(fileName + " ?? " + absoluteFile.matcher(fileName).matches());
    if(absoluteFile.matcher(fileName).matches()) return new File(fileName);
    return new File(parent, fileName);
  }
}
