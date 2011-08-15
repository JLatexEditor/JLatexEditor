package jlatexeditor.tools;

import jlatexeditor.JLatexEditorJFrame;
import jlatexeditor.gproperties.GProperties;
import jlatexeditor.gui.StatusBar;
import sce.component.SCEDiff;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;

public class SVNView extends JPanel implements ActionListener {
  private JLatexEditorJFrame jle;
  private StatusBar statusBar;

  private Node root = new Node("Current Project", null);
  private DefaultTreeModel treeModel = new DefaultTreeModel(root);
  private JTree tree = new JTree(treeModel);

  private boolean hadException = false;
  private CheckForUpdates updateChecker = new CheckForUpdates();

  public SVNView(JLatexEditorJFrame jle, StatusBar statusBar) {
    this.jle = jle;
    this.statusBar = statusBar;

    if (GProperties.getBoolean("check_for_svn_updates")) {
      updateChecker.start();
    }

    GridBagLayout layout = new GridBagLayout();
    GridBagConstraints constraints = new GridBagConstraints();
    setLayout(layout);

    constraints.fill = GridBagConstraints.BOTH;
    constraints.insets = new Insets(0, 0, 0, 0);

    constraints.gridy = 0; constraints.gridx = 0;
    constraints.gridwidth = 2;
    constraints.weightx = 1; constraints.weighty = 1;
    add(new JScrollPane(tree), constraints);
    constraints.gridx = 1;

    tree.setCellRenderer(new Renderer());
  }

  public void actionPerformed(ActionEvent e) {
    checkForUpdates();
  }

  public void checkForUpdates() {
    updateChecker.check();
  }

  private synchronized void checkForUpdates_() {
    boolean hasUpdates = false;

    File file = jle.getActiveEditor().getFile();
    if (!file.exists()) return;
    File dir = file.getParentFile();

    root.setFile(dir);
    updateLocal(root, dir);
    tree.expandRow(0);

    if (!new File(dir, ".svn").exists()) return;

    try {
      ArrayList<SVN.StatusResult> results = SVN.getInstance().status(dir, true);
      for (SVN.StatusResult result : results) {
        String relativePath = result.getRelativePath();

        Node node = get(root, relativePath);
        if(node == null) {
          if(result.getServerStatus() == SVN.StatusResult.SERVER_UP_TO_DATE) continue;
          node = add(root, relativePath);
        }

        node.setSvnStatus(result);
        treeModel.nodeChanged(node);

        if (result.getServerStatus() == SVN.StatusResult.SERVER_OUTDATED) hasUpdates = true;
      }
    } catch (Exception e) {
      if (!hadException) e.printStackTrace();
      hadException = true;
    }

    statusBar.setUpdatesAvailableVisible(hasUpdates);
  }

  private Node get(Node node, String relativePath) {
    int delim = relativePath.indexOf('/');
    if(delim > 0) {
      String directoryName = relativePath.substring(0,delim);
      String pathRemainder = relativePath.substring(delim+1);

      int geIndex = getNodeGE(node, directoryName, true);
      Node ge = geIndex < node.getChildCount() ? (Node) node.getChildAt(geIndex) : null;

      if(ge != null && ge.getName().equals(directoryName)) return get(ge, pathRemainder);
    } else {
      int geIndex = getNodeGE(node, relativePath, false);
      Node ge = geIndex < node.getChildCount() ? (Node) node.getChildAt(geIndex) : null;

      if(ge.getName().equals(relativePath)) return ge;
    }

    return null;
  }

