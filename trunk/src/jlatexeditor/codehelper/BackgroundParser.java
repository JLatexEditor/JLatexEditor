package jlatexeditor.codehelper;

import jlatexeditor.Doc;
import jlatexeditor.JLatexEditorJFrame;
import sce.component.AbstractResource;
import sce.component.SourceCodeEditor;
import util.ParseUtil;
import util.StreamUtils;
import util.Trie;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsing files in background.
 */
public class BackgroundParser extends Thread {
  private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-ZäöüÄÖÜß]*");
  private static final Pattern TODO_PATTERN = Pattern.compile("\\btodo\\b");
  private JLatexEditorJFrame jle;

  private long bibModified = 0;
  private ArrayList<BibEntry> bibEntries = new ArrayList<BibEntry>();

  private Trie<? extends Object> words = new Trie<Object>();
  private Trie<FilePos> commandNames = new Trie<FilePos>();
  private Trie<Command> commands = new Trie<Command>();
  private Trie<FilePos> labelDefs = new Trie<FilePos>();
	private Trie<FilePos> labelRefs = new Trie<FilePos>();
	private Trie<BibEntry> cites = new Trie<BibEntry>();

  private DefaultTreeModel structure = new DefaultTreeModel(new DefaultMutableTreeNode());

  private ArrayList<TODO> todos = new ArrayList<TODO>();

  public BackgroundParser(JLatexEditorJFrame jle) {
	  super("BackgroundParser");
    this.jle = jle;
    setPriority(Thread.MIN_PRIORITY);
  }

  public ArrayList<BibEntry> getBibEntries() {
    return bibEntries;
  }

	public Trie<? extends Object> getWords() {
    return words;
  }

  public Trie<FilePos> getCommandNames() {
    return commandNames;
  }

  public Trie<Command> getCommands() {
    return commands;
  }

  public Trie<FilePos> getLabelDefs() {
    return labelDefs;
  }

	public Trie<FilePos> getLabelRefs() {
		return labelRefs;
	}

	public Trie<BibEntry> getCites() {
		return cites;
	}

  public void run() {
    while (true) {
	    SourceCodeEditor<Doc> editor;
	    try {
        editor = jle.getMainEditor();
	    } catch (ArrayIndexOutOfBoundsException e) {
		    try {
			    sleep(1000);
		    } catch (InterruptedException e2) {
			    return;
		    }
		    continue;
	    }
      AbstractResource resource = editor.getResource();
      if (!(resource instanceof Doc.FileDoc)) continue;

      File file = ((Doc.FileDoc) resource).getFile();
      File directory = file.getParentFile();

	    Trie words = new Trie();
      Trie<FilePos> commandNames = new Trie<FilePos>();
      Trie<FilePos> labelDefs = new Trie<FilePos>();
      Trie<FilePos> labelRefs = new Trie<FilePos>();
      Trie<Command> commands = new Trie<Command>();

      ArrayList<StructureEntry> structureStack = new ArrayList<StructureEntry>();
      structureStack.add(new StructureEntry("Project", 0, resource.getName(), 0));

      parseTex(directory, file.getName(), words, commandNames, commands, labelDefs, labelRefs, structureStack, new HashSet<String>());

	    this.words = words;
      this.commandNames = commandNames;
      this.commands = commands;
      this.labelDefs = labelDefs;
      this.labelRefs = labelRefs;
      structure.setRoot(structureStack.get(0));

      try {
        synchronized (this) {
          wait();
        }
      } catch (InterruptedException ignored) {
      }
    }
  }

  public synchronized void parse() {
    notify();
  }

  private void parseTex(File directory, String fileName, Trie words, Trie<FilePos> commandNames, Trie<Command> commands,
                        Trie<FilePos> labelDefs, Trie<FilePos> labelRefs, ArrayList<StructureEntry> structure, HashSet<String> done) {
    if (done.contains(fileName)) return;
    else done.add(fileName);

    File file = new File(directory, fileName);
    if (!file.exists()) file = new File(directory, fileName + ".tex");
    if (!file.exists()) return;

    String tex;
    String fileCanonicalPath;
    try {
      tex = StreamUtils.readFile(file.getAbsolutePath());
      fileCanonicalPath = file.getCanonicalPath();
    } catch (IOException e) {
      return;
    }

    int index = 0;
    int line = 0;
    while (index < tex.length()) {
      char c = tex.charAt(index);

      // skip comments
      if (c == '%') {
        int startIndex = index;
        while (index < tex.length() && tex.charAt(index) != '\n') index++;
        line++;
        index++;
        String commentString = tex.substring(startIndex + 1, index - 1);
        Matcher matcher = TODO_PATTERN.matcher(commentString.toLowerCase());
        if (matcher.find()) {
          // found a "todo" -> extract message after todo (if available)
	        String todoMsg = null;
	        if (commentString.length() > matcher.start() + 5) {
            todoMsg = commentString.substring(matcher.start() + 5);
	        }
          todos.add(new TODO(todoMsg, file, line));
        }
        continue;
      }

      // newline?
      if (c == '\n') {
        line++;
      }

      // starting of commands?
      if (c != '\\') { index++; continue; }
      index++;
      if (index >= tex.length()) return;
      if (!Character.isLetter(tex.charAt(index))) { index++; continue; }

      // find the end of the command
      int begin = index;
      index++;
      while (index < tex.length() && Character.isLetter(tex.charAt(index))) index++;

      // the command
      String command = tex.substring(begin, index);
      commandNames.add(command);

      // newcommand
      if (command.equals("newcommand") || command.equals("renewcommand") || command.equals("DeclareRobustCommand")) {
        String name = ParseUtil.parseBalanced(tex, index+1, '}');
        index += 2 + name.length();
        // number of arguments
        int numberOfArgs = 0;
	      String optional = null, body = null;
	      try {
					if(tex.charAt(index) == '[') {
						try {
							String number = ParseUtil.parseBalanced(tex, index+1, ']');
							index += 2 + number.length();
							numberOfArgs = Integer.parseInt(number);
						} catch(NumberFormatException ignore) {}
					}
					// default argument
					if(tex.charAt(index) == '[') {
						try {
							optional = ParseUtil.parseBalanced(tex, index+1, ']');
							index += 2 + optional.length();
						} catch(NumberFormatException ignore) {}
					}
					body = ParseUtil.parseBalanced(tex, index+1, '}');
	      } catch (StringIndexOutOfBoundsException ignored) {
	      }
        name = name.substring(1);
        commands.add(name, new Command(name, fileCanonicalPath, line, numberOfArgs, optional, body));
      // label, input, include
      } else if (command.equals("label") || command.equals("bibliography") || command.equals("input") || command.equals("include") ||
	               command.equals("ref")) {
        String argument = ParseUtil.parseBalanced(tex, index+1, '}');

        if (command.equals("label")) {
          labelDefs.add(argument, new FilePos(argument, fileCanonicalPath, line));
        } else if (command.equals("ref")) {
          labelRefs.add(argument, new FilePos(argument, fileCanonicalPath, line));
        } else if (command.equals("bibliography")) {
          parseBib(directory, Command.unfoldRecursive(argument, commands, 10));
        } else {
          parseTex(directory, Command.unfoldRecursive(argument, commands, 10), words, commandNames, commands, labelDefs, labelRefs, structure, done);
        }
      // sections
      } else if (command.equals("chapter") || command.equals("section") || command.equals("subsection") || command.equals("subsubsection")) {
        int depth = 0;
        if (command.equals("chapter")) depth = 1;
        if (command.equals("section")) depth = 2;
        else if (command.equals("subsection")) depth = 3;
        else if (command.equals("subsubsection")) depth = 4;

        // skip*
        if(tex.charAt(index) == '*') index++;

        String name = "";
        if (depth <= 4) {
          name = ParseUtil.parseBalanced(tex, index+1, tex.charAt(index) == '[' ? ']' : '}');
        }

        while (structure.get(structure.size() - 1).getDepth() >= depth) structure.remove(structure.size() - 1);

        StructureEntry parent = structure.get(structure.size() - 1);
        StructureEntry entry = new StructureEntry(name, depth, fileCanonicalPath, line);
        parent.add(entry);
        structure.add(entry);
      }
    }

    // collect words
    Matcher matcher = WORD_PATTERN.matcher(tex);
    while (matcher.find()) {
      words.add(matcher.group());
    }
  }

  private void parseBib(File directory, String fileName) {
    if (!fileName.endsWith(".bib")) fileName = fileName + ".bib";
    File bibFile = new File(directory, fileName);
    if (bibFile.lastModified() == bibModified) return;
    bibModified = bibFile.lastModified();

    bibEntries = BibParser.parseBib(bibFile);
	  cites = new Trie<BibEntry>();
	  for (BibEntry bibEntry : bibEntries) {
		  cites.add(bibEntry.getEntryName(), bibEntry);
	  }
  }

  public ArrayList<BibEntry> getBibEntries(String search) {
    ArrayList<String> keys = ParseUtil.splitBySpace(search);
    ArrayList<BibEntry> entries = new ArrayList<BibEntry>();

    for (BibEntry entry : bibEntries) {
      boolean all = true;
      for (String key : keys) {
        if (entry.getText().toLowerCase().indexOf(key) == -1) {
          all = false;
          break;
        }
      }
      if (all) entries.add(entry);
    }

    return entries;
  }

  public DefaultTreeModel getStructure() {
    return structure;
  }

  public static class TODO {
    private String msg;
    private File file;
    private int line;

    private TODO(String msg, File file, int line) {
      this.msg = msg;
      this.file = file;
      this.line = line;
    }
  }

	public static class FilePos extends DefaultMutableTreeNode {
		private String name;

		private String file;
		private int lineNr;

		public FilePos(String name, String file, int lineNr) {
			this.name = name;
			this.file = file;
			this.lineNr = lineNr;
		}

		public String getName() {
			return name;
		}

		public String getFile() {
			return file;
		}

		public int getLineNr() {
			return lineNr;
		}

		@Override
		public String toString() {
			return name;
		}
	}

  public static class StructureEntry extends FilePos {
    private int depth;

    public StructureEntry(String name, int depth, String file, int lineNr) {
      super(name, file, lineNr);

      this.depth = depth;
    }

    public int getDepth() {
      return depth;
    }
  }
}
