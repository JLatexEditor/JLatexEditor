package sce.codehelper;

import sce.component.SCEPane;

/**
 * Alt+Enter assistant.
 */
public interface CodeAssistant {
  /**
   * Called by the SCEPaneUI if alt+enter was pressed.
   * If the code assistant (this) wants to handle the event it has to return true.
   * Otherwise it must return false.
   *
   * @param pane SCE pane
   * @return true if the code assistant performs the code assistance; otherwise false
   */
  public boolean assistAt(SCEPane pane);
}
