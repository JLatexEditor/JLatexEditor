package sce.codehelper;

import jlatexeditor.codehelper.PatternCompletion;
import sce.component.SCEPane;

import java.util.ArrayList;

/**
 * Combined code helper that handles several pattern based code helpers.
 * It determines which PatternCompletion can be applied in the current context
 * when you call matches and uses this helper for the next calls of
 * documentChanged, getWordToReplace, getCompletions, and getMaxCommonPrefix.
 *
 * @author Stefan Endrullis
 */
public class CombinedCodeCompletion extends PatternCompletion {
  private ArrayList<PatternCompletion> patternHelpers = new ArrayList<PatternCompletion>();
  private PatternCompletion currentPatternHelper = null;

	@Override
  public boolean matches() {
    for (PatternCompletion patternHelper : patternHelpers) {
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
  public Iterable<? extends CHCommand> getCompletions(int level) {
    return currentPatternHelper.getCompletions(level);
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
  public void addPatternHelper(PatternCompletion patternHelper) {
    patternHelpers.add(patternHelper);
  }

  @Override
  public void setSCEPane(SCEPane pane) {
    super.setSCEPane(pane);
    for (PatternCompletion patternHelper : patternHelpers) {
      patternHelper.setSCEPane(pane);
    }
  }
}
