
/**
 * @author Jörg Endrullis
 */

package editor.quickhelp;

import java.util.Enumeration;

public interface QuickHelp{
  public String getHelpFileName(String command);
  public Enumeration getCommands();
}
