package jlatexeditor.addon;

import jlatexeditor.JLatexEditorJFrame;
import sce.component.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Table column aligner.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class ColumnRealigner extends AddOn {
	protected ColumnRealigner() {
		super("realign table columns", "Realign Table Columns", "control alt R");
	}

	@Override
	public void run(JLatexEditorJFrame jle) {
		SCEPane pane = jle.getActiveEditor().getTextPane();
		SCEDocument doc = pane.getDocument();

		SCEPosition oldPosition = new SCEDocumentPosition(pane.getCaret().getRow(), pane.getCaret().getColumn());

		ArrayList<Character> columns = new ArrayList<Character>();
		if (!doc.hasSelection()) {
			Iterator<EnvironmentUtils.Env> envIterator = EnvironmentUtils.getEnvIterator(pane);
			EnvironmentUtils.Env env = envIterator.next();
			if (env == null) return;

			int row = env.openWord.getStartRow();
			int col = env.openWord.getEndCol();

			// search for column definition after begin command
			String restOfLine = doc.getRowsModel().getRowAsString(row).substring(col + 1);
			if (restOfLine.startsWith("{")) {
				int openBraces = 0;
				boolean validHeader = true;
				columns = new ArrayList<Character>();

				for (int i=1; i<restOfLine.length(); i++) {
					char c = restOfLine.charAt(i);
					if (openBraces > 0) {
						if (c == '{') openBraces++;
						if (c == '}') openBraces--;
					} else
					if (c == 'l' || c == 'c' || c == 'r' || c == 'p' || c == 'X') {
						columns.add(c);
					} else
					if (c == '{') {
						openBraces++;
					} else
					if (c == '}') {
						break;
					} else
					if (c == '|') {
					} else {
						validHeader = false;
						break;
					}
				}

				if (!validHeader) {
					columns.clear();
				}
			}

			doc.setSelectionRange(new SCERange(env.openWord.getStartRow()+1, 0, env.closeWord.getEndRow(), 0));

//			JOptionPane.showMessageDialog(jle, "Before using this function you have to select the lines that shall be realigned.");
		}

		SCEDocumentPosition starPos = doc.getSelectionStart();
		SCEDocumentPosition endPos = doc.getSelectionEnd();

		String selectedText = doc.getSelectedText();
		Table table = new Table(columns);
		String newText = table.process(selectedText.split("\n"));

		doc.replace(starPos, endPos, newText);
		pane.getCaret().moveTo(oldPosition, false);
	}

	public static class Table {
		private ArrayList<Col> cols = new ArrayList<Col>();
		private ArrayList<Row> rows = new ArrayList<Row>();

		public Table(List<Character> cols) {
			// convert col characters into Cols
			for (Character c : cols) {
				Col.Ori ori = Col.Ori.l;
				if (c == 'c') {
					ori = Col.Ori.c;
				} else
				if (c == 'r') {
					ori = Col.Ori.r;
				}
				this.cols.add(new Col(ori));
			}
		}

		public String process (String[] rowStrings) {
			// count columns
			int colCount = cols.size();
			int indentation = Integer.MAX_VALUE;
			for (String rowString : rowStrings) {
				Row row = new Row(rowString);
				rows.add(row);
				colCount = Math.max(colCount, row.cols.size());
				indentation = Math.min(indentation, row.indentation);
			}
			// adjust column count
			for (int i = cols.size(); i < colCount; i++) {
				cols.add(new Col(Col.Ori.l));
			}
			for (Row row : rows) {
				for (int i = 0; i < row.cols.size(); i++) {
					cols.get(i).width = Math.max(cols.get(i).width, row.cols.get(i).length());
				}
				for (int i = row.cols.size(); i < colCount; i++) {
					row.cols.add("");
				}
			}

			String indentString = fillWithSpaces("", Col.Ori.l, indentation);

			StringBuffer sb = new StringBuffer();
			for (Row row : rows) {
				sb.append(indentString).append(row.toString(cols)).append('\n');
			}

			return sb.toString();
		}
	}

	public static class Row {
		public ArrayList<String> cols = new ArrayList<String>();
		public String end = "";
		public int indentation;

		public Row (String row) {
			String[] parts = row.split("\\\\\\\\", 2);
			if (parts.length == 2) {
				end = parts[1];
			}
			String[] split = parts[0].split("&");
			indentation = 0;
			while (indentation < split[0].length() && split[0].charAt(indentation) == ' ') indentation++;

			for (String s : split) {
				cols.add(s.trim());
			}
		}

		public String toString (List<Col> colTypes) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < colTypes.size(); i++) {
				if (i != 0) {
					sb.append(" & ");
				}
				Col col = colTypes.get(i);
				sb.append(fillWithSpaces(cols.get(i), col.ori, col.width));
			}
			sb.append(" \\\\").append(end);
			return sb.toString();
		}
	}

	public static class Col {
		enum Ori {l, c, r}

		public Col(Ori ori) {
			this.ori = ori;
		}

		public Ori ori = Ori.l;
		public int width = 0;
	}

	private static String fillWithSpaces(String text, Col.Ori ori, int length) {
		if (text.length() == length) return text;

		int diff = length - text.length();
		int startSpaces = 0;
		switch (ori) {
			case c:
				startSpaces = diff / 2;
				break;
			case r:
				startSpaces = diff;
				break;
		}

		StringBuffer sb = new StringBuffer(length);
		for(int i=0; i<startSpaces; i++) {
			sb.append(' ');
		}
		sb.append(text);
		for(int i=sb.length(); i<length; i++) {
			sb.append(' ');
		}

		return sb.toString();
	}
}
