package jlatexeditor.codehelper;

import jlatexeditor.*;
import sce.codehelper.CodeAssistant;
import sce.codehelper.PatternPair;
import sce.codehelper.SCEPopup;
import sce.codehelper.WordWithPos;
import sce.component.SCEDocument;
import sce.component.SCEPane;
import sce.component.SCESearch;
import sce.component.SourceCodeEditor;

import java.io.File;
import java.io.IOException;
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

		ArrayList<String> packages = new ArrayList<String>();
		for (PackagesExtractor.Command command : commands) {
			String packageName = command.getPack().getName();
			if (!packages.contains(packageName)) {
				packages.add(packageName);
			}
		}
		Collections.sort(packages);

		ArrayList<PackageImport> packageImports = new ArrayList<PackageImport>();
		for (String pack : packages) {
			packageImports.add(new PackageImport(pack));
		}

		pane.getPopup().openPopup(packageImports, this);
		return true;
	}

	public void perform(Object item) {
	  if (item instanceof PackageImport) {
		  PackageImport packageImport = (PackageImport) item;
		  importPackage(packageImport.name);
	  }
	}

	private void importPackage(String packageName) {
		Package lastPackagePos = getLastPackagePos();

		SourceCodeEditor<Doc> editor = jle.getEditor(new Doc.FileDoc(new File(lastPackagePos.getFile())));
		SCEPane pane = editor.getTextPane();

		SourceCodeEditor<Doc> activeEditor = jle.getActiveEditor();

		String usePackageString = "\\usepackage{" + packageName + "}\n";

		pane.getDocument().insert(usePackageString, lastPackagePos.getLineNr() + 1, 0);

		//jle.open(activeEditor.getResource(), 0);
	}

	private Package getLastPackagePos() {
		Package lastPackage = null;
		for (Package pack : SCEManager.getBackgroundParser().getPackages().getObjectsIterable("")) {
			if (lastPackage == null || pack.getLineNr() > lastPackage.getLineNr()) {
				lastPackage = pack;
			}
		}

		// TODO
		if (lastPackage == null) {
		}

		return lastPackage;
	}


// inner classes

	private class PackageImport {
	  String name;

	  PackageImport(String name) {
	    this.name = name;
	  }

	  @Override
	  public String toString() {
	    return "<html><body><font color='#808080'>import package</font> " + name;
	  }
	}
}
