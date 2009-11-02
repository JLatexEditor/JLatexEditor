
/**
 * @author Jörg Endrullis
 */

package editor.quickhelp;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;

public class QuickHelpPane extends JScrollPane implements HyperlinkListener, KeyListener{
  JEditorPane htmlPane = null;

  public QuickHelpPane(){
    htmlPane = new JEditorPane();
    htmlPane.addHyperlinkListener(this);
    htmlPane.setEditable(false);

    // add the component to the viewport
    JViewport viewPort = new JViewport();
    viewPort.add(htmlPane);
    setViewport(viewPort);

    addKeyListener(this);
    htmlPane.addKeyListener(this);
  }

  public void hyperlinkUpdate(HyperlinkEvent e){
    try{
      if(e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) htmlPane.setPage(e.getURL());
    } catch(IOException e1){ }
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

  // KeyListener methods
  public void keyTyped(KeyEvent e){
  }

  public void keyPressed(KeyEvent e){
    // hide on escape
    if(e.getKeyCode() == KeyEvent.VK_ESCAPE){
      setVisible(false);
      getParent().requestFocus();
    }
  }

  public void keyReleased(KeyEvent e){
  }
}
