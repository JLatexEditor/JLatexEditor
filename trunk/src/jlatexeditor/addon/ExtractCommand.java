package jlatexeditor.addon;

import de.endrullis.utils.ExtIterable;
import jlatexeditor.Doc;
import jlatexeditor.JLatexEditorJFrame;
import jlatexeditor.SCEManager;
import jlatexeditor.codehelper.BackgroundParser;
import jlatexeditor.codehelper.Command;
import sce.component.*;
import sun.security.provider.ParameterCache;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * AddOn for declaring commands based on the selected text.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class ExtractCommand extends AddOn {
	private static final int RESTRICTED_ARG_LENGTH = 50;

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

	private void declareCommand(JLatexEditorJFrame jle, BackgroundParser.FilePos lastCommandPos, String commandName, String templateText) {
		SourceCodeEditor<Doc> editor = jle.getEditor(new Doc.FileDoc(new File(lastCommandPos.getFile())));
		SCEPane pane = editor.getTextPane();

		SourceCodeEditor<Doc> activeEditor = jle.getActiveEditor();

		Template template = new Template(commandName, templateText);

		pane.getDocument().insert(template.toDeclaration(), lastCommandPos.getLineNr() + 1, 0);

		jle.open(activeEditor.getResource(), 0);
		activeEditor.getTextPane().getCaret().moveTo(0, 0, false);
		SCESearch search = new SCESearch(activeEditor);
		search.setShowReplace(true);
		search.getInput().setText(template.toInputRegEx());
		search.getReplace().setText(template.toReplacement());
		search.getRegExp().setSelected(true);
		search.getCaseSensitive().setSelected(false);
		search.getSelectionOnly().setSelected(false);
		activeEditor.search(search);
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

	private static String escapeRegEx(String text) {
		return text.replaceAll("([\\\\.\\[\\]\\{\\}\\(\\)*+-])", "\\\\$1");
	}


	public static class Template {
		public String commandName;
		public ArrayList<TemplateElement> elements = new ArrayList<TemplateElement>();
		public int argCount = 0;
		public int maxArg = 0;
		private HashMap<Integer, Integer> argNr2regExGroup;

		public Template(String commandName, String template) {
			this.commandName = commandName;

			char[] chars = template.toCharArray();

			int lastPos = 0;

			for (int i = 0; i < chars.length; i++) {
				switch (chars[i]) {
					case '\\':
						i++;
						break;
					case '#':
						if (i != lastPos) {
							elements.add(new Text(template.subSequence(lastPos, i)));
						}
						int argNr = chars[++i] - '0';
						elements.add(new Argument(argNr));
						lastPos = i+1;
						break;
				}
			}
			if (lastPos < chars.length) {
				elements.add(new Text(template.subSequence(lastPos, chars.length)));
			}

			finalizeElements();
		}

		private void finalizeElements() {
			HashSet<Integer> appearedArguments = new HashSet<Integer>();
			HashSet<Integer> multiUsedArgs = new HashSet<Integer>();

			int regExGroup = 0;
			argNr2regExGroup = new HashMap<Integer, Integer>();

			for (TemplateElement element : elements) {
				if (element instanceof Argument) {

					Argument argument = (Argument) element;
					argument.firstOcc = !appearedArguments.contains(argument.nr);
					if (argument.firstOcc) {
						regExGroup++;
						argNr2regExGroup.put(argument.nr, regExGroup);
						argument.regExGroup = regExGroup;
						maxArg = argument.nr;
					} else {
						multiUsedArgs.add(argument.nr);
						argument.regExGroup = argNr2regExGroup.get(argument.nr);
					}
					appearedArguments.add(argument.nr);
				}
			}

			for (TemplateElement element : elements) {
				if (element instanceof Argument) {
					Argument argument = (Argument) element;
					if (argument.firstOcc && multiUsedArgs.contains(argument.nr)) {
						argument.restrictLength = true;
					}
				}
			}

			argCount = appearedArguments.size();
		}

		public boolean isValid() {
			return maxArg == argCount;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (TemplateElement element : elements) {
				sb.append(element.toString());
			}
			return sb.toString();
		}

		public String toInputRegEx() {
			StringBuilder sb = new StringBuilder();
			for (TemplateElement element : elements) {
				sb.append(element.toInputRegEx());
			}
			return sb.toString();
		}

		public String toReplacement() {
			String replacement = "\\\\" + commandName;
			for (int argNr = 1; argNr <= argCount; argNr++) {
				replacement += "{\\" + argNr2regExGroup.get(argNr) + "}";
			}
			return replacement;
		}

		public String toDeclaration() {
			String templateDeclaration = "\\DeclareRobustCommand{\\" + commandName + "}";
			if (argCount > 0) {
				templateDeclaration += "[" + argCount + "]";
			}

			String template = toString();
			if (template.contains("\n")) {
				templateDeclaration += "{%\n" + template + "%\n}";
			} else {
				templateDeclaration += "{" + template + "}";
			}

			return templateDeclaration;
		}
	}


	public static interface TemplateElement {
		public String toInputRegEx();
	}

	public static class Text implements TemplateElement {
		public String value;

		public Text(CharSequence value) {
			this.value = value.toString();
		}

		@Override
		public String toString() {
			return value;
		}

		public String toInputRegEx() {
			return escapeRegEx(value);
		}
	}

	public static class Argument implements TemplateElement {
		public int nr;
		public boolean firstOcc = false;
		public int regExGroup = -1;
		public boolean restrictLength = false;

		public Argument(int nr) {
			this.nr = nr;
		}

		@Override
		public String toString() {
			return "#" + nr;
		}

		public String toInputRegEx() {
			if (firstOcc) {
				if (restrictLength) {
					return "(.{1-" + RESTRICTED_ARG_LENGTH + "})";
				} else {
					return "(.*)";
				}
			} else {
				return "\\" + regExGroup;
			}
		}
	}
}
