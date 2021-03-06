package sce.component;

import de.endrullis.utils.KeyUtils;
import jlatexeditor.gproperties.GProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Search pane.
 */
public class SCESearch extends JPanel implements ActionListener, KeyListener, SCEDocumentListener, SCESelectionListener {
	private SourceCodeEditor editor;

	private JTextField input = new JTextField();
	private ImageButton buttonNext;
	private ImageButton buttonPrevious;
	private JCheckBox caseSensitive = new JCheckBox("Case sensitive", false);
	private JCheckBox regExp = new JCheckBox("Regexp", false);
	private JCheckBox selectionOnly = new JCheckBox("Selection only", false);
	private ImageButton buttonClose;
	private ImageButton buttonShowReplace;

	private boolean showReplace = false;
	private JTextField replace = new JTextField();
	private ImageButton buttonReplace;
	private ImageButton buttonReplaceAll;

	private GroupLayout layout;

	/** Listeners: searchChangeListeners of type SearchChangeListener. */
	private ArrayList<SearchChangeListener> searchChangeListeners = new ArrayList<SearchChangeListener>();

	/** Search update thread. */
	private UpdateThread updateThread = new UpdateThread();

	/** Search results. */
	private ArrayList<SCEDocumentRange> results = new ArrayList<SCEDocumentRange>();
	/** Maps the search terms to the selection index in results. */
	private LinkedHashMap<String, Integer> searchPositions = new LinkedHashMap<String, Integer>();

	// selection before searching
	private SCEDocumentRange selection = null;

	private GroupLayout.Group groupHorizontal;
	private GroupLayout.Group groupVertical;
	private static final Color ERROR_COLOR = new Color(255, 204, 204);
	private static final Color ERROR_FOREGROUND_COLOR = new Color(255, 0, 0);

	/** Last search. */
	private static SCESearch lastSearch;

	public static SCESearch getLastSearch() {
		return lastSearch;
	}

