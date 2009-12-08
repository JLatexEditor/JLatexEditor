package util.diff;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface for the gnu diff util.
 */
public class SystemDiff {
  public List<Modification> diff(String text1, String text2) {
    ArrayList<Modification> modifications = new ArrayList<Modification>();
    try {
      File file1 = File.createTempFile("diff", ".tex");
      file1.deleteOnExit();
      File file2 = File.createTempFile("diff", ".tex");
      file2.deleteOnExit();

      PrintWriter writer1 = new PrintWriter(new FileOutputStream(file1));
      writer1.write(text1);
      writer1.close();

      PrintWriter writer2 = new PrintWriter(new FileOutputStream(file2));
      writer2.write(text2);
      writer2.close();

      Process process = Runtime.getRuntime().exec(new String[]{
        "diff",
        file1.getCanonicalPath(),
        file2.getCanonicalPath()
      });
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

      String line;
      while((line = reader.readLine()) != null) {
        if(line.startsWith("<") || line.startsWith(">") || line.startsWith("---")) continue;

        int typePos = Math.max(line.indexOf('a'), Math.max(line.indexOf('d'), line.indexOf('c')));
        char type = line.charAt(typePos);
        String sourceLines = line.substring(0, typePos);
        String targetLines = line.substring(typePos+1);

        int sourceStart;
        int sourceLength;
        int sourceComma = sourceLines.indexOf(',');
        if(sourceComma != -1) {
          sourceStart = Integer.parseInt(sourceLines.substring(0, sourceComma));
          sourceLength = Integer.parseInt(sourceLines.substring(sourceComma+1)) - sourceStart + 1;
        } else {
          sourceStart = Integer.parseInt(sourceLines);
          sourceLength = type == 'a' ? 0 : 1;
        }

        int targetStart;
        int targetLength;
        int targetComma = targetLines.indexOf(',');
        if(targetComma != -1) {
          targetStart = Integer.parseInt(targetLines.substring(0, targetComma));
          targetLength = Integer.parseInt(targetLines.substring(targetComma+1)) - targetStart + 1;
        } else {
          targetStart = Integer.parseInt(targetLines);
          targetLength = type == 'd' ? 0 : 1;
        }

        modifications.add(new Modification(
                type == 'a' ? Modification.TYPE_ADD : (type == 'd' ? Modification.TYPE_REMOVE : Modification.TYPE_CHANGED),
                sourceStart, sourceLength, targetStart, targetLength));
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return modifications;
  }
}
