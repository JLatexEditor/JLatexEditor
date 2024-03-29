package jlatexeditor;

import de.endrullis.utils.LazyVal;
import jlatexeditor.bib.BibAssistant;
import jlatexeditor.bib.BibCodeCompletion;
import jlatexeditor.bib.BibSyntaxHighlighting;
import jlatexeditor.changelog.ChangeLogStyles;
import jlatexeditor.changelog.ChangeLogSyntaxHighlighting;
import jlatexeditor.codehelper.*;
import jlatexeditor.gproperties.GProperties;
import jlatexeditor.gproperties.GPropertiesCodeCompletion;
import jlatexeditor.gproperties.GPropertiesStyles;
import jlatexeditor.gproperties.GPropertiesSyntaxHighlighting;
import jlatexeditor.gui.TemplateEditor;
import jlatexeditor.quickhelp.LatexQuickHelp;
import jlatexeditor.syntaxhighlighting.LatexStyles;
import jlatexeditor.syntaxhighlighting.LatexSyntaxHighlighting;
import jlatexeditor.syntaxhighlighting.TemplateSyntaxHighlighting;
import sce.codehelper.*;
import sce.component.SCEDocument;
import sce.component.SCEPane;
import sce.component.SourceCodeEditor;
import sce.syntaxhighlighting.SyntaxHighlighting;
import util.*;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Properties;

/**
 * Managing the creation of SourceCodeEditors.
 */
public class SCEManager {
  private static SCEManagerInteraction instance = null;
  private static JFrame mainWindow = null;

  private static BackgroundParser backgroundParser = null;
	private static final String USER_LIVE_TEMPLATES_FILENAME = GProperties.SETTINGS_DIR + "/liveTemplates.xml";
  private static StaticCommandsReader latexCommands = new StaticCommandsReader("data/codehelper/commands.xml");
  private static StaticCommandsReader systemTabCompletion = new StaticCommandsReader("data/codehelper/liveTemplates.xml");
	private static StaticCommandsReader userTabCompletion = new StaticCommandsReader(USER_LIVE_TEMPLATES_FILENAME, true);
  private static SimpleCommandsReader tabCompletion = new SimpleCommandsReader(new SimpleMergedTrie(new Comparator<CHCommand>() {
	  public int compare(CHCommand o1, CHCommand o2) {
		  return o1.getName().compareTo(o2.getName());
	  }
  }, userTabCompletion.getCommands(), systemTabCompletion.getCommands()));

	private static Properties iconMap = new Properties() {{
		try {
			load(StreamUtils.getInputStream("data/icons/icon_map.properties"));
		} catch (IOException ignored) {}
	}};

	private static ArrayList<Image> WINDOW_ICONS = new ArrayList<Image>() {{
		try {
			for (int size : new int[]{16, 32, 64}) {
				add(getDirectImageIcon("images/tex-cookie_" + size + ".png").getImage());
			}
		} catch (Exception ignored) {}
	}};

  public static SCEManagerInteraction getInstance() {
    return instance;
  }

  public static void setInstance(SCEManagerInteraction instance) {
    SCEManager.instance = instance;
  }

	public static JFrame getMainWindow() {
		return mainWindow;
	}

