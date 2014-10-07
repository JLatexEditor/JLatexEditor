package jlatexeditor;

import jlatexeditor.errorhighlighting.LatexCompileError;
import sce.component.SCEPane;
import sce.component.SourceCodeEditor;
import util.StreamUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Error view.
 */
public class ErrorView extends JSplitPane implements TreeSelectionListener, ListSelectionListener, MouseListener, KeyListener {
  // color definitions
  private static final Color SELECTION_BACKGROUND = new Color(241, 244, 248);
  private static final Color SELECTION_BORDER = new Color(199, 213, 229);

  private JLatexEditorJFrame latexEditor;

  private ArrayList<LatexCompileError> errors = new ArrayList<LatexCompileError>();

  private JTextArea latexOutput = new JTextArea();
  private JScrollPane scrollOutput = new JScrollPane(latexOutput);

  private JTree tree;
  private ErrorTreeNode nodeRoot, nodeError, nodeHbox, nodeWarning, nodeWarningCitation, nodeWarningReference, nodeOutput;
  private ErrorListModel lmError = new ErrorListModel();
  private WarningListModel lmHbox = new WarningListModel();
  private WarningListModel lmWarning = new WarningListModel();
  private WarningListModel lmWarningCitation = new WarningListModel();
  private WarningListModel lmWarningReference = new WarningListModel();
  private JList listError = new JList(lmError);
  private JList listHbox = new JList(lmHbox);
  private JList listWarning = new JList(lmWarning);
  private JList listWarningCitation = new JList(lmWarningCitation);
  private JList listWarningReference = new JList(lmWarningReference);
  private JScrollPane scrollError = new JScrollPane(listError);
  private JScrollPane scrollHbox = new JScrollPane(listHbox);
  private JScrollPane scrollWarning = new JScrollPane(listWarning);
  private JScrollPane scrollWarningCitation = new JScrollPane(listWarningCitation);
  private JScrollPane scrollWarningReference = new JScrollPane(listWarningReference);

  private JLabel working;
  private JLabel stop;

	ErrorPopupMenu popup = new ErrorPopupMenu();

  public ErrorView(JLatexEditorJFrame latexEditor) {
    this.latexEditor = latexEditor;

    latexOutput.setFont(new Font("MonoSpaced", 0, 13));

	  AggregatedErrors aggregatedErrors = new AggregatedErrors(new ErrorLevelInterface[]{lmError, lmHbox, lmWarning, lmWarningCitation, lmWarningReference});
    nodeRoot = new ErrorTreeNode(aggregatedErrors, "compile...", "black");
    nodeError = new ErrorTreeNode(lmError, "error", "red");
    nodeRoot.add(nodeError);
    nodeHbox = new ErrorTreeNode(lmHbox, "overfull hboxes", "black");
    nodeRoot.add(nodeHbox);
    nodeWarning = new ErrorTreeNode(lmWarning, "warning", "black");
    nodeWarningCitation = new ErrorTreeNode(lmWarningCitation, "citations", "black");
    nodeWarningReference = new ErrorTreeNode(lmWarningReference, "references", "black");
    nodeRoot.add(nodeWarning);
    nodeRoot.add(nodeWarningCitation);
    nodeRoot.add(nodeWarningReference);
    nodeOutput = new ErrorTreeNode("output", "text-x-generic.png");
    nodeRoot.add(nodeOutput);
    update();
    tree = new JTree(nodeRoot);
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.getSelectionModel().addTreeSelectionListener(this);
    tree.setCellRenderer(new ErrorTreeCellRenderer());

    for (JList list : new JList[]{listError, listHbox, listWarning, listWarningCitation, listWarningReference}) {
      list.setCellRenderer(new ErrorListCellRenderer());
      DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
      selectionModel.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
      selectionModel.setLeadAnchorNotificationEnabled(false);
      list.setSelectionModel(selectionModel);
      list.addListSelectionListener(this);
	    list.addMouseListener(this);
	    list.addKeyListener(this);
    }

    JPanel treePanel = new JPanel();
    treePanel.setBackground(Color.WHITE);
    treePanel.setLayout(new BorderLayout());

    JPanel workingPanel = new JPanel();
    workingPanel.setBackground(Color.WHITE);
    workingPanel.setLayout(new FlowLayout());
	  working = new JLabel(new ImageIcon(getClass().getResource("/images/working32.gif")));
	  working.setVisible(false);
    stop = new JLabel("stop");
    stop.setBackground(new Color(255,128,128));
    stop.setOpaque(true);
    stop.addMouseListener(this);
    stop.setBorder(BorderFactory.createMatteBorder(3,3,3,3,new Color(255,128,128)));
    stop.setVisible(false);
    workingPanel.add(working);
    workingPanel.add(stop);

	  treePanel.add(workingPanel, BorderLayout.NORTH);
	  JScrollPane treeScrollPane = new JScrollPane(tree);
	  treeScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
	  treePanel.add(treeScrollPane, BorderLayout.CENTER);

    setLeftComponent(treePanel);
	  selectNode(nodeOutput);

    setResizeWeight(0);
  }

