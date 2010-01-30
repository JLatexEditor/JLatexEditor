package jlatexeditor.codehelper;

import sce.codehelper.CHCommand;
import sce.codehelper.WordReplacement;
import sce.codehelper.WordWithPos;
import sce.component.SCEPane;

import java.util.ArrayList;

/**
 * Latex code helper that determine the current context of the caret
 * and forwards the code helping request to the specific code helper.
 *
 * @author Stefan Endrullis
 */
public class LatexCodeHelper extends PatternHelper {
	private ArrayList<PatternHelper> patternHelpers = new ArrayList<PatternHelper>();
	private PatternHelper currentPatternHelper = null;

	@Override
	public boolean matches() {
		for (PatternHelper patternHelper : patternHelpers) {
			if (patternHelper.matches()) {
				currentPatternHelper = patternHelper;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean documentChanged() {
		return currentPatternHelper.documentChanged();
	}

	@Override
	public WordWithPos getWordToReplace() {
		return currentPatternHelper.getWordToReplace();
	}

	@Override
	public Iterable<? extends CHCommand> getCompletions() {
		return currentPatternHelper.getCompletions();
	}

	@Override
	public String getMaxCommonPrefix() {
		return currentPatternHelper.getMaxCommonPrefix();
	}

	/**
	 * Adds a code helper to this code helper collection.
	 *
	 * @param patternHelper code helper to add
	 */
	public void addPatternHelper(PatternHelper patternHelper) {
		patternHelpers.add(patternHelper);
	}

	@Override
	public void setSCEPane(SCEPane pane) {
		super.setSCEPane(pane);
		for (PatternHelper patternHelper : patternHelpers) {
			patternHelper.setSCEPane(pane);
		}
	}
}
