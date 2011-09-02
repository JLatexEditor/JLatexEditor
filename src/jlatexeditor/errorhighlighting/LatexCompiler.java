package jlatexeditor.errorhighlighting;

import de.endrullis.utils.StringUtils;
import jlatexeditor.Doc;
import jlatexeditor.ErrorView;
import jlatexeditor.gproperties.GProperties;
import sce.component.AbstractResource;
import sce.component.SourceCodeEditor;
import util.ProcessUtil;
import util.SystemUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
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

  private static Pattern fileLineError = Pattern.compile("((?:[A-Z]:)?[^:]+):([\\d]+): (.*)");

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
	    parseLatexOutput(file, in);
    } catch (IOException ignored) {
    }

    try {
      String bibExecutable = GProperties.getString("compiler.bibtex.executable");
      String dvipsExecutable = GProperties.getString("compiler.dvips.executable");
      String ps2pdfExecutable = GProperties.getString("compiler.ps2pdf.executable");

      errorView.compileStarted("bibtex");
      bibtex = ProcessUtil.exec(new String[]{bibExecutable, baseName}, file.getParentFile());
      bibtex.waitFor();

      if (type == Type.dvi_ps || type == Type.dvi_ps_pdf) {
        Process dvips = ProcessUtil.exec(new String[]{dvipsExecutable, baseName + ".dvi"}, file.getParentFile());
        dvips.waitFor();
      }
      if (type == Type.dvi_ps_pdf) {
        Process ps2pdf = ProcessUtil.exec(new String[]{ps2pdfExecutable, baseName + ".ps"}, file.getParentFile());
        ps2pdf.waitFor();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    compileEnd();
    errorView.compileFinished();
  }

  private static final Pattern whiteSpacePattern = Pattern.compile("($|[^\\\\])(?:\\\\\\\\)*%.*|[ \\n\\r]");
  private static String removeWhiteSpace(String string) {
    return whiteSpacePattern.matcher(string).replaceAll("$1");
  }

  private class FileContent {
    private StringBuilder fileText = new StringBuilder();
    private ArrayList<String> fileLines = new ArrayList<String>();
    private TreeMap<Integer,Integer> pos2line = new TreeMap<Integer, Integer>();
    private HashMap<Integer,Integer> line2pos = new HashMap<Integer, Integer>();

    public FileContent(File file) throws IOException {
      BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
      pos2line.put(0,0);
      line2pos.put(0,0);

      String fileLine;
      for (int fileLineNr=0; (fileLine = r.readLine()) != null; fileLineNr++) {
        pos2line.put(fileText.length(), fileLineNr);
        line2pos.put(fileLineNr, fileText.length());

        fileLines.add(fileLine);
        fileText.append(removeWhiteSpace(fileLine));
      }

      r.close();
    }

    /**
     * Returns the text of the file without whitespace.
     */
    public StringBuilder getText() {
      return fileText;
    }

    public ArrayList<String> getLines() {
      return fileLines;
    }

    public TreeMap<Integer, Integer> getPos2Line() {
      return pos2line;
    }

    public HashMap<Integer, Integer> getLine2Pos() {
      return line2pos;
    }

    public int getLineStart(int line) {
      Integer pos = getLine2Pos().get(line);
      return pos != null ? pos : fileText.length();
    }

    public int getLineEnd(int line) {
      return getLineStart(line+1);
    }
  }

	public void parseLatexOutput(File file, BufferedReader in) throws IOException {
		LatexCompileError error;

    HashMap<File,FileContent> file2content = new HashMap<File,FileContent>();

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
			    String message = errorMatcher.group(3);
			    while (!message.endsWith(".")) {
				    line = in.readLine();
				    errorView.appendLine(line);
				    message += line;
			    }
		      error.setMessage(message);

		      // bug
		      if (!fileStack.get(fileStack.size() - 1).equals(fileName)) fileStack.add(fileName);
		    }

			  ArrayList<String> linesBeforeLineNumber = new ArrayList<String>();
			  boolean discoveredEmptyLine = false;
		    while (line != null && !line.startsWith("l.")) {
			    if (line.equals("")) {
				    discoveredEmptyLine = true;
			    }
			    if (!discoveredEmptyLine) {
				    linesBeforeLineNumber.add(line);
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

			    if (linesBeforeLineNumber.size() >= 2) {
				    // take the last 2 lines and check whether they mark an error
				    int size = linesBeforeLineNumber.size();
				    String firstLine  = linesBeforeLineNumber.get(size - 2);
				    String secondLine = linesBeforeLineNumber.get(size - 1);
				    if (firstLine.length() > 0 && secondLine.length() >= firstLine.length()
					    && secondLine.substring(0, firstLine.length()).matches("^\\s+$")) {
					    LatexCompileError cause = error.clone();

					    String before = firstLine;
					    if (before.startsWith("<")) {
						    before = StringUtils.stringAfter(before, "> ").getOrElse(before);
					    }
					    before = StringUtils.stringAfter(before, "...").getOrElse(before);
					    before = before.substring(0, Math.max(0, before.length() - 1));
					    String after = secondLine.substring(firstLine.length());
					    after  = StringUtils.stringBefore(after, "...", 'l').getOrElse(after);

					    // search for the corresponding line in the source file
					    if (cause.getFile().exists()) {
                before = removeWhiteSpace(before);
                after = removeWhiteSpace(after);
						    String searchString = before + after;

                FileContent fileContent = file2content.get(cause.getFile());
                if(fileContent == null) {
                  fileContent = new FileContent(cause.getFile());
                  file2content.put(cause.getFile(), fileContent);
                }

                int lastOccurrence = fileContent.getText().lastIndexOf(searchString, fileContent.getLineStart(cause.getLineStart()));
                if(lastOccurrence >= 0) {
                  Map.Entry<Integer,Integer> causeLineEntry = fileContent.getPos2Line().floorEntry(lastOccurrence + before.length()-1);
                  if(causeLineEntry != null) {
                    int errorLine = causeLineEntry.getValue();

                    // find the error column
                    String theLine = fileContent.getLines().get(errorLine);
                    int nrOfLetters = lastOccurrence + before.length() - causeLineEntry.getKey();
                    int errorColumn = 0;
                    while (errorColumn < theLine.length()) {
                      char c = theLine.charAt(errorColumn);
                      if(c != ' ' && c != '\n' && c != '\r') {
                        nrOfLetters--;
                        if(nrOfLetters == 0) break;
                      }
                      errorColumn++;
                    }

                    cause.setLine(errorLine+1);
                    cause.setColumn(errorColumn);
                  }
                }
					    }

					    cause.setTextBefore(before);
					    cause.setTextAfter(after);
					    compileError(cause);

					    error.setFollowUpError(true);
					    error.setMessage("Follow-up error of \"" + error.getMessage() + "\"");
				    }
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

			  String unwrappedLine = line;
			  while (!unwrappedLine.endsWith(".")) {
				  line = in.readLine();
				  errorView.appendLine(line);
				  unwrappedLine += line;
			  }

		    int linePos = unwrappedLine.indexOf("on input line ");
		    if (linePos != -1) {
		      linePos += "on input line ".length();
		      try {
		        error.setLine(Integer.parseInt(unwrappedLine.substring(linePos, unwrappedLine.indexOf('.', linePos))));
		      } catch (Exception ignored) {
		      }
		    }

		    StringBuffer errorMessage = new StringBuffer(unwrappedLine.substring(unwrappedLine.indexOf(':') + 1).trim());
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