	public SCESearch(SourceCodeEditor editor) {
		this.editor = editor;
		setBackground(new Color(233, 244, 255));

		buttonNext = new ImageButton(
			new ImageIcon(getClass().getResource("/images/buttons/arrow_down.png")),
			new ImageIcon(getClass().getResource("/images/buttons/arrow_down_highlight.png")),
			new ImageIcon(getClass().getResource("/images/buttons/arrow_down_press.png")));

		buttonPrevious = new ImageButton(
			new ImageIcon(getClass().getResource("/images/buttons/arrow_up.png")),
			new ImageIcon(getClass().getResource("/images/buttons/arrow_up_highlight.png")),
			new ImageIcon(getClass().getResource("/images/buttons/arrow_up_press.png")));

		buttonClose = new ImageButton(
			new ImageIcon(getClass().getResource("/images/buttons/close.png")),
			new ImageIcon(getClass().getResource("/images/buttons/close_highlight.png")),
			new ImageIcon(getClass().getResource("/images/buttons/close_press.png")));

		buttonShowReplace = new ImageButton(
			new ImageIcon(getClass().getResource("/images/buttons/showReplace.png")),
			new ImageIcon(getClass().getResource("/images/buttons/showReplace_highlight.png")),
			new ImageIcon(getClass().getResource("/images/buttons/showReplace_press.png")));

		buttonReplace = new ImageButton(
			new ImageIcon(getClass().getResource("/images/buttons/replace.png")),
			new ImageIcon(getClass().getResource("/images/buttons/replace_highlight.png")),
			new ImageIcon(getClass().getResource("/images/buttons/replace_press.png")));

		buttonReplaceAll = new ImageButton(
			new ImageIcon(getClass().getResource("/images/buttons/replace_all.png")),
			new ImageIcon(getClass().getResource("/images/buttons/replace_all_highlight.png")),
			new ImageIcon(getClass().getResource("/images/buttons/replace_all_press.png")));

		layout = new GroupLayout(this);
		setLayout(layout);

		input.setColumns(40);
		input.setMinimumSize(new Dimension(200, 20));
		add(input);
		input.addKeyListener(this);
		add(buttonNext);
		buttonNext.addActionListener(this);
		add(buttonPrevious);
		buttonPrevious.addActionListener(this);
		caseSensitive.setOpaque(false);
		add(caseSensitive);
		caseSensitive.addActionListener(this);
		regExp.setOpaque(false);
		add(regExp);
		regExp.addActionListener(this);
		selectionOnly.setOpaque(false);
		add(selectionOnly);
		selectionOnly.addActionListener(this);
		add(buttonShowReplace);
		buttonShowReplace.addActionListener(this);
		add(buttonClose);
		buttonClose.addActionListener(this);

		replace.setColumns(40);
		add(replace);
		replace.setToolTipText("For regexp groups use \\1, \\2, ...");
		replace.addKeyListener(this);
		add(buttonReplace);
		buttonReplace.addActionListener(this);
		add(buttonReplaceAll);
		buttonReplaceAll.addActionListener(this);

		groupHorizontal =
			layout.createSequentialGroup()
				.addGap(5)
				.addGroup(
					layout.createParallelGroup()
						.addComponent(input)
						.addComponent(replace)
				)
				.addGap(2, 6, 8)
				.addGroup(
					layout.createParallelGroup()
						.addGroup(
							layout.createSequentialGroup()
								.addComponent(buttonNext)
								.addGap(2, 4, 4)
								.addComponent(buttonPrevious)
								.addGap(2, 4, 4)
								.addComponent(caseSensitive)
								.addComponent(regExp)
								.addComponent(selectionOnly)
								.addGap(2, 50, Short.MAX_VALUE)
								.addComponent(buttonShowReplace)
								.addGap(2, 15, 20)
								.addComponent(buttonClose)
						)
						.addGroup(
							layout.createSequentialGroup()
								.addComponent(buttonReplace)
								.addGap(2, 4, 4)
								.addComponent(buttonReplaceAll)
						)
				);

		groupVertical =
			layout.createSequentialGroup()
				.addGap(2)
				.addGroup(
					layout.createParallelGroup(GroupLayout.Alignment.CENTER)
						.addComponent(input)
						.addComponent(buttonNext)
						.addComponent(buttonPrevious)
						.addComponent(caseSensitive)
						.addComponent(regExp)
						.addComponent(selectionOnly)
						.addComponent(buttonShowReplace)
						.addComponent(buttonClose)
				)
				.addGroup(
					layout.createParallelGroup(GroupLayout.Alignment.CENTER)
						.addComponent(replace)
						.addComponent(buttonReplace)
						.addComponent(buttonReplaceAll)
				)
				.addGap(2);

		layout.setHorizontalGroup(groupHorizontal);
		layout.setVerticalGroup(groupVertical);

		SCEPane pane = editor.getTextPane();
		SCEDocument document = pane.getDocument();
		pane.addKeyListener(this);
		document.addSCEDocumentListener(this);
		document.addSCESelectionListener(this);

		updateThread = new UpdateThread();
		updateThread.start();

		setShowReplace(false);
		setVisible(false);

		for (KeyStroke stopKeyStroke : KeyUtils.stopKeyStrokes) {
			getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stopKeyStroke, "close search");
		}
		getActionMap().put("close search", new AbstractAction("close search") {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
	}

	public boolean isShowReplace() {
		return showReplace;
	}

	public void setShowReplace(boolean showReplace) {
		this.showReplace = showReplace;

		replace.setVisible(showReplace);
		buttonReplace.setVisible(showReplace);
		buttonReplaceAll.setVisible(showReplace);
	}

	public JTextField getInput() {
		return input;
	}

	public JTextField getReplace() {
		return replace;
	}

	public JCheckBox getRegExp() {
		return regExp;
	}

	public JCheckBox getCaseSensitive() {
		return caseSensitive;
	}

	public JCheckBox getSelectionOnly() {
		return selectionOnly;
	}

	@Override
	public boolean hasFocus() {
		return input.hasFocus();
	}

	public void focus() {
		input.requestFocusInWindow();
	}

	private void clearHighlights(boolean highlightSelection, boolean selectionOnly) {
		SCEMarkerBar markerBar = editor.getMarkerBar();
		markerBar.clear(SCEMarkerBar.TYPE_SEARCH);
		SCEPane pane = editor.getTextPane();
		pane.removeAllTextHighlights();

		if (highlightSelection && selection != null) {
			int rows = pane.getDocument().getRowsModel().getRowsCount();
			SCEDocumentPosition endDocPos = new SCEDocumentPosition(rows, 0);

			if (selectionOnly) {
				pane.addTextHighlight(new SCETextHighlight(pane, new SCEDocumentPosition(0, 0), selection.getStartPos(), SCEPane.nonSelectionHighlightColor));
				pane.addTextHighlight(new SCETextHighlight(pane, selection.getEndPos(), endDocPos, SCEPane.nonSelectionHighlightColor));
			} else {
				pane.addTextHighlight(new SCETextHighlight(pane, selection.getStartPos(), selection.getEndPos(), SCEPane.selectionHighlightColorLight));
			}
		}
	}

	public void setVisible(boolean visibility) {
		setVisible(visibility, true);
	}

	public void setVisible(boolean visibility, boolean reset) {
		if (visibility == isVisible()) return;

		SCEPane pane = editor.getTextPane();
		SCEDocument document = pane.getDocument();
		if (visibility) {
			if (reset) {
				input.setText("");
			}

			SCEDocumentPosition selectionStart = document.getSelectionStart();
			SCEDocumentPosition selectionEnd = document.getSelectionEnd();
			if (selectionStart == null || selectionEnd == null) {
				selection = null;
			} else {
				selection = new SCEDocumentRange(selectionStart, selectionEnd);
				pane.getCaret().moveTo(selectionStart, false);
				document.clearSelection();
			}
		} else {
			results.clear();
			searchPositions.clear();
			String closingAction = GProperties.getString("editor.when_closing_search");
			if (closingAction.equals("reset selection")) {
				if (selection == null) {
					pane.clearSelection();
				} else {
					document.setSelectionRange(selection, true);
				}
				pane.repaint();
			} else if (closingAction.equals("clear selection")) {
				pane.clearSelection();
				pane.repaint();
			}
		}
		clearHighlights(visibility, selectionOnly.isSelected());
		super.setVisible(visibility);
	}

	private void moveTo(SCEDocumentRange range) {
		SCEPosition start = range.getStartPos();
		SCEPosition end = range.getEndPos();

		editor.moveTo(start.getRow(), start.getColumn());
		SCEDocument document = editor.getTextPane().getDocument();
		document.setSelectionRange(start, end, false);
		editor.getTextPane().repaint();
	}

	public void previous() {
		SCECaret caret = editor.getTextPane().getCaret();
		SCEDocumentRange last = null;
		for (SCEDocumentRange result : results) {
			SCEPosition start = result.getStartPos();
			if (start.getRow() > caret.getRow() || (start.getRow() == caret.getRow() && start.getColumn() >= caret.getColumn()))
				break;
			last = result;
		}
		if (last != null) moveTo(last);
		else first();
	}

	public void next(boolean includeCurrentPos, boolean jumpToLast) {
		SCECaret caret = editor.getTextPane().getCaret();
		for (SCEDocumentRange result : results) {
			SCEPosition start = result.getStartPos();
			if (start.getRow() < caret.getRow()) continue;
			if (start.getRow() == caret.getRow()) {
				if (start.getColumn() < caret.getColumn()) continue;
				if ((start.getColumn() == caret.getColumn()) && !includeCurrentPos) continue;
			}
			moveTo(result);
			return;
		}
		if (jumpToLast) last();
	}

	public void first() {
		if (results.size() > 0) moveTo(results.get(0));
	}

	public void last() {
		if (results.size() > 0) moveTo(results.get(results.size() - 1));
	}

	private void replace(SCEPosition start, SCEPosition end) {
		SCEDocument document = editor.getTextPane().getDocument();
		String text = document.getText(start, end);

		if (!regExp.isSelected()) {
			document.replace(start, end, replace.getText());
		} else {
			Pattern pattern = Pattern.compile(input.getText(), Pattern.MULTILINE | (caseSensitive.isSelected() ? Pattern.CASE_INSENSITIVE : 0));
			Matcher matcher = pattern.matcher(text);
			matcher.find();

			String replaceBy = replace.getText();
			StringBuilder builder = new StringBuilder();
			// handle escaped characters and groups
			boolean escape = false;
			for (char c : replaceBy.toCharArray()) {
				if (escape) {
					if ('1' <= c && c <= '9') {
						int groupNr = c - '0';
						if (groupNr <= matcher.groupCount()) {
							builder.append(matcher.group(groupNr));
						}
					} else if (c == 'n') {
						builder.append('\n');
					} else if (c == 'r') {
					} else if (c == 't') {
						builder.append("  ");
					} else {
						builder.append(c);
					}
					escape = false;
				} else {
					if (c == '\\') {
						escape = true;
					} else {
						builder.append(c);
					}
				}
			}

			document.replace(start, end, builder.toString());
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == buttonClose) setVisible(false);
		if (e.getSource() == caseSensitive) {
			updateThread.documentChanged();
			searchChanged();
		}
		if (e.getSource() == buttonNext) next(false, true);
		if (e.getSource() == buttonPrevious) previous();
		if (e.getSource() == regExp) searchChanged();
		if (e.getSource() == selectionOnly) {
			clearHighlights(true, selectionOnly.isSelected());
			searchChanged();
		}
		if (e.getSource() == buttonShowReplace) setShowReplace(!isShowReplace());

		if (e.getSource() == buttonReplace) {
			SCEDocument document = editor.getTextPane().getDocument();
			replace(document.getSelectionStart(), document.getSelectionEnd());
			next(true, false);
		}

		if (e.getSource() == buttonReplaceAll) {
			ArrayList<SCEDocumentRange> matches = results;
			for (int matchNr = matches.size() - 1; matchNr >= 0; matchNr--) {
				SCEDocumentRange match = matches.get(matchNr);
				replace(match.getStartPos(), match.getEndPos());
			}
		}
	}

	public void keyTyped(KeyEvent e) {
	}

	public void keyPressed(KeyEvent e) {
		if (isVisible()) {
			if (KeyUtils.isStopKey(e)) {
				setVisible(false);
				e.consume();
			}
		}
		if (e.getSource() == input) {
			/*
			if ((e.getModifiers() == 0 && e.getKeyCode() == KeyEvent.VK_UP) ||
					(e.getModifiers() == KeyEvent.SHIFT_MASK && e.getKeyCode() == KeyEvent.VK_F3)) {
				previous();
				e.consume();
			}
			if ((e.getModifiers() == 0 && e.getKeyCode() == KeyEvent.VK_DOWN) ||
					(e.getModifiers() == 0 && e.getKeyCode() == KeyEvent.VK_F3)) {
				next(false, true);
				e.consume();
			}
			*/
			if (e.getModifiers() == 0) {
				if (e.getKeyCode() == KeyEvent.VK_UP) {
					previous();
					e.consume();
				}
				if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					next(false, true);
					e.consume();
				}
			}
			if (e.getModifiers() == KeyEvent.CTRL_MASK) {
				if (e.getKeyCode() == KeyEvent.VK_HOME) {
					first();
					e.consume();
				}
				if (e.getKeyCode() == KeyEvent.VK_END) {
					last();
					e.consume();
				}
			}
		}
	}

