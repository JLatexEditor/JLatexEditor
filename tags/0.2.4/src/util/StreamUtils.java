/** StreamUtils
 *
 * @author Jörg Endrullis, Stefan Endrullis
 * @version 1.1
 */

package util;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class StreamUtils {
  /** Size of the temporary buffer for reading. */
  private static final int TEMP_BUFFER_SIZE = 64 * 1024;

  /**
   * Reads an InputStream and returns its content as byte array.
   *
   * @param stream input stream
   * @return content of input stream as byte array
   * @throws java.io.IOException if an I/O error occurs
   */
  public static byte[] readBytesFromInputStream(InputStream stream) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    copyStream(stream, outputStream);
    stream.close();
    outputStream.close();

    return outputStream.toByteArray();
  }

  /**
   * Reads the contents of the file.
   *
   * @param filename the filename
   * @return the content of the file
   * @throws java.io.IOException if an I/O error occurs
   */
  public static String readFile(String filename) throws IOException {
    InputStream inputStream = getInputStream(filename);

    byte data[] = readBytesFromInputStream(inputStream);
    if (data == null) return null;

    return new String(data);
  }

  /**
   * Reads the contents of the file.
   *
   * @param filename the filename
   * @return the content of the file
   * @throws java.io.IOException if an I/O error occurs
   */
  public static ArrayList<String> readLines(String filename) throws IOException {
    InputStream inputStream = getInputStream(filename);

    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    ArrayList<String> lines = new ArrayList<String>();
    String line;
    while ((line = reader.readLine()) != null) lines.add(line);

    return lines;
  }


  /**
   * Opens an InputStream for the given resource name.
   *
   * @param fileName resource name / file name (does not have to start with a "/")
   * @return input stream of the file
   * @throws java.io.FileNotFoundException if file does not exist
   */
  public static InputStream getInputStream(String fileName) throws FileNotFoundException {
    // try to find the resource in local class resources
    InputStream localStream = StreamUtils.class.getResourceAsStream("/" + fileName);
    if (localStream != null) return new BufferedInputStream(localStream);

    //find file in local data directory
    //if (new java.io.File(fileName).exists()) {
    return new FileInputStream(fileName);
    //}

    // load the resource from web
    //return new URL(DATA_DIRECTORY_CLIENT + fileName).openStream();
  }

  public static URL getURL(String fileName) {
    // try to find the resource in local class resources
    URL url = StreamUtils.class.getResource("/" + fileName);
    if (url != null) return url;

    File file = new File(fileName);

    if (file.exists())
      try {
        return file.toURI().toURL();
      } catch (MalformedURLException e) {
        return null;
      }
    else
      return null;
  }

  /**
   * Copy the content of one stream to another.
   *
   * @param in  input stream
   * @param out output stream
   * @throws IOException thrown if reading or writing has failed
   */
  public static void copyStream(InputStream in, OutputStream out) throws IOException {
    byte[] bytes = new byte[TEMP_BUFFER_SIZE];
    int len;
    while ((len = in.read(bytes)) != -1) out.write(bytes, 0, len);
  }

  /**
   * Reads the specified InputStream and returns it as String in an encoding
   * found in the first 100 bytes defined by encoding="...".
   * If no encoding definition is found the default UTF-8 is used.
   *
   * @param stream input stream.
   * @return decoded xml string
   * @throws java.io.IOException if an I/O error occurs
   */
  public static String readXmlStream(InputStream stream) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    copyStream(stream, outputStream);
    stream.close();
    outputStream.close();

    // try to guess the encoding  todo evaluate also POST header
    String encoding = "UTF-8";
    int length = Math.min(outputStream.size(), 100);
    String beginning = new String(outputStream.toByteArray(), 0, length);
    if (beginning.contains("encoding=\"")) {
      int encodingStart = beginning.indexOf("encoding=\"") + 10;
      int encodingEnd = beginning.indexOf('"', encodingStart);
      if (encodingStart < encodingEnd) encoding = beginning.substring(encodingStart, encodingEnd);
    }

    if (encoding != null) {
      try {
        return outputStream.toString(encoding);
      } catch (UnsupportedEncodingException e) {
        System.err.println("Error in StreamUtils.readStringFromInputStream during encoding.");
        e.printStackTrace();
        return null;
      }
    } else {
      return outputStream.toString();
    }
	}
}
