package jlatexeditor.gui;

import jlatexeditor.JLatexEditorJFrame;
import sce.component.SourceCodeEditor;
import util.StreamUtils;
import util.diff.levenstein.Modification;
import util.diff.system.SystemDiff;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Local history.
 */
public class LocalHistory extends JPanel implements ComponentListener, ListSelectionListener {
  public static final String REVISION = "revision: ";

  private JLatexEditorJFrame latexEditor;

  private JList list;
  private DefaultListModel model;

  private ArrayList<String> backup = null;

  public LocalHistory(JLatexEditorJFrame latexEditor) {
    this.latexEditor = latexEditor;

    setLayout(new BorderLayout());

    model = new DefaultListModel();
    list = new JList(model);
    add(new JScrollPane(list), BorderLayout.CENTER);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.addListSelectionListener(this);

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
      if (!file_backup.exists()) return;
      backup = StreamUtils.readLines(file_backup.getCanonicalPath());

      if (file_revisions.exists()) {
        int revisionNr = 1;
        String revision = null;
        ArrayList<String> diff = new ArrayList<String>();

        BufferedReader reader = new BufferedReader(new FileReader(file_revisions));
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.startsWith(REVISION)) {
            if (revision != null) {
              model.add(0, new HistoryEntry(revisionNr++, revision, SystemDiff.parse(diff)));
            }
            revision = line.substring(REVISION.length());
            diff.clear();
            continue;
          }
          diff.add(line);
        }
        reader.close();

        model.add(0, new HistoryEntry(revisionNr, revision, new ArrayList<Modification<String>>()));
      }
    } catch (IOException e) {
      e.printStackTrace();
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

  public void valueChanged(ListSelectionEvent e) {
    if (list.getSelectedIndex() == -1) {
      latexEditor.getActiveEditor().closeDiffView();
      return;
    }

    List<String> document = (ArrayList<String>) backup.clone();
    for (int changeNr = 0; changeNr <= list.getSelectedIndex(); changeNr++) {
      HistoryEntry historyEntry = (HistoryEntry) model.getElementAt(changeNr);
      document = Modification.apply(document, historyEntry.getDiff());
    }
    HistoryEntry historyEntry = (HistoryEntry) model.getElementAt(list.getSelectedIndex());

    StringBuilder stringBuilder = new StringBuilder();
    for (String line : document) {
      stringBuilder.append(line).append('\n');
    }
    latexEditor.getActiveEditor().diffView(historyEntry.toString(), stringBuilder.toString());
  }

  @Override
  public void requestFocus() {
    list.requestFocus();
    System.out.println(list.getSelectedIndex());
    if (list.getSelectedIndex() == -1) {
      list.setSelectedIndex(0);
    }
  }

  private static class HistoryEntry {
    private int revisionNr;
    private String revision;
    private List<Modification<String>> diff;

    private HistoryEntry(int revisionNr, String revision, List<Modification<String>> diff) {
      this.revisionNr = revisionNr;
      this.revision = revision;
      this.diff = diff;
    }

    public String getRevision() {
      return revision;
    }

    public List<Modification<String>> getDiff() {
      return diff;
    }

    public String toString() {
      return "revision " + revisionNr + ": " + revision;
    }
  }
}
