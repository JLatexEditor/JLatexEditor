/** StreamUtils
 *
 * @author Jörg Endrullis
 * @version 1.0
 */

package util;

import java.io.*;

public class StreamUtils{
  /** Size of the temporary buffer for reading. */
  private static int temporary_buffer_size = 10000;

  /** Reads an InputStream and returns it as byte array. */
  public static byte[] readBytesFromInputStream(InputStream stream){
    BufferedInputStream bufferedStream = new BufferedInputStream(stream, 10000);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // create temp buffer for reading
    byte temporary_buffer[] = new byte[temporary_buffer_size];

    // read input stream in blocks
    try{
      int bytes_read = 0;
      while((bytes_read = bufferedStream.read(temporary_buffer)) != -1){
        if(bytes_read != 0) outputStream.write(temporary_buffer, 0, bytes_read);
      }
    } catch(IOException e){
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
  public static String readFile(String filename) throws FileNotFoundException {
    InputStream inputStream = getDataInputStream(filename);

    byte data[] = readBytesFromInputStream(inputStream);
    if(data == null) return null;

    return new String(data);
  }

	/**
	 * Opens an InputStream for the given resource name.
	 */
	public static InputStream getDataInputStream(String fileName) throws FileNotFoundException {
	  // try to find the resource local in class resources
	  InputStream localStream = StreamUtils.class.getResourceAsStream("/" + fileName);
	  if(localStream != null) return new BufferedInputStream(localStream);

	  //find file in local data directory
	  //if (new java.io.File(fileName).exists()) {
		return new FileInputStream(fileName);
	  //}

	  // load the resource from web
	  //return new URL(DATA_DIRECTORY_CLIENT + fileName).openStream();
	}
}
