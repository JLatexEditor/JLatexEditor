package jlatexeditor.codehelper;

import jlatexeditor.Doc;
import jlatexeditor.JLatexEditorJFrame;
import jlatexeditor.PackagesExtractor;
import sce.component.AbstractResource;
import sce.component.SourceCodeEditor;
import util.ParseUtil;
import util.SetTrie;
import util.SimpleTrie;
import util.Tuple;

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
  private static final Pattern WORD_PATTERN = Pattern.compile("\\p{L}*");
  private static final Pattern TODO_PATTERN = Pattern.compile("\\btodo\\b");
  private JLatexEditorJFrame jle;

  private long bibModified = 0;

	private ParserState stableState = new ParserState();
	private ParserState buildingState = new ParserState();

	private DefaultTreeModel structure = new DefaultTreeModel(new DefaultMutableTreeNode());

	private boolean parsing = false;
	private final Object syncObject = new Object();

	public BackgroundParser(JLatexEditorJFrame jle) {
	  super("BackgroundParser");
		setDaemon(true);
    this.jle = jle;
    setPriority(Thread.MIN_PRIORITY);
  }

  public ArrayList<FilePos<BibEntry>> getBibEntries() {
    return stableState.bibEntries;
  }

	public HashSet<File> getFiles() {
		return stableState.files;
	}

	public SimpleTrie<?> getWords() {
    return stableState.words;
  }

	public DocumentClass getDocumentClass() {
		return stableState.documentClass;
	}

	/**
	 * Returns all imported packages as SimpleTrie.
	 *
	 * @return all imported packages as SimpleTrie
	 */
	public SimpleTrie<Package> getPackages() {
		return stableState.packages;
	}

	/**
	 * Returns a HashSet with all directly and indirectly imported packages.
	 *
	 * @return HashSet with all directly and indirectly imported packages
	 */
	public HashSet<PackagesExtractor.Package> getIndirectlyImportedPackages() {
		return stableState.getIndirectlyImportedPackages();
	}

	public SimpleTrie<?> getCommandNames() {
    return stableState.commandNames;
  }

	public SetTrie<String> getCommandsAndFiles() {
		return stableState.commandsAndFiles;
	}

	public SimpleTrie<Command> getCommands() {
    return stableState.commands;
  }

	public SimpleTrie<Environment> getEnvironments() {
		return stableState.environments;
	}

  public SimpleTrie<FilePos> getLabelDefs() {
    return stableState.labelDefs;
  }

	public SetTrie<FilePos> getLabelRefs() {
		return stableState.labelRefs;
	}

  public SetTrie<FilePos> getBibRefs() {
    return stableState.bibRefs;
  }

	public SimpleTrie<FilePos<BibEntry>> getBibKeys2bibEntries() {
		return stableState.bibKeys2bibEntries;
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

	    buildingState = new ParserState();
	    buildingState.structureStack.add(new StructureEntry("Project", 0, resource.getName(), 0));

      parseTex(directory, file.getName(), new HashSet<String>());

	    stableState = buildingState;
      structure.setRoot(stableState.structureStack.get(0));

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

  private void parseTex(File directory, String fileName, HashSet<String> done) {
    if (done.contains(fileName)) return;
    else done.add(fileName);

	  File file = new File(directory, fileName);
    if (!file.exists()) file = new File(directory, fileName + ".tex");
    if (!file.exists()) return;

	  buildingState.files.add(file);

	  ArrayList<StructureEntry> structure = buildingState.structureStack;

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
          buildingState.todos.add(new TODO(todoMsg, file, line));
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
      buildingState.commandNames.add(command);
	    buildingState.commandsAndFiles.add(command, file.getAbsolutePath());

      // newcommand
      if (command.equals("newcommand") || command.equals("renewcommand") || command.equals("DeclareRobustCommand") ||
	        command.equals("newenvironment") || command.equals("renewenvironment")) {

	      boolean isCommand = command.equals("newcommand") || command.equals("renewcommand") || command.equals("DeclareRobustCommand");

	      Tuple<String,Integer> itemPositionPair = ParseUtil.parseItem(tex, index);
	      String name = itemPositionPair.first;
        index = itemPositionPair.second;
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
	      if (isCommand) {
          name = name.length() > 0 ? name.substring(1) : "";
		      buildingState.commands.add(name, new Command(name, fileCanonicalPath, line, numberOfArgs, optional, body));
			    buildingState.commandsAndFiles.add(name, file.getAbsolutePath());
	      } else {
		      buildingState.environments.add(name, new Environment(name, fileCanonicalPath, line, numberOfArgs, optional));
	      }
	      // commandNames.add(name);
      // label, input, include
      } else if (command.equals("label") || command.equals("bibliography") || command.equals("input") || command.equals("include") ||
	               command.equals("ref") || command.equals("cite") || command.equals("documentclass") || command.equals("usepackage")) {
        String optionalArgument = null;
        if(tex.charAt(index) == '[') {
          optionalArgument = ParseUtil.parseBalanced(tex, index+1, ']');
          index += optionalArgument.length() + 2;
        }
        String argument = ParseUtil.parseBalanced(tex, index+1, '}');

        if (command.equals("label")) {
          buildingState.labelDefs.add(argument, new FilePos(argument, fileCanonicalPath, line));
        } else if (command.equals("ref")) {
          buildingState.labelRefs.add(argument, new FilePos(argument, fileCanonicalPath, line));
        } else if (command.equals("documentclass")) {
	        String[] options = new String[0];
	        if (optionalArgument != null) {
	          options = optionalArgument.split(",");
	        }
					buildingState.documentClass = new DocumentClass(argument, fileCanonicalPath, line, options);
        } else if (command.equals("usepackage")) {
	        String[] packageNames = argument.split(",");
	        String[] options = new String[0];
	        if (optionalArgument != null) {
	          options = optionalArgument.split(",");
	        }
	        for (String packageName : packageNames) {
		        buildingState.packages.add(packageName, new Package(packageName, fileCanonicalPath, line, options));
	        }
        } else if (command.equals("cite")) {
          argument = argument.replaceAll("(?:\\{|\\})", "");
          for(String ref : argument.split(",")) {
            buildingState.bibRefs.add(ref.trim(), new FilePos(ref.trim(), fileCanonicalPath, line));
          }
        } else if (command.equals("bibliography")) {
          parseBib(directory, Command.unfoldRecursive(argument, buildingState.commands, 10));
        } else {
          parseTex(directory, Command.unfoldRecursive(argument, buildingState.commands, 10), done);
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
      buildingState.words.add(matcher.group());
    }
  }

	private void parseBib(File directory, String fileName) {
		// TODO: consider jle.getCurrentContent(file) to get last version
    if (!fileName.endsWith(".bib")) fileName = fileName + ".bib";
    File bibFile = new File(directory, fileName);
    if (bibFile.lastModified() == bibModified) {
	    buildingState.bibEntries = stableState.bibEntries;
	    buildingState.bibKeys2bibEntries = stableState.bibKeys2bibEntries;
	    buildingState.bibWords2bibEntries = stableState.bibWords2bibEntries;
	    return;
    }
    bibModified = bibFile.lastModified();

    buildingState.bibEntries = BibParser.parseBib(bibFile);
	  buildingState.bibKeys2bibEntries = new SimpleTrie<FilePos<BibEntry>>();
	  for (FilePos<BibEntry> bibEntry : buildingState.bibEntries) {
		  buildingState.bibKeys2bibEntries.add(bibEntry.getName(), bibEntry);
	  }
	  buildingState.bibWords2bibEntries = new SetTrie<BibEntry>();
	  for (FilePos<BibEntry> filePos : buildingState.bibEntries) {
		  BibEntry bibEntry = filePos.element;
		  buildingState.bibWords2bibEntries.add(bibEntry.getEntryName().toLowerCase(), bibEntry);
		  buildingState.bibWords2bibEntries.add(bibEntry.getYear(), bibEntry);

		  addBibWords(bibEntry.getAuthors().toLowerCase(), bibEntry);
		  addBibWords(bibEntry.getTitle().toLowerCase(), bibEntry);
		  addBibWords(bibEntry.getText().toLowerCase(), bibEntry);
	  }
  }

	private void addBibWords(String authors, BibEntry bibEntry) {
		Matcher matcher = WORD_PATTERN.matcher(authors);
		while (matcher.find()) {
			String author = matcher.group();
			buildingState.bibWords2bibEntries.add(author, bibEntry);
		}
	}

	public List<BibEntry> getBibEntries(String search) {
    ArrayList<String> keys = ParseUtil.splitBySpace(search.toLowerCase());
		ArrayList<BibEntry> entries = new ArrayList<BibEntry>();

		if (keys.size() > 0) {
			int count = keys.size() < 2 ? 10 : 100;
			List<BibEntry> selectedEntries = stableState.bibWords2bibEntries.getObjects(keys.get(0), count);

			for (BibEntry entry : selectedEntries) {
				boolean all = true;
				for (int i = 1; i < keys.size(); i++) {
					String key = keys.get(i);
					if (!entry.getText().toLowerCase().contains(key)) {
						all = false;
						break;
					}
				}
				if (all) entries.add(entry);
			}
		} else {
			return stableState.bibWords2bibEntries.getObjects("", 10);
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
		return stableState.files.contains(file);
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

	class ParserState {
		DocumentClass documentClass;
		HashSet<File> files = new HashSet<File>();
	  SimpleTrie words = new SimpleTrie();
	  SimpleTrie<Package> packages = new SimpleTrie<Package>();
	  SimpleTrie commandNames = new SimpleTrie();
	  SetTrie<String> commandsAndFiles = new SetTrie<String>();
	  SimpleTrie<Command> commands = new SimpleTrie<Command>();
	  SimpleTrie<Environment> environments = new SimpleTrie<Environment>();
	  SimpleTrie<FilePos> labelDefs = new SimpleTrie<FilePos>();
		SetTrie<FilePos> labelRefs = new SetTrie<FilePos>();
		SimpleTrie<FilePos<BibEntry>> bibKeys2bibEntries = new SimpleTrie<FilePos<BibEntry>>();
		ArrayList<FilePos<BibEntry>> bibEntries = new ArrayList<FilePos<BibEntry>>();
		SetTrie<BibEntry> bibWords2bibEntries = new SetTrie<BibEntry>();
	  SetTrie<FilePos> bibRefs = new SetTrie<FilePos>();

		ArrayList<StructureEntry> structureStack = new ArrayList<StructureEntry>();

	  ArrayList<TODO> todos = new ArrayList<TODO>();

		private HashSet<PackagesExtractor.Package> indirectlyImportedPackages = null;

		public HashSet<PackagesExtractor.Package> getIndirectlyImportedPackages() {
			if (indirectlyImportedPackages == null) {
				// evaluate lazy value
				indirectlyImportedPackages = new HashSet<PackagesExtractor.Package>();
				// add packages directly or indirectly imported by imported packages
				for (Package pack : packages) {
					PackagesExtractor.Package aPackage = PackagesExtractor.getPackageParser().getPackages().get(pack.getName());
					if (aPackage != null) {
						aPackage.addRequiredPackagesRecursively(indirectlyImportedPackages);
					}
				}
				// add packages directly or indirectly imported by documentclass
				if (documentClass != null) {
					PackagesExtractor.Package aPackage = PackagesExtractor.getDocClassesParser().getPackages().get(documentClass.getName());
					aPackage.addRequiredPackagesRecursively(indirectlyImportedPackages);
				}
			}
			return indirectlyImportedPackages;
		}
	}
}
