/** StreamUtils
 *
 * @author Jörg Endrullis
 * @version 1.0
 */

package util;

import java.io.*;

public class StreamUtils{
  // größe des temporären Buffers zum Einlesen
  private static int temporary_buffer_size = 10000;

  /* den InputStream auslesen und als byte-Array zurückliefern */
  public static byte[] readBytesFromInputStream(InputStream stream){
    BufferedInputStream bufferedStream = new BufferedInputStream(stream, 10000);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // den temporären Buffer zum Einlesen der Daten anlegen
    byte temporary_buffer[] = new byte[temporary_buffer_size];

    // die Daten blockweise Einlesen
    try{
      int bytes_read = 0;
      while((bytes_read = bufferedStream.read(temporary_buffer)) != -1){
        if(bytes_read != 0) outputStream.write(temporary_buffer, 0, bytes_read);
      }
    } catch(IOException e){
      // beim Lesen ist ein Fehler aufgetreten
      System.out.println(e);
    }

    return outputStream.toByteArray();
  }

  /**
   * Reads the contents of the file.
   *
   * @param filename the filename
   * @return the content of the file
   */
  public static String readFile(String filename){
    FileInputStream fileStream = null;
    try {
      fileStream = new FileInputStream(filename);
    } catch (FileNotFoundException e) {
      return null;
    }

    byte data[] = readBytesFromInputStream(fileStream);
    if(data == null) return null;

    return new String(data);
  }
}
