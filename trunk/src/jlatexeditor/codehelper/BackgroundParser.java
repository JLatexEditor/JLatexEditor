package jlatexeditor.codehelper;

import jlatexeditor.Doc;
import jlatexeditor.JLatexEditorJFrame;
import sce.component.AbstractResource;
import sce.component.SourceCodeEditor;
import util.ParseUtil;
import util.StreamUtils;
import util.Trie;
import util.TrieSet;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsing files in background.
 */
public class BackgroundParser extends Thread {
	// TODO: allow all unicode letters (enable with flags?)
  private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-ZäöüÄÖÜß]*");
  private static final Pattern TODO_PATTERN = Pattern.compile("\\btodo\\b");
  private JLatexEditorJFrame jle;

  private long bibModified = 0;
  private ArrayList<FilePos<BibEntry>> bibEntries = new ArrayList<FilePos<BibEntry>>();

	private HashSet<File> files = new HashSet<File>();
  private Trie<? extends Object> words = new Trie<Object>();
  private Trie<FilePos> commandNames = new Trie<FilePos>();
  private Trie<Command> commands = new Trie<Command>();
  private Trie<FilePos> labelDefs = new Trie<FilePos>();
	private Trie<FilePos> labelRefs = new Trie<FilePos>();
	private Trie<FilePos<BibEntry>> bibKeys2bibEntries = new Trie<FilePos<BibEntry>>();
	private TrieSet<BibEntry> bibWords2bibEntries = new TrieSet<BibEntry>();

  private DefaultTreeModel structure = new DefaultTreeModel(new DefaultMutableTreeNode());

  private ArrayList<TODO> todos = new ArrayList<TODO>();

	private boolean parsing = false;
	private final Object syncObject = new Object();

	public BackgroundParser(JLatexEditorJFrame jle) {
	  super("BackgroundParser");
    this.jle = jle;
    setPriority(Thread.MIN_PRIORITY);
  }

  public ArrayList<FilePos<BibEntry>> getBibEntries() {
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

	public Trie<FilePos<BibEntry>> getBibKeys2bibEntries() {
		return bibKeys2bibEntries;
	}

  public void run() {
    while (true) {
	    SourceCodeEditor<Doc> editor = null;
	    try {
        editor = jle.getMainEditor();
	    } catch (ArrayIndexOutOfBoundsException ignored) {
	    } catch (NullPointerException ignored) {
	    }

	    AbstractResource resource = null;
	    if (editor != null) {
		    resource = editor.getResource();
	    }

	    // wait a second if we could not determine a saved master document
      if (resource == null || !(resource instanceof Doc.FileDoc)) {
	      try {
		      sleep(1000);
	      } catch (InterruptedException e2) {
		      return;
	      }
	      continue;
      }

      File file = ((Doc.FileDoc) resource).getFile();
      File directory = file.getParentFile();

	    HashSet<File> files = new HashSet<File>();
	    Trie words = new Trie();
      Trie<FilePos> commandNames = new Trie<FilePos>();
      Trie<FilePos> labelDefs = new Trie<FilePos>();
      Trie<FilePos> labelRefs = new Trie<FilePos>();
      Trie<Command> commands = new Trie<Command>();

      ArrayList<StructureEntry> structureStack = new ArrayList<StructureEntry>();
      structureStack.add(new StructureEntry("Project", 0, resource.getName(), 0));

      parseTex(directory, file.getName(), files, words, commandNames, commands, labelDefs, labelRefs, structureStack, new HashSet<String>());

	    this.files = files;
	    this.words = words;
      this.commandNames = commandNames;
      this.commands = commands;
      this.labelDefs = labelDefs;
      this.labelRefs = labelRefs;
      structure.setRoot(structureStack.get(0));

      try {
        synchronized (syncObject) {
	        parsing = false;
	        syncObject.notify();
          syncObject.wait();
	        parsing = true;
        }
      } catch (InterruptedException ignored) {
      }
    }
  }

  public void parse() {
	  synchronized (syncObject) {
		  if (parsing) return;
		  syncObject.notify();
	  }
  }

	public void waitForParseFinished() throws InterruptedException {
		synchronized (syncObject) {
			if (!parsing) return;
			syncObject.wait();
		}
	}

  private void parseTex(File directory, String fileName, HashSet<File> files, Trie words, Trie<FilePos> commandNames, Trie<Command> commands,
                        Trie<FilePos> labelDefs, Trie<FilePos> labelRefs, ArrayList<StructureEntry> structure, HashSet<String> done) {
    if (done.contains(fileName)) return;
    else done.add(fileName);

    File file = new File(directory, fileName);
    if (!file.exists()) file = new File(directory, fileName + ".tex");
    if (!file.exists()) return;

	  files.add(file);

    String tex;
    String fileCanonicalPath;
    try {
      tex = jle.getCurrentContent(file);
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
          parseTex(directory, Command.unfoldRecursive(argument, commands, 10), files, words, commandNames, commands, labelDefs, labelRefs, structure, done);
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
		// TODO: consider jle.getCurrentContent(file) to get last version
    if (!fileName.endsWith(".bib")) fileName = fileName + ".bib";
    File bibFile = new File(directory, fileName);
    if (bibFile.lastModified() == bibModified) return;
    bibModified = bibFile.lastModified();

    bibEntries = BibParser.parseBib(bibFile);
	  bibKeys2bibEntries = new Trie<FilePos<BibEntry>>();
	  for (FilePos<BibEntry> bibEntry : bibEntries) {
		  bibKeys2bibEntries.add(bibEntry.getName(), bibEntry);
	  }
	  bibWords2bibEntries = new TrieSet<BibEntry>();
	  for (FilePos<BibEntry> filePos : bibEntries) {
		  BibEntry bibEntry = filePos.element;
		  bibWords2bibEntries.add(bibEntry.getEntryName().toLowerCase(), bibEntry);
		  bibWords2bibEntries.add(bibEntry.getYear(), bibEntry);

		  addBibWords(bibEntry.getAuthors().toLowerCase(), bibEntry);
		  addBibWords(bibEntry.getTitle().toLowerCase(), bibEntry);
		  addBibWords(bibEntry.getText().toLowerCase(), bibEntry);
	  }
  }

	private void addBibWords(String authors, BibEntry bibEntry) {
		Matcher matcher = WORD_PATTERN.matcher(authors);
		while (matcher.find()) {
			String author = matcher.group();
			bibWords2bibEntries.add(author, bibEntry);
		}
	}

	public List<BibEntry> getBibEntries(String search) {
    ArrayList<String> keys = ParseUtil.splitBySpace(search.toLowerCase());
		ArrayList<BibEntry> entries = new ArrayList<BibEntry>();

		if (keys.size() > 0) {
			int count = keys.size() < 2 ? 10 : 100;
			List<BibEntry> selectedEntries = bibWords2bibEntries.getObjects(keys.get(0), count);

			if (selectedEntries != null) {
				for (BibEntry entry : selectedEntries) {
					boolean all = true;
					for (int i = 1; i < keys.size(); i++) {
						String key = keys.get(i);
						if (entry.getText().toLowerCase().indexOf(key) == -1) {
							all = false;
							break;
						}
					}
					if (all) entries.add(entry);
				}
			}
		} else {
			return bibWords2bibEntries.getObjects("", 10);
		}

    return entries;
  }

  public DefaultTreeModel getStructure() {
    return structure;
  }

	/**
	 * Returns true if the file is referenced by the master document.
	 *
	 * @param file file
	 * @return true if the file is referenced by the master document
	 */
	public boolean isResponsibleFor(File file) {
		return files.contains(file);
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

	public static class FilePos<E> extends DefaultMutableTreeNode {
		private String name;

		private String file;
		private int lineNr;

		private E element;

		public FilePos(String name, String file, int lineNr) {
			this.name = name;
			this.file = file;
			this.lineNr = lineNr;
		}

		public FilePos(String name, String file, int lineNr, E element) {
			super(element);
			this.name = name;
			this.file = file;
			this.lineNr = lineNr;
			this.element = element;
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

		public E getElement() {
			return element;
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