	public void keyReleased(KeyEvent e) {
		if (e.getSource() == input) {
			if (!e.isActionKey()) {
				searchChanged();
			}
		}
	}

	private void searchChanged() {
		lastSearch = this;

		informSearchChangeListeners();
		updateThread.searchChanged();
	}

	/**
	 * SearchChangeListener can register themselves at SCESearch
	 * to be informed about search changes.
	 *
	 * @param listener listener who wants to be informed about changes
	 */
	public void addSearchChangeListener(SearchChangeListener listener) {
		if (searchChangeListeners.contains(listener)) return;
		searchChangeListeners.add(listener);
	}

	/**
	 * SearchChangeListener can sign off at SCESearch
	 * to be no longer informed about search changes.
	 *
	 * @param listener listener who wants to sign off
	 */
	public void removeSearchChangeListener(SearchChangeListener listener) {
		searchChangeListeners.remove(listener);
	}

	/**
	 * Informs SearchChangeListeners about search changes.
	 */
	private void informSearchChangeListeners() {
		for (SearchChangeListener listener : searchChangeListeners) {
			listener.searchChanged(this);
		}
	}

	public void documentChanged(SCEDocument sender, SCEDocumentEvent event) {
		updateThread.documentChanged();
	}

	public void selectionChanged(SCEDocument sender, SCEDocumentPosition start, SCEDocumentPosition end) {
		if (start == null || end == null) {
			replaceEnabled(false, results.size() > 0);
			return;
		}

		for (SCEDocumentRange result : results) {
			SCEPosition rstart = result.getStartPos();
			SCEPosition rend = result.getEndPos();

			if (start.equals(rstart) && end.equals(rend)) {
				replaceEnabled(true, results.size() > 0);
				return;
			}
		}

		replaceEnabled(false, results.size() > 0);
	}

