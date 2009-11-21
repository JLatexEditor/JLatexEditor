package jlatexeditor.errorhighlighting;

import sce.component.SCEDocument;
import sce.component.SCEPane;
import sce.component.SourceCodeEditor;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * LaTeX compiler.
 *
 * @author Jörg Endrullis
 */
public class LatexCompiler extends Thread {
  private SourceCodeEditor editor;
  private JTextArea errorMessages;

  // The listeners
  private ArrayList<LatexCompileListener> compileListeners = new ArrayList<LatexCompileListener>();

  public LatexCompiler(SourceCodeEditor editor, JTextArea errorMessages){
    this.editor = editor;
    this.errorMessages = errorMessages;
  }

  public void run(){
    File file = new File(editor.getFileName());

    errorMessages.setText("");
    compileStart();

    // Command line shell
    Process latexCompiler = null;
    try{
      String compileCommand = "pdflatex -file-line-error -interaction=nonstopmode " + file.getName();
      latexCompiler = Runtime.getRuntime().exec(compileCommand, new String[0], file.getParentFile());
    } catch(Exception e){
      e.printStackTrace();
    }

    PrintWriter out = new PrintWriter(new OutputStreamWriter(latexCompiler.getOutputStream()));
    BufferedReader in = new BufferedReader(new InputStreamReader(latexCompiler.getInputStream()));

    // Compile messages -> errors ?
    try{
      // Collect information about the error -> inform the listener, if we read a "?"
      LatexCompileError error = new LatexCompileError();

      String line = null;
      while((line = in.readLine()) != null){
        errorMessages.append(line);
        errorMessages.append("\n");

        /*
          // Inform listeners
	        for (LatexCompileListener compileListener : compileListeners) {
		        compileListener.latexError(error);
	        }
	      */

        // Reset error information
        //error = new LatexCompileError();
      }
    } catch(IOException ignored){
    }

    compileEnd();
  }

  public void halt() {
    stop();
  }

  private void compileStart(){
    // Inform listeners
	  for (LatexCompileListener compileListener : compileListeners) {
		  compileListener.compileStarted();
	  }
  }

  private void compileEnd(){
    // Inform listeners
	  for (LatexCompileListener compileListener : compileListeners) {
		  compileListener.compileEnd();
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
