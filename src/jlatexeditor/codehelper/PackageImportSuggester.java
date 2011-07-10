package jlatexeditor.codehelper;

import jlatexeditor.*;
import sce.codehelper.CodeAssistant;
import sce.codehelper.PatternPair;
import sce.codehelper.SCEPopup;
import sce.codehelper.WordWithPos;
import sce.component.SCEPane;
import sce.component.SourceCodeEditor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Package import suggester.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class PackageImportSuggester implements CodeAssistant, SCEPopup.ItemHandler {
	public static final PatternPair wordPattern = new PatternPair("\\\\(\\p{L}*)", "(\\p{L}*)");

	private SCEManagerInteraction jle;
	/** Last misspelled word ant its position in the document. */
	private WordWithPos wordUnderCaret = null;

	public PackageImportSuggester(SCEManagerInteraction jle) {
		this.jle = jle;
	}

	public boolean assistAt(SCEPane pane) {
	  // get the word under the caret
	  List<WordWithPos> wordList = wordPattern.find(pane);

	  if (wordList == null) return false;

	  wordUnderCaret = wordList.get(0);
		String commandName = wordUnderCaret.word;
	  if(commandName.length() == 0) return false;

		ArrayList<Object> suggestionList = new ArrayList<Object>();

		BackgroundParser backgroundParser = SCEManager.getBackgroundParser();
		if (backgroundParser.getDocumentClass() != null) {
			String docClass = backgroundParser.getDocumentClass().getName();

			HashSet<PackagesExtractor.Command> commands = PackagesExtractor.getDocClassesParser().getCommands().get(commandName);

			if (commands != null) {
				for (PackagesExtractor.Command command : commands) {
					if (command.getPack().getName().equals(docClass)) {
						suggestionList.add("<html><body bgcolor='#202080'><table><tr><td width='500'><font color='#ffff00'>already provided</font> <font color='#A0A0A0'>by documentclass</font> <font color='#ffffff'>" + command.getPack().getName());
						break;
					}
				}
			}
		}

		HashSet<PackagesExtractor.Command> commands = PackagesExtractor.getPackageParser().getCommands().get(commandName);

		if (commands != null) {
			// build up lists of imported and importable packages providing the command
			ArrayList<PackagesExtractor.Package> importablePackages = new ArrayList<PackagesExtractor.Package>();
			ArrayList<PackagesExtractor.Package> importedPackages = new ArrayList<PackagesExtractor.Package>();
			for (PackagesExtractor.Command command : commands) {
				PackagesExtractor.Package pack = command.getPack();
				if (!importedPackages.contains(pack) && !importablePackages.contains(pack)) {
					if (backgroundParser.getPackages().contains(pack.getName())) {
						importedPackages.add(pack);
					} else {
						importablePackages.add(pack);
					}
				}
			}
			Collections.sort(importedPackages);
			Collections.sort(importablePackages);

			for (PackagesExtractor.Package pack : importedPackages) {
				String descString = pack.getDescription() != null ? pack.getDescription() : "";
				suggestionList.add("<html><body bgcolor='#202080'><table><tr><td width='200'><font color='#ffff00'>already provided</font> <font color='#A0A0A0'>by package</font> <font color='#ffffff'>" + pack.getName() + "</font></td><td width='300' color='#ffffff'>" + descString);
			}
			for (PackagesExtractor.Package pack : importablePackages) {
				suggestionList.add(new ImportPackage(pack));
			}
		}

		if (!suggestionList.isEmpty()) {
			pane.getPopup().openPopup(suggestionList, this);
		}

		return true;
	}

	public void perform(Object item) {
	  if (item instanceof ImportPackage) {
		  ImportPackage importPackage = (ImportPackage) item;
		  importPackage(importPackage.pack.getName());
	  }
	}

	private void importPackage(String packageName) {
		BackgroundParser.FilePos lastPackagePos = getLastPackagePos();

		SourceCodeEditor<Doc> editor = jle.getEditor(new Doc.FileDoc(new File(lastPackagePos.getFile())));
		SCEPane pane = editor.getTextPane();

		SourceCodeEditor<Doc> activeEditor = jle.getActiveEditor();

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
