package jlatexeditor.addon;

import de.endrullis.utils.ExtIterable;
import jlatexeditor.Doc;
import jlatexeditor.JLatexEditorJFrame;
import jlatexeditor.SCEManager;
import jlatexeditor.codehelper.BackgroundParser;
import jlatexeditor.codehelper.Command;
import sce.component.SCEDocument;
import sce.component.SCEDocumentRow;
import sce.component.SCEPane;
import sce.component.SourceCodeEditor;

import javax.swing.*;
import javax.xml.bind.JAXBElement;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AddOn for declaring commands based on the selected text.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class ExtractCommand extends AddOn {
	protected ExtractCommand() {
		super("extract command", "Extract Command", "control shift M", "Refactoring: Extract Command");
	}

	@Override
	public void run(JLatexEditorJFrame jle) {
		SCEDocument document = jle.getActiveEditor().getTextPane().getDocument();
		if (document.hasSelection()) {
			String selectedText = document.getSelectedText();

			BackgroundParser.FilePos lastCommandPos = getLastCommandPos(jle);
			if (lastCommandPos == null) return;

			JTextField commandNameTextField = new JTextField();
			SourceCodeEditor<Doc> templateSce = SCEManager.createLatexSourceCodeEditor();
			templateSce.setText(selectedText);

			Object[] message = {
				"Template name:", commandNameTextField,
				"\nDefine your template (use #1-#9 for parameters):\n", templateSce
			};
			int resp = JOptionPane.showConfirmDialog(null, message, "Declare command", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

			if (resp != JOptionPane.OK_OPTION) {
				return;
			}

			String commandName = commandNameTextField.getText();
			String template = templateSce.getText().trim();
			if (commandName.startsWith("\\")) {
				commandName = commandName.substring(1);
			}

			declareCommand(jle, lastCommandPos, commandName, template);
		}
	}

	private void declareCommand(JLatexEditorJFrame jle, BackgroundParser.FilePos lastCommandPos, String commandName, String template) {
		SourceCodeEditor<Doc> editor = jle.getEditor(new Doc.FileDoc(new File(lastCommandPos.getFile())));
		SCEPane pane = editor.getTextPane();

		Pattern argPattern = Pattern.compile("[^\\\\]#([0-9])");
		Matcher matcher = argPattern.matcher(template);

		int maxParam = 0;
		while (matcher.find()) {
			maxParam = Math.max(maxParam, Integer.parseInt(matcher.group(1)));
		}

		String templateDeclaration = "\\DeclareRobustCommand{\\" + commandName + "}[" + maxParam + "]{";

		if (template.contains("\n")) {
			templateDeclaration += "%\n" + template + "%\n}";
		} else {
			templateDeclaration += template + "}";
		}

		pane.getDocument().insert(templateDeclaration, lastCommandPos.getLineNr() + 1, 0);
	}

	private BackgroundParser.FilePos getLastCommandPos(JLatexEditorJFrame jle) {
		// determine map from file to last declared command
		HashMap<String, Command> files2lastCommand = new HashMap<String, Command>();

		ExtIterable<Command> commands = SCEManager.getBackgroundParser().getCommands().getObjectsIterable("");
		for (Command command : commands) {
			if (files2lastCommand.containsKey(command.getFile())) {
				Command lastCommand = files2lastCommand.get(command.getFile());
				if (lastCommand.getLineNr() < command.getLineNr()) {
					files2lastCommand.put(command.getFile(), command);
				}
			} else {
				files2lastCommand.put(command.getFile(), command);
			}
		}

		if (files2lastCommand.size() == 0) {
			// determine end of packages in main editor
			SCEDocument mainDoc = jle.getMainEditor().getTextPane().getDocument();
			int lastPackageRow = 0;
			for (SCEDocumentRow row : mainDoc.getRowsModel().getRows()) {
				if (row.toString().contains("\\usepackage")) {
					lastPackageRow = row.row_nr;
				}
			}

			File file = jle.getMainEditor().getFile();

			return new BackgroundParser.FilePos(file.getName(), file.getAbsolutePath(), lastPackageRow);
		}
		else
		if (files2lastCommand.size() == 1) {
			return files2lastCommand.values().iterator().next();
		}
		else {
			// ask user in which file command should be declared
			// main file has to be on first position
			JComboBox comboBox = new JComboBox();

			String mainEditorFile = jle.getMainEditor().getFile().getAbsolutePath();
			if (files2lastCommand.keySet().contains(mainEditorFile)) {
				comboBox.addItem(mainEditorFile);
			}
			for (String file : files2lastCommand.keySet()) {
				if (!file.equals(mainEditorFile)) {
					comboBox.addItem(file);
				}
			}

			Object[] message = {"Choose a file in which the command shall be declared:\n", comboBox};
			int resp = JOptionPane.showConfirmDialog(null, message, "Declare command", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

			if (resp == JOptionPane.OK_OPTION) {
				return files2lastCommand.get(comboBox.getSelectedItem().toString());
			} else {
				return null;
			}
		}
	}
}
