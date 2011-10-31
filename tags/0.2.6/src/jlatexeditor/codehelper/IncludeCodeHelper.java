package jlatexeditor.codehelper;

import jlatexeditor.Doc;
import jlatexeditor.SCEManager;
import sce.codehelper.CHCommand;
import sce.codehelper.CHCommandArgument;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import sce.component.AbstractResource;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

public class IncludeCodeHelper extends PatternHelper {
	protected final Pattern defaultFilePattern = Pattern.compile(".*");
  protected Pattern filePattern = null;
	protected File path;
  protected WordWithPos fileName;

  public IncludeCodeHelper() {
    pattern = new PatternPair("\\\\(include(?:graphics)?|input|bibliography|lstinputlisting)(?:\\[[^\\]]*\\])?\\{([^{}]*/)?([^{}/]*)");
  }

  @Override
  public boolean matches() {
    if (super.matches()) {
      // determine dir where this file is located
      AbstractResource resource = pane.getSourceCodeEditor().getResource();
      if (resource instanceof Doc.FileDoc) {
        Doc.FileDoc fileDoc = (Doc.FileDoc) resource;
        path = fileDoc.getFile().getParentFile();
      }

      WordWithPos pathName = params.get(1);
      // check if a path is given in the latex command
      if (pathName.word != null) {
        // is it absolute
        if (pathName.word.startsWith("/")) {
          path = new File(pathName.word);
        } else {
	        if (path == null) return false;
          path = new File(path.getAbsolutePath() + "/" + pathName.word);
        }
      }
	    if (path == null) return false;

      // do not complete if the dir does not exists
      if (!path.isDirectory()) return false;

	    // determine file extensions
	    filePattern = defaultFilePattern;
	    try {
				ArrayList<CHCommandArgument> arguments = SCEManager.getLatexCommands().getCommands().get(params.get(0).word).getArguments();
				for (CHCommandArgument argument : arguments) {
					if (!argument.isOptional()) {
						filePattern = Pattern.compile("\\.(" + argument.getType().getProperty("extensions").replaceAll(",", "|") + ")$");
					}
				}
	    } catch (Exception ignored) {}

	    fileName = params.get(2);
      return true;
    }
    return false;
  }

  @Override
  public WordWithPos getWordToReplace() {
    return fileName;
  }

  @Override
  public Iterable<? extends CHCommand> getCompletions(int level) {
	  Pattern filePattern;
	  switch (level) {
		  case 1:  filePattern = this.filePattern;
			  break;
			default: filePattern = defaultFilePattern;
	  }
    return getCompletions(path, fileName.word, filePattern);
  }

  @Override
  public String getMaxCommonPrefix() {
    return getMaxCommonPrefix(path, fileName.word);
  }

  public Iterable<FileCompletion> getCompletions(File path, final String fileName, final Pattern filePattern) {
    ArrayList<FileCompletion> list = new ArrayList<FileCompletion>();

    if ("..".startsWith(fileName)) {
      list.add(new FileCompletion("../"));
    }

    File[] fileList = path.listFiles(new FileFilter() {
	    public boolean accept(File file) {
		    String name = file.getName();
		    return name.startsWith(fileName) && (file.isDirectory() || filePattern.matcher(name).find());
	    }
    });

	  Arrays.sort(fileList);

    for (File file : fileList) {
      list.add(new FileCompletion(file.getName() + (file.isDirectory() ? "/" : "")));
    }

    return list;
  }

  /**
   * Searches for the best completion of the prefix.
   *
   * @param path     directory
   * @param fileName filename
   * @return the completion suggestion (without the prefix)
   */
  public String getMaxCommonPrefix(File path, String fileName) {
    int prefixLength = fileName.length();
    String completion = null;

    for (CHCommand command : getCompletions(path, fileName, defaultFilePattern)) {
      String commandName = command.getName();
      if (commandName.startsWith(fileName)) {
        if (completion == null) {
          completion = commandName;
        } else {
          // find the common characters
          int commonIndex = prefixLength;
          int commonLength = Math.min(completion.length(), commandName.length());
          while (commonIndex < commonLength) {
            if (completion.charAt(commonIndex) != commandName.charAt(commonIndex)) break;
            commonIndex++;
          }
          completion = completion.substring(0, commonIndex);
        }
      }
    }

    return completion;
  }

  public static class FileCompletion extends CHCommand {
    /**
     * Creates a command with the given name.
     *
     * @param name the name
     */
    public FileCompletion(String name) {
      super(name);
    }
	}
}