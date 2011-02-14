package jlatexeditor.addon;

import jlatexeditor.JLatexEditorJFrame;
import sce.component.SCEDocument;
import sce.component.SCEDocumentPosition;

import javax.swing.*;

/**
 * Table column aligner.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class ColumnRealigner extends AddOn {
	protected ColumnRealigner() {
		super("realign table columns", "Realign Table Columns", "");
	}

	@Override
	public void run(JLatexEditorJFrame jle) {
		SCEDocument doc = jle.getActiveEditor().getTextPane().getDocument();
		if (doc.hasSelection()) {
			SCEDocumentPosition starPos = doc.getSelectionStart();
			SCEDocumentPosition endPos = doc.getSelectionEnd();

			String selectedText = doc.getSelectedText();
			String[] lines = selectedText.split("\n");
			String[][] columns = new String[lines.length][];
			int maxColumns = 0;
			for (int i = 0; i < lines.length; i++) {
				String line = lines[i];
				columns[i] = line.split("&");
				maxColumns = Math.max(maxColumns, columns[i].length);
			}
			int[] columnWidths = new int[maxColumns];
			for (int colNr = 0; colNr < columnWidths.length; colNr++) {
				int width = 0;
				for (int lineNr = 0; lineNr < lines.length; lineNr++) {
					if (colNr < columns[lineNr].length) {
						width = Math.max(width, columns[lineNr][colNr].length());
					}
				}
				columnWidths[colNr] = width;
			}

			StringBuffer sb = new StringBuffer();
			for (int lineNr = 0; lineNr < columns.length; lineNr++) {
				String[] column = columns[lineNr];
				for (int colNr = 0; colNr < column.length; colNr++) {
					sb.append(fillWithSpaces(column[colNr], columnWidths[colNr]));
					if (colNr < maxColumns - 1) {
						sb.append('&');
					}
				}
				sb.append('\n');
			}

			doc.replace(starPos, endPos, sb.toString());
		} else {
			JOptionPane.showMessageDialog(jle, "Before using this function you have to select the lines that shall be realigned.");
		}
	}

	private String fillWithSpaces(String text, int length) {
		if (text.length() == length) return text;

		StringBuffer sb = new StringBuffer(length);
		sb.append(text);
		for(int i=text.length(); i<length; i++) {
			sb.append(' ');
		}

		return sb.toString();
	}
}
