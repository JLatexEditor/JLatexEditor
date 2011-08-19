package jlatexeditor.quickhelp;

import de.endrullis.utils.StringUtils;
import jlatexeditor.codehelper.CodePattern;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import sce.component.SCEDocument;
import sce.quickhelp.QuickHelp;
import util.StreamUtils;

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
  static final Pattern htmlComEnvPattern = Pattern.compile("<li>.*<a href=\"([^\"]+)\">(.*)</a>.*</li>");

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
  Hashtable<String, String> comEnvs = new Hashtable<String, String>();

  public LatexQuickHelp(String directory) {
    this.directory = directory;

	  HelpUrlHandler.register();

	  // Open the table of contents
    String toc_file = directory + "ltx-2.html";

	  comEnvs.put("<empty>", "empty.html");

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
        Matcher matcher = htmlComEnvPattern.matcher(line);
        if (matcher.find()) {
          String file = matcher.group(1);
          String command = matcher.group(2);

          // exclude some non commands
          if (!command.toLowerCase().equals(command)) continue;

          comEnvs.put(command, file);
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
  public Element findElement(int row, int column) {
    String line = document.getRowsModel().getRowAsString(row);

	  List<WordWithPos> groups = CodePattern.commandPattern.find(line, row, column);
	  if (groups != null) {
      return new Element(Element.Type.command, groups.get(0).word);
    }

	  groups = CodePattern.environmentPattern.find(line, row, column);
	  if (groups != null) {
      return new Element(Element.Type.environment, groups.get(1).word);
    }

    return null;
  }

  /**
   * Returns the path to the help file.
   *
   * @param element element to help on
   * @return help file
   */
  public String getHelpUrl(Element element) {
	  if (element == null) return null;

	  String fileName = null;
	  switch (element.type) {
		  case command:
			  fileName = comEnvs.get(element.name);
				break;
		  case environment:
			  fileName = comEnvs.get(element.name);
			  break;
	  }
    if (fileName == null) fileName = comEnvs.get("<empty>");
	  // if (fileName == null) return null;

    URL url = StreamUtils.getURL(directory + fileName);
    if (url == null) return null;

    return "help:" + url.toString() + "#" + element.getHelpCode();
  }

  public String getHelpUrlAt(int row, int column) {
    Element element = findElement(row, column);
    return getHelpUrl(element);
  }

	public void setDocument(SCEDocument document) {
    this.document = document;
  }

  /**
   * Returns an enumeration with all command and environment names extracted from the help pages.
   */
  public Enumeration<String> getComEnvs() {
    return comEnvs.keys();
  }

	public static class Element {
		public enum Type { command, environment, docclass, pack }

		public Type type;
		public String name;

		public Element(Type type, String name) {
			this.type = type;
			this.name = name;
		}

		public Element(String code) {
			this.type = Type.valueOf(StringUtils.stringBefore(code, ":").get());
			this.name = StringUtils.stringAfter(code, ":").get();
		}

		public String getHelpCode() {
			return type + ":" + name;
		}

		@Override
		public String toString() {
			return getHelpCode();
		}
	}
}
