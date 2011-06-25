package jlatexeditor.errorhighlighting;

import jlatexeditor.Doc;
import jlatexeditor.ErrorView;
import jlatexeditor.gproperties.GProperties;
import sce.component.AbstractResource;
import sce.component.SourceCodeEditor;
import util.ProcessUtil;
import util.SystemUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LaTeX compiler.
 *
 * @author Jörg Endrullis
 */
public class LatexCompiler extends Thread {
	public enum Type {pdf, dvi, dvi_ps, dvi_ps_pdf}

  private Type type = Type.pdf;
  private SourceCodeEditor editor;
  private ErrorView errorView;

  // The listeners
  private ArrayList<LatexCompileListener> compileListeners = new ArrayList<LatexCompileListener>();

  private static Pattern fileLineError = Pattern.compile("((?:[A-Z]:)?[^:]+):([\\d]+):(.*)");

  // instance
  private static LatexCompiler instance = null;
  // compile process
  private Process latexCompiler = null;
  private Process bibtex = null;

  private LatexCompiler(Type type, SourceCodeEditor editor, ErrorView errorView) {
	  super("LatexCompiler");
	  setDaemon(true);
    this.type = type;
    this.editor = editor;
    this.errorView = errorView;
  }

  public static synchronized LatexCompiler createInstance(Type type, SourceCodeEditor editor, ErrorView errorView) {
    if (instance != null) instance.halt();
    instance = new LatexCompiler(type, editor, errorView);
    return instance;
  }

