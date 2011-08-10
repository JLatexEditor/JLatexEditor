package jlatexeditor.tools;

import jlatexeditor.JLatexEditorJFrame;
import jlatexeditor.gproperties.GProperties;
import jlatexeditor.gui.StatusBar;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

public class SVNView extends JPanel implements ActionListener {
  private JLatexEditorJFrame jle;
  private StatusBar statusBar;

  private JTree tree = new JTree();

  private boolean hadException = false;
  private CheckForUpdates updateChecker = new CheckForUpdates();

  public SVNView(JLatexEditorJFrame jle, StatusBar statusBar) {
    this.jle = jle;
    this.statusBar = statusBar;

    if (GProperties.getBoolean("check_for_svn_updates")) {
      updateChecker.start();
    }
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
    if (!new File(dir, ".svn").exists()) return;

    try {
      ArrayList<SVN.StatusResult> results = SVN.getInstance().status(dir);
      for (SVN.StatusResult result : results) {
        if (result.getServerStatus() == SVN.StatusResult.SERVER_OUTDATED) hasUpdates = true;
      }
    } catch (Exception e) {
      if (!hadException) e.printStackTrace();
      hadException = true;
    }

    statusBar.setUpdatesAvailableVisible(hasUpdates);
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
}
