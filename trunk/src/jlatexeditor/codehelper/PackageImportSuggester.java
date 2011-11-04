package jlatexeditor.codehelper;

import jlatexeditor.*;
import jlatexeditor.addon.ExtractCommand;
import jlatexeditor.quickhelp.LatexQuickHelp;
import sce.codehelper.CodeAssistant;
import sce.codehelper.PatternPair;
import sce.codehelper.SCEPopup;
import sce.codehelper.WordWithPos;
import sce.component.SCEDocument;
import sce.component.SCEDocumentRow;
import sce.component.SCEPane;
import sce.component.SourceCodeEditor;
import sun.print.BackgroundLookupListener;
import util.ParseUtil;

import java.io.File;
import java.util.*;

/**
 * Package import suggester.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class PackageImportSuggester implements CodeAssistant, SCEPopup.ItemHandler {
	private SCEManagerInteraction jle;
	/** Last misspelled word ant its position in the document. */
	private WordWithPos wordUnderCaret = null;

	public PackageImportSuggester(SCEManagerInteraction jle) {
		this.jle = jle;
	}

	public boolean assistAt(SCEPane pane) {
	  // get the word under the caret
	  List<WordWithPos> wordList = CodePattern.commandPattern.find(pane);

		LatexQuickHelp.Element element;
		if (wordList != null) {
			wordUnderCaret = wordList.get(0);
			element = new LatexQuickHelp.Element(LatexQuickHelp.Element.Type.command, wordUnderCaret.word);
		} else {
			wordList = CodePattern.environmentPattern.find(pane);

			if (wordList == null) return false;
			wordUnderCaret = wordList.get(1);
			element = new LatexQuickHelp.Element(LatexQuickHelp.Element.Type.environment, wordUnderCaret.word);
		}

		boolean provided = false;
		String comEnvName = wordUnderCaret.word;
	  if(comEnvName.length() == 0) return false;

		LinkedList<Object> suggestionList = new LinkedList<Object>();

		if (LatexDependencies.isCurrentDocumentClassProviding(element)) {
			suggestionList.add("<html><body bgcolor='#202080'><table><tr><td width='500'><font color='#ffff00'>already provided</font> <font color='#A0A0A0'>by documentclass</font> <font color='#ffffff'>" + element.name);
			provided = true;
		}

		ArrayList<LatexDependencies.PackInfo> packInfos = LatexDependencies.getPackagesProviding(element);
		for (LatexDependencies.PackInfo packInfo : packInfos) {
			switch (packInfo.state) {
				case imported:
					suggestionList.add("<html><body bgcolor='#202080'><table><tr><td width='200'><font color='#ffff00'>already provided</font> <font color='#A0A0A0'>by package</font> <font color='#ffffff'>" + packInfo.pack.getName() + "</font></td><td width='300' color='#ffffff'>" + packInfo.pack.getDescription());
					provided = true;
					break;
				case importable:
					suggestionList.add(new ImportPackage(packInfo.pack));
					break;
			}
		}

		if (!provided) {
			switch (element.type) {
				case command:
					suggestionList.addFirst(new CreateCommand(element.name, argumentsOfCommand(pane, wordUnderCaret)));
			}
		}
		
		if (!suggestionList.isEmpty()) {
			pane.getPopup().openPopup(suggestionList, this);
		}

		return true;
	}

	private int argumentsOfCommand(SCEPane pane, WordWithPos wordUnderCaret) {
		SCEDocument document = pane.getDocument();
		String rest = document.getText(wordUnderCaret.getStartPos(), document.getEndPos());

		int charNr = 0;
		while (Character.isLetter(rest.charAt(charNr))) charNr++;

		int argCount = 0;
		while (rest.charAt(charNr) == '{') {
			String part = ParseUtil.parseBalanced(rest, charNr+1, '}');
			charNr += part.length() + 2;
			argCount++;
		}

		return argCount;
	}

	public void perform(Object item) {
		if (item instanceof CreateCommand) {
			CreateCommand createCommand = (CreateCommand) item;
			String commandBody = "";
			for (int i=0; i<createCommand.arguments; i++) {
				commandBody += "#" + (i+1);
			}
			ExtractCommand.askToDeclareCommand((JLatexEditorJFrame) SCEManager.getInstance(), "\\" + createCommand.commandName, commandBody);
		}
	  if (item instanceof ImportPackage) {
		  ImportPackage importPackage = (ImportPackage) item;
		  importPackage(importPackage.pack.getName());
	  }
	}

	private void importPackage(String packageName) {
		BackgroundParser.FilePos lastPackagePos = getLastPackagePos();

		SourceCodeEditor<Doc> editor = jle.open(new Doc.FileDoc(new File(lastPackagePos.getFile())), false);
		SCEPane pane = editor.getTextPane();

		String usePackageString = "\\usepackage{" + packageName + "}\n";

		pane.getDocument().insert(usePackageString, lastPackagePos.getLineNr() + 1, 0);

		//jle.open(activeEditor.getResource(), 0);
	}

	private BackgroundParser.FilePos getLastPackagePos() {
		BackgroundParser.FilePos lastPos = null;

		// determine last position of \\usepackage
		for (Package pack : SCEManager.getBackgroundParser().getPackages().getObjectsIterable("")) {
			if (lastPos == null || pack.getLineNr() > lastPos.getLineNr()) {
				lastPos = pack;
			}
		}

		if (lastPos == null) {
			// determine position of \documentclass
			lastPos = SCEManager.getBackgroundParser().getDocumentClass();
		}

		if (lastPos == null) {
			// return first line of main document
			SourceCodeEditor<Doc> mainEditor = SCEManager.getInstance().getMainEditor();
			lastPos = new BackgroundParser.FilePos(mainEditor.getName(), mainEditor.getFile().getAbsolutePath(), -1);
		}

		return lastPos;
	}


// inner classes

	private class CreateCommand {
		String commandName;
		int arguments;

		private CreateCommand(String commandName, int arguments) {
			this.commandName = commandName;
			this.arguments = arguments;
		}

		@Override
		public String toString() {
			return "create new command \\" + commandName;
		}
	}
	
	private class ImportPackage {
	  PackagesExtractor.Package pack;

	  ImportPackage(PackagesExtractor.Package pack) {
	    this.pack = pack;
	  }

	  @Override
	  public String toString() {
		  String descString = pack.getDescription() != null ? pack.getDescription() : "";

		  return "<html><body><table><tr><td width='200'><font color='#808080'>import package</font> " + pack.getName() + "</td><td width='300'>" + descString;
	  }
	}
}
