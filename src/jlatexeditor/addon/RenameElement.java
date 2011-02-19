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
	  List<WordWithPos> words = CodePattern.parameterPattern.find(pane);
    if (words != null) {
	    // extract the parameter
      WordWithPos parameter = words.get(0);

	    // get command name
	    List<WordWithPos> commandList = CodePattern.commandParamPattern.find(pane);
	    if (commandList != null) {
		    String command = commandList.get(0).word;

		    if (command.equals("label") || command.equals("ref") || command.equals("eqref")) {
			    // start background parser to update document states
			    jle.getBackgroundParser().parse();

			    // ask user for new label
			    String oldLabel = parameter.word;
			    String newLabel = JOptionPane.showInputDialog(jle, "Rename label: ", oldLabel);
			    if (newLabel == null || newLabel.equals(oldLabel)) return;

			    // wait for background parser to finish
			    try {
				    jle.getBackgroundParser().waitForParseFinished();
			    } catch (InterruptedException e) {
				    return;
			    }

			    // determine all files/editors that contain this label
			    List<BackgroundParser.FilePos> filePoses = jle.getBackgroundParser().getLabelRefs().getObjects(parameter.word, 1000);
			    if (filePoses == null) {
				    filePoses = new ArrayList<BackgroundParser.FilePos>();
			    }
					filePoses.add(jle.getBackgroundParser().getLabelDefs().get(parameter.word));
			    // put them into a set
			    HashSet<SourceCodeEditor<Doc>> editors = new HashSet<SourceCodeEditor<Doc>>();
			    for (BackgroundParser.FilePos filePos : filePoses) {
				    editors.add(jle.open(new Doc.FileDoc(new File(filePos.getFile())), false));
			    }
			    // add active editor to this set, since we will anyway process this file
			    editors.add(jle.getActiveEditor());

			    // replace in all files/editors
			    for (SourceCodeEditor<Doc> ed : editors) {
				    // open these files
				    ed.getTextPane().getDocument().replaceInAllRows("\\\\(label|ref|eqref)\\{" + oldLabel + "\\}", "\\\\$1{" + newLabel + "}");
			    }

			    // start background parser to update document states
			    jle.getBackgroundParser().parse();

					return;
		    } else
		    if (command.equals("cite")) {
			    // todo
			    /*
			    BackgroundParser.FilePos filePos = jle.getBackgroundParser().getBibKeys2bibEntries().get(parameter.word);
			    if (filePos != null) {
				    // todo
						jle.open(new Doc.FileDoc(new File(filePos.getFile())), filePos.getLineNr());
						return;
			    }
			    */
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
      if (editor.getResource() instanceof Doc.FileDoc) {
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
	}
}
