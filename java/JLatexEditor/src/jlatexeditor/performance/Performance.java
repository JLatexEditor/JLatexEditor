package jlatexeditor.performance;

import jlatexeditor.ErrorView;
import jlatexeditor.codehelper.LatexCodeHelper;
import jlatexeditor.codehelper.SpellCheckSuggester;
import jlatexeditor.quickhelp.LatexQuickHelp;
import jlatexeditor.syntaxhighlighting.LatexStyles;
import jlatexeditor.syntaxhighlighting.LatexSyntaxHighlighting;
import sce.component.*;
import sce.syntaxhighlighting.SyntaxHighlighting;
import util.StreamUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

/**
 * Performance measurement.
 */
public class Performance {
  public static void main(String[] args) throws IOException, FileNotFoundException {
    File file = new File("./src/jlatexeditor/performance/test.tex");
    SourceCodeEditor editor = new SourceCodeEditor(file);

    editor.setText(StreamUtils.readFile(file.toString()));
    SCEPane pane = editor.getTextPane();
    SCEDocument document = pane.getDocument();
    SCECaret caret = pane.getCaret();

    LatexStyles.addStyles(document);

    SyntaxHighlighting syntaxHighlighting = new LatexSyntaxHighlighting(pane);
    syntaxHighlighting.start();

    pane.setCodeHelper(new LatexCodeHelper("data/codehelper/commands.xml"));
    pane.setTabCompletion(new LatexCodeHelper("data/codehelper/tabCompletion.xml"));
    pane.setQuickHelp(new LatexQuickHelp("data/quickhelp/"));

    try {
      pane.addCodeAssistantListener(new SpellCheckSuggester());
    } catch (IOException e) {
      System.err.println("Initialization of the spell check suggester failed!");
      e.printStackTrace();
    }

    long startTime = System.nanoTime();

    String chars = "abcdefghijklmnopqrstuvwxyz \n{}(){}";
    Random random = new Random(8923489);
    for(int i = 0; i < 200000; i++) {
      int row = random.nextInt(document.getRowsCount());
      int column = random.nextInt(document.getRowLength(row)+1);
      caret.moveTo(row, column);

      if(random.nextBoolean()) {
        char c = chars.charAt(random.nextInt(chars.length()-1));
        document.insert(c + "", caret.getRow(), caret.getColumn());
      }
      if(random.nextBoolean()) {
        document.remove(caret.getRow(), Math.max(0, caret.getColumn() - random.nextInt(3)), caret.getRow(), caret.getColumn());
      }
    }

    System.out.println("time: " + (((System.nanoTime() - startTime) / 10000000) / 100.));
  }
}
