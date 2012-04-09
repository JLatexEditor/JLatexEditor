package util;

import java.io.*;

public class FileUtil {
  /**
   * Creates a temporary directory.
   */
  public static File createTempDirectory(String prefix) throws IOException {
    File file = File.createTempFile(prefix, Long.toString(System.nanoTime()));
    File dir = new File(file.getAbsolutePath() + ".d");
    dir.deleteOnExit();
    file.delete();

    if(!dir.mkdir()) {
      throw new IOException("Failed to create temporary directory: " + dir.getAbsolutePath());
    }

    return dir;
  }

  /**
   * Copies a file.
   */
  public static void copyFile(File in, File out) throws IOException {
    BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(in));
    BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(out));

    StreamUtils.copyStream(inStream, outStream);

    inStream.close();
    outStream.close();
  }
}