  private Node add(Node node, String relativePath) {
    int delim = relativePath.indexOf('/');
    if(delim > 0) {
      String directoryName = relativePath.substring(0,delim);
      String pathRemainder = relativePath.substring(delim+1);

      int geIndex = getNodeGE(node, directoryName, true);
      Node ge = geIndex < node.getChildCount() ? (Node) node.getChildAt(geIndex) : null;

      // insert directory if needed
      if(ge == null || !ge.getName().equals(directoryName)) {
        ge = new Node(new File(node.getFile(), directoryName));
        treeModel.insertNodeInto(ge, node, geIndex);
      }
      ge.setDirectory(true);

      return add(ge, pathRemainder);
    } else {
      int geIndex = getNodeGE(node, relativePath, false);
      Node ge = geIndex < node.getChildCount() ? (Node) node.getChildAt(geIndex) : null;

      // insert file if needed
      if(ge == null || !ge.getName().equals(relativePath)) {
        ge = new Node(new File(node.getFile(), relativePath));
        ge.setDirectory(false);
        treeModel.insertNodeInto(ge, node, geIndex);
      }

      return ge;
    }
  }

  /**
   * Returns the first child of the given parent that is larger than the given name.
   */
  private int getNodeGE(Node parent, String name, boolean isDirectory) {
    for(int childNr = 0; childNr < parent.getChildCount(); childNr++) {
      Node child = (Node) parent.getChildAt(childNr);
      boolean childIsDirectory = child.isDirectory();
      if(isDirectory && !childIsDirectory) return childNr;
      if(!isDirectory && childIsDirectory) continue;

      if(child.getName().compareTo(name) >= 0) return childNr;
    }

    return parent.getChildCount();
  }

  private HashSet<String> disallowedExtension = new HashSet<String>() {{
    add(".aux");
    add(".bbl");
    add(".log");
    add(".blg");
    add(".vtc");
    add(".ent");
    add(".out");
    add(".toc");
  }};

  private boolean nameFilter(String name) {
    if(name.startsWith(".")) return false;
    if(disallowedExtension.contains(name.substring(name.length()-4))) return false;
    if(name.contains(".synctex.gz")) return false;

    return true;
  }

  private void updateLocal(Node node, File dir) {
    File files[] = dir.listFiles();
    Arrays.sort(files, fileComparator);

    int childNr = 0;
    for(File file : files) {
      if(!nameFilter(file.getName())) continue;
      Node child = null;

      // prune tree
      while(childNr < node.getChildCount()) {
        child = (Node) node.getChildAt(childNr);
        if(fileComparator.compare(child.getFile(), file) >= 0) break;
        treeModel.removeNodeFromParent(child);
      }

      Node fileNode = null;
      if(child != null && child.getFile().equals(file)) {
        fileNode = child;
        fileNode.setFile(file);
      } else {
        fileNode = new Node(file);
        treeModel.insertNodeInto(fileNode, node, childNr);
      }

      if(file.isDirectory()) updateLocal(fileNode, file);
      childNr++;
    }
  }

  private class CheckForUpdates extends Thread {
    private CheckForUpdates() {
	    super("CheckForUpdates");
	    setDaemon(true);
      setPriority(Thread.MIN_PRIORITY);
    }

    public synchronized void check() {
      notifyAll();
    }

    public void run() {
	    try {
				while (!isInterrupted()) {
					checkForUpdates_();

					synchronized (this) {
						wait(GProperties.getInt("check_for_svn_updates.interval") * 1000);
					}
				}
	    } catch (InterruptedException ignored) {}
    }
  }

  public static class Node extends DefaultMutableTreeNode {
    private File file = null;
    private boolean isDirectory;
    private SVN.StatusResult svnStatus = null;

    private Node(String name, File file) {
      setFile(file);
      setUserObject(name);
    }

    private Node(File file) {
      setFile(file);
    }

    public String getName() {
      return (String) getUserObject();
    }

    public File getFile() {
      return file;
    }

    public void setFile(File file) {
      this.file = file;
      if(file != null) {
        this.isDirectory = file.isDirectory();
        setUserObject(file.getName());
      }
    }

    public boolean isDirectory() {
      return isDirectory;
    }

    public void setDirectory(boolean directory) {
      isDirectory = directory;
    }

    public SVN.StatusResult getSvnStatus() {
      return svnStatus;
    }

    public void setSvnStatus(SVN.StatusResult svnStatus) {
      this.svnStatus = svnStatus;
    }

