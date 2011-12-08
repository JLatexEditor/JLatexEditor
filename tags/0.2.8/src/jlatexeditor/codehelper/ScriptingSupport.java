package jlatexeditor.codehelper;

import sce.codehelper.CodeAssistant;
import sce.component.*;
import util.FileUtil;
import util.ProcessOutput;
import util.ProcessUtil;

import javax.swing.*;
import java.io.*;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Scripting support.
 */
public class ScriptingSupport implements CodeAssistant {
  public static final String genericBegin = "%!begin{";
  public static final String[] languages = new String [] {"haskell", "tree"};
  public static final HashSet<String> languagesSet = new HashSet<String>(Arrays.asList(languages));

	public ScriptingSupport() {
	}

  public boolean assistAt(SCEPane pane) {
    SCEDocument document = pane.getDocument();

    final SCEDocumentRows rows = document.getRowsModel();
    synchronized (rows) {
      int rowNr = pane.getCaret().getRow();
      while(rowNr >= 0) {
        String row = rows.getRowAsString(rowNr).trim();

        if(!row.startsWith("%")) return false;

        if(row.startsWith(genericBegin)) {
          for(String language : languages) {
            if(row.startsWith(getBegin(language))) {
              execute(language, rows, rowNr, pane);
              return true;
            }
          }
        }

        rowNr--;
      }
    }

    return false;
  }

  public String getBegin(String language) {
    return genericBegin + language + "}";
  }


  public String getEnd(String language, String magic) {
    return "%!end{" + language + "}" + magic;
  }

  public void execute(String language, SCEDocumentRows rows, int startRow, SCEPane pane) {
    SCECaret caret = pane.getCaret();
    SCEDocumentPosition caretPos = new SCEDocumentPosition(caret.getRow(), caret.getColumn());

    String row = rows.getRowAsString(startRow);
    String rowTrimmed = row.trim();
    String begin = getBegin(language);
    String magic = rowTrimmed.substring(begin.length());

    int commentIndex = row.indexOf('%');
    String indentation = row.substring(0, commentIndex);

    if(magic.trim().equals("")) {
      magic = ("{magic:" + Math.random() + "}").replaceAll("[,\\.]","");

      caret.moveTo(startRow, row.length(), false);
      pane.insert(magic + "\n" + indentation + "% \n" + indentation + getEnd(language, magic));
      caret.moveTo(startRow + 1, indentation.length() + 2, false);
      return;
    }

    String end = getEnd(language, magic);

    // find end of code
    int codeEnd = startRow+1;
    while(codeEnd < rows.getRowsCount()) {
      String line = rows.getRowAsString(codeEnd).trim();

      if(!line.startsWith("%")) break;
      if(line.startsWith(end)) break;

      codeEnd++;
    }
    codeEnd--;

    // find end of generated code
    int endRow = codeEnd+1;
    boolean endFound = false;
    while(endRow < rows.getRowsCount()) {
      String line = rows.getRowAsString(endRow).trim();

      if(line.startsWith(end)) { endFound = true; break; }

      endRow++;
    }

    // no end marker found?
    if(!endFound) {
      caret.moveTo(codeEnd, rows.getRow(codeEnd).length, false);
      pane.insert("\n" + indentation + getEnd(language, magic));
      caret.moveTo(caretPos, false);

      endRow = codeEnd+1;
    }

    try {
      File scriptDir = FileUtil.createTempDirectory("script");
      scriptDir.deleteOnExit();

      String libDir = language;
      if(language.equals("tree")) libDir = "haskell";

      File[] files = new File("./scripting/" + libDir).listFiles();
      for(File file : files) {
        if(file.getName().startsWith(".")) continue;
        if(!file.isFile()) continue;

        FileUtil.copyFile(file, new File(scriptDir, file.getName()));
      }

      String output = "";

      StringBuilder contentBuilder = new StringBuilder();
      for(int rowNr = startRow+1; rowNr <= codeEnd; rowNr++) {
        contentBuilder.append(rows.getRowAsString(rowNr).trim().substring(1));
      }
      String content = contentBuilder.toString();

      if(language.equals("haskell") || language.equals("tree")) {
        String sourceName = "Main.hs";
        String executableName = "Main";

        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(scriptDir, sourceName), true)));
        if(language.equals("haskell")) {
          writer.println(content);
        } else {
          writer.println("main = tree \"" + content.replace('\n', ' ').replaceAll("\"", "\\\"").replaceAll("\\\\", "\\\\\\\\") + "\"");
        }
        writer.close();

	      try {
		      ProcessOutput processOutput = ProcessUtil.execAndWait(new String[]{"ghc", "--make", sourceName}, scriptDir);
		      if (processOutput.getReturnCode() > 0) {
			      System.out.println(processOutput.getStderr());
			      JOptionPane.showMessageDialog(null, processOutput.getStderr().trim(), "GHC error", JOptionPane.ERROR_MESSAGE);
			      throw new IOException("GHC error");
		      }
		      ProcessOutput result = ProcessUtil.execAndWait(new String[] {"./" + executableName}, scriptDir);
	        output = result.getStdout();
	      } catch (IOException e1) {
		      if (e1.getMessage().startsWith("Cannot run program")) {
		 		    JOptionPane.showMessageDialog(null, "Failed to run ghc.  Is ghc installed on your system?.\nMore details about this error are written to STDOUT after you clicked OK.", "Failed to run ghc", JOptionPane.ERROR_MESSAGE);
		 	    }
		      throw e1;
	      }
      }

      pane.getDocument().replace(codeEnd+1,0,endRow,0, output + "\n");
    } catch (IOException e) {
	    e.printStackTrace();
    } catch (InterruptedException ignored) {
    }

    caret.moveTo(caretPos, false);
  }
}
