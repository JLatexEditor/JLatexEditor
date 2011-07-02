package jlatexeditor.performance;

import jlatexeditor.Doc;
import jlatexeditor.codehelper.BackgroundParser;
import jlatexeditor.codehelper.SpellCheckSuggester;
import jlatexeditor.gproperties.GProperties;
import jlatexeditor.quickhelp.LatexQuickHelp;
import jlatexeditor.syntaxhighlighting.LatexStyles;
import jlatexeditor.syntaxhighlighting.LatexSyntaxHighlighting;
import sce.codehelper.StaticCommandsCodeHelper;
import sce.codehelper.StaticCommandsReader;
import sce.component.SCECaret;
import sce.component.SCEDocument;
import sce.component.SCEPane;
import sce.component.SourceCodeEditor;
import sce.syntaxhighlighting.SyntaxHighlighting;
import util.Aspell;
import util.SpellChecker;
import util.StreamUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

/**
 * Performance measurement.
 */
public class Performance {
	private static StaticCommandsReader latexCommands = new StaticCommandsReader("data/codehelper/commands.xml");
	private static StaticCommandsReader tabCompletions = new StaticCommandsReader("data/codehelper/liveTemplates.xml");
	private static BackgroundParser backgroundParser = null;

	public static void main(String[] args) throws IOException, FileNotFoundException {
    File file = new File("./src/jlatexeditor/performance/test.tex");
    SourceCodeEditor editor = new SourceCodeEditor(new Doc.FileDoc(file));

    editor.setText(StreamUtils.readFile(file.toString()));
    SCEPane pane = editor.getTextPane();
    SCEDocument document = pane.getDocument();
    SCECaret caret = pane.getCaret();

    LatexStyles.addStyles(document);

	  SpellChecker spellChecker = Aspell.getInstance(GProperties.getAspellLang());
	  //if (spellChecker == null) throw new Exception("Initialization of the spell check suggester failed!");

    SyntaxHighlighting syntaxHighlighting = new LatexSyntaxHighlighting(pane, spellChecker, latexCommands.getCommands(), backgroundParser);
    syntaxHighlighting.start();

    pane.setCodeHelper(new StaticCommandsCodeHelper("(\\\\[a-zA-Z]*)", latexCommands));
    pane.setTabCompletion(new StaticCommandsCodeHelper("([a-zA-Z]*)", tabCompletions));
    pane.setQuickHelp(new LatexQuickHelp("data/quickhelp/"));

		if (spellChecker != null) {
	    pane.addCodeAssistantListener(new SpellCheckSuggester(spellChecker));
		}

    long startTime = System.nanoTime();

    String chars = "abcdefghijklmnopqrstuvwxyz \n{}(){}";
    Random random = new Random(8923489);
    for (int i = 0; i < 200000; i++) {
      int row = random.nextInt(document.getRowsModel().getRowsCount());
      int column = random.nextInt(document.getRowsModel().getRowLength(row) + 1);
      caret.moveTo(row, column, false);

      if (random.nextBoolean()) {
        char c = chars.charAt(random.nextInt(chars.length() - 1));
        document.insert(c + "", caret.getRow(), caret.getColumn());
      }
      if (random.nextBoolean()) {
        document.remove(caret.getRow(), Math.max(0, caret.getColumn() - random.nextInt(3)), caret.getRow(), caret.getColumn());
      }
    }

    System.out.println("time: " + (((System.nanoTime() - startTime) / 10000000) / 100.));
  }
}
