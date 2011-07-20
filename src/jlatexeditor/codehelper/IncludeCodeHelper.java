package jlatexeditor.codehelper;

import jlatexeditor.Doc;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import sce.component.AbstractResource;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class IncludeCodeHelper extends PatternHelper {
  protected File path;
  protected WordWithPos fileName;

  public IncludeCodeHelper() {
    pattern = new PatternPair("\\\\(include(?:graphics)?|input|bibliography)(?:\\[[^\\]]*\\])?\\{([^{}]*/)?([^{}/]*)");
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
    return getCompletions(path, fileName.word);
  }

  @Override
  public String getMaxCommonPrefix() {
    return getMaxCommonPrefix(path, fileName.word);
  }

  public Iterable<FileCompletion> getCompletions(File path, final String fileName) {
    ArrayList<FileCompletion> list = new ArrayList<FileCompletion>();

    if ("..".startsWith(fileName)) {
      list.add(new FileCompletion("../"));
    }

    File[] fileList = path.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.startsWith(fileName);
      }
    });

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

    for (CHCommand command : getCompletions(path, fileName)) {
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