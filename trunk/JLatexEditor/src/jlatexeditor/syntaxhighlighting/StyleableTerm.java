package jlatexeditor.syntaxhighlighting;

import sce.component.SCEDocumentChar;

/**
 * Term and it's position in the document.
 */
public class StyleableTerm {
	private String term;
	private SCEDocumentChar[] docChars;
	private int start;
	private int end;
	private byte overlayStyle;

	public StyleableTerm(String term, SCEDocumentChar[] docChars, int start, byte overlayStyle) {
		this.term = term;
		this.docChars = docChars;
		this.start = start;
		this.end = start + term.length();
		this.overlayStyle = overlayStyle;
	}

	public void applyStyleToDoc() {
		for (int i=start; i<end; i++) {
			docChars[i].overlayStyle = overlayStyle;
		}
	}
}
