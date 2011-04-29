package jlatexeditor.addon;

import jlatexeditor.Doc;
import jlatexeditor.JLatexEditorJFrame;
import sce.codehelper.SCEPopup;
import sce.codehelper.WordWithPos;
import sce.component.SCEPane;

import java.io.File;
import java.util.*;

/**
 * Close current environment.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class FileSearch extends AddOn implements SCEPopup.ItemHandler {
	protected FileSearch() {
		super("file search", "File Search", "control shift N");
	}

	@Override
	public void run(JLatexEditorJFrame jle) {
		HashSet<File> files = jle.getBackgroundParser().getFiles();
		File[] filesArray = new File[files.size()];
		files.toArray(filesArray);
		Arrays.sort(filesArray);

		List<Object> list = new ArrayList<Object>();
		for (File file : filesArray) {
			list.add(new FileOpenAction(file, jle));
		}

		// open popup
		SCEPane pane = jle.getActiveEditor().getTextPane();
		pane.getPopup().openPopup(list, this);
	}

	@Override
	public void perform(Object item) {
		if (item instanceof FileOpenAction) {
		  FileOpenAction fileOpenAction = (FileOpenAction) item;
			JLatexEditorJFrame jle = fileOpenAction.jle;
			jle.open(new Doc.FileDoc(fileOpenAction.file));
		}
	}

	class FileOpenAction {
		private File file;
		private JLatexEditorJFrame jle;

		FileOpenAction(File file, JLatexEditorJFrame jle) {
			this.file = file;
			this.jle = jle;
		}

		@Override
		public String toString() {
			return "<html>" + file.getName() + " <span style='color: #808080;'>(" + file.getParent() + ")</span></html>";
		}
	}
}
