package util.diff;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface for the gnu diff util.
 */
public class SystemDiff {
  public static List<Modification> diff(String text1, String text2) {
    ArrayList<String> lines = new ArrayList<String>();
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
        "-w",
        file1.getCanonicalPath(),
        file2.getCanonicalPath()
      });

      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;
      while((line = reader.readLine()) != null) lines.add(line);
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return parse(lines);
  }

  public static List<Modification> parse(List<String> lines) {
    ArrayList<Modification> modifications = new ArrayList<Modification>();

    ArrayList<String> sourceLines = new ArrayList<String>();
    ArrayList<String> targetLines = new ArrayList<String>();
    for(int lineNr = 0; lineNr < lines.size(); lineNr++) {
      String line = lines.get(lineNr);

      int typePos = Math.max(line.indexOf('a'), Math.max(line.indexOf('d'), line.indexOf('c')));
      char type = line.charAt(typePos);
      String sourceRange = line.substring(0, typePos);
      String targetRange = line.substring(typePos+1);

      int sourceStart;
      int sourceLength;
      int sourceComma = sourceRange.indexOf(',');
      if(sourceComma != -1) {
        sourceStart = Integer.parseInt(sourceRange.substring(0, sourceComma));
        sourceLength = Integer.parseInt(sourceRange.substring(sourceComma+1)) - sourceStart + 1;
      } else {
        sourceStart = Integer.parseInt(sourceRange);
        sourceLength = type == 'a' ? 0 : 1;
      }

      int targetStart;
      int targetLength;
      int targetComma = targetRange.indexOf(',');
      if(targetComma != -1) {
        targetStart = Integer.parseInt(targetRange.substring(0, targetComma));
        targetLength = Integer.parseInt(targetRange.substring(targetComma+1)) - targetStart + 1;
      } else {
        targetStart = Integer.parseInt(targetRange);
        targetLength = type == 'd' ? 0 : 1;
      }
      if(type != 'a') sourceStart--;
      if(type != 'd') targetStart--;

      // parse < ... --- > ...
      boolean inSource = type != 'a';
      lineNr++;
      for(; lineNr < lines.size(); lineNr++) {
        line = lines.get(lineNr);
        if(line.startsWith("<") || line.startsWith(">")) {
          if(inSource) sourceLines.add(line.substring(2)); else targetLines.add(line.substring(2));
          continue;
        }
        if(line.startsWith("---")) {
          inSource = false;
          continue;
        }
        lineNr--;
        break;
      }

      // diff gives faulty output?
      if(sourceLength != sourceLines.size() || targetLength != targetLines.size()) {
        throw new RuntimeException("SystemDiff: I don't understand the diff output.");
      }

      modifications.add(new Modification<String>(
              type == 'a' ? Modification.TYPE_ADD : (type == 'd' ? Modification.TYPE_REMOVE : Modification.TYPE_CHANGED),
              sourceStart, sourceLines, targetStart, targetLines));
    }

    return modifications;
  }
}
