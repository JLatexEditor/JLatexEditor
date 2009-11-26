package sce.quickhelp;

import sce.component.SCECaret;
import sce.component.SCEDocument;
import sce.component.SCEPane;
import sce.component.SourceCodeEditor;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;

/**
 * Window showing the help information for commands.
 *
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */
public class QuickHelpPane extends JScrollPane implements HyperlinkListener, KeyListener, PropertyChangeListener {
	// the source code pane
	SCEPane pane = null;
	SCEDocument document = null;
	SCECaret caret = null;

	/** Window like component showing the help information. */
  JPopupMenu popup = null;
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
    htmlPane.addPropertyChangeListener(this);

    // add the component to the viewport
    JViewport viewPort = new JViewport();
    viewPort.add(htmlPane);
    setViewport(viewPort);

    // popup menu
    popup = new JPopupMenu();
    popup.add(this);
    popup.setFocusable(false);

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

  public void setVisible(boolean visible){
    popup.setVisible(visible);
    if(visible) requestFocus();
  }

  private void showPopup() {
    Point caretPos = pane.modelToView(caret.getRow(), caret.getColumn());

    setVisible(true);
    popup.show(pane, caretPos.x, caretPos.y + pane.getLineHeight());

    setSize(getPreferredSize());
    popup.pack();
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
			  try{
			    String url = quickHelp.getHelpUrlAt(caret.getRow(), caret.getColumn());
			    if(url == null) return;

          htmlPane.setPage(url + "?" + Math.random());
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

  public void propertyChange(PropertyChangeEvent evt) {
    if(evt.getPropertyName().equals("page")) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          showPopup();
        }
      });
    }
  }
}
