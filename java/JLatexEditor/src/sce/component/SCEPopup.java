package sce.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Popup window to show options for code completion.
 *
 * @author Jörg Endrullis
 * @author Stefan Endrullis
 */
public class SCEPopup extends JScrollPane implements KeyListener {
  // the source code pane
  SCEPane pane = null;
  SCEDocument document = null;

  // the popup
  JPopupMenu popup = null;

  // the model
  private JList list = null;
  private DefaultListModel model = null;

  // the position of the last popup
	private int lastRow;
	private int lastColumn;

	public SCEPopup(SCEPane pane){
    this.pane = pane;
    document = pane.getDocument();

    // create the list
    list = new JList();
    list.setBackground(new Color(235, 244, 254));
    model = new DefaultListModel();
    list.setModel(model);

    // set colors
    list.setBackground(new Color(235, 244, 254));
    list.setForeground(Color.BLACK);
    list.setSelectionBackground(new Color(0, 82, 164));
    list.setSelectionForeground(Color.WHITE);

    // add the component to the viewport
    JViewport viewPort = new JViewport();
    viewPort.add(list);
    setViewport(viewPort);

    // popup menu
    popup = new JPopupMenu();
    popup.add(this);
    popup.setFocusable(false);

    // add listeners
    pane.addKeyListener(this);
  }

  public void setVisible(boolean visible){
    super.setVisible(visible);
    popup.setVisible(visible);
  }

  public boolean isVisible(){
    return popup.isVisible();
  }

	/**
   * Opens the popup at the current caret position.
   *
	 * @param newContent new content of the popup
   */
  public void openPopup(List<Callable> newContent){
		openPopup(pane.getCaret(), newContent);
  }

	/**
	 * Opens the popup at the given position.
   *
   * @param pos position of the popup
	 * @param newContent new content of the popup
   */
  public void openPopup(SCEPosition pos, List<Callable> newContent){
		setContent(newContent);
		showPopupAt(pos.getRow(), pos.getColumn());
  }

	/**
	 * Opens the popup at the given position.
   *
	 * @param row row of the popup position
	 * @param column column of the popup position
	 * @param newContent new content of the popup
   */
  public void openPopup(int row, int column, List<Callable> newContent){
		setContent(newContent);
		showPopupAt(row, column);
  }

	private void showPopupAt(int row, int column) {
		//if (lastRow == row && lastColumn == column && isVisible()) return;

	  Point caretPos = pane.modelToView(row, column);

	  setVisible(true);
	  popup.show(pane, caretPos.x, caretPos.y + pane.getLineHeight());

	  setSize(getPreferredSize());
	  popup.pack();

		lastRow = row;
		lastColumn = column;
	}

	private void setContent(List<Callable> newContent) {
		Object selectedValue = list.getSelectedValue();

		model.removeAllElements();
		for (Callable item : newContent) {
			model.addElement(item);
		}

		list.setSelectedValue(selectedValue, true);
		if(selectedValue == null || !model.contains(selectedValue)) list.setSelectedIndex(0);
	}

  public Dimension getPreferredSize(){
    Dimension dimension = list.getPreferredSize();

    dimension.width = Math.min(480, dimension.width + 30);
    dimension.height = Math.min(320, dimension.height + 5);

    return dimension;
  }

	public void destroy() {
		pane.removeKeyListener(this);
	}

  // KeyListener methods
  public void keyTyped(KeyEvent e){
  }

  public void keyPressed(KeyEvent e){
	  // continue only if the popup is visible
	  if(!isVisible()) return;

    // hide on escape
    if(e.getKeyCode() == KeyEvent.VK_ESCAPE){
      // hide popup
      setVisible(false);
    }

    // hide on cursor movement
    if(e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT){
	    // in future there could be sub menus that could be opened when pressing RIGHT
      setVisible(false);
    }

    // up and down
    if(e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN){
      int direction = e.getKeyCode() == KeyEvent.VK_UP ? -1 : 1;
      int size = model.getSize();
      int index = list.getSelectedIndex() + direction;
      if(index >= 0 && index < size){
        list.setSelectedIndex(index);
        Rectangle scrollRect = list.getCellBounds(Math.max(index-2, 0), Math.min(index+2, size-1));
        list.scrollRectToVisible(scrollRect);
      }

      e.consume();
    }
    // page up and down
    if(e.getKeyCode() == KeyEvent.VK_PAGE_UP || e.getKeyCode() == KeyEvent.VK_PAGE_DOWN){
      int direction = e.getKeyCode() == KeyEvent.VK_PAGE_UP ? -1 : 1;
      direction *= list.getLastVisibleIndex() - list.getFirstVisibleIndex();
      int size = model.getSize();
      int index = list.getSelectedIndex() + direction;
      if(index < 0) index = 0;
      if(index > size - 1) index = size - 1;
      list.setSelectedIndex(index);

      e.consume();
    }
    // enter
    if((e.getKeyCode() == KeyEvent.VK_ENTER || (e.getKeyCode() == KeyEvent.VK_SPACE) && !e.isControlDown())){
      if(model.size() == 0) return;

      // remove the current text and then start the template
      Callable callable = (Callable) list.getSelectedValue();
	    try {
		    callable.call();
	    } catch (Exception e1) {
		    e1.printStackTrace();
	    }
      e.consume();
    }
  }

  public void keyReleased(KeyEvent e){
  }
}
