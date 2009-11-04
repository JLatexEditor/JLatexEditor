package jlatexeditor.errorhighlighting;

import sce.component.SCEDocument;
import sce.component.SCEPane;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Jörg Endrullis
 */

public class LatexCompiler{
  // The text to compiler
  private SCEPane pane = null;
  private SCEDocument document = null;

  // The temorary tex-file
  private String directory = null;
  private String temporaryFileName = null;
  private String dviFileName = null;

  // The listeners
  private ArrayList compileListeners = new ArrayList();

  public LatexCompiler(SCEPane pane, String temporaryFileName){
    this.pane = pane;
    this.document = pane.getDocument();
    this.temporaryFileName = temporaryFileName;
  }

  public void run(){
    // Cut of the ".tex"
    directory = temporaryFileName.substring(0, temporaryFileName.lastIndexOf(File.separatorChar));

    temporaryFileName = temporaryFileName.substring(temporaryFileName.lastIndexOf(File.separatorChar) + 1);
    if(temporaryFileName.toLowerCase().endsWith(".tex")){
      temporaryFileName = temporaryFileName.substring(0, temporaryFileName.length() - 4);
    }
    // ".~tex"
    dviFileName = temporaryFileName + "_bak.dvi";
    temporaryFileName = temporaryFileName + "_bak.tex";

    compileStart();

    // Write the actual content to a file
    String text = document.getText();
    try{
      PrintWriter writer = new PrintWriter(new FileOutputStream(directory + File.separatorChar + temporaryFileName));
      writer.write(text);
      writer.close();
    } catch(IOException e){
      compileEnd();
      return;
    }

    // Command line shell
    Process latexCompiler = null;
    try{
      String compileCommand = "latex -src-specials " + temporaryFileName;
      latexCompiler = Runtime.getRuntime().exec(compileCommand, new String[0], new File(directory));
    } catch(Exception e){
      e.printStackTrace();
    }

    PrintWriter out = new PrintWriter(new OutputStreamWriter(latexCompiler.getOutputStream()));
    BufferedReader in = new BufferedReader(new InputStreamReader(latexCompiler.getInputStream()));

    // Compile messages -> errors ?
    try{
      // Collect information about the error -> inform the listener, if we read a "?"
      LatexCompileError error = new LatexCompileError();

      boolean compileReady = false;
      while(!compileReady){
        int firstChar = in.read();
        // End of stream?
        if(firstChar == -1){
          compileReady = true;
          break;
        }

        // Serios error ?
        if(firstChar == '*'){
          out.println("\\end");
          out.flush();
        }

        // Error ?
        if(firstChar == '?'){
          // A question -> ask latex for help :)
          out.println("h");
          out.flush();

          // Read the help
          String helpText = "";
          boolean helpReady = false;
          while(!helpReady){
            firstChar = in.read();
            if(firstChar == '?') break;

            String helpLine = new String(new char[]{(char) firstChar}) + in.readLine();
            helpText += helpLine + "\n";
          }
          error.setHelp(helpText);

          // Continue parsing
          out.println(" ");
          out.flush();

          // Inform listeners
          Iterator iterator = compileListeners.iterator();
          while(iterator.hasNext()){
            LatexCompileListener listener = (LatexCompileListener) iterator.next();
            listener.latexError(error);
          }

          // Reset error information
          error = new LatexCompileError();

          continue;
        }

        // Read the rest of the line
        String outLine = new String(new char[]{(char) firstChar}) + in.readLine();

        // Error message
        if(outLine.startsWith("!")) error.setError(outLine);

        // Missing } after argument
        if(outLine.startsWith("Runaway argument?")){
          error.setError(outLine);
          // Read the next line of latex output
          error.setTextBefore(in.readLine());
        }

        // Where did the error occur?
        if(outLine.startsWith("l.")){
          // Read the line number
          error.setLine(Integer.parseInt(outLine.substring(2, outLine.indexOf(' '))));

          // The text before the error
          error.setTextBefore(outLine.substring(outLine.indexOf(' ')).trim());
          // The text after the error: read the next line of latex output
          error.setTextAfter(in.readLine().trim());
        }
      }
    } catch(IOException e){
    }

    // View the document with yap
    try{
      // the pane should stay focused
      pane.getFocusBack();

      String viewCommand = "yap -1 " + " -s " + (pane.getCaret().getRow()+1) + temporaryFileName + " " + dviFileName;
      Runtime.getRuntime().exec(viewCommand, new String[0], new File(directory));
    } catch(Exception e){
      e.printStackTrace();
    }

    compileEnd();
  }

  private void compileStart(){
    // Inform listeners
    Iterator iteratorStart = compileListeners.iterator();
    while(iteratorStart.hasNext()){
      LatexCompileListener listener = (LatexCompileListener) iteratorStart.next();
      listener.compileStarted();
    }
  }

  private void compileEnd(){
    // Inform listeners
    Iterator iteratorEnd = compileListeners.iterator();
    while(iteratorEnd.hasNext()){
      LatexCompileListener listener = (LatexCompileListener) iteratorEnd.next();
      listener.compileEnd();
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
