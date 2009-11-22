package jlatexeditor;

import jlatexeditor.errorhighlighting.LatexCompileError;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.ArrayList;

/**
 * Error view.
 */
public class ErrorView extends JSplitPane implements TreeSelectionListener {
  // color definitions
  private static final Color SELECTION_BACKGROUND = new Color(241, 244, 248);
  private static final Color SELECTION_BORDER = new Color(199, 213, 229);

  private JScrollPane outputScroll;

  private JTextArea latexOutput;
  private ArrayList<LatexCompileError> errors = new ArrayList<LatexCompileError>();

  private JTree tree;
  private DefaultMutableTreeNode nodeRoot, nodeError, nodeHbox, nodeWarning, nodeOutput;
  private DefaultListModel lmError = new DefaultListModel();
  private DefaultListModel lmHbox = new DefaultListModel();
  private DefaultListModel lmWarning = new DefaultListModel();
  private JList listError = new JList(lmError);
  private JList listHbox = new JList(lmHbox);
  private JList listWarning = new JList(lmWarning);

  public ErrorView() {
    latexOutput = new JTextArea();
    latexOutput.setFont(new Font("MonoSpaced", 0, 13));
    outputScroll = new JScrollPane(latexOutput);

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

    listError.setCellRenderer(new ErrorListCellRenderer());
    listError.setSelectionModel(new DefaultListSelectionModel());
    listError.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    listHbox.setCellRenderer(new ErrorListCellRenderer());
    listHbox.setSelectionModel(new DefaultListSelectionModel());
    listHbox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    listWarning.setCellRenderer(new ErrorListCellRenderer());
    listWarning.setSelectionModel(new DefaultListSelectionModel());
    listWarning.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    setLeftComponent(tree);
    setRightComponent(outputScroll);

    setResizeWeight(.2);
  }

  public void clear() {
    latexOutput.setText("");
    lmError.clear();
    lmHbox.clear();
    lmWarning.clear();
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

  public void setText(String text) {
    latexOutput.setText(text);
  }

  public String getText() {
    return latexOutput.getText();
  }

  public void valueChanged(TreeSelectionEvent e) {
    TreeNode node = (TreeNode) e.getNewLeadSelectionPath().getLastPathComponent();
    if (node == nodeError) {
      setRightComponent(listError);
      return;
    }
    if (node == nodeHbox) {
      setRightComponent(listHbox);
      return;
    }
    if (node == nodeWarning) {
      setRightComponent(listWarning);
      return;
    }
    setRightComponent(outputScroll);
  }

  private class ErrorComponent extends JLabel {
    private LatexCompileError error;
    private boolean isSelected = false;

    public ErrorComponent(LatexCompileError error) {
      this.error = error;
      setText("<html><pre>" + error.toString() + "</pre></html>");
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
}
