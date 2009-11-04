package editor.quickhelp;

import editor.component.SCECaret;
import editor.component.SCEDocument;
import editor.component.SCEPane;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;

/**
 * Window showing the help information for commands.
 *
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */
public class QuickHelpPane extends JScrollPane implements HyperlinkListener, KeyListener{
	// the source code pane
	SCEPane pane = null;
	SCEDocument document = null;
	SCECaret caret = null;

	/** Window like component showing the help information. */
	JEditorPane htmlPane = null;
	/** Quick help. */
	QuickHelp quickHelp;

	public QuickHelpPane(SCEPane pane){
	  this.pane = pane;
	  document = pane.getDocument();
	  caret = pane.getCaret();

    htmlPane = new JEditorPane();
    htmlPane.addHyperlinkListener(this);
    htmlPane.setEditable(false);

    // add the component to the viewport
    JViewport viewPort = new JViewport();
    viewPort.add(htmlPane);
    setViewport(viewPort);

		// add it to the pane (parent component)
		pane.add(this);

	  // add listeners
	  pane.addKeyListener(this);
    addKeyListener(this);
    htmlPane.addKeyListener(this);
  }

	public void setQuickHelp (QuickHelp quickHelp) {
		this.quickHelp = quickHelp;
		this.quickHelp.setDocument(document);
	}

  public void hyperlinkUpdate(HyperlinkEvent e){
    try{
      if(e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) htmlPane.setPage(e.getURL());
    } catch(IOException ignored){ }
  }

  public void setPage(String page) throws IOException{
    htmlPane.setPage(page);
  }

  public void setVisible(boolean visible){
    super.setVisible(visible);

    if(visible) requestFocus();
  }

  public Dimension getPreferredSize(){
    Dimension dimension = htmlPane.getPreferredSize();

    dimension.width = Math.min(480, dimension.width);
    dimension.height = Math.min(320, dimension.height);

    return dimension;
  }

  public void validate(){
    super.validate();
    setSize(getPreferredSize());
  }

	public void destroy() {
		pane.remove(this);
		pane.removeKeyListener(this);
	  removeKeyListener(this);
	  htmlPane.removeKeyListener(this);
	}
	
  // KeyListener methods
  public void keyTyped(KeyEvent e){
  }

  public void keyPressed(KeyEvent e){
	  Object src = e.getSource();

	  if (src == pane) {
		  if (e.getKeyCode() == KeyEvent.VK_Q && e.getModifiers() == KeyEvent.CTRL_MASK) {
			  int row = caret.getRow();
			  int column = caret.getColumn();

			  try{
			    Point caretPos = pane.modelToView(row, column);

			    String url = quickHelp.getHelpUrlAt(row, column);
			    if(url == null) return;

			    setPage(url);
			    setVisible(true);
			    setLocation(caretPos.x, caretPos.y + pane.getLineHeight());
			    setSize(getPreferredSize());
			  } catch(IOException e1){
			    System.out.println("SCEPaneUI: " + e1);
			  }
			  e.consume();
		  }
	  } else {
			// hide on escape
			if(e.getKeyCode() == KeyEvent.VK_ESCAPE){
				setVisible(false);
				getParent().requestFocus();
				e.consume();
			}
	  }
  }

  public void keyReleased(KeyEvent e){
  }
}
