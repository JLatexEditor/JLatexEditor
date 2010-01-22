package jlatexeditor.codehelper;

import jlatexeditor.JLatexEditorJFrame;
import sce.component.AbstractResource;
import sce.component.SourceCodeEditor;
import util.ParseUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Parsing files in background.
 */
public class BackgroundParser extends Thread {
  private JLatexEditorJFrame jle;

  private long bibModified = 0;
  private ArrayList<BibEntry> bibEntries = new ArrayList<BibEntry>();

  public BackgroundParser(JLatexEditorJFrame jle) {
    this.jle = jle;
    setPriority(Thread.MIN_PRIORITY);
  }

  public void run() {
    while(true) {
      try {
        synchronized (this) { wait(1000); }
      } catch (InterruptedException e) { }

      SourceCodeEditor editor = jle.getMainEditor();
      AbstractResource resource = editor.getResource();
      if(!(resource instanceof JLatexEditorJFrame.FileDoc)) continue;

      String text= editor.getText();
      File file = ((JLatexEditorJFrame.FileDoc) resource).getFile();
      File directory = file.getParentFile();

      String bibCommand = "\\bibliography{";
      int bibStart = text.indexOf(bibCommand);
      if(bibStart != -1) {
        int bibClose = text.indexOf('}', bibStart + bibCommand.length());
        if(bibClose != -1) {
          parseBib(directory, text.substring(bibStart + bibCommand.length(), bibClose));
        }
      }
    }
  }

  private void parseBib(File directory, String fileName) {
    if(!fileName.endsWith(".bib")) fileName = fileName + ".bib";
    File bibFile = new File(directory, fileName);
    if(bibFile.lastModified() == bibModified) return;
    bibModified = bibFile.lastModified();

    bibEntries = BibParser.parseBib(bibFile);
  }

  public ArrayList<BibEntry> getBibEntries() {
    return bibEntries;
  }

  public ArrayList<BibEntry> getBibEntries(String search) {
    ArrayList<String> keys = ParseUtil.splitBySpace(search);
    ArrayList<BibEntry> entries = new ArrayList<BibEntry>(bibEntries);

    for(String key : keys) {
      Iterator<BibEntry> iterator = entries.iterator();
      while(iterator.hasNext()) {
        BibEntry entry = iterator.next();
        if(entry.getBlock().indexOf(key) == -1) iterator.remove();
      }
    }

    return bibEntries;
  }
}
