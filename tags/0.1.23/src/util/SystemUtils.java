package util;

/**
 * SystemUtils includes some special system functions.
 */
public class SystemUtils {
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
}
