/**
 * @author Jörg Endrullis
 */

package sce.component;

public interface SCESelectionListener {
  /**
   * Informs the selection listener about a selection change.
   *
   * @param sender the document
   * @param start  selection start (null if there is no selection)
   * @param end    selection end (null if there is no selection)
   */
  public void selectionChanged(SCEDocument sender, SCEDocumentPosition start, SCEDocumentPosition end);
}