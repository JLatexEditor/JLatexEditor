package jlatexeditor.gui;

import jlatexeditor.JLatexEditorJFrame;
import sce.component.SourceCodeEditor;
import util.diff.Modification;
import util.diff.SystemDiff;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Local history.
 */
public class LocalHistory extends JPanel implements ComponentListener {
  public static final String REVISION = "revision: ";

  private JLatexEditorJFrame latexEditor;

  private JList list;
  private DefaultListModel model;

  private String backup = null;

  public LocalHistory(JLatexEditorJFrame latexEditor) {
    this.latexEditor = latexEditor;

    setLayout(new BorderLayout());

    model = new DefaultListModel();
    list = new JList(model);
    add(list, BorderLayout.CENTER);

    addComponentListener(this);
  }

  public static File getHistoryDir(File file) {
    return new File(file.getParent(), ".jle/history");
  }

  public static File getBackupFile(File file) {
    return new File(getHistoryDir(file), file.getName());
  }

  public static File getRevisionsFile(File file) {
    return new File(getHistoryDir(file), file.getName() + ".rev");
  }

  public void update() {
    SourceCodeEditor editor = latexEditor.getActiveEditor();

    File file = editor.getFile();
    File file_backup = getBackupFile(file);
    File file_revisions = getRevisionsFile(file);

    model.clear();

    try {
      if(!file_backup.exists()) return;
      backup = SourceCodeEditor.readFile(file_backup.getCanonicalPath());

      model.add(0, new HistoryEntry(1, "a", new ArrayList<Modification>()));

      if(file_revisions.exists()) {
        int revisionNr = 2;
        String revision = null;
        ArrayList<String> diff = new ArrayList<String>();

        BufferedReader reader = new BufferedReader(new FileReader(file_revisions));
        String line;
        while((line = reader.readLine()) != null) {
          if(line.startsWith(REVISION)) {
            if(revision != null) {
              model.add(0, new HistoryEntry(revisionNr++, revision, SystemDiff.parse(diff)));
            }
            revision = line.substring(REVISION.length());
            diff.clear();
            continue;
          }
          diff.add(line);
        }
        reader.close();


      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
  }

  public void componentResized(ComponentEvent e) {
  }

  public void componentMoved(ComponentEvent e) {
  }

  public void componentShown(ComponentEvent e) {
    update();
  }

  public void componentHidden(ComponentEvent e) {
  }

  private static class HistoryEntry {
    private int revisionNr;
    private String revision;
    private List<Modification> diff;

    private HistoryEntry(int revisionNr, String revision, List<Modification> diff) {
      this.revisionNr = revisionNr;
      this.revision = revision;
      this.diff = diff;
    }

    public String getRevision() {
      return revision;
    }

    public List<Modification> getDiff() {
      return diff;
    }

    public String toString() {
      return "revision " + revisionNr + ": " + revision;
    }
  }
}
