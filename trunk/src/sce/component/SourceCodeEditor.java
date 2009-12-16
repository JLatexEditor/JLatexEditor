
/**
 * @author Jörg Endrullis
 */

package sce.component;

import jlatexeditor.syntaxhighlighting.LatexStyles;
import sce.syntaxhighlighting.BracketHighlighting;
import util.StreamUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class SourceCodeEditor<Rs extends AbstractResource> extends JPanel implements ActionListener {
  /** The resource that has been opened in this editor. */
	private Rs resource = null;

  // The source code pane
  private SCEPane textPane = null;
  private JScrollPane scrollPane = null;
  private SCEMarkerBar markerBar = null;
  private SCESearch search = null;

  // diff
  private SCEDiff diff = null;

  public SourceCodeEditor(Rs resource){
    this.resource = resource;

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
   * Returns the resource associated to this editor.
   * @return resource
   */
  public Rs getResource() {
    return resource;
  }

  /**
   * Sets the resource associated with this editor.
   * @param resource resource
   */
  public void setResource(Rs resource) {
    this.resource = resource;
  }

  public static String readFile(String fileName) throws IOException {
	  return makeEditorConform(StreamUtils.readFile(fileName));
  }

	private static String makeEditorConform(String text) {
    text = text.replaceAll("\n\r", "\n");
    text = text.replaceAll("\t", "  ");

    return text;
  }

  /**
   * Opens the given resource.
   * @param resource resource
   */
  public void open(Rs resource) throws IOException {
    String text = makeEditorConform(resource.getContent());
    this.resource = resource;

    textPane.setText(text);
    textPane.getCaret().moveTo(0,0);
    textPane.getUndoManager().clear();
    textPane.getDocument().setModified(false);
  }

  public File getFile() {
    return new File(resource.toString());
  }

  public void reload() throws IOException {
	  String text = makeEditorConform(resource.getContent());
    textPane.setText(text);
    textPane.getDocument().setModified(false);
  }

  public boolean isDiffView() {
    return diff != null && diff.isVisible();
  }

  public void diffView(String title, String text) {
    if(!isDiffView()) {
      remove(scrollPane);

      diff = new SCEDiff(getResource().getName(), textPane, title, text);
      LatexStyles.addStyles(diff.getDiffPane().getDocument());
      add(diff, BorderLayout.CENTER);

      ImageButton buttonClose = markerBar.getButtonClose();
      buttonClose.addActionListener(this);
      buttonClose.setVisible(true);
    } else {
      diff.getDiffPane().setText(text);
      diff.updateDiff();
    }
    diff.updateLayout();
  }

  public void closeDiffView() {
    if(!isDiffView()) return;

    ImageButton buttonClose = markerBar.getButtonClose();
    buttonClose.removeActionListener(this);
    buttonClose.setVisible(false);

    remove(diff);

    diff.close();
    diff = null;

    scrollPane.setViewportView(textPane);
    add(scrollPane, BorderLayout.CENTER);
  }

  public SCEDiff getDiffView() {
    return diff;
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
	 * Sets the text of the SourceCodePane.
	 * @param text text of the SourceCodePane
	 */
	public void setText(String text) {
		textPane.setText(text);
	}

  /**
   * Search.
   */
  public SCESearch getSearch() {
    return search;
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
   * Enable/ disable editing within the SourceCodePane.
   * @param editable true, if the TextPane should be editable
   */
  public void setEditable(boolean editable){
    textPane.getDocument().setEditable(editable);
  }

  public void moveTo(int row, int column) {
    Point pos = textPane.modelToView(row, column);
    scrollPane.scrollRectToVisible(new Rectangle(pos.x-150, pos.y - 300, 300, pos.y - pos.y + 300));
    textPane.getCaret().moveTo(row, column);
  }

  private boolean hasDiffFocus() {
    return diff != null && diff.getDiffPane() != null && diff.getDiffPane().hasFocus();
  }

  public void copy() {
    if(hasDiffFocus()) { diff.getDiffPane().copy(); } else { textPane.copy(); }
  }

  public void cut() {
    if(hasDiffFocus()) { diff.getDiffPane().cut(); } else { textPane.cut(); }
  }

  public void paste() {
    if(hasDiffFocus()) { diff.getDiffPane().paste(); } else { textPane.paste(); }
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

  public void actionPerformed(ActionEvent e) {
    closeDiffView();
  }
}