	private void replaceEnabled(boolean replaceEnabled, boolean replaceAllEnabled) {
		buttonReplace.setEnabled(replaceEnabled);
		buttonReplaceAll.setEnabled(replaceAllEnabled);
	}

	public void openSearch(SCESearch lastSearch) {
		setVisible(true, false);
		setProperties(lastSearch);
		searchChanged();
	}

	public void setProperties(SCESearch lastSearch) {
		if (lastSearch == null) return;

		input.setText(lastSearch.input.getText());
		replace.setText(lastSearch.replace.getText());
		caseSensitive.setSelected(lastSearch.caseSensitive.isSelected());
		regExp.setSelected(lastSearch.regExp.isSelected());
		setShowReplace(lastSearch.isShowReplace());
	}

	private class UpdateThread extends Thread {
		private boolean searchChanged = true;
		private boolean documentChanged = true;
		private boolean setSelection = true;

		private String text = "";
		private int text2row[] = new int[0];
		private int text2column[] = new int[0];

		private UpdateThread() {
			super("SCESearch-UpdateThread");
			setDaemon(true);
			setPriority(Thread.NORM_PRIORITY);
		}

		public synchronized void searchChanged() {
			setSelection = true;
			searchChanged = true;
			if (!isVisible()) return;
			notify();
		}