    public boolean isConflict(boolean deep) {
      boolean isConflict =
              svnStatus != null
              && svnStatus.getLocalStatus() == SVN.StatusResult.LOCAL_CONFLICT;
      if(!deep || children == null) return isConflict;

      for(Object child : children) {
        isConflict = isConflict || ((Node) child).isConflict(deep);
      }
      return isConflict;
    }

    public boolean isLocallyModified(boolean deep) {
      int local = svnStatus != null ? svnStatus.getLocalStatus() : SVN.StatusResult.LOCAL_UNCHANGED;
      boolean isLocallyModified = local != SVN.StatusResult.LOCAL_UNCHANGED && local != SVN.StatusResult.LOCAL_NOT_SVN;
      if(!deep || children == null) return isLocallyModified;

      for(Object child : children) {
        isLocallyModified = isLocallyModified || ((Node) child).isLocallyModified(deep);
      }
      return isLocallyModified;
    }

    public boolean isNotInSVN() {
      return svnStatus != null
              && svnStatus.getLocalStatus() == SVN.StatusResult.LOCAL_NOT_SVN;
    }

    public boolean isServerModified(boolean deep) {
      boolean isServerModified =
              svnStatus != null
              && svnStatus.getServerStatus() != SVN.StatusResult.SERVER_UP_TO_DATE;
      if(!deep || children == null) return isServerModified;

      for(Object child : children) {
        isServerModified = isServerModified || ((Node) child).isServerModified(deep);
      }
      return isServerModified;
    }
  }

  public static FileComparator fileComparator = new FileComparator();
  public static class FileComparator implements Comparator<File> {
    public int compare(File entry1, File entry2) {
      if(entry1.isDirectory() && !entry2.isDirectory()) return -1;
      if(!entry1.isDirectory() && entry2.isDirectory()) return 1;

      return entry1.getName().compareTo(entry2.getName());
    }
  }

  public class Renderer extends DefaultTreeCellRenderer {
    private Node node = null;
    private ImageIcon lightning = new ImageIcon(Renderer.class.getResource("/images/icons/lightning_16.png"));

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      node = (Node) value;
      super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);    //To change body of overridden methods use File | Settings | File Templates.

      SVN.StatusResult status = node.getSvnStatus();
      if(status != null) {
        int local = status.getLocalStatus();
        if(local == SVN.StatusResult.LOCAL_CONFLICT) setIcon(lightning);
      }

      return this;
    }

    public void paint(Graphics g) {
      Graphics2D g2D = (Graphics2D) g;

      TreePath path = new TreePath(node.getPath());
      boolean expanded = tree.isExpanded(path);
      boolean deep = !expanded && node.getChildCount() > 0;
      boolean localModification = node.isLocallyModified(deep);
      boolean conflict = node.isConflict(deep);
      boolean notInSVN = node.isNotInSVN();
      boolean serverModification = node.isServerModified(deep);

      if(localModification || serverModification || conflict) {
        int middle = 16 + ((getWidth() - 16) /2 + 1);

        if(localModification || conflict) {
          g.setColor(conflict ? SCEDiff.COLOR_CHANGE : SCEDiff.COLOR_ADD);
          int x = 1;
          int width = serverModification ? middle : getWidth()-1;
          g.fillRect(x,1,width,getHeight()-1);
        }

        if(serverModification) {
          g.setColor(SCEDiff.COLOR_REMOVE);
          int width = conflict || localModification ? getWidth() - middle : getWidth()-1;
          int x = getWidth() - 1 - width;
          g.fillRect(x,1,width,getHeight()-1);
        }

        g.setColor(Color.GRAY);
        g.drawRect(0,1,getWidth()-1,getHeight()-2);
      }

      if(notInSVN) g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.3f));

      g.setColor(Color.BLACK);

      Icon icon = getIcon();
      icon.paintIcon(this, g2D, (16 - icon.getIconWidth())/2 ,2);
      g2D.drawString(getText(), 20, getHeight()-5);

      if(notInSVN) g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }
  }
}
