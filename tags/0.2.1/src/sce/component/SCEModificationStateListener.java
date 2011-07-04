package sce.component;

/**
 * Listener that want to be informed about document
 */
public interface SCEModificationStateListener {
  /**
   * Informs the listener about the change of the modification state of the document.
   *
   * @param modified true if the document contains unsaved modifications
   */
  public void modificationStateChanged(boolean modified);
}
