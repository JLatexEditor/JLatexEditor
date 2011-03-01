package jlatexeditor;

import sce.component.*;
import util.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

/**
 * Abstract document.
 */
public abstract class Doc implements AbstractResource, SCEDocumentListener {
	private ArrayList todos = new ArrayList();
	private Vector<Line> oldLines = new Vector<Line>();
	private Vector<Line> currLines = new Vector<Line>();
	public static String UNTITLED = "Untitled";
	private HashMap<String,String> properties = new HashMap<String, String>();

	public String getProperty(String key) {
		return properties.get(key);
	}

	public void setProperty(String key, String value) {
		properties.put(key, value);
	}

	private static class DocState {
		private Line root = new Line(Line.State.parsed, -1, null);
		// small cache of one element
		private Line lastLine;

		private DocState() {
		}

		private DocState(SCEDocument document) {
			// build a balanced binary tree from the rows of the document
			SCEDocumentRow[] rows = document.getRows();

			int start = 0;
			int end = rows.length - 1;
			int pivot = end / 2;
			root = new Line(Line.State.added, pivot, rows[pivot].toString());

		}

		public int addLine(int lineNr) {
			//if (lineNr == lastLine) return lastIndex;
			return -1;
		}
	}

	private static class Line {
		enum State { parsed, added, deleted, changed }
		private Line.State state;
		private int lineNr;
		private String oldLine;

		public Line(Line.State state, int lineNr, String oldLine) {
			this.state = state;
			this.lineNr = lineNr;
			this.oldLine = oldLine;
		}
	}

	@Override
	public void documentChanged(SCEDocument sender, SCEDocumentEvent event) {
		// TODO
		/*
		if (event.isInsert()) {
			String text = event.getText();
			int newLines = 0;
			for (char c : text.toCharArray()) {
				if (c == '\n') {
					newLines++;
				}
			}
		}
		*/
		//currLines.insertElementAt();
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
				return new URI("unsaved:" + URLEncoder.encode(name, "UTF-8"));
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return null;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return null;
			}
		}

		public String toString() { return name; }
	}
}