  public void run() {
    File file;
    AbstractResource resource = editor.getResource();
    if (resource instanceof Doc.FileDoc) {
      Doc.FileDoc fileDoc = (Doc.FileDoc) resource;
      file = fileDoc.getFile();
    } else {
      return;
    }

    errorView.clear();
    compileStart();

    String baseName = file.getName();
    baseName = baseName.substring(0, baseName.lastIndexOf(".tex"));

    // Command line shell
    try {
      if (type == Type.pdf) {
	      // build commandline string
	      String exe = GProperties.getString("compiler.pdflatex.executable");
	      String additionalParameters = GProperties.getString("compiler.pdflatex.parameters");
	      String command = exe + " -file-line-error -interaction=nonstopmode " +
			      additionalParameters + " \"" + baseName + ".tex\"";

	      // start the compiler
        latexCompiler = ProcessUtil.exec(command, file.getParentFile());
        errorView.compileStarted("pdflatex");
      } else {
	      // build commandline string
	      String exe = GProperties.getString("compiler.latex.executable");
	      String additionalParameters = GProperties.getString("compiler.latex.parameters");
	      String command = exe + " -file-line-error -interaction=nonstopmode -output-format=dvi " +
			      additionalParameters + " \"" + baseName + ".tex\"";

	      // start the compiler
        latexCompiler = ProcessUtil.exec(command, file.getParentFile());
        errorView.compileStarted("latex");
      }
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    BufferedReader in = new BufferedReader(new InputStreamReader(latexCompiler.getInputStream()), 100000);
    try {
      LatexCompileError error;

      ArrayList<String> fileStack = new ArrayList<String>();
      String versionString = in.readLine();
	    errorView.appendLine(versionString);
      String line = in.readLine();
      errorView.appendLine(line);
      while (line != null) {
        // error messages
        Matcher errorMatcher = fileLineError.matcher(line);
        if (line.startsWith("!") || errorMatcher.matches()) {
          error = new LatexCompileError();
	        error.setOutputLine(errorView.getLinesCount()-2);
          error.setType(LatexCompileError.TYPE_ERROR);
          if (line.startsWith("!")) {
            String fileName = fileStack.get(fileStack.size() - 1);
            error.setFile(SystemUtils.newFile(file.getParentFile(), fileName), fileName);
            error.setMessage(line.substring(1).trim());
          } else {
            String fileName = errorMatcher.group(1);
            error.setFile(SystemUtils.newFile(file.getParentFile(), fileName), fileName);
            error.setLine(Integer.parseInt(errorMatcher.group(2)));
            error.setMessage(errorMatcher.group(3).trim());

            // bug
            if (!fileStack.get(fileStack.size() - 1).equals(fileName)) fileStack.add(fileName);
          }

          while (line != null && !line.startsWith("l.")) {
            if (line.startsWith("<argument>")) {
              error.setCommand(line.substring("<argument>".length()).trim());
            }
            line = in.readLine();
            errorView.appendLine(line);
          }

          if (line == null) {
            compileError(error);
            continue;
          }

          if (line.startsWith("l.")) {
            int space = line.indexOf(' ');
            if (space == -1) space = line.length();

            try {
              error.setLine(Integer.parseInt(line.substring(2, space)));
            } catch (Exception e) {
              continue;
            }

            String before = line.substring(space + 1);
            if (before.startsWith("...")) before = before.substring(3);
            error.setTextBefore(before);

            int position = line.length();
            line = in.readLine();
            errorView.appendLine(line);
            error.setTextAfter(line.substring(position));

            compileError(error);
            line = in.readLine();
            errorView.appendLine(line);
            continue;
          } else {
            compileError(error);
            continue;
          }
        }

        if (line.startsWith("LaTeX Warning:") || line.startsWith("LaTeX Font Warning:")) {
          error = new LatexCompileError();
	        error.setOutputLine(errorView.getLinesCount()-2);
          error.setType(LatexCompileError.TYPE_WARNING);
          String fileName = fileStack.get(fileStack.size() - 1);
          error.setFile(SystemUtils.newFile(file.getParentFile(), fileName), fileName);

          int linePos = line.indexOf("on input line ");
          if (linePos != -1) {
            linePos += "on input line ".length();
            try {
              error.setLine(Integer.parseInt(line.substring(linePos, line.indexOf('.', linePos))));
            } catch (Exception ignored) {
            }
          }

          StringBuffer errorMessage = new StringBuffer(line.substring(line.indexOf(':') + 1).trim());
          for (int i = 0; i < 5; i++) {
            line = in.readLine();
            errorView.appendLine(line);
            if (line.trim().equals("")) break;
          }
          error.setMessage(errorMessage.toString());

          compileError(error);
          line = in.readLine();
          errorView.appendLine(line);
          continue;
        }

        if (line.startsWith("Overfull \\hbox") || line.startsWith("Underfull \\hbox")) {
          error = new LatexCompileError();
	        error.setOutputLine(errorView.getLinesCount()-2);
          error.setType(LatexCompileError.TYPE_OVERFULL_HBOX);
          String fileName = fileStack.get(fileStack.size() - 1);
          error.setFile(SystemUtils.newFile(file.getParentFile(), fileName), fileName);
          error.setMessage(line);

          while (!line.trim().equals("")) {
            int linePos = line.indexOf("at lines ");
            if (linePos != -1) {
              linePos += "at lines ".length();
              int mmPos = line.indexOf("--", linePos);
              try {
                error.setLineStart(Integer.parseInt(line.substring(linePos, mmPos)));
                error.setLineEnd(Integer.parseInt(line.substring(mmPos + 2)));
              } catch (Exception e) {
                continue;
              }
            }
            line = in.readLine();
            errorView.appendLine(line);
            if (!line.contains("/10")) break;
          }

          compileError(error);
          line = in.readLine();
          errorView.appendLine(line);
          continue;
        }

        // opening and closing files
        if ((line.contains("(") && !line.startsWith("(see")) || line.startsWith(")") ||
                (line.indexOf(')') != -1 && (line.startsWith("[") || line.indexOf(".tex") != 0 || line.indexOf(".sty") != 0 || line.indexOf(".bbl") != 0 || line.indexOf(".aux") != 0))) {
          int position = 0;

          while (position < line.length()) {
            int open = line.indexOf('(', position);
            int close = line.indexOf(')', position);

            if (close == -1 && open == -1) break;

            if (close == -1 || (open != -1 && open < close)) {
              String fileName = "";
              while (true) {
                int space = line.indexOf(' ', open);
                if (space == -1) space = line.length();
                close = line.indexOf(')', open);
                if (close != -1 && close < space) space = close;

                position = space;
                fileName += line.substring(open + 1, space);
                if (line.length() == 79 && position == line.length()) {
                  line = in.readLine();
                  errorView.appendLine(line);
                  open = -1;
                } else break;
              }
              fileStack.add(fileName);
            } else {
              // never empty the stack... parsing bugs
              if (fileStack.size() > 1) fileStack.remove(fileStack.size() - 1);
              position = close + 1;
            }
          }

          // for debugging: print the stack of open files
          // for(String fileName : fileStack) System.out.print(fileName + " ");
          // System.out.println();

          line = in.readLine();
          errorView.appendLine(line);
          continue;
        }

        line = in.readLine();
        errorView.appendLine(line);
      }
    } catch (IOException ignored) {
    }

    try {
      errorView.compileStarted("bibtex");
      String exe = GProperties.getString("compiler.bibtex.executable");
      bibtex = ProcessUtil.exec(new String[]{exe, baseName}, file.getParentFile());
      bibtex.waitFor();

      if (type == Type.dvi_ps || type == Type.dvi_ps_pdf) {
        Process dvips = ProcessUtil.exec(new String[]{"dvips", baseName + ".dvi"}, file.getParentFile());
        dvips.waitFor();
      }
      if (type == Type.dvi_ps_pdf) {
        Process ps2pdf = ProcessUtil.exec(new String[]{"ps2pdf", baseName + ".ps"}, file.getParentFile());
        ps2pdf.waitFor();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    compileEnd();
    errorView.compileFinished();
  }

  public void halt() {
    try {
      if (latexCompiler != null) latexCompiler.destroy();
    } catch (Exception ignore) {
    }
    try {
      if (bibtex != null) bibtex.destroy();
    } catch (Exception ignore) {
    }
    stop();
  }

  private void compileStart() {
    for (LatexCompileListener compileListener : compileListeners) {
      compileListener.compileStarted();
    }
  }

  private void compileEnd() {
    for (LatexCompileListener compileListener : compileListeners) {
      compileListener.compileEnd();
    }
  }

  private void compileError(LatexCompileError error) {
    errorView.addError(error);
    for (LatexCompileListener compileListener : compileListeners) {
      compileListener.latexError(error);
    }
  }

  // Manage LatexCompileListeners

  public void addLatexCompileListener(LatexCompileListener listener) {
    compileListeners.add(listener);
  }

  public void removeLatexCompileListener(LatexCompileListener listener) {
    compileListeners.remove(listener);
  }
}
