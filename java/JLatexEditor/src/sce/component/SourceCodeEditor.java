
/**
 * @author Jörg Endrullis
 */

package sce.component;

import sce.syntaxhighlighting.BracketHighlighting;
import util.StreamUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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

    new BracketHighlighting(this);
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

  public static String readFile(String fileName) throws IOException {
    FileInputStream fileInputStream = new FileInputStream(fileName);
    byte data[] = StreamUtils.readBytesFromInputStream(fileInputStream);
    fileInputStream.close();

    // Set the text contend
    String text = new String(data);
    text = text.replaceAll("\n\r", "\n");
    text = text.replaceAll("\t", "  ");

    return text;
  }

  /**
   * Opens the given file.
   * @param file file
   */
  public void open(File file) throws IOException {
    String text = readFile(file.getAbsolutePath());
    this.file = file;

    textPane.setText(text);
    textPane.getCaret().moveTo(0,0);
    textPane.getUndoManager().clear();
    textPane.getDocument().setModified(false);
  }

  public void reload() throws IOException {
    String text = readFile(file.getAbsolutePath());
    textPane.setText(text);
    textPane.getDocument().setModified(false);
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
