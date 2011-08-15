package jlatexeditor.syntaxhighlighting;

import jlatexeditor.codehelper.BackgroundParser;
import jlatexeditor.gui.TemplateEditor;
import sce.codehelper.CHCommand;
import sce.codehelper.CHCommandArgument;
import sce.component.SCEDocumentChar;
import sce.component.SCEDocumentRow;
import sce.component.SCEPane;
import sce.syntaxhighlighting.ParserState;
import sce.syntaxhighlighting.ParserStateStack;
import util.SpellChecker;
import util.Trie;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Template syntax highlighting.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class TemplateSyntaxHighlighting extends LatexSyntaxHighlighting {
	private static final Pattern TEMPLATE_ARGUMENT_PATTERN = Pattern.compile("@([^@]+)@");
	private Collection<String> templateArguments = new HashSet<String>();
	private TemplateEditor templateEditor;

	public TemplateSyntaxHighlighting(SCEPane pane, SpellChecker spellChecker, Trie<CHCommand> commands, BackgroundParser backgroundParser, TemplateEditor templateEditor) {
		super(pane, spellChecker, commands, backgroundParser);
		this.templateEditor = templateEditor;
	}

	@Override
	protected Iterator<CHCommandArgument> parseRow(int row_nr, Iterator<CHCommandArgument> argumentsIterator, SCEDocumentRow row, ParserStateStack stateStack, ParserState state) {
		Iterator<CHCommandArgument> commandArgumentIterator = super.parseRow(row_nr, argumentsIterator, row, stateStack, state);

		Matcher matcher = TEMPLATE_ARGUMENT_PATTERN.matcher(row.toString());
		while(matcher.find()) {
			boolean argumentDefined = matcher.group(1).equals("|") || templateEditor.hasTemplateArgument(matcher.group(1));
			String styleString = argumentDefined ? "template_argument_defined" : "template_argument_not_defined";
			byte style = state.getStyles()[getStyle(styleString, LatexStyles.TEXT)];
			StyleableTerm term = new StyleableTerm(matcher.group(), row.chars, matcher.start(), style);
			term.applyStyleToDoc();
		}

		return commandArgumentIterator;
	}
}
