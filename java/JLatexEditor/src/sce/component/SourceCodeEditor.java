
/**
 * @author Jörg Endrullis
 */

package sce.component;

import jlatexeditor.errorhighlighting.LatexErrorHighlighting;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SourceCodeEditor extends JPanel{
  // The file name
  private File file = null;

  // The source code pane
  private SCEPane textPane = null;
  private JScrollPane scrollPane = null;
  private LatexErrorHighlighting errorHighlighting = null;

  // The debug mode
  private boolean debugMode = false;
  private Color debugHighlightColor = new Color(200, 255, 200);
  private int debugLine = -1;
  private SCERowHighlight debugHighlight = null;

  // The error line
  private Color errorLineHighlightColor = new Color(255, 155, 155);
  private int errorLine = -1;
  private String errorText = null;
  private SCERowHighlight errorHighlight = null;

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

    setLayout(new BorderLayout());
    add(scrollPane, BorderLayout.CENTER);
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
   * Returns the error highlighter.
   * @return error highlighter
   */
  public LatexErrorHighlighting getErrorHighlighting() {
    return errorHighlighting;
  }

  /**
   * Sets the error highlighter.
   * @param errorHighlighting error highlighter
   */
  public void setErrorHighlighting(LatexErrorHighlighting errorHighlighting) {
    this.errorHighlighting = errorHighlighting;
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
   * Enable/ disable editing within the SourceCodePane.
   * @param editable true, if the TextPane should be editable
   */
  public void setEditable(boolean editable){
    textPane.setEditable(editable);
  }

  /**
   * Starts the debug mode -> current debug line will be highlighted.
   */
  public void startDebugMode(){
    debugMode = true;
    setEditable(false);
  }

  /**
   * Ends the debug mode -> debug line will no longer be highlighted.
   */
  public void endDebugMode(){
    debugMode = false;
    debugLine = -1;
    if(debugHighlight != null) textPane.removeRowHighlight(debugHighlight);

    setEditable(true);
  }

  /**
   * Updates the current debug line.
   * @param lineNr the line to go to
   */
  public void setDebugLine(int lineNr){
    if(lineNr == debugLine) return;
    debugLine = lineNr;

    if(debugHighlight != null) textPane.removeRowHighlight(debugHighlight);
    debugHighlight = new SCERowHighlight(textPane, debugLine + 1, debugHighlightColor);
    textPane.addRowHighlight(debugHighlight);
  }

  /**
   * Highlights the error line.
   *
   * @param lineNr the line number
   */
  public void setErrorLine(int lineNr){
    setErrorLine(lineNr, null);
  }

  /**
   * Highlights the error line and shows the reason as hint.
   *
   * @param lineNr the line number
   * @param text the hint
   */
  public void setErrorLine(int lineNr, String text){
    errorLine = lineNr + 1;
    errorText = text;

    if(errorHighlight != null) textPane.removeRowHighlight(errorHighlight);
    if(lineNr != -1){
      errorHighlight = new SCERowHighlight(textPane, errorLine, errorLineHighlightColor);
      textPane.addRowHighlight(errorHighlight);
    }
  }
}
