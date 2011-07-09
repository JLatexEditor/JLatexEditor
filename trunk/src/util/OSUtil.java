package util;

/**
 * Util for detecting the operating system.
 */
public class OSUtil {
  public static final int OS_LINUX = 0;
  public static final int OS_MAC = 1;
  public static final int OS_WINDOWS = 2;

  private static int OS = OS_LINUX;
  static {
    String osName= System.getProperty("os.name");
    if(osName != null && osName.toLowerCase().indexOf("mac") >= 0) {
      OS = OS_MAC;
    }
    if(osName != null && osName.toLowerCase().indexOf("windows") >= 0) {
      OS = OS_WINDOWS;
    }
  }

  public static int getOS() {
    return OS;
  }
}
