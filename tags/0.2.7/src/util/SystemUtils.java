package util;

import java.io.*;
import java.util.regex.Pattern;

/**
 * SystemUtils includes some special system functions.
 */
public class SystemUtils {
  private static Pattern absoluteFile = Pattern.compile("^(?:/|[A-Z]:).*");

  /**
   * Return the name of the operating system.
   *
   * @return name of the operating system
   */
  public static String getOSName() {
    return System.getProperty("os.name").toLowerCase();
  }

  /**
   * Returns if the operating system is a ms windows system.
   *
   * @return true if os is windows
   */
  public static boolean isWinOS() {
    return getOSName().startsWith("win") || getOSName().contains("windows");
  }

  /**
   * Returns if the operating system is a mac osx.
   *
   * @return true if os is mac
   */
  public static boolean isMacOS() {
    return getOSName().contains("mac");
  }

  /**
   * Returns if the operating system is linux.
   *
   * @return true if os is linux
   */
  public static boolean isLinuxOS() {
    return !isWinOS() && !isMacOS();
  }

  /**
   * Returns if the simple operating system type (windows/unix).
   *
   * @return windows or unix
   */
  public static String getSimpleOSType() {
    return isWinOS() ? "windows" : "unix";
  }

	public static String getLinuxDistribution() throws IOException {
		Process process = Runtime.getRuntime().exec(new String[]{"lsb_release", "-i"});
		InputStream in = process.getInputStream();
		BufferedReader r = new BufferedReader(new InputStreamReader(in));
		try {
			String[] parts = r.readLine().split(":");
			return parts[1].trim();
		} catch (Exception e) {
			throw new IOException("lsb_release produced unexpected output");
		}
	}

  public static File newFile(File parent, String fileName) {
    if (absoluteFile.matcher(fileName).matches()) return new File(fileName);
    return new File(parent, fileName);
  }
}
