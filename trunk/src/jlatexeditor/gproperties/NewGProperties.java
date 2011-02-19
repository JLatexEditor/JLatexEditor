package jlatexeditor.gproperties;

import de.endrullis.utils.ConfigProperties;

import javax.swing.*;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class NewGProperties extends ConfigProperties {
	/*
	public static NewGProperties inst = new NewGProperties();

	public BooleanProperty check_for_updates;
	public BooleanProperty ask_for_saving_files_before_closing;
	public BooleanProperty check_for_svn_updates;
	public MainWindow main_window;
	public Editor editor;
	public Shortcut shortcut;
	public Compiler compiler;
	public StringProperty forward_search_viewer;
	public IntProperty inverse_search_port;
	public LogLevelProperty log_level_jlatexeditor;
	public LogLevelProperty log_level_sce;

	public NewGProperties() {
		comment("\n## General properties");
		comment(" Check for updates");
		check_for_updates = new BooleanProperty("check_for_updates", true);
		comment(" When closing the editor");
		ask_for_saving_files_before_closing = new BooleanProperty("ask_for_saving_files_before_closing", true);
		comment(" Check for svn updates in the background");
		check_for_svn_updates = new BooleanProperty("check_for_svn_updates", true);

		main_window = new MainWindow();
		editor = new Editor();
		shortcut = new Shortcut();
		compiler = new Compiler();

		comment("\n## Forward and inverse search");
		forward_search_viewer = new StringProperty("forward search.viewer", "");
		inverse_search_port = new IntProperty("inverse search.port", 13231, null, 0, Integer.MAX_VALUE);

		comment("\n## Debugging");
		log_level_jlatexeditor = new LogLevelProperty("log level.jlatexeditor", null, "<ignored>");
		log_level_sce = new LogLevelProperty("log level.sce", null, "<ignored>");
	}

	public class MainWindow {
		public IntProperty x;
		public IntProperty y;
		public IntProperty width;
		public IntProperty height;
		public IntProperty maximized_state_width;
		public IntProperty symbols_panel_height;
		public DoubleProperty tools_panel_height;
		public IntProperty title_number_of_parent_dirs_shown;

		public MainWindow() {
			comment("\n## Window properties");
			comment(" Position, width, and height of the main window");
			x      = new IntProperty("main_window.x", 0, null, 0, Integer.MAX_VALUE);
			y      = new IntProperty("main_window.y", 0, null, 0, Integer.MAX_VALUE);
			width  = new IntProperty("main_window.width", 1024, null, 100, Integer.MAX_VALUE);
			height = new IntProperty("main_window.height", 800, null, 100, Integer.MAX_VALUE);
			maximized_state_width =
				new IntProperty("main_window.maximized_state", JFrame.MAXIMIZED_BOTH, null, 0, JFrame.MAXIMIZED_BOTH);
			comment(" Width of the symbols panel as part of the main window");
			symbols_panel_height =
				new IntProperty("main_window.symbols_panel.width", 220, null, 0, Integer.MAX_VALUE);
			comment(" Height of the tools panel as part of the main window");
			tools_panel_height =
				new DoubleProperty("main_window.tools_panel.height", 0.15, null, 0, 1);
			comment(" Number of parent directories of the open file shown in the window title");
			title_number_of_parent_dirs_shown =
				new IntProperty("main_window.title.number_of_parent_dirs_shown", 2, null, 0, Integer.MAX_VALUE);
		}
	}

	public class Editor {
		public

		public Editor() {
			comment("\n## Editor properties");
			comment(" Font settings");
			addEntry(new Def(EDITOR_FONT_NAME, new PSet(MONOSPACE_FONTS_ARRAY), "Monospaced");
			addEntry(new Def(EDITOR_FONT_SIZE, INT_GT_0, "13");
			addEntry(new Def(EDITOR_FONT_ANTIALIASING, new PSet(TEXT_ANTIALIAS_KEYS), "On");
			addEntry(new Def("editor.columns_per_row", INT_GT_0, "80");
			comment(" Spell checker settings");
			addEntry(new Def("editor.spell_checker", new PSet("none", "aspell", "hunspell"), "aspell");
			addEntry(new Def("aspell.executable", STRING, "aspell");
			addEntry(new Def("aspell.lang", new PSet(aspellDicts), getFromList(aspellDicts, "en_GB"));
			addEntry(new Def("hunspell.executable", STRING, "hunspell");
			addEntry(new Def("hunspell.lang", new PSet(hunspellDicts), getFromList(hunspellDicts, "en_GB"));
			comment(" Automatic completion");
			addEntry(new Def("editor.auto_completion.activated", BOOLEAN, "false");
			addEntry(new Def("editor.auto_completion.min_number_of_letters", INT_GT_0, "3");
			addEntry(new Def("editor.auto_completion.delay", INT_GT_0, "200");
			addEntry(new Def("editor.clear_selection_when_closing_search", BOOLEAN, "true");
		}
	}

	public class Shortcut {
		public Shortcut() {
			comment("\n## Shortcuts");
			comment(" File menu");
			addEntry(new Def("shortcut.new", SHORTCUT, "control N");
			addEntry(new Def("shortcut.open", SHORTCUT, "control O");
			addEntry(new Def("shortcut.save", SHORTCUT, "control S");
			addEntry(new Def("shortcut.save as", SHORTCUT, "control A");
			addEntry(new Def("shortcut.close", SHORTCUT, "control W");
			addEntry(new Def("shortcut.exit", SHORTCUT, "");
			comment(" Edit menu");
			addEntry(new Def("shortcut.undo", SHORTCUT, "control Z");
			addEntry(new Def("shortcut.redo", SHORTCUT, "control shift Z");
			addEntry(new Def("shortcut.find", SHORTCUT, "control F");
			addEntry(new Def("shortcut.replace", SHORTCUT, "control R");
			addEntry(new Def("shortcut.find next", SHORTCUT, "F3");
			addEntry(new Def("shortcut.find previous", SHORTCUT, "shift F3");
			addEntry(new Def("shortcut.cut", SHORTCUT, "control X");
			addEntry(new Def("shortcut.copy", SHORTCUT, "control C");
			addEntry(new Def("shortcut.paste", SHORTCUT, "control V");
			addEntry(new Def("shortcut.comment", SHORTCUT, "control D");
			addEntry(new Def("shortcut.uncomment", SHORTCUT, "control shift D");
			addEntry(new Def("shortcut.diff", SHORTCUT, "alt D");
			addEntry(new Def("shortcut.forward search", SHORTCUT, "control shift F");
			comment(" View");
			addEntry(new Def("shortcut.symbols", SHORTCUT, "alt Y");
			addEntry(new Def("shortcut.structure", SHORTCUT, "alt X");
			addEntry(new Def("shortcut.compile", SHORTCUT, "alt L");
			addEntry(new Def("shortcut.local history", SHORTCUT, "");
			addEntry(new Def("shortcut.status bar", SHORTCUT, "");
			comment(" Build menu");
			addEntry(new Def("shortcut.pdf", SHORTCUT, "alt 1");
			addEntry(new Def("shortcut.dvi", SHORTCUT, "alt 2");
			addEntry(new Def("shortcut.dvi + ps", SHORTCUT, "alt 3");
			addEntry(new Def("shortcut.dvi + ps + pdf", SHORTCUT, "alt 4");
			comment(" Version control menu");
			addEntry(new Def("shortcut.svn update", SHORTCUT, "alt U");
			addEntry(new Def("shortcut.svn commit", SHORTCUT, "alt C");
			addEntry(new Def("shortcut.font", SHORTCUT, "");
			addEntry(new Def("shortcut.global settings", SHORTCUT, "control alt S");
			comment(" Editors/tabs");
			addEntry(new Def("shortcut.set master document", SHORTCUT, "");
			addEntry(new Def("shortcut.select next tab", SHORTCUT, "alt RIGHT");
			addEntry(new Def("shortcut.select previous tab", SHORTCUT, "alt LEFT");
			addEntry(new Def("shortcut.move tab left", SHORTCUT, "control alt LEFT");
			addEntry(new Def("shortcut.move tab right", SHORTCUT, "control alt RIGHT");
			comment(" Initiate update");
			addEntry(new Def("shortcut.update", SHORTCUT, "");
			comment(" About screen");
			addEntry(new Def("shortcut.about", SHORTCUT, "");
		}
	}

	public class Compiler {
		public Compiler() {
			comment("\n## Compiler settings");
			comment(" pdflatex");
			addEntry(new Def("compiler.pdflatex.executable", STRING, "pdflatex");
			addEntry(new Def("compiler.pdflatex.parameters", STRING, "-synctex=1");
			comment(" latex");
			addEntry(new Def("compiler.latex.executable", STRING, "latex");
			addEntry(new Def("compiler.latex.parameters", STRING, "--src-specials");
		}
	}
	*/
}
