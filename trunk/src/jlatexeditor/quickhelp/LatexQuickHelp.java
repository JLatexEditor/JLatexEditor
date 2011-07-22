package jlatexeditor.quickhelp;

import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import sce.component.SCEDocument;
import sce.quickhelp.QuickHelp;
import util.ProcessUtil;
import util.StreamUtils;
import util.SystemUtils;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quick help for LaTeX.
 *
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */
public class LatexQuickHelp implements QuickHelp {
  /**
   * Logger.
   */
  private static Logger logger = Logger.getLogger(LatexQuickHelp.class.getName());

  /**
   * Regular expression to extract commands from HTML pages.
   */
  static final Pattern htmlCommandsPattern = Pattern.compile("<li>.*<a href=\"([^\"]+)\">(.*)</a>.*</li>");
	/**
	 * PatternPair to find command under cursor.
	 */
  static final PatternPair commandPattern = new PatternPair("(\\\\\\w*)", "(\\w*)");

  /**
   * Source code.
   */
  SCEDocument document = null;
  /**
   * Directory where help files are located.
   */
  String directory = null;

  /**
   * Quick help commands -> html file.
   */
  Hashtable<String, String> commands = new Hashtable<String, String>();

  public LatexQuickHelp(String directory) {
    this.directory = directory;

	  HelpUrlHandler.register();

	  // Open the table of contents
    String toc_file = directory + "ltx-2.html";

	  commands.put("<empty>", "empty.html");

    BufferedReader reader;
    try {
      reader = new BufferedReader(new InputStreamReader(StreamUtils.getInputStream(toc_file)));
    } catch (FileNotFoundException e) {
      logger.warning("TOC File " + toc_file + " not found. Quick help will not be available.");
      return;
    }

    // Extract commands
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        Matcher matcher = htmlCommandsPattern.matcher(line);
        if (matcher.find()) {
          String file = matcher.group(1);
          String command = matcher.group(2);

          // exclude some non commands
          if (!command.toLowerCase().equals(command)) continue;

          commands.put(command, file);
        }
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Parser error", e);
    }
  }

  /**
   * Searches for a command at the given position.
   *
   * @param row    the row
   * @param column the column
   * @return the command
   */
  public String findCommand(int row, int column) {
    String line = document.getRowsModel().getRowAsString(row);

	  List<WordWithPos> groups = commandPattern.find(line, row, column);

	  if (groups != null) {
      return groups.get(0).word;
    }

    return null;
  }

  /**
   * Returns the path for the help file.
   *
   * @param command the command
   * @return the help file
   */
  public String getHelpUrl(String command) {
	  if (command == null) return null;

    String fileName = commands.get(command);
    if (fileName == null) fileName = commands.get("<empty>");
	  // if (fileName == null) return null;

    URL url = StreamUtils.getURL(directory + fileName);
    if (url == null) return null;

    return "help:" + url.toString() + "#" + command;
  }

  public String getHelpUrlAt(int row, int column) {
    String command = findCommand(row, column);
    return getHelpUrl(command);
  }

	public void setDocument(SCEDocument document) {
    this.document = document;
  }

  /**
   * Returns a enumeration for all commands.
   */
  public Enumeration<String> getCommands() {
    return commands.keys();
  }
}
