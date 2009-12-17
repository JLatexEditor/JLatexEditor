
/**
 * @author Jörg Endrullis
 */

package sce.component;

public interface SCEDocumentListener{
  /**
   * Informs the document listener about a change in the document.
   *
   * @param sender the document
   * @param event the document event
   */
  public void documentChanged(SCEDocument sender, SCEDocumentEvent event);
}