		public synchronized void documentChanged() {
			setSelection = false;
			documentChanged = true;
			if (!isVisible()) return;
			notify();
		}

		private void updateDocument() {
			documentChanged = false;

			SCEDocument document = editor.getTextPane().getDocument();
			SCEDocumentRow[] documentRows = document.getRowsModel().getRows();

			StringBuilder builder = new StringBuilder(100000);
			for (SCEDocumentRow row : documentRows) {
				builder.append(row.toString());
				builder.append('\n');

				if (documentChanged) return;
			}
			text = builder.toString();
			if (!caseSensitive.isSelected()) text = text.toLowerCase();

			// update position map
			if (text2row.length <= text.length()) {
				text2row = new int[text.length()+1];
				text2column = new int[text.length()+1];
			}
			int rowNr = 0;
			int columnNr = 0;
			for (int charNr = 0; charNr < text.length(); charNr++) {
				text2row[charNr] = rowNr;
				text2column[charNr] = columnNr;

				columnNr++;
				char c = text.charAt(charNr);
				if (c == '\n') {
					rowNr++;
					columnNr = 0;
				}
			}
      text2row[text.length()] = rowNr;
      text2column[text.length()] = columnNr;
		}

		private void search(boolean move) {
			ArrayList<SCEDocumentRange> resultsTemp = new ArrayList<SCEDocumentRange>();

			searchChanged = false;
			SCEDocument document = editor.getTextPane().getDocument();

			clearHighlights(true, selectionOnly.isSelected());
			SCEMarkerBar markerBar = editor.getMarkerBar();
			SCEPane pane = editor.getTextPane();

			input.setForeground(Color.BLACK);
			input.setBackground(Color.WHITE);

			SCECaret caret = editor.getTextPane().getCaret();

			String search = input.getText();
			int length = search.length();

			SCEDocumentRange selectionRange = null;

			try {
				if (length != 0) {
					if (!regExp.isSelected()) {
						// normal search
						if (!caseSensitive.isSelected()) search = search.toLowerCase();

						int index = -1;
						while ((index = text.indexOf(search, index + 1)) != -1) {
							int rowStart = text2row[index];
							int columnStart = text2column[index];

							int rowEnd = rowStart;
							int columnEnd = columnStart + length;

							selectionRange = processOccurrence(resultsTemp, document, markerBar, pane, caret, selectionRange, rowStart, columnStart, rowEnd, columnEnd);
						}
					} else {
						// regexp search
						Pattern pattern = Pattern.compile(search, Pattern.MULTILINE | (caseSensitive.isSelected() ? 0 : Pattern.CASE_INSENSITIVE));
						Matcher matcher = pattern.matcher(text);
						while (matcher.find()) {
							int startIndex = matcher.start();
							int endIndex = matcher.end();
							// skip matches of length 0
							if (startIndex == endIndex) continue;

							int rowStart = text2row[startIndex];
							int columnStart = text2column[startIndex];

							int rowEnd = text2row[endIndex];
							int columnEnd = text2column[endIndex];

							selectionRange = processOccurrence(resultsTemp, document, markerBar, pane, caret, selectionRange, rowStart, columnStart, rowEnd, columnEnd);
						}
					}
				} else {
					document.clearSelection();
				}

				results = resultsTemp;

				if (length > 0 && resultsTemp.size() == 0) {
					input.setBackground(ERROR_COLOR);
					document.clearSelection();
				} else {
          // set the selection
					if (selectionRange != null && setSelection) document.setSelectionRange(selectionRange, false);

					if (move) next(true, true);
				}
			} catch (PatternSyntaxException e) {
				input.setForeground(ERROR_FOREGROUND_COLOR);
			}

			markerBar.repaint();
			pane.repaint();
		}

