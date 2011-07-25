package jlatexeditor;

import sce.component.SourceCodeEditor;

public interface SCEManagerInteraction {
  public int getEditorCount();
  public SourceCodeEditor<Doc> getEditor(int tab);
	public SourceCodeEditor<Doc> getEditor(Doc doc);
  public SourceCodeEditor<Doc> getActiveEditor();
  public SourceCodeEditor<Doc> getMainEditor();

  public SourceCodeEditor<Doc> open(Doc doc);
  public SourceCodeEditor<Doc> open(Doc doc, int lineNr);
}
