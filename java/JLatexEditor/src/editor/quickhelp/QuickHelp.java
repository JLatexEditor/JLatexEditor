package editor.quickhelp;

import editor.component.SCEDocument;

/**
 * Quick help interface.
 *
 * @author Jörg Endrullis
 * @author Stefan Endrullis 
 */
public interface QuickHelp{
  public String getHelpUrl(String command);
	public String getHelpUrlAt(int row, int column);
	public void setDocument(SCEDocument document);
}
