package jlatexeditor.quickhelp;

import sce.component.SCEDocument;
import sce.quickhelp.QuickHelp;

import java.io.*;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quick help for LaTeX.
 *
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */
public class LatexQuickHelp implements QuickHelp {
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
      reader = new BufferedReader(new FileReader(toc_file));
    } catch(FileNotFoundException e){
      System.out.println(e);
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
      e.printStackTrace();
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

		/*
	  int commandStart = document.get.findSplitterInRow(row, column, -1);
	  int commandEnd = pane.findSplitterInRow(row, column, 1);
	  if(column < documentRow.length && documentRow.chars[column].character == '\\') commandEnd = pane.findSplitterInRow(row, column+1, 1);
	  if(column < documentRow.length && documentRow.chars[column].character == ' ') commandEnd = column;
	  if(column > 0 && documentRow.chars[column - 1].character == ' ') commandStart = column;
	  if(commandStart > 0 && documentRow.chars[commandStart - 1].character == '\\') commandStart--;

	  return documentRow.toString().substring(commandStart, commandEnd);
	  */
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

	  File file = new File(directory + fileName);

	  if (file.exists())
		  try {
			  return file.toURI().toURL().toString();
		  } catch (MalformedURLException e) {
			  return null;
		  }
	  else
		  return null;
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