  public synchronized void clear() {
    latexOutput.setText("");
    lmError.clear();
    lmHbox.clear();
    lmWarning.clear();
    lmWarningCitation.clear();
    lmWarningReference.clear();
    errors.clear();
    update();
    repaint();
  }

  public synchronized void compileStarted(String text) {
    working.setText(text);
    working.setVisible(true);
    stop.setVisible(true);
  }

  public synchronized void compileFinished() {
    working.setVisible(false);
    stop.setVisible(false);
  }

  public synchronized void update() {
	  nodeError.update();
	  nodeHbox.update();
	  nodeWarning.update();
	  nodeWarningCitation.update();
	  nodeWarningReference.update();
  }

  public synchronized void appendLine(String line) {
    latexOutput.append(line);
    latexOutput.append("\n");
  }

	public synchronized int getLinesCount() {
		return latexOutput.getLineCount();
	}

  public synchronized void addError(final LatexCompileError error) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        errors.add(error);
        if (error.getType() == LatexCompileError.TYPE_ERROR) lmError.addElement(new ErrorComponent(error));
        if (error.getType() == LatexCompileError.TYPE_OVERFULL_HBOX) lmHbox.addElement(new ErrorComponent(error));
        if (error.getType() == LatexCompileError.TYPE_WARNING) {
          String message = error.getMessage().toLowerCase();
          if(message.contains("citation")) {
            lmWarningCitation.addElement(new ErrorComponent(error));
          } else
          if(message.contains("reference")) {
            lmWarningReference.addElement(new ErrorComponent(error));
          } else {
            lmWarning.addElement(new ErrorComponent(error));
          }
        }
        update();
        repaint();
      }
    });
  }

  public synchronized ArrayList<LatexCompileError> getErrors() {
    return (ArrayList<LatexCompileError>) errors.clone();
  }

  public synchronized void setText(String text) {
    latexOutput.setText(text);
  }

  public synchronized String getText() {
    return latexOutput.getText();
  }

  public synchronized void valueChanged(TreeSelectionEvent e) {
    TreeNode node = (TreeNode) e.getNewLeadSelectionPath().getLastPathComponent();
    listError.clearSelection();
    listHbox.clearSelection();
    listWarning.clearSelection();
    listWarningCitation.clearSelection();
    listWarningReference.clearSelection();
    if (node == nodeError) {
      setRightComponent(scrollError);
    }
    if (node == nodeHbox) {
      setRightComponent(scrollHbox);
    }
    if (node == nodeWarning) {
      setRightComponent(scrollWarning);
    }
    if (node == nodeWarningCitation) {
      setRightComponent(scrollWarningCitation);
    }
    if (node == nodeWarningReference) {
      setRightComponent(scrollWarningReference);
    }
    if (node == nodeOutput) {
      setRightComponent(scrollOutput);
    }
  }

	public synchronized void selectNode(ErrorTreeNode node) {
		Object[] path = {nodeRoot, node};
		if (node == nodeRoot) {
			path = new Object[]{nodeRoot};
		}

		tree.getSelectionModel().setSelectionPath(new TreePath(path));
	}

	@Override
  public void requestFocus() {
    tree.requestFocus();
    if (tree.getSelectionCount() == 0) {
      tree.setSelectionRow(1);
    }
  }

	private void listElementClicked(JList list, boolean focusEditor) {
		DefaultListModel model = (DefaultListModel) list.getModel();
		DefaultListSelectionModel selectionModel = (DefaultListSelectionModel) list.getSelectionModel();
		int index = selectionModel.getLeadSelectionIndex();
		if (index < 0 || index >= model.size()) return;

		ErrorComponent errorComponent = (ErrorComponent) model.getElementAt(index);
		LatexCompileError error = errorComponent.getError();

		SourceCodeEditor<Doc> editor = latexEditor.open(new Doc.FileDoc(error.getFile()));
		SCEPane pane = editor.getTextPane();

		int column = 0;
		if (error.getTextBefore() != null) {
		  String line = pane.getDocument().getRowsModel().getRowAsString(error.getLineStart()-1);

			int i = line.indexOf(error.getTextBefore());
			if (i >= 0) {
				column = i + error.getTextBefore().length();
			}
		}

		int rowStart = Math.max(0, error.getLineStart() - 1);
		editor.moveTo(rowStart, column);

		if (focusEditor) {
			editor.requestFocus();
		}
	}

	public void valueChanged(ListSelectionEvent e) {
	  JList list = (JList) e.getSource();

		listElementClicked(list, false);
	}
	
	// MouseListener
	public void mouseClicked(MouseEvent e) {
		JList list = (JList) e.getSource();

		listElementClicked(list, e.getClickCount() == 2);
	}

	public void mousePressed(MouseEvent e) {
    if (e.getSource() == stop) {
      latexEditor.stopCompile();
      compileFinished();
    }

		if (e.getButton() == 3) {
			if (e.getSource() instanceof JList) {
				JList list = (JList) e.getSource();
				int index = list.locationToIndex(e.getPoint());
        try {
          Object elementAt = list.getModel().getElementAt(index);
          if (elementAt instanceof ErrorComponent) {
            ErrorComponent ec = (ErrorComponent) elementAt;
            popup.show(list, ec.error, e.getX(), e.getY());
          }
        } catch(Exception ex) {
          // index sometimes fails to exist?
        }
			}
		}
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	// KeyListener
	public void keyTyped(KeyEvent e) {
	}

	public void keyPressed(KeyEvent e) {
		JList list = (JList) e.getSource();

		if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getModifiers() == 0) {
			listElementClicked(list, true);
		}
	}

	public void keyReleased(KeyEvent e) {
	}



	// inner classes
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
      if (isSelected) {
        g.setColor(SELECTION_BACKGROUND);
        g.fillRect(0, 0, getWidth(), getHeight());
      }
	    Graphics2D g2d = (Graphics2D) g;
	    if (error.isFollowUpError()) {
		    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.5f));
	    }
      super.paint(g);
	    if (error.isFollowUpError()) {
		    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
	    }

      if (isSelected) {
        g.setColor(SELECTION_BORDER);
        g.drawLine(0, 0, getWidth(), 0);
        g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
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
	  /*
	  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
	      DefaultMutableTreeNode currentTreeNode = (DefaultMutableTreeNode) value;
	      TreeEntry userObject = (TreeEntry) currentTreeNode
	              .getUserObject();
	      if (Vehicle.CAR.equals(userObject.getCategory())) {
	          setLeafIcon(CAR_ICON);
	      } else if (Vehicle.MOTO_BIKE.equals(userObject.getCategory())) {
	          setLeafIcon(MOTOBIKE_ICON);
	      }
	      return super.getTreeCellRendererComponent(tree, value, sel,
	              expanded, leaf, row, hasFocus);
	  }
	  */

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
	    ErrorTreeNode treeNode = (ErrorTreeNode) value;

	    ImageIcon icon = treeNode.getIcon();
			setLeafIcon(icon);
			setClosedIcon(icon);
	    setOpenIcon(icon);

      Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
      Dimension dimension = component.getPreferredSize();
      component.setPreferredSize(new Dimension(Math.max(dimension.width, 150), dimension.height));

      return component;
    }
  }

	public static class ErrorTreeNode extends DefaultMutableTreeNode {
		private final static ImageIcon ICON_CLEAR             = loadIcon("weather-clear.png");
		private final static ImageIcon ICON_FEW_CLOUDS        = loadIcon("weather-few-clouds.png");
		private final static ImageIcon ICON_OVERCASET         = loadIcon("weather-overcast.png");
		private final static ImageIcon ICON_SHOWERS_SCATTERED = loadIcon("weather-showers-scattered.png");
		private final static ImageIcon ICON_SHOWERS           = loadIcon("weather-showers.png");
		private final static ImageIcon ICON_STORM             = loadIcon("weather-storm.png");
		private final static ImageIcon ICON_SEVERE_ALERT      = loadIcon("weather-severe-alert.png");
		private final static ImageIcon[] icons = new ImageIcon[]{
			ICON_CLEAR, ICON_FEW_CLOUDS, ICON_OVERCASET, ICON_SHOWERS_SCATTERED, ICON_SHOWERS, ICON_STORM, ICON_SEVERE_ALERT
		};

		private static ImageIcon loadIcon(String filename) {
			try {
				return new ImageIcon(StreamUtils.readBytesFromInputStream(StreamUtils.getInputStream("data/icons/tango/" + filename)));
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		private ErrorLevelInterface lm;
		private String errorColor;
		private String label;
		private ImageIcon icon = null;

		public ErrorTreeNode(ErrorLevelInterface lm, String label, String errorColor) {
			super(label);
			this.lm = lm;
			this.errorColor = errorColor;
			this.label = label;
		}

		public ErrorTreeNode(Object userObject, String iconFile) {
			super(userObject);
			icon = loadIcon(iconFile);
		}

		public ImageIcon getIcon() {
			if (icon != null) return icon;

			return icons[lm.getErrorLevel()];
		}

		public void update() {
			setUserObject("<html><font color=\"" + (lm.getSize() > 0 ? errorColor : "gray") + "\">" + label + " (" + lm.getSize() + ")</font></html>");
		}
	}

	public static class WarningListModel extends DefaultListModel implements ErrorLevelInterface {
		private final static int[] maxErrors = {0, 10, Integer.MAX_VALUE};

		@Override
		public int getErrorLevel() {
			for (int i = 0; i < maxErrors.length; i++) {
				if (getSize() <= maxErrors[i]) {
					return i;
				}
			}
			return 0;
		}
	}
	public static class ErrorListModel extends DefaultListModel implements ErrorLevelInterface {
		private final static int[] maxErrors = {0, -1, -1, 3, 10, 99, Integer.MAX_VALUE};

		@Override
		public int getErrorLevel() {
			for (int i = 0; i < maxErrors.length; i++) {
				if (getSize() <= maxErrors[i]) {
					return i;
				}
			}
			return 0;
		}
	}
	public static class AggregatedErrors implements ErrorLevelInterface {
		private ErrorLevelInterface[] errorLevels;

		public AggregatedErrors(ErrorLevelInterface[] errorLevels) {
			this.errorLevels = errorLevels;
		}

		@Override
		public int getSize() {
			int sum = 0;
			for (ErrorLevelInterface size : errorLevels) {
				sum += size.getSize();
			}
			return sum;
		}

		public int getErrorLevel() {
			int maxErrorLevel = 0;
			for (ErrorLevelInterface errorLevel : errorLevels) {
				maxErrorLevel = Math.max(maxErrorLevel, errorLevel.getErrorLevel());
			}
			return maxErrorLevel;
		}
	}

	public static interface ErrorLevelInterface {
		public int getSize();
		public int getErrorLevel();
	}

	public class ErrorPopupMenu extends JPopupMenu {
		private JList jList;
		private LatexCompileError error;

		public ErrorPopupMenu() {
			add(new JMenuItem("View compiler output") {{
				addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							int line = error.getOutputLine();
							int start = latexOutput.getLineStartOffset(line);
							int end = latexOutput.getLineEndOffset(line);
							selectNode(nodeOutput);
							//setRightComponent(scrollOutput);
							latexOutput.scrollRectToVisible(new Rectangle(0, 30*latexOutput.getLineCount(), 0, 30*latexOutput.getLineCount()));
							latexOutput.setCaretPosition(start);
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}
				});
			}});
		}

		public void show(JList jList, LatexCompileError error, int x, int y) {
			this.jList = jList;
			this.error = error;
			show(jList, x, y);
		}
	}
}
