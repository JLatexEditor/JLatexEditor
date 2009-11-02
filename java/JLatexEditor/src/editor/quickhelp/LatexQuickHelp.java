
/**
 * @author Jörg Endrullis
 */

package editor.quickhelp;

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LatexQuickHelp implements QuickHelp{
  // quick help directory
  String directory = null;

  // quick help commands -> html file
  Hashtable commands = new Hashtable();

  public LatexQuickHelp(String directory){
    this.directory = directory;

    // Open the table of contents
    String toc_file = directory + "ltx-2.html";

    BufferedReader reader = null;
    try{
      reader = new BufferedReader(new FileReader(toc_file));
    } catch(FileNotFoundException e){
      System.out.println(e);
      return;
    }

    // Extract commands
    Pattern commandsPattern = Pattern.compile("<li>.*<a href=\"([^\"]+)\">(.*)</a>.*</li>");

    try{
      String line = null;
      while((line = reader.readLine()) != null){
        Matcher matcher = commandsPattern.matcher(line);
        if(matcher.find()){
          String file = matcher.group(1);
          String command = matcher.group(2);

          // exclude some non commands
          if(!command.toLowerCase().equals(command)) continue;

          commands.put(command, file);
        }
      }
    } catch(IOException e){
      System.out.println(e);
      return;
    }
  }

  /**
   * Returns the path for the help file.
   *
   * @param command the command
   * @return the help file
   */
  public String getHelpFileName(String command){
    String fileName = (String) commands.get(command);
    if(fileName == null) return null;

    try{
      return new File(directory + fileName).getCanonicalPath();
    } catch(IOException e){
      System.out.println("LatexQuickHelp: " + e);
      return null;
    }
  }

  /**
   * Returns a enumeration for all commands.
   */
  public Enumeration getCommands(){
    return commands.keys();
  }
}
