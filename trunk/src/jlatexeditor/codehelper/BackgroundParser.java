package jlatexeditor.codehelper;

import jlatexeditor.JLatexEditorJFrame;

/**
 * Parsing files in background.
 */
public class BackgroundParser extends Thread {
  private JLatexEditorJFrame editor;

  public BackgroundParser(JLatexEditorJFrame editor) {
    this.editor = editor;
  }

  public void run() {
  }
}
