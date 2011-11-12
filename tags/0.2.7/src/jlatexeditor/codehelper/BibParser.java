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
  public static ArrayList<BackgroundParser.FilePos<BibEntry>> parseBib(File file) {
    ArrayList<BackgroundParser.FilePos<BibEntry>> results = new ArrayList<BackgroundParser.FilePos<BibEntry>>();

    String bib;
    try {
      bib = StreamUtils.readFile(file.getAbsolutePath());
    } catch (IOException e) {
      return results;
    }

	  int lineNr = 0;
	  int lastAt = 0;
    int at = -1;
    while ((at = bib.indexOf("@", at + 1)) != -1) {
	    lineNr += countNewLinesBetween(bib, lastAt, at);

      int openBracket = bib.indexOf('{', at);
      if (openBracket == -1) break;
      String type = bib.substring(at + 1, openBracket).trim();

      // parse block
      String block = ParseUtil.parseBalanced(bib, openBracket + 1, '}');

      int comma = block.indexOf(',');
      if (comma == -1) continue;
      String name = block.substring(0, comma).trim();

      BibEntry entry = new BibEntry();
	    BackgroundParser.FilePos<BibEntry> filePos = new BackgroundParser.FilePos<BibEntry>(name, file.getAbsolutePath(), lineNr, entry);

      int index = comma + 1;
      while (index < block.length()) {
        String line = ParseUtil.parseBalanced(block, index, ',');
        index += line.length() + 1;

        int eq = line.indexOf('=');
        if (eq == -1) continue;

        String key = line.substring(0, eq).trim().toLowerCase();
        String value = removeBraces(line.substring(eq + 1).trim());

        if (key.equals("title")) entry.setTitle(value);
        if (key.equals("author")) entry.setAuthors(value);
        if (key.equals("year")) entry.setYear(value);
      }

      entry.setEntryName(name);
      entry.setText(
              entry.getEntryName() + " " +
                      entry.getTitle() + " " +
                      entry.getAuthors() + " " +
                      entry.getYear()
      );
      results.add(filePos);

	    lastAt = at;
    }

    return results;
  }

	private static int countNewLinesBetween(String bib, int start, int end) {
		int count = 0;
		for (int i = start; i < end; i++) {
			if (bib.charAt(i) == '\n') count++;
		}
		return count;
	}

	private static String removeBraces(String string) {
    if (string.startsWith("{") || string.startsWith("\"")) string = string.substring(1);
    if (string.endsWith("}") || string.endsWith("\"")) string = string.substring(0, string.length() - 1);
    return string;
  }
}
