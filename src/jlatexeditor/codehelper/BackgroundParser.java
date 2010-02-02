package jlatexeditor.codehelper;

import jlatexeditor.JLatexEditorJFrame;
import sce.component.AbstractResource;
import sce.component.SourceCodeEditor;
import util.ParseUtil;
import util.StreamUtils;
import util.Trie;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Parsing files in background.
 */
public class BackgroundParser extends Thread {
  private JLatexEditorJFrame jle;

  private long bibModified = 0;
  private ArrayList<BibEntry> bibEntries = new ArrayList<BibEntry>();

  private Trie commands = new Trie();
  private Trie labels = new Trie();

  public BackgroundParser(JLatexEditorJFrame jle) {
    this.jle = jle;
    setPriority(Thread.MIN_PRIORITY);
  }

  public ArrayList<BibEntry> getBibEntries() {
    return bibEntries;
  }

  public Trie getCommands() {
    return commands;
  }

  public Trie getLabels() {
    return labels;
  }

  public void run() {
    while(true) {
      SourceCodeEditor editor = jle.getMainEditor();
      AbstractResource resource = editor.getResource();
      if(!(resource instanceof JLatexEditorJFrame.FileDoc)) continue;

      File file = ((JLatexEditorJFrame.FileDoc) resource).getFile();
      File directory = file.getParentFile();

      Trie commands = new Trie();
      Trie labels = new Trie();
      parseTex(directory, file.getName(), commands, labels, new HashSet<String>());

      this.commands = commands;
      this.labels = labels;

      try {
        synchronized (this) { wait(); }
      } catch (InterruptedException e) { }
    }
  }

  public synchronized void parse() {
    notify();
  }

  private void parseTex(File directory, String fileName, Trie commands, Trie labels, HashSet<String> done) {
    if(done.contains(fileName)) return; else done.add(fileName);

    File file = new File(directory, fileName);
    if(!file.exists()) file = new File(directory, fileName + ".tex");
    if(!file.exists()) return;

    String tex;
    try {
      tex = StreamUtils.readFile(file.getAbsolutePath());
    } catch (IOException e) { return; }

    int index = 0;
    while(index < tex.length()) {
      char c = tex.charAt(index);

      // skip comments
      if(c == '%') {
        while(index < tex.length() && tex.charAt(index) != '\n') index++;
        index++;
        continue;
      }
      
      // starting of commands?
      if(c != '\\') { index++; continue; }
      index++;
      if(index >= tex.length()) return;
      if(!Character.isLetter(tex.charAt(index))) { index++; continue; }

      // find the end of the command
      int begin = index;
      index++;
      while(index < tex.length() && Character.isLetter(tex.charAt(index))) index++;

      // the command
      String command = tex.substring(begin, index);
      commands.add(command);

      // label, input, include
      if(command.equals("label") || command.equals("bibliography") || command.equals("input") || command.equals("include")) {
        int closing = tex.indexOf('}', index);
        if(closing == -1) continue;

        String argument = tex.substring(index+1,closing);
        if(command.equals("label")) {
          labels.add(argument);
        } else
        if(command.equals("bibliography")) {
          parseBib(directory, argument);
        } else {
          parseTex(directory, argument, commands, labels, done);
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

  public ArrayList<BibEntry> getBibEntries(String search) {
    ArrayList<String> keys = ParseUtil.splitBySpace(search);
    ArrayList<BibEntry> entries = new ArrayList<BibEntry>();

    for(BibEntry entry : bibEntries) {
      boolean all = true;
      for(String key : keys) {
        if(entry.getText().indexOf(key) == -1) { all = false; break; }
      }
      if(all) entries.add(entry);
    }

    return entries;
  }
}
