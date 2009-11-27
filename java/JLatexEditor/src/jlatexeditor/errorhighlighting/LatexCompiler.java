package jlatexeditor.errorhighlighting;

import jlatexeditor.ErrorView;
import sce.component.SourceCodeEditor;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;

/**
 * LaTeX compiler.
 *
 * @author Jörg Endrullis
 */
public class LatexCompiler extends Thread {
  public static int TYPE_PDF        = 0;
  public static int TYPE_DVI        = 1;
  public static int TYPE_DVI_PS     = 2; 
  public static int TYPE_DVI_PS_PDF = 3;

  private int type = 0;
  private SourceCodeEditor editor;
  private ErrorView errorView;

  // The listeners
  private ArrayList<LatexCompileListener> compileListeners = new ArrayList<LatexCompileListener>();

  public LatexCompiler(int type, SourceCodeEditor editor, ErrorView errorView){
    this.type = type;
    this.editor = editor;
    this.errorView = errorView;
  }

  public void run(){
    File file = editor.getFile();

    errorView.clear();
    compileStart();

    String baseName = file.getName();
    baseName = baseName.substring(0, baseName.lastIndexOf(".tex"));

    // Command line shell
    Process latexCompiler = null;
    try{
      if(type == TYPE_PDF) {
        latexCompiler = exec("pdflatex -interaction=nonstopmode " + baseName + ".tex", file.getParentFile());
      } else {
        latexCompiler = exec("latex -interaction=nonstopmode -output-format=dvi " + baseName + ".tex", file.getParentFile());
      }
    } catch(Exception e){
      e.printStackTrace();
      return;
    }

    BufferedReader in = new BufferedReader(new InputStreamReader(latexCompiler.getInputStream()), 100000);

    try{
      LatexCompileError error;

      ArrayList<String> fileStack = new ArrayList<String>();
      String line = in.readLine(); errorView.appendLine(line);
      while(line != null){
        // error messages
        if(line.startsWith("!")) {
          error = new LatexCompileError();
          error.setType(LatexCompileError.TYPE_ERROR);
          String fileName = fileStack.get(fileStack.size() - 1);
          error.setFile(new File(editor.getFile().getParentFile(), fileName), fileName);

          error.setMessage(line.substring(1).trim());

          while(line != null && !line.startsWith("l.")) {
            if(line.startsWith("<argument>")) {
              error.setCommand(line.substring("<argument>".length()).trim());
            }
            line = in.readLine(); errorView.appendLine(line);
          }

          if(line == null) {
            compileError(error);
            continue;
          }

          if(line.startsWith("l.")) {
            int space = line.indexOf(' ');
            if(space == -1) space = line.length();

            try {
              error.setLine(Integer.parseInt(line.substring(2, space)));
            } catch (Exception e) { continue; }

            String before = line.substring(space + 1);
            if(before.startsWith("...")) before = before.substring(3);
            error.setTextBefore(before);

            int position = line.length();
            line = in.readLine(); errorView.appendLine(line);
            error.setTextAfter(line.substring(position));

            compileError(error);
            line = in.readLine(); errorView.appendLine(line);
            continue;
          } else {
            compileError(error);
            continue;
          }
        }

        if(line.startsWith("LaTeX Warning:") || line.startsWith("LaTeX Font Warning:")) {
          error = new LatexCompileError();
          error.setType(LatexCompileError.TYPE_WARNING);
          String fileName = fileStack.get(fileStack.size() - 1);
          error.setFile(new File(editor.getFile().getParentFile(), fileName), fileName);

          int linePos = line.indexOf("on input line ");
          if(linePos != -1) {
            linePos += "on input line ".length();
            try {
              error.setLine(Integer.parseInt(line.substring(linePos, line.indexOf('.', linePos))));
            } catch (Exception e) { }
          }

          StringBuffer errorMessage = new StringBuffer(line.substring(line.indexOf(':')+1).trim());
          for(int i = 0; i < 5; i++) {
            line = in.readLine(); errorView.appendLine(line);
            if(line.trim().equals("")) break;
          }
          error.setMessage(errorMessage.toString());

          compileError(error);
          line = in.readLine(); errorView.appendLine(line);
          continue;
        }

        if(line.startsWith("Overfull \\hbox") || line.startsWith("Underfull \\hbox")) {
          error = new LatexCompileError();
          error.setType(LatexCompileError.TYPE_OVERFULL_HBOX);
          String fileName = fileStack.get(fileStack.size() - 1);
          error.setFile(new File(editor.getFile().getParentFile(), fileName), fileName);
          error.setMessage(line);

          while(!line.trim().equals("")) {
            int linePos = line.indexOf("at lines ");
            if(linePos != -1) {
              linePos += "at lines ".length();
              int mmPos = line.indexOf("--", linePos);
              try {
                error.setLineStart(Integer.parseInt(line.substring(linePos,mmPos)));
                error.setLineEnd(Integer.parseInt(line.substring(mmPos+2)));
              } catch (Exception e) { continue; }
            }
            line = in.readLine(); errorView.appendLine(line);
            if(line.indexOf("/10") == -1) break;
          }

          compileError(error);
          line = in.readLine(); errorView.appendLine(line);
          continue;
        }

        // opening and closing files
        if((line.indexOf("(") != -1 && !line.startsWith("(see")) || line.indexOf(')') != -1) {
          int position = 0;

          while(position < line.length()) {
            int open = line.indexOf('(', position);
            int close = line.indexOf(')', position);

            if(close == -1 && open == -1) break;

            if(close == -1 || (open != -1 && open < close)) {
              String fileName = "";
              while(true) {
                int space = line.indexOf(' ', open);
                if(space == -1) space = line.length();
                close = line.indexOf(')', open);
                if(close != -1 && close < space) space = close;

                position = space;
                fileName += line.substring(open + 1, space);
                if(line.length() == 79 && position == line.length()) {
                  line = in.readLine(); errorView.appendLine(line);
                  open = -1;
                } else break;
              }
              fileStack.add(fileName);
            } else {
              fileStack.remove(fileStack.size() - 1);
              position = close + 1;
            }
          }
          
          // for debugging: print the stack of open files
          // for(String fileName : fileStack) System.out.print(fileName + " ");
          // System.out.println();

          line = in.readLine(); errorView.appendLine(line);
          continue;
        }

        line = in.readLine(); errorView.appendLine(line);
      }
    } catch(IOException ignored){
    }

    try {
      Process bibtex = exec("bibtex " + baseName, file.getParentFile());
      bibtex.waitFor();

      if(type == TYPE_DVI_PS || type == TYPE_DVI_PS_PDF) {
        Process dvips = exec("dvips " + baseName + ".dvi", file.getParentFile());
        dvips.waitFor();
      }
      if(type == TYPE_DVI_PS_PDF) {
        Process ps2pdf = exec("ps2pdf " + baseName + ".ps", file.getParentFile());
        ps2pdf.waitFor();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    compileEnd();
  }

  public void halt() {
    stop();
  }

  private Process exec(String command, File dir) throws IOException {
    Process process;

    ArrayList<String> env = new ArrayList<String>();
    for(Map.Entry<String, String> entry : System.getenv().entrySet()) {
      if(entry.getKey().equalsIgnoreCase("PWD")) continue;
      env.add(entry.getKey() + "=" + entry.getValue());
    }
    String[] envArray = new String[env.size()];
    env.toArray(envArray);

    process = Runtime.getRuntime().exec(command, envArray, dir);

    return process;
  }

  private void compileStart(){
	  for (LatexCompileListener compileListener : compileListeners) {
		  compileListener.compileStarted();
	  }
  }

  private void compileEnd(){
	  for (LatexCompileListener compileListener : compileListeners) {
		  compileListener.compileEnd();
	  }
  }

  private void compileError(LatexCompileError error){
    errorView.addError(error);
    for (LatexCompileListener compileListener : compileListeners) {
      compileListener.latexError(error);
    }
  }

  // Manage LatexCompileListeners
  public void addLatexCompileListener(LatexCompileListener listener){
    compileListeners.add(listener);
  }

  public void removeLatexCompileListener(LatexCompileListener listener){
    compileListeners.remove(listener);
  }
}
