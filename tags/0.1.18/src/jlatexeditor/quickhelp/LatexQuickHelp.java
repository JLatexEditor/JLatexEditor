package jlatexeditor.quickhelp;

import sce.component.SCEDocument;
import sce.quickhelp.QuickHelp;
import util.StreamUtils;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
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
	/** Logger. */
	private static Logger logger = Logger.getLogger(LatexQuickHelp.class.getName());

	/** Regular expression to extract commands from HTML pages. */
	static final Pattern htmlCommandsPattern = Pattern.compile("<li>.*<a href=\"([^\"]+)\">(.*)</a>.*</li>");
	/** Command has to start with a backslash and may only contain letters. */
	static final Pattern commandStartPattern = Pattern.compile("(\\\\\\w*)$");
	/** End of a command may end with an arbitrary number of letters. */
	static final Pattern commandEndPattern = Pattern.compile("^(\\w*)");

	/** Source code. */
	SCEDocument document = null;
  /** Directory where help files are located. */
  String directory = null;

  /** Quick help commands -> html file. */
  Hashtable<String, String> commands = new Hashtable<String, String>();

	public LatexQuickHelp(String directory){
    this.directory = directory;

    // Open the table of contents
    String toc_file = directory + "ltx-2.html";

    BufferedReader reader;
    try{
      reader = new BufferedReader(new InputStreamReader(StreamUtils.getInputStream(toc_file)));
    } catch(FileNotFoundException e){
	    logger.warning("TOC File " + toc_file + " not found. Quick help will not be available.");
      return;
    }

    // Extract commands
		try{
      String line;
      while((line = reader.readLine()) != null){
        Matcher matcher = htmlCommandsPattern.matcher(line);
        if(matcher.find()){
          String file = matcher.group(1);
          String command = matcher.group(2);

          // exclude some non commands
          if(!command.toLowerCase().equals(command)) continue;

          commands.put(command, file);
        }
      }
    } catch(IOException e){
			logger.log(Level.SEVERE, "Parser error", e);
    }
  }

	/**
	 * Searches for a command at the given position.
	 *
	 * @param row the row
	 * @param column the column
	 * @return the command
	 */
	public String findCommand(int row, int column){
		String line = document.getRow(row);
		
		Matcher startMatcher = commandStartPattern.matcher(line.substring(0, column));
		Matcher endMatcher = commandEndPattern.matcher(line.substring(column, line.length()));

		if (startMatcher.find() && endMatcher.find()) {
			return startMatcher.group(1) + endMatcher.group(1);
		}

		return "";
	}

  /**
   * Returns the path for the help file.
   *
   * @param command the command
   * @return the help file
   */
  public String getHelpUrl(String command){
    String fileName = commands.get(command);
    if(fileName == null) return null;

	  URL url = StreamUtils.getURL(directory + fileName);
	  if (url == null) return null;

	  return url.toString();
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
  public Enumeration<String> getCommands(){
    return commands.keys();
  }
}
