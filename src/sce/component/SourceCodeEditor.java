/**
 * @author Jörg Endrullis
 */

package sce.component;

import jlatexeditor.syntaxhighlighting.LatexStyles;
import sce.syntaxhighlighting.BracketHighlighting;
import util.StreamUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

public class SourceCodeEditor<Rs extends AbstractResource> extends JPanel implements ActionListener {
  /**
   * The resource that has been opened in this editor.
   */
  private Rs resource = null;

  // The source code pane
  private SCEPane textPane = null;
  private JScrollPane scrollPane = null;
  private SCEMarkerBar markerBar = null;
  private SCESearch search = null;

  // diff
  private SCEDiff diff = null;

  public SourceCodeEditor(Rs resource) {
    this.resource = resource;

    // Change scoll bar colors to nice blue
    UIManager.put("ScrollBar.thumb", new Color(91, 135, 206));
    UIManager.put("ScrollBar.thumbShadow", new Color(10, 36, 106));
    UIManager.put("ScrollBar.thumbHighlight", new Color(166, 202, 240));

    // Create the TextPane
    textPane = new SCEPane(this);
    scrollPane = new JScrollPane(textPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    scrollPane.getVerticalScrollBar().setUnitIncrement(30);
    scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

    markerBar = new SCEMarkerBar(this);

    search = new SCESearch(this);

    setLayout(new BorderLayout());
    add(scrollPane, BorderLayout.CENTER);
    add(markerBar, BorderLayout.EAST);

    new BracketHighlighting(this);
  }

  /**
   * Returns the resource associated to this editor.
   *
   * @return resource
   */
  public Rs getResource() {
    return resource;
  }

  /**
   * Sets the resource associated with this editor.
   *
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
   *
   * @param resource resource
   */
  public void open(Rs resource) throws IOException {
    String text = makeEditorConform(resource.getContent());
    this.resource = resource;

    textPane.setText(text);
    textPane.getCaret().moveTo(0, 0, false);
    textPane.getUndoManager().clear();
    textPane.getDocument().setModified(false);

    openDiffIfConflicts();
  }

  public File getFile() {
    return new File(resource.toString());
  }

  public void reload() throws IOException {
    String text = makeEditorConform(resource.getContent());
    textPane.setText(text);
    textPane.getDocument().setModified(false);

    openDiffIfConflicts();
  }

  public boolean isDiffView() {
    return diff != null && diff.isVisible();
  }

  public void diffView(String title, String text) {
    if (!isDiffView()) {
      remove(scrollPane);

      diff = new SCEDiff(getResource().getName(), textPane, title, text, getMarkerBar());
      LatexStyles.addStyles(diff.getDiffPane().getDocument());
      add(diff, BorderLayout.CENTER);

      ImageButton buttonClose = markerBar.getButtonClose();
      buttonClose.addActionListener(this);
      buttonClose.setVisible(true);
    } else {
      diff.getDiffPane().setText(text);
      diff.updateDiff();
    }
    diff.updateLayout(true);
    markerBar.repaint();
    validate();
  }

  /**
   * Opens the diff view if SVN conflicts are found.
   */
  public void openDiffIfConflicts() {
    // check for conflict
    SCEDocumentRows rows = getTextPane().getDocument().getRowsModel();

    boolean hasOpening = false, hasMiddle = false, hasClosing = false;
    for(int rowNr = 0; rowNr < rows.getRowsCount(); rowNr++) {
      String prefix = rows.getRowAsString(rowNr, 0, 8);
      if(!hasOpening) if(prefix.equals("<<<<<<< ")) { hasOpening = true; } else continue;
      if(!hasMiddle) if(prefix.equals("=======")) { hasMiddle = true; } else continue;
      if(prefix.equals(">>>>>>> ")) { hasClosing = true; break; }
    }

    if(!hasOpening || !hasMiddle || !hasClosing) return;

    boolean inLeft = true; // outside of conflict or in left part
    boolean inRight = true; // outside of conflict or in right part
    boolean firstLeft = true;
    boolean firstRight = true;
    StringBuilder left = new StringBuilder();
    StringBuilder right = new StringBuilder();
    for(int rowNr = 0; rowNr < rows.getRowsCount(); rowNr++) {
      String line = rows.getRowAsString(rowNr);
      if(inLeft && inRight && line.startsWith("<<<<<<< ")) { inRight = false; continue; }
      if(!inRight && line.equals("=======")) { inLeft = false; inRight = true; continue; }
      if(!inLeft && line.startsWith(">>>>>>> ")) { inLeft = true; continue; }

      if(inLeft) {
        if(!firstLeft) left.append("\n");
        firstLeft = false;
        left.append(line);
      }

      if(inRight) {
        if(!firstRight) right.append("\n");
        firstRight = false;
        right.append(line);
      }
    }

    setText(left.toString());
    diffView("Remote Version with Conflicts", right.toString());
  }

  public void closeDiffView() {
    if (!isDiffView()) return;

    ImageButton buttonClose = markerBar.getButtonClose();
    buttonClose.removeActionListener(this);
    buttonClose.setVisible(false);

    remove(diff);

    diff.close();
    diff = null;

    scrollPane.setViewportView(textPane);
    add(scrollPane, BorderLayout.CENTER);

    textPane.removeAllRowHighlights();
  }

  public SCEDiff getDiffView() {
    return diff;
  }

  /**
   * Returns the sce.component.SCEPane.
   *
   * @return the text pane
   */
  public SCEPane getTextPane() {
    return textPane;
  }

  /**
   * Returns the current text of the SourceCodePane.
   *
   * @return the text/ source code
   */
  public String getText() {
    return textPane.getText();
  }

  /**
   * Sets the text of the SourceCodePane.
   *
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
   *
   * @param editable true, if the TextPane should be editable
   */
  public void setEditable(boolean editable) {
    textPane.getDocument().setEditable(editable);
  }

  public void moveTo(int row, int column) {
    Point pos = textPane.modelToView(row, column);
    scrollPane.scrollRectToVisible(new Rectangle(pos.x - 150, pos.y - 300, 300, pos.y - pos.y + 300));
    textPane.getCaret().moveTo(row, column, false);
  }

  private boolean hasDiffFocus() {
    return diff != null && diff.getDiffPane() != null && diff.getDiffPane().hasFocus();
  }

  public SCEPane getFocusedPane() {
    return hasDiffFocus() ? diff.getDiffPane() : textPane;
  }

  public int getVirtualLines() {
    if (isDiffView()) {
      return diff.getVirtualLines();
    } else {
      return textPane.getDocument().getRowsModel().getRowsCount();
    }
  }

  public void copy() {
    getFocusedPane().copy();
  }

  public void cut() {
    getFocusedPane().cut();
  }

  public void paste() {
    getFocusedPane().paste();
  }

  public void lineComment(String commentPrefix) {
    getFocusedPane().lineComment(commentPrefix);
  }

  public void lineUncomment(String[] commentPrefix) {
    getFocusedPane().lineUncomment(commentPrefix);
  }

  /**
   * Show search field.
   *
   * @param replace show replace or not
   */
  public void toggleSearch(boolean replace) {
	  if (search.hasFocus() && (replace == search.isShowReplace())) {
		  textPane.requestFocusInWindow();
	  } else {
		  SCEDocument document = textPane.getDocument();

		  if (!search.isVisible()) {
				// automatically check "selection only" if user has selected text

				search.getSelectionOnly().setSelected(document.isUserTriggeredSelection() && document.hasMultiLineSelection());
		  }

			add(search, BorderLayout.NORTH);
			search.setVisible(true);
			validate();
			search.focus();
	  }

	  search.setShowReplace(replace);
  }

  /**
   * Show replace field.
   */
  public void replace() {
    toggleSearch(true);
  }

  public void actionPerformed(ActionEvent e) {
    closeDiffView();
  }

  @Override
  public void requestFocus() {
    getFocusedPane().requestFocus();
	}

	/**
	 * Takes the properties from the last search to start a new search.
	 *
	 * @param lastSearch last search
	 */
	public void search(SCESearch lastSearch) {
		add(search, BorderLayout.NORTH);
		search.openSearch(lastSearch);
		search.getInputMap().put(KeyStroke.getKeyStroke("f3"), SCEPaneUI.Actions.FIND_NEXT);
		search.setActionMap(textPane.getActionMap());
		validate();
	}

	/**
	 * If this pane is added to a container.
	 */
	@Override
	public void addNotify() {
		super.addNotify();

		scrollPane.setInputMap(JComponent.WHEN_FOCUSED, new InputMap());
		scrollPane.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new InputMap());

		// install actions
		for (String action : SourceCodeEditorUI.globalActions) {
			getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("F3"), "find next");
			getActionMap().put(action, new SCEPaneUI.Actions(action, textPane));
		}

		/*
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("F3"), "foo");
		getActionMap().put("foo", new AbstractAction() {
		  @Override
		  public void actionPerformed(ActionEvent e) {
			  System.out.println("asd");
		  }
		});
		*/
	}

	public void dispose() {
		textPane.dispose();
	}
}
