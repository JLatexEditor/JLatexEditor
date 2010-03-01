package jlatexeditor;

import sce.component.*;
import util.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

/**
 * Abstract document.
 */
public abstract class Doc implements AbstractResource, SCEDocumentListener {
	private ArrayList todos = new ArrayList();
	private Vector<Line> oldLines = new Vector<Line>();
	private Vector<Line> currLines = new Vector<Line>();
	public static String UNTITLED = "Untitled";

	public static class Line {
		enum State { parsed, added, deleted, changed }
		private Line.State state;
		private String line;

		public Line(Line.State state, String line) {
			this.state = state;
			this.line = line;
		}
	}

	@Override
	public void documentChanged(SCEDocument sender, SCEDocumentEvent event) {
		// TODO
		if (event.isInsert()) {
			String text = event.getText();
			int newLines = 0;
			for (char c : text.toCharArray()) {
				if (c == '\n') {
					newLines++;
				}
			}
		}
		//lines.insertElementAt();
	}

	/**
	 * Document read from file.
	 */
	public static class FileDoc extends Doc {
		private File file;
		private String id;

		public FileDoc(File file) {
			this.file = file;
			try {
				this.id = file.getCanonicalPath();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof FileDoc) {
			  FileDoc that = (FileDoc) obj;
			  return this.id.equals(that.id);
			}
			return false;
		}

		public File getFile() {
			return file;
		}

		public String getContent() throws IOException {
			return StreamUtils.readFile(file.getAbsolutePath());
		}

		public String getName() { return file.getName(); }

		public URI getUri() {
			return file.toURI();
		}

		public String toString() { return file.toString(); }
	}

	/**
	 * Unsaved document.
	 */
	public static class UntitledDoc extends Doc {
		private static int untitledNr = 1;
		private String name;

		public UntitledDoc() {
			name = UNTITLED + " " + untitledNr++;
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof UntitledDoc) {
				UntitledDoc that = (UntitledDoc) obj;
				return this.name.equals(that.name);
			}
			return false;
		}

		public String getContent() { return ""; }
		public String getName() { return name; }

		public URI getUri() {
			try {
				return new URI("unsaved:" + name);
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return null;
			}
		}

		public String toString() { return name; }
	}
}