		private SCEDocumentRange processOccurrence(ArrayList<SCEDocumentRange> resultsTemp, SCEDocument document, SCEMarkerBar markerBar, SCEPane pane, SCECaret caret, SCEDocumentRange selectionRange, int rowStart, int columnStart, int rowEnd, int columnEnd) {
			SCEDocumentPosition start = document.createDocumentPosition(rowStart, columnStart);
			SCEDocumentPosition end = document.createDocumentPosition(rowEnd, columnEnd);

			if (filter(start, end)) {
				resultsTemp.add(new SCEDocumentRange(start, end));
				pane.addTextHighlight(new SCETextHighlight(pane, start, end, Color.YELLOW));
				markerBar.addMarker(new SCEMarkerBar.Marker(SCEMarkerBar.TYPE_SEARCH, rowStart, columnStart, ""));

				if (caret.getRow() == rowStart && caret.getColumn() == columnStart) {
					selectionRange = new SCEDocumentRange(new SCEDocumentPosition(rowStart, columnStart), new SCEDocumentPosition(rowEnd, columnEnd));
				}
			}
			return selectionRange;
		}

		private boolean filter(SCEDocumentPosition start, SCEDocumentPosition end) {
			if (!selectionOnly.isSelected() || selection == null) return true;
			if (start.compareTo(selection.getStartPos()) < 0) return false;
			if (end.compareTo(selection.getEndPos()) > 0) return false;
			return true;
		}

		public void run() {
			try {
				while (!Thread.currentThread().isInterrupted()) {
					synchronized (this) {
						if ((!documentChanged && !searchChanged) || !isVisible()) {
							wait();
						}
					}
					if (!searchChanged && !documentChanged) continue;
					if (!isVisible()) continue;

					try {
						// update document information
						if (documentChanged) updateDocument();
						if (documentChanged) continue;

						// search
            search(searchChanged);
					} catch (Throwable ignored) {
						// unexpected error, recover
						try {
							sleep(500);
						} catch (InterruptedException ignored2) {
						}
						documentChanged = true;

            ignored.printStackTrace();
					}
				}
			} catch (InterruptedException ignored) {
			}
		}
	}
}
