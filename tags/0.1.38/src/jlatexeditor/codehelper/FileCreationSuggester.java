package jlatexeditor.codehelper;

import de.endrullis.utils.StringUtils;
import jlatexeditor.Doc;
import jlatexeditor.JLatexEditorJFrame;
import sce.codehelper.CodeAssistant;
import sce.codehelper.PatternPair;
import sce.codehelper.SCEPopup;
import sce.codehelper.WordWithPos;
import sce.component.SCEPane;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Suggests to create a file.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class FileCreationSuggester implements CodeAssistant, SCEPopup.ItemHandler {
	private PatternPair parameterPattern = new PatternPair("\\\\(input|include)\\{([^\\{]*)", "([^\\}]*)\\}");

	private JLatexEditorJFrame jle;

	public FileCreationSuggester(JLatexEditorJFrame jle) {
		this.jle = jle;
	}

	@Override
	public boolean assistAt(SCEPane pane) {
		List<WordWithPos> words = parameterPattern.find(pane);
		if (words != null) {
			WordWithPos word = words.get(1);

			Doc resource = jle.getMainEditor().getResource();
			if (resource instanceof Doc.FileDoc) {
				Doc.FileDoc fileDoc = (Doc.FileDoc) resource;
				String filename = word.word;

				// add .tex if no extension given
				String lastPart = StringUtils.stringAfter(filename, "/", 'l');
				if (lastPart == null) {
					lastPart = filename;
				}
				if (!lastPart.contains(".")) {
					filename += ".tex";
				}

				try {
					File file;

					// absolute or relative path?
					if (filename.startsWith("/")) {
						file = new File(filename);
					} else {
						String dirPath = fileDoc.getFile().getParentFile().getCanonicalPath();
						file = new File(dirPath + "/" + filename);
					}

					// if file does not exists
					if (!file.exists()) {
						List<Object> list = new ArrayList<Object>();
						list.add(new FileCreationAction(filename, file));

						// open popup
						pane.getPopup().openPopup(list, this);
						return true;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return false;
	}

	@Override
	public void perform(Object item) {
		if (item instanceof FileCreationAction) {
			FileCreationAction action = (FileCreationAction) item;

			try {
				File file = action.getFile();
				if (file.createNewFile())
					jle.open(new Doc.FileDoc(file));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


// inner class

	class FileCreationAction {
		private String filename;
		private File file;

		FileCreationAction(String filename, File file) {
			this.filename = filename;
			this.file = file;
		}

		public String getFilename() {
			return filename;
		}

		public File getFile() {
			return file;
		}

		@Override
		public String toString() {
			return "<create \"" + filename + "\">";
		}
	}
}
