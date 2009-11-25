
/**
 * @author Jörg Endrullis
 */

package sce.component;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SourceCodeEditor extends JPanel{
  // The file name
  private File file = null;

  // The source code pane
  private SCEPane textPane = null;
  private JScrollPane scrollPane = null;
  private SCEMarkerBar markerBar = null;
  private SCESearch search = null;

  public SourceCodeEditor(File file){
    this.file = file;

    // Change scoll bar colors to nice blue
    UIManager.put("ScrollBar.thumb", new Color(91, 135, 206));
    UIManager.put("ScrollBar.thumbShadow", new Color(10, 36, 106));
    UIManager.put("ScrollBar.thumbHighlight", new Color(166, 202, 240));

    // Create the TextPane
    textPane = new SCEPane();
    scrollPane = new JScrollPane(textPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    scrollPane.getVerticalScrollBar().setUnitIncrement(30);

    markerBar = new SCEMarkerBar(this);

    search = new SCESearch(this);

    setLayout(new BorderLayout());
    add(scrollPane, BorderLayout.CENTER);
    add(markerBar, BorderLayout.EAST);
  }

  /**
   * Returns the file associated to this editor.
   * @return file
   */
  public File getFile() {
    return file;
  }

  /**
   * Sets the file associated with this editor.
   * @param file file
   */
  public void setFile(File file) {
    this.file = file;
  }

  /**
   * Returns the sce.component.SCEPane.
   * @return the text pane
   */
  public SCEPane getTextPane(){
    return textPane;
  }

  /**
   * Returns the current text of the SourceCodePane.
   * @return the text/ source code
   */
  public String getText(){
    return textPane.getText();
  }

  /**
   * Returns the marker bar.
   */
  public SCEMarkerBar getMarkerBar() {
    return markerBar;
  }

  /**
   * Sets the marker bar.
   */
  public void setMarkerBar(SCEMarkerBar markerBar) {
    this.markerBar = markerBar;
  }

  /**
	 * Sets the text of the SourceCodePane.
	 * @param text text of the SourceCodePane
	 */
	public void setText(String text) {
		textPane.setText(text);
	}

	/**
   * Enable/ disable editing within the SourceCodePane.
   * @param editable true, if the TextPane should be editable
   */
  public void setEditable(boolean editable){
    textPane.setEditable(editable);
  }

  public void moveTo(int row, int column) {
    Point pos = textPane.modelToView(row, column);
    scrollPane.scrollRectToVisible(new Rectangle(pos.x-150, pos.y - 300, 300, pos.y - pos.y + 300));
    textPane.getCaret().moveTo(row, column);
  }

  /**
   * Show search field.
   */
  public void search() {
    add(search, BorderLayout.NORTH);
    search.setVisible(true);
    validate();
    search.focus();
  }
}
