package jlatexeditor.addon;

import jlatexeditor.Doc;
import jlatexeditor.JLatexEditorJFrame;
import jlatexeditor.codehelper.BackgroundParser;
import jlatexeditor.codehelper.CodePattern;
import sce.codehelper.WordWithPos;
import sce.component.SCEPane;
import sce.component.SourceCodeEditor;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Rename element under cursor.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class RenameElement extends AddOn {
	private static List<String> defaultExtensions = Arrays.asList("", ".tex", ".bib");

	protected RenameElement() {
		super("rename element", "Rename Element", "F2");
	}

	@Override
	public void run(JLatexEditorJFrame jle) {
		SourceCodeEditor<Doc> editor = jle.getActiveEditor();
		SCEPane pane = editor.getTextPane();

	  // cursor placed under command parameter?
		List<WordWithPos> words = CodePattern.commandPattern.find(pane);
	  if (words != null) {
		  String oldCommandName = words.get(0).word;

			// start background parser to update document states
			backgroundParserUpdate(jle);

			// ask user for new command name
			String newCommandName = JOptionPane.showInputDialog(jle, "Command name: ", oldCommandName);
			if (newCommandName == null || newCommandName.equals(oldCommandName)) return;

			// wait for background parser to finish
      backgroundParserWaitFor(jle);

		  List<String> fileNames = jle.getBackgroundParser().getCommandsAndFiles().getObjects(oldCommandName, 1000);
		  HashSet<File> files = new HashSet<File>();
		  if (fileNames != null) {
			  for (String fileName : fileNames) {
				  files.add(new File(fileName));
			  }
		  }

		  replaceInAllFiles(jle, files, "\\\\" + oldCommandName + "([^\\p{L}]|$)", "\\\\" + newCommandName + "$1", false);

		  // start background parser to update document states
			jle.getBackgroundParser().parse();

		  return;
	  }

	  words = CodePattern.parameterPattern.find(pane);
    if (words != null) {
	    // extract the parameter
      WordWithPos parameter = words.get(0);

	    // get command name
	    List<WordWithPos> commandList = CodePattern.commandParamPattern.find(pane);
	    if (commandList != null) {
		    String command = commandList.get(0).word;

        // update background parser
		    if (command.equals("label") || command.equals("ref") || command.equals("eqref")) {
          // start background parser to update document states
          backgroundParserUpdate(jle);

			    // ask user for new label
			    String oldLabel = parameter.word;
			    String newLabel = JOptionPane.showInputDialog(jle, "Rename label: ", oldLabel);
			    if (newLabel == null || newLabel.equals(oldLabel)) return;

			    // wait for background parser to finish
          if(!backgroundParserWaitFor(jle)) return;

			    // determine all files/editors that contain this label
			    List<BackgroundParser.FilePos> filePoses = jle.getBackgroundParser().getLabelRefs().getObjects(oldLabel, 1000);
			    if (filePoses == null) {
				    filePoses = new ArrayList<BackgroundParser.FilePos>();
			    }
			    filePoses.add(jle.getBackgroundParser().getLabelDefs().get(oldLabel));

			    replaceInAllFiles(jle, filePoses, "\\\\(label|ref|eqref)\\{" + oldLabel + "\\}", "\\\\$1{" + newLabel + "}", false);

					return;
		    } else
		    if (command.equals("cite")) {
          words = CodePattern.citeParameterPattern.find(pane);
          parameter = words.get(0);

          renameBibRef(jle, parameter.word);

			    return;
		    } else
		    if (command.equals("begin")) {
			    WordWithPos endEnv = EnvironmentUtils.getCloseEnvIterator(pane).next();
			    pane.editAsTemplate(Arrays.asList(Arrays.asList(parameter, endEnv)));
			    return;
		    } else
		    if (command.equals("end")) {
			    WordWithPos openEnv = EnvironmentUtils.getOpenEnvIterator(pane).next();
			    pane.editAsTemplate(Arrays.asList(Arrays.asList(parameter, openEnv)));
			    return;
		    }
	    }

	    // todo
	    if (1==1) return;

	    // try to jump to file un
      if (jle.getMainEditor().getResource() instanceof Doc.FileDoc) {
        Doc.FileDoc fileDoc = (Doc.FileDoc) editor.getResource();
        String thisFileName = fileDoc.getFile().getName();
        File dir = fileDoc.getFile().getParentFile();

        for (String extension : defaultExtensions) {
          String thatFileName = parameter.word + extension;
          if (thisFileName.equals(thatFileName)) continue;
          File fileUnderCaret = new File(dir, thatFileName);

          if (fileUnderCaret.exists() && fileUnderCaret.isFile()) {
	          // todo
            jle.open(new Doc.FileDoc(fileUnderCaret));
            return;
          }
        }
      }
    }

    // cursor placed on a name of a bibtex item?
    words = CodePattern.bibItemPattern.find(pane);
    if (words != null) {
      WordWithPos parameter = words.get(0);
      renameBibRef(jle, parameter.word);
    }
  }

  private void backgroundParserUpdate(JLatexEditorJFrame jle) {
    // let parser finish current run (user might have changes)
    backgroundParserWaitFor(jle);

    // start background parser to update document states
    jle.getBackgroundParser().parse();
  }

  private boolean backgroundParserWaitFor(JLatexEditorJFrame jle) {
    // wait for background parser to finish
    try {
      jle.getBackgroundParser().waitForParseFinished();
    } catch (InterruptedException e) {
      return false;
    }

    return true;
  }

  private void renameBibRef(JLatexEditorJFrame jle, String oldRef) {
      // start background parser to update document states
      backgroundParserUpdate(jle);

      // ask user for new label
      String newRef = JOptionPane.showInputDialog(jle, "Rename bibtex entry: ", oldRef);
      if (newRef == null || newRef.equals(oldRef)) return;

      // wait for background parser to finish
      if(!backgroundParserWaitFor(jle)) return;

      // determine all files/editors that contain this citation
      List<BackgroundParser.FilePos> filePoses = jle.getBackgroundParser().getBibRefs().getObjects(oldRef, 1000);
      if (filePoses == null) {
        filePoses = new ArrayList<BackgroundParser.FilePos>();
      }
      filePoses.add(jle.getBackgroundParser().getBibKeys2bibEntries().get(oldRef));

      String balanced = "[^\\{\\}]*(?:\\{[^\\{\\}]*\\}[^\\{\\}]*)*";
      replaceInAllFiles(jle, filePoses, "(\\\\cite(?:\\[[^\\{\\}\\[\\]]*\\])?\\{(?:" + balanced + ",)?\\{? *)" + oldRef + "( *\\}?(?:," + balanced + ")?\\})", "$1" + newRef + "$2", false);
      replaceInAllFiles(jle, filePoses, "(@[\\w\\W]+ *\\{ *)" + oldRef + "([ ,\\}])", "$1" + newRef + "$2", false);
  }

	private void replaceInAllFiles(JLatexEditorJFrame jle, List<BackgroundParser.FilePos> filePoses, String from, String to, boolean everywhere) {
		HashSet<File> files = new HashSet<File>();

		if (filePoses != null) {
			// put them into a set
			for (BackgroundParser.FilePos filePos : filePoses) {
				if (filePos != null) {
					files.add(new File(filePos.getFile()));
				}
			}
		}

		replaceInAllFiles(jle, files, from, to, everywhere);
	}

	private void replaceInAllFiles(JLatexEditorJFrame jle, HashSet<File> files, String from, String to, boolean everywhere) {
		// put them into a set
		HashSet<SourceCodeEditor<Doc>> editors = new HashSet<SourceCodeEditor<Doc>>();
		if (files != null) {
			for (File file : files) {
				if (file != null) {
					editors.add(jle.open(new Doc.FileDoc(file), false));
				}
			}
		}
		// add active editor to this set, since we will anyway process this file
		editors.add(jle.getActiveEditor());

		// replace in all files/editors
		for (SourceCodeEditor<Doc> ed : editors) {
			if(!everywhere) {
        ed.getTextPane().getDocument().replaceInAllRows(from, to);
      } else {
        ed.getTextPane().getDocument().replaceAll(from, to);
      }
		}

		// start background parser to update document states
		jle.getBackgroundParser().parse();
	}
}
