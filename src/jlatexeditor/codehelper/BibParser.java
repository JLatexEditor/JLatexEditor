package jlatexeditor.codehelper;

import util.ParseUtil;
import util.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Bib parser.
 */
public class BibParser {
  public static ArrayList<BibEntry> parseBib(File file) {
    ArrayList<BibEntry> results = new ArrayList<BibEntry>();

    String bib;
    try {
      bib = StreamUtils.readFile(file.getAbsolutePath());
    } catch (IOException e) { return results; }

    int at = -1;
    while((at = bib.indexOf("@", at+1)) != -1) {
      int openBracket = bib.indexOf('{', at);
      if(openBracket == -1) break;
      String type = bib.substring(at+1, openBracket).trim();

      // parse block
      String block = ParseUtil.parseBalanced(bib, openBracket + 1, '}');

      int comma = block.indexOf(',');
      if(comma == -1) break;
      String name = block.substring(0, comma).trim();

      BibEntry entry = new BibEntry();

      int index = comma + 1;
      while(index < block.length()) {
        String line = ParseUtil.parseBalanced(block, index, ',');
        index += line.length()+1;

        int eq = line.indexOf('=');
        if(eq == -1) continue;

        String key = line.substring(0,eq).trim().toLowerCase();
        String value = removeBraces(line.substring(eq+1).trim());

        if(key.equals("title")) entry.setTitle(value);
        if(key.equals("author")) entry.setAuthors(value);
        if(key.equals("year")) entry.setYear(value);
      }

      entry.setEntryName(name);
      entry.setText(block);
      results.add(entry);
    }

    return results;
  }

  private static String removeBraces(String string) {
    if(string.startsWith("{") || string.startsWith("\"")) string = string.substring(1);
    if(string.endsWith("}") || string.endsWith("\"")) string = string.substring(0,string.length()-1);
    return string;
  }
}
