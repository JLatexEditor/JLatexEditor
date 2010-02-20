package jlatexeditor;

import jlatexeditor.errorhighlighting.LatexCompileError;
import sce.component.SCEPane;
import sce.component.SourceCodeEditor;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.ArrayList;

/**
 * Error view.
 */
public class ErrorView extends JSplitPane implements TreeSelectionListener, ListSelectionListener {
  // color definitions
  private static final Color SELECTION_BACKGROUND = new Color(241, 244, 248);
  private static final Color SELECTION_BORDER = new Color(199, 213, 229);

  private JLatexEditorJFrame latexEditor;

  private ArrayList<LatexCompileError> errors = new ArrayList<LatexCompileError>();

  private JTextArea latexOutput = new JTextArea();
  private JScrollPane scrollOutput = new JScrollPane(latexOutput);

  private JTree tree;
  private DefaultMutableTreeNode nodeRoot, nodeError, nodeHbox, nodeWarning, nodeOutput;
  private DefaultListModel lmError = new DefaultListModel();
  private DefaultListModel lmHbox = new DefaultListModel();
  private DefaultListModel lmWarning = new DefaultListModel();
  private JList listError = new JList(lmError);
  private JList listHbox = new JList(lmHbox);
  private JList listWarning = new JList(lmWarning);
  private JScrollPane scrollError = new JScrollPane(listError);
  private JScrollPane scrollHbox = new JScrollPane(listHbox);
  private JScrollPane scrollWarning = new JScrollPane(listWarning);

  private JLabel working;

  public ErrorView(JLatexEditorJFrame latexEditor) {
    this.latexEditor = latexEditor;

    latexOutput.setFont(new Font("MonoSpaced", 0, 13));

    nodeRoot = new DefaultMutableTreeNode("compile...");
    nodeError = new DefaultMutableTreeNode();
    nodeRoot.add(nodeError);
    nodeHbox = new DefaultMutableTreeNode();
    nodeRoot.add(nodeHbox);
    nodeWarning = new DefaultMutableTreeNode();
    nodeRoot.add(nodeWarning);
    nodeOutput = new DefaultMutableTreeNode("output");
    nodeRoot.add(nodeOutput);
    update();
    tree = new JTree(nodeRoot);
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.getSelectionModel().addTreeSelectionListener(this);
    tree.setCellRenderer(new ErrorTreeCellRenderer());

    for(JList list : new JList[] {listError, listHbox, listWarning}) {
      list.setCellRenderer(new ErrorListCellRenderer());
      DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
      selectionModel.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
      selectionModel.setLeadAnchorNotificationEnabled(false);
      list.setSelectionModel(selectionModel);
      list.addListSelectionListener(this);
    }

    JPanel treePanel = new JPanel();
    treePanel.setBackground(Color.WHITE);
    treePanel.setLayout(new BorderLayout());
    treePanel.add(tree, BorderLayout.NORTH);

    working = new JLabel(new ImageIcon(getClass().getResource("/images/working32.gif")));
    working.setVisible(false);
    treePanel.add(working, BorderLayout.CENTER);

    setLeftComponent(treePanel);
    setRightComponent(scrollOutput);

    setResizeWeight(0);
  }

  public void clear() {
    latexOutput.setText("");
    lmError.clear();
    lmHbox.clear();
    lmWarning.clear();
    errors.clear();
    update();
    repaint();
  }

  public void compileStarted(String text) {
    working.setText(text);
    working.setVisible(true);
  }

  public void compileFinished() {
    working.setVisible(false);
  }
  
  public void update() {
    nodeError.setUserObject("<html><font color=\"" + (lmError.getSize() > 0 ? "red" : "gray") + "\">errors (" + lmError.getSize() + ")</font></html>");
    nodeHbox.setUserObject("<html><font color=\"" + (lmHbox.getSize() > 0 ? "black" : "gray") + "\">overfull hboxes (" + lmHbox.getSize() + ")</font></html>");
    nodeWarning.setUserObject("<html><font color=\"" + (lmWarning.getSize() > 0 ? "black" : "gray") + "\">warnings (" + lmWarning.getSize() + ")</font></html>");
  }

