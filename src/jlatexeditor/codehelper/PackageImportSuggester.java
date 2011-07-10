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

		HashSet<PackagesExtractor.Command> commands = PackagesExtractor.getPackageParser().getCommands().get(commandName);

		// build up lists of imported and importable packages providing the command
		ArrayList<String> importablePackages = new ArrayList<String>();
		ArrayList<String> importedPackages = new ArrayList<String>();
		for (PackagesExtractor.Command command : commands) {
			String packageName = command.getPack().getName();
			if (!importedPackages.contains(packageName) && !importablePackages.contains(packageName)) {
				if (SCEManager.getBackgroundParser().getPackages().contains(packageName)) {
					importedPackages.add(packageName);
				} else {
					importablePackages.add(packageName);
				}
			}
		}
		Collections.sort(importedPackages);
		Collections.sort(importablePackages);

		ArrayList<Object> importPackages = new ArrayList<Object>();
		for (String pack : importedPackages) {
			importPackages.add("<html><body bgcolor='#404040'><font color='#808080'>package</font> <font color='#ffffff'>" + pack + "</font> <font color='#808080'>already imported</font></s>");
		}
		for (String pack : importablePackages) {
			importPackages.add(new ImportPackage(pack));
		}

		pane.getPopup().openPopup(importPackages, this);
		return true;
	}

	public void perform(Object item) {
	  if (item instanceof ImportPackage) {
		  ImportPackage importPackage = (ImportPackage) item;
		  importPackage(importPackage.name);
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
	  String name;

	  ImportPackage(String name) {
	    this.name = name;
	  }

	  @Override
	  public String toString() {
	    return "<html><body><font color='#808080'>import package</font> " + name;
	  }
	}
}
