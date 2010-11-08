package util;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * System utilities.
 *
 * @author Stefan Endrullis
 * @version 1.3
 */
public class SystemUtils {
  /**
   * Executes a specified command.
   *
   * @param cmd specified command
   * @return output of the command
   * @throws IOException if an I/O error occurs
   * @throws InterruptedException if the current thread is interrupted by another thread
   *   while it is waiting for the completion of the command execution.
   */
  public static ArrayList<String> exec(String cmd) throws IOException, InterruptedException {
    Process process = Runtime.getRuntime().exec(cmd);

    return proceed(process, cmd);
  }

  /**
   * Executes a specified command.
   *
   * @param cmd specified command
   * @return output of the command
   * @throws IOException if an I/O error occurs
   * @throws InterruptedException if the current thread is interrupted by another thread
   *   while it is waiting for the completion of the command execution.
   */
  public static ArrayList<String> exec(String[] cmd) throws IOException, InterruptedException {
    Process process = Runtime.getRuntime().exec(cmd);

    String cmdString = cmd[0];
    for (int i = 0; i < cmd.length; i++) {
      cmdString += " \"" + cmd[i] + "\"";
    }

    return proceed(process, cmdString);
  }

  /**
   * Waits for the completion of the command execution and checks the return code.
   *
   * @param process process running the command
   * @param cmd command string
   * @return output of the command
   * @throws IOException if an I/O error occurs
   * @throws InterruptedException if the current thread is interrupted by another thread
   *   while it is waiting for the completion of the command execution.
   */
  private static ArrayList<String> proceed(Process process, String cmd) throws IOException, InterruptedException {
    InputStream in = process.getInputStream();
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));

    ArrayList<String> lines = new ArrayList<String>();
    String line;
    while((line = reader.readLine()) != null) lines.add(line);

    int returnCode = process.waitFor();
    if(returnCode != 0) {
      InputStream errIn = process.getErrorStream();
      BufferedReader errReader = new BufferedReader(new InputStreamReader(errIn));

      ArrayList<String> errLines = new ArrayList<String>();
      String errLine;
      while((errLine = errReader.readLine()) != null) errLines.add(errLine);

      throw new ExecutionFailedException(returnCode, "Execution of \"" + cmd + "\" failed!", errLines);
    }

    return lines;
  }

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