	public static void setMainWindow(JFrame mainWindow) {
		SCEManager.mainWindow = mainWindow;
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

	public static SimpleTrie<CHCommand> getSystemTabCompletion() {
		return systemTabCompletion.getCommands();
	}

	public static SimpleTrie<CHCommand> getUserTabCompletion() {
		return userTabCompletion.getCommands();
	}

	public static void saveUserTabCompletion() {
		try {
			StaticCommandsWriter.writeToFile(new File(USER_LIVE_TEMPLATES_FILENAME), userTabCompletion);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static AbstractSimpleTrie<CHCommand> getTabCompletion() {
    return tabCompletion.getCommands();
  }

  public static SourceCodeEditor<Doc> createLatexSourceCodeEditor() {
    SourceCodeEditor<Doc> editor = new SourceCodeEditor<Doc>(null);
    setupLatexSCEPane(editor.getTextPane());

    new JumpTo(editor);

    return editor;
  }

  public static SourceCodeEditor<Doc> createTemplateSourceCodeEditor(TemplateEditor templateEditor) {
    SourceCodeEditor<Doc> editor = new SourceCodeEditor<Doc>(null);
    SCEPane scePane = editor.getTextPane();

	  SpellChecker spellChecker = null;
	  try {
		  spellChecker = createSpellChecker();
	  } catch (Exception ignored) {}

    // syntax highlighting
    SyntaxHighlighting syntaxHighlighting = new TemplateSyntaxHighlighting(scePane, spellChecker, latexCommands.getCommands(), backgroundParser, templateEditor);
	  CombinedCodeCompletion codeCompletion = new CombinedCodeCompletion();
	  CombinedCodeAssistant codeAssistant = new CombinedCodeAssistant();
	  codeAssistant.addAssistant(new TemplateArgumentSuggester(templateEditor));

	  setupLatexSCEPane(scePane, syntaxHighlighting, codeCompletion, codeAssistant);

    return editor;
  }

	public static void setupLatexSCEPane(SCEPane scePane) {
		SpellChecker spellChecker = null;
		try {
			spellChecker = createSpellChecker();
		} catch (Exception ignored) {}

	  // syntax highlighting
	  SyntaxHighlighting syntaxHighlighting = new LatexSyntaxHighlighting(scePane, spellChecker, latexCommands.getCommands(), backgroundParser);
		CombinedCodeCompletion codeCompletion = new CombinedCodeCompletion();
		CombinedCodeAssistant codeAssistant = new CombinedCodeAssistant();

		setupLatexSCEPane(scePane, syntaxHighlighting, codeCompletion, codeAssistant);
	}

  private static void setupLatexSCEPane(SCEPane scePane, SyntaxHighlighting syntaxHighlighting, CombinedCodeCompletion codeCompletion, CombinedCodeAssistant codeAssistant) {
    setPaneProperties(scePane);
    SCEDocument document = scePane.getDocument();

    // add some styles to the document
    LatexStyles.addStyles(document);

    syntaxHighlighting.start();

    // code completion and quick help
	  if (backgroundParser != null) {
			codeCompletion.addPatternCompletion(new CiteCompletion(backgroundParser));
		  // add completion for \ref and \eqref
			codeCompletion.addPatternCompletion(new GenericCodeCompletion("\\\\(?:ref|eqref)\\{([^{}]*)", new Function0<Trie<?>>() {
				public SimpleTrie<?> apply() {
					return backgroundParser.getLabelDefs();
				}
			}));
		  // add completion for \label
			codeCompletion.addPatternCompletion(new GenericCodeCompletion("\\\\label\\{([^{}]*)", new Function0<Trie<?>>() {
				public Trie<?> apply() {
					return backgroundParser.getLabelRefs();
				}
			}));
	  }
    codeCompletion.addPatternCompletion(new UsePackageCodeCompletion());
    codeCompletion.addPatternCompletion(new DocumentClassCodeCompletion());
    codeCompletion.addPatternCompletion(new BeamerCodeCompletion());
    codeCompletion.addPatternCompletion(new IncludeCodeCompletion());
	  codeCompletion.addPatternCompletion(new CommandsCodeCompletion());
	  codeCompletion.addPatternCompletion(new EnvironmentCodeCompletion());
	  codeCompletion.addPatternCompletion(new ValueListCompletion(new HashMap<String, LazyVal<SimpleTrie<String>>>() {{
      put("bibliographystyle", PackagesExtractor.getBibStyles());
      put("usetikzlibrary", PackagesExtractor.getTikzLibraries());
    }}));
	  if (backgroundParser != null) {
	    codeCompletion.addPatternCompletion(new WordCompletion(backgroundParser));
	  }
	  codeCompletion.setAutoCompletion(GProperties.getBoolean("editor.auto_completion.activated"));
	  codeCompletion.setAutoCompletionMinLetters(GProperties.getInt("editor.auto_completion.min_number_of_letters"));
	  codeCompletion.setAutoCompletionDelay(GProperties.getInt("editor.auto_completion.delay"));
    scePane.setCodeCompletion(codeCompletion);
    scePane.setTabCompletion(new StaticCommandsCodeCompletion("(\\p{L}*)", tabCompletion));
    scePane.setQuickHelp(new LatexQuickHelp("data/quickhelp/"));
    scePane.setLineBreakListener(new LatexLineBreakListener());

    try {
      codeAssistant.addAssistant(new ScriptingSupport());
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

	public static SourceCodeEditor<Doc> createBibSourceCodeEditor(JLatexEditorJFrame jle) {
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
		CombinedCodeCompletion codeCompletion = new CombinedCodeCompletion();
		if (backgroundParser != null) {
			codeCompletion.addPatternCompletion(new BibCodeCompletion());
		}
		//codeCompletion.addPatternCompletion(new IncludeCodeCompletion());
		codeCompletion.addPatternCompletion(new CommandsCodeCompletion());
		if (backgroundParser != null) {
			codeCompletion.addPatternCompletion(new WordCompletion(backgroundParser));
		}
		codeCompletion.setAutoCompletion(GProperties.getBoolean("editor.auto_completion.activated"));
		codeCompletion.setAutoCompletionMinLetters(GProperties.getInt("editor.auto_completion.min_number_of_letters"));
		codeCompletion.setAutoCompletionDelay(GProperties.getInt("editor.auto_completion.delay"));
		scePane.setCodeCompletion(codeCompletion);
		scePane.setTabCompletion(new StaticCommandsCodeCompletion("(\\p{L}*)", tabCompletion));
		scePane.setQuickHelp(new LatexQuickHelp("data/quickhelp/"));

		CombinedCodeAssistant codeAssistant = new CombinedCodeAssistant();
		try {
		  codeAssistant.addAssistant(new BibAssistant(jle));
		  codeAssistant.addAssistant(new SpellCheckSuggester(createSpellChecker()));
		} catch (Exception ignored) { ignored.printStackTrace(); }
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
    CombinedCodeCompletion codeCompletion = new CombinedCodeCompletion();
    codeCompletion.addPatternCompletion(new GPropertiesCodeCompletion());
    scePane.setCodeCompletion(codeCompletion);

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

	/**
	 * Loads an icon by filename (including path).
	 *
	 * @param filename path to file
	 * @return icon
	 * @throws IOException if an I/O error occurs
	 */
	public static ImageIcon getDirectImageIcon(String filename) throws IOException {
		return new ImageIcon(StreamUtils.readBytesFromInputStream(StreamUtils.getInputStream(filename)));
	}

	/**
	 * Loads the icon defined by the mapping data/icons/icon_map.properties.
	 *
	 * @param key mapping key
	 * @return icon
	 * @throws IOException if an I/O error occurs
	 */
	public static ImageIcon getMappedImageIcon(String key) throws IOException {
		return getDirectImageIcon(iconMap.getProperty(key));
	}

	public static void setWindowIcon(Window window) {
		// set icons
		try {
			window.setIconImages(WINDOW_ICONS);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