  public void appendLine(String line) {
    latexOutput.append(line);
    latexOutput.append("\n");
  }

  public void addError(LatexCompileError error) {
    errors.add(error);
    if (error.getType() == LatexCompileError.TYPE_ERROR) lmError.addElement(new ErrorComponent(error));
    if (error.getType() == LatexCompileError.TYPE_OVERFULL_HBOX) lmHbox.addElement(new ErrorComponent(error));
    if (error.getType() == LatexCompileError.TYPE_WARNING) lmWarning.addElement(new ErrorComponent(error));
    update();
    repaint();
  }

  public ArrayList<LatexCompileError> getErrors() {
    return (ArrayList<LatexCompileError>) errors.clone();
  }

  public void setText(String text) {
    latexOutput.setText(text);
  }

  public String getText() {
    return latexOutput.getText();
  }

  public void valueChanged(TreeSelectionEvent e) {
    TreeNode node = (TreeNode) e.getNewLeadSelectionPath().getLastPathComponent();
    listError.clearSelection();
    listHbox.clearSelection();
    listWarning.clearSelection();
    if (node == nodeError) { setRightComponent(scrollError); }
    if (node == nodeHbox) { setRightComponent(scrollHbox); }
    if (node == nodeWarning) { setRightComponent(scrollWarning); }
    if (node == nodeOutput) { setRightComponent(scrollOutput); }
  }

  public void valueChanged(ListSelectionEvent e) {
    JList list = (JList) e.getSource();
    DefaultListModel model = (DefaultListModel) list.getModel();
    DefaultListSelectionModel selectionModel = (DefaultListSelectionModel) list.getSelectionModel();
    int index = selectionModel.getLeadSelectionIndex();
    if(index < 0 || index >= model.size()) return;
    
    ErrorComponent errorComponent = (ErrorComponent) model.getElementAt(index);
    LatexCompileError error = errorComponent.getError();

    SourceCodeEditor editor = latexEditor.open(new Doc.FileDoc(error.getFile()));
    SCEPane pane = editor.getTextPane();

    int column = 0;
    if(error.getTextBefore() != null) {
      String line = pane.getDocument().getRow(error.getLineStart());
      column = Math.max(0, line.indexOf(error.getTextBefore()));
    }

    int rowStart = Math.max(0, error.getLineStart()-1);
    editor.moveTo(rowStart, column);
  }

	@Override
	public void requestFocus() {
		tree.requestFocus();
		if (tree.getSelectionCount() == 0) {
			tree.setSelectionRow(1);
		}
	}

	private class ErrorComponent extends JLabel {
    private LatexCompileError error;
    private boolean isSelected = false;

    public ErrorComponent(LatexCompileError error) {
      this.error = error;
      setText("<html><pre>" + error.toString() + "</pre></html>");
    }

    public LatexCompileError getError() {
      return error;
    }

    public boolean isSelected() {
      return isSelected;
    }

    public void setSelected(boolean selected) {
      isSelected = selected;
    }

    public void paint(Graphics g) {
      if(isSelected) {
        g.setColor(SELECTION_BACKGROUND);
        g.fillRect(0,0,getWidth(),getHeight());
      }
      super.paint(g);

      if(isSelected) {
        g.setColor(SELECTION_BORDER);
        g.drawLine(0,0,getWidth(),0);
        g.drawLine(0,getHeight()-1,getWidth(),getHeight()-1);
      }
    }
  }

  private class ErrorListCellRenderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      if (value instanceof JLabel) {
        ErrorComponent label = (ErrorComponent) value;
        label.setSelected(isSelected);
        return label;
      }

      return this;
    }
  }

  private class ErrorTreeCellRenderer extends DefaultTreeCellRenderer {
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
      Dimension dimension = component.getPreferredSize();
      component.setPreferredSize(new Dimension(Math.max(dimension.width,150),dimension.height));

      return component;
    }
  }
}
