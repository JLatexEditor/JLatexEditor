package jlatexeditor.codehelper;

import jlatexeditor.*;
import sce.codehelper.CodeAssistant;
import sce.codehelper.PatternPair;
import sce.codehelper.SCEPopup;
import sce.codehelper.WordWithPos;
import sce.component.SCEPane;
import sce.component.SourceCodeEditor;
import sun.print.BackgroundLookupListener;

import java.io.File;
import java.util.*;

/**
 * Package import suggester.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class PackageImportSuggester implements CodeAssistant, SCEPopup.ItemHandler {
	public static final PatternPair commandPattern = new PatternPair("\\\\(\\p{L}*)", "(\\p{L}*)");
	public static final PatternPair environmentPattern = new PatternPair("\\\\begin\\{(\\p{L}*)", "(\\p{L}*)\\}");

	private SCEManagerInteraction jle;
	/** Last misspelled word ant its position in the document. */
	private WordWithPos wordUnderCaret = null;

	public PackageImportSuggester(SCEManagerInteraction jle) {
		this.jle = jle;
	}

	public boolean assistAt(SCEPane pane) {
	  // get the word under the caret
	  List<WordWithPos> wordList = commandPattern.find(pane);

		boolean isCommand = wordList != null;

		if (!isCommand) {
			wordList = environmentPattern.find(pane);

			if (wordList == null) return false;
		}

	  wordUnderCaret = wordList.get(0);
		String comEnvName = wordUnderCaret.word;
	  if(comEnvName.length() == 0) return false;

		ArrayList<Object> suggestionList = new ArrayList<Object>();

		BackgroundParser backgroundParser = SCEManager.getBackgroundParser();
		if (backgroundParser.getDocumentClass() != null) {
			String docClass = backgroundParser.getDocumentClass().getName();

			HashSet<? extends PackagesExtractor.ComEnv> comEnvs;
			if (isCommand) {
				comEnvs = PackagesExtractor.getDocClassesParser().getCommands().get(comEnvName);
			} else {
				comEnvs = PackagesExtractor.getDocClassesParser().getEnvironments().get(comEnvName);
			}

			// is document class providing the command / environment
			if (comEnvs != null) {
				for (PackagesExtractor.ComEnv comEnv : comEnvs) {
					if (comEnv.getPack().getName().equals(docClass)) {
						suggestionList.add("<html><body bgcolor='#202080'><table><tr><td width='500'><font color='#ffff00'>already provided</font> <font color='#A0A0A0'>by documentclass</font> <font color='#ffffff'>" + comEnv.getPack().getName());
						break;
					}
				}
			}
		}

		HashSet<? extends PackagesExtractor.ComEnv> comEnvs;
		if (isCommand) {
			comEnvs = PackagesExtractor.getPackageParser().getCommands().get(comEnvName);
		} else {
			comEnvs = PackagesExtractor.getPackageParser().getEnvironments().get(comEnvName);
		}

		if (comEnvs != null) {
			// build HashSet with all packages directly and indirectly imported in this document
			HashSet<PackagesExtractor.Package> indirectlyImportedPackagesHash = backgroundParser.getIndirectlyImportedPackages();

			// build HashSet with all packages directly or indirectly providing the given command
			HashSet<PackagesExtractor.Package> dependentPackagesHash = new HashSet<PackagesExtractor.Package>();
			for (PackagesExtractor.ComEnv comEnv : comEnvs) {
				comEnv.getPack().addDependantPackagesRecursively(dependentPackagesHash);
			}

			// build up lists of imported and importable packages providing the command
			ArrayList<PackagesExtractor.Package> importablePackages = new ArrayList<PackagesExtractor.Package>();
			ArrayList<PackagesExtractor.Package> importedPackages = new ArrayList<PackagesExtractor.Package>();
			for (PackagesExtractor.Package pack : dependentPackagesHash) {
				if (indirectlyImportedPackagesHash.contains(pack)) {
					importedPackages.add(pack);
				} else {
					importablePackages.add(pack);
				}
			}
			/*
			for (PackagesExtractor.Command command : commands) {
				PackagesExtractor.Package pack = command.getPack();
				for (PackagesExtractor.Package depPack : pack.getDependantPackagesRecursively()) {
					if (!importedPackages.contains(depPack) && !importablePackages.contains(depPack)) {
						if (backgroundParser.getPackages().contains(pack.getName())) {
							importedPackages.add(pack);
						} else {
							importablePackages.add(pack);
						}
					}
				}
			}
			*/
			Comparator<PackagesExtractor.Package> comparator = new Comparator<PackagesExtractor.Package>() {
				@Override
				public int compare(PackagesExtractor.Package o1, PackagesExtractor.Package o2) {
					if (o1.getUsageCount() > o2.getUsageCount()) return -1;
					if (o1.getUsageCount() == o2.getUsageCount()) return o1.getName().compareToIgnoreCase(o2.getName());
					return 1;
				}
			};
			Collections.sort(importedPackages, comparator);
			Collections.sort(importablePackages, comparator);

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