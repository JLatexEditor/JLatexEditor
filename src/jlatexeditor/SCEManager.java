package jlatexeditor;

import jlatexeditor.bib.BibCodeHelper;
import jlatexeditor.bib.BibSyntaxHighlighting;
import jlatexeditor.changelog.ChangeLogStyles;
import jlatexeditor.changelog.ChangeLogSyntaxHighlighting;
import jlatexeditor.codehelper.*;
import jlatexeditor.gproperties.GProperties;
import jlatexeditor.gproperties.GPropertiesCodeHelper;
import jlatexeditor.gproperties.GPropertiesStyles;
import jlatexeditor.gproperties.GPropertiesSyntaxHighlighting;
import jlatexeditor.quickhelp.LatexQuickHelp;
import jlatexeditor.syntaxhighlighting.LatexStyles;
import jlatexeditor.syntaxhighlighting.LatexSyntaxHighlighting;
import sce.codehelper.CombinedCodeAssistant;
import sce.codehelper.CombinedCodeHelper;
import sce.codehelper.StaticCommandsCodeHelper;
import sce.codehelper.StaticCommandsReader;
import sce.component.SCEDocument;
import sce.component.SCEPane;
import sce.component.SourceCodeEditor;
import sce.syntaxhighlighting.SyntaxHighlighting;
import util.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Managing the creation of SourceCodeEditors.
 */
public class SCEManager {
  private static SCEManagerInteraction instance = null;

  private static BackgroundParser backgroundParser = null;
  private static StaticCommandsReader latexCommands = new StaticCommandsReader("data/codehelper/commands.xml");
  private static StaticCommandsReader tabCompletions = new StaticCommandsReader("data/codehelper/liveTemplates.xml");

  public static SCEManagerInteraction getInstance() {
    return instance;
  }

  public static void setInstance(SCEManagerInteraction instance) {
    SCEManager.instance = instance;
  }

  public static BackgroundParser getBackgroundParser() {
    return backgroundParser;
  }

  public static void setBackgroundParser(BackgroundParser backgroundParser) {
    SCEManager.backgroundParser = backgroundParser;
  }

  public static StaticCommandsReader getLatexCommands() {
    return latexCommands;
  }

  public static StaticCommandsReader getTabCompletions() {
    return tabCompletions;
  }

  public static SourceCodeEditor<Doc> createLatexSourceCodeEditor() {
    SourceCodeEditor<Doc> editor = new SourceCodeEditor<Doc>(null);
    setupLatexSCEPane(editor.getTextPane());

    new JumpTo(editor);

    return editor;
  }

  public static void setupLatexSCEPane(SCEPane scePane) {
    setPaneProperties(scePane);
    SCEDocument document = scePane.getDocument();

    // add some styles to the document
    LatexStyles.addStyles(document);

	  SpellChecker spellChecker = null;
	  try {
		  spellChecker = createSpellChecker();
	  } catch (Exception ignored) {}

    // syntax highlighting
    SyntaxHighlighting syntaxHighlighting = new LatexSyntaxHighlighting(scePane, spellChecker, latexCommands.getCommands(), backgroundParser);
    syntaxHighlighting.start();

    // code completion and quick help
    CombinedCodeHelper codeHelper = new CombinedCodeHelper();
	  if (backgroundParser != null) {
			codeHelper.addPatternHelper(new CiteHelper(backgroundParser));
		  // add completion for \ref and \eqref
			codeHelper.addPatternHelper(new GenericCodeHelper("\\\\(?:ref|eqref)\\{([^{}]*)", new Function0<AbstractTrie<?>>() {
				public Trie<?> apply() {
					return backgroundParser.getLabelDefs();
				}
			}));
		  // add completion for \label
			codeHelper.addPatternHelper(new GenericCodeHelper("\\\\label\\{([^{}]*)", new Function0<AbstractTrie<?>>() {
				public AbstractTrie<?> apply() {
					return backgroundParser.getLabelRefs();
				}
			}));
	  }
    codeHelper.addPatternHelper(new UsePackageCodeHelper());
    codeHelper.addPatternHelper(new DocumentClassCodeHelper());
    codeHelper.addPatternHelper(new BeamerCodeHelper());
    codeHelper.addPatternHelper(new IncludeCodeHelper());
	  codeHelper.addPatternHelper(new CommandsCodeHelper());
	  codeHelper.addPatternHelper(new EnvironmentCodeHelper());
	  if (backgroundParser != null) {
	    codeHelper.addPatternHelper(new WordCompletion(backgroundParser));
	  }
	  codeHelper.setAutoCompletion(GProperties.getBoolean("editor.auto_completion.activated"));
	  codeHelper.setAutoCompletionMinLetters(GProperties.getInt("editor.auto_completion.min_number_of_letters"));
	  codeHelper.setAutoCompletionDelay(GProperties.getInt("editor.auto_completion.delay"));
    scePane.setCodeHelper(codeHelper);
    scePane.setTabCompletion(new StaticCommandsCodeHelper("(\\p{L}*)", tabCompletions));
    scePane.setQuickHelp(new LatexQuickHelp("data/quickhelp/"));
    scePane.setLineBreakListener(new LatexLineBreakListener());

	  CombinedCodeAssistant codeAssistant = new CombinedCodeAssistant();
    try {
	    codeAssistant.addAssistant(new FileCreationSuggester());
      codeAssistant.addAssistant(new PackageImportSuggester(instance));
      codeAssistant.addAssistant(new SpellCheckSuggester(createSpellChecker()));
    } catch (Exception ignored) {
    }
	  scePane.addCodeAssistantListener(codeAssistant);
  }

	public static SpellChecker createSpellChecker() throws Exception {
		String program = GProperties.getString("editor.spell_checker");

		if (program.equals("aspell")) {
			SpellChecker spellChecker = Aspell.getInstance(GProperties.getAspellLang());
			if (spellChecker == null) throw new Exception("Initialization of the spell checker failed!");
			return spellChecker;
		} else
		if (program.equals("hunspell")) {
			// set executables
			Hunspell.HUNSPELL_EXECUTABLE = GProperties.getString("hunspell.executable");

			SpellChecker spellChecker = Hunspell.getInstance(GProperties.getString("hunspell.lang"));
			if (spellChecker == null) throw new Exception("Initialization of the spell checker failed!");
			return spellChecker;
		}

		return null;
	}

	public static SourceCodeEditor<Doc> createBibSourceCodeEditor() {
	  SourceCodeEditor<Doc> editor = new SourceCodeEditor<Doc>(null);

	  SCEPane scePane = editor.getTextPane();
	  setPaneProperties(scePane);
	  SCEDocument document = scePane.getDocument();

	  // TODO: use other styles
	  // add some styles to the document
		LatexStyles.addStyles(document);

	  // syntax highlighting
	  BibSyntaxHighlighting syntaxHighlighting = new BibSyntaxHighlighting(scePane, backgroundParser);
	  syntaxHighlighting.start();

		// code completion and quick help
		CombinedCodeHelper codeHelper = new CombinedCodeHelper();
		if (backgroundParser != null) {
			codeHelper.addPatternHelper(new BibCodeHelper());
		}
		//codeHelper.addPatternHelper(new IncludeCodeHelper());
		codeHelper.addPatternHelper(new CommandsCodeHelper());
		if (backgroundParser != null) {
			codeHelper.addPatternHelper(new WordCompletion(backgroundParser));
		}
		codeHelper.setAutoCompletion(GProperties.getBoolean("editor.auto_completion.activated"));
		codeHelper.setAutoCompletionMinLetters(GProperties.getInt("editor.auto_completion.min_number_of_letters"));
		codeHelper.setAutoCompletionDelay(GProperties.getInt("editor.auto_completion.delay"));
		scePane.setCodeHelper(codeHelper);
		scePane.setTabCompletion(new StaticCommandsCodeHelper("(\\p{L}*)", tabCompletions));
		scePane.setQuickHelp(new LatexQuickHelp("data/quickhelp/"));

		CombinedCodeAssistant codeAssistant = new CombinedCodeAssistant();
		try {
		  codeAssistant.addAssistant(new SpellCheckSuggester(createSpellChecker()));
		} catch (Exception ignored) {
		}
		scePane.addCodeAssistantListener(codeAssistant);

	  return editor;
	}

  public static SourceCodeEditor<Doc> createGPropertiesSourceCodeEditor() {
    SourceCodeEditor<Doc> editor = new SourceCodeEditor<Doc>(null);

    SCEPane scePane = editor.getTextPane();
    setPaneProperties(scePane);
    SCEDocument document = scePane.getDocument();

    // add some styles to the document
    GPropertiesStyles.addStyles(document);

    // syntax highlighting
    GPropertiesSyntaxHighlighting syntaxHighlighting = new GPropertiesSyntaxHighlighting(scePane);
    syntaxHighlighting.start();

    // code completion and quick help
    CombinedCodeHelper codeHelper = new CombinedCodeHelper();
    codeHelper.addPatternHelper(new GPropertiesCodeHelper());
    scePane.setCodeHelper(codeHelper);

    return editor;
  }

  public static SourceCodeEditor<Doc> createChangeLogSourceCodeEditor() {
    SourceCodeEditor<Doc> editor = new SourceCodeEditor<Doc>(null);

    SCEPane scePane = editor.getTextPane();
    setPaneProperties(scePane);
    SCEDocument document = scePane.getDocument();

    // add some styles to the document
    ChangeLogStyles.addStyles(document);

    // syntax highlighting
    ChangeLogSyntaxHighlighting syntaxHighlighting = new ChangeLogSyntaxHighlighting(scePane);
    syntaxHighlighting.start();

    return editor;
  }

  private static void setPaneProperties(final SCEPane pane) {
    pane.setColumnsPerRow(GProperties.getInt("editor.columns_per_row"));
    GProperties.addPropertyChangeListener("editor.columns_per_row", new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        pane.setColumnsPerRow(GProperties.getInt("editor.columns_per_row"));
      }
    });
  }
}
