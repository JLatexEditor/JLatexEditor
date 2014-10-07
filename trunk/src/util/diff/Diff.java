package util.diff;

import util.ArrayUtil;
import util.diff.gnu.DiffPrint;
import util.diff.gnu.GnuDiff;
import util.diff.levenstein.Modification;
import util.diff.system.SystemDiff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Diff interface.
 */
public class Diff {
  public static String diffPlain(String text1, String text2) {
    String[] lines1 = ArrayUtil.toStringArray(lines(text1));
    String[] lines2 = ArrayUtil.toStringArray(lines(text2));

    return diffPlain(lines1, lines2);
  }

  public static String diffPlain(String[] lines1, String[] lines2) {
    GnuDiff diff = new GnuDiff(lines1, lines2);
    GnuDiff.change changeScript = diff.diff_2(false);
    DiffPrint.Base printer = new DiffPrint.NormalPrint(lines1,lines2);
    StringWriter stringWriter = new StringWriter();
    printer.setOutput(stringWriter);
    printer.print_script(changeScript);

    return stringWriter.toString();
  }

  public static List<Modification<String>> diff(String text1, String text2) {
    return SystemDiff.parse(lines(diffPlain(text1,text2)));
  }

  public static List<Modification<String>> diff(String[] lines1, String[] lines2) {
    return SystemDiff.parse(lines(diffPlain(lines1,lines2)));
  }

  public static ArrayList<String> lines(String text) {
    ArrayList<String> lines = new ArrayList<String>();

    try {
      BufferedReader reader = new BufferedReader(new StringReader(text));
      String line;
      while((line = reader.readLine()) != null) lines.add(line);
    } catch (IOException e) { }

    return lines;
  }
}
