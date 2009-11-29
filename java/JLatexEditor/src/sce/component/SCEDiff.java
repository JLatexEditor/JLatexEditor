package sce.component;

import util.diff.Diff;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

/**
 * Diff component.
 */
public class SCEDiff extends JSplitPane implements AdjustmentListener {
  private static final int WIDTH = 50;

  private String text;
  private SCEPane pane;
  private JScrollPane scrollPane;
  private SCEPane diff;
  private JScrollPane scrollDiff;

  public SCEDiff(JScrollPane scrollPane, SCEPane pane, String text, JScrollPane scrollDiff, SCEPane diff) {
    super(JSplitPane.HORIZONTAL_SPLIT, scrollPane, scrollDiff);
    this.text = text;
    this.pane = pane;
    this.scrollPane = scrollPane;
    this.diff = diff;
    this.scrollDiff = scrollDiff;

    scrollPane.getVerticalScrollBar().setName("paneV");
    scrollPane.getVerticalScrollBar().addAdjustmentListener(this);
    scrollPane.getHorizontalScrollBar().setName("paneH");
    scrollPane.getHorizontalScrollBar().addAdjustmentListener(this);

    scrollDiff.getVerticalScrollBar().setName("diffV");
    scrollDiff.getVerticalScrollBar().addAdjustmentListener(this);
    scrollDiff.getHorizontalScrollBar().setName("diffH");
    scrollDiff.getHorizontalScrollBar().addAdjustmentListener(this);

    setResizeWeight(.5);
    setDividerSize(50);

    setUI(new SCEDiffUI());
  }

  public void updateDiff() {
    SCEDocument paneDocument = pane.getDocument();
    SCEDocument diffDocument = diff.getDocument();

    String paneRows[] = getRows(paneDocument);
    String diffRows[] = getRows(diffDocument);

  }

  private String[] getRows(SCEDocument document) {
    SCEDocumentRow[] sceRows = document.getRows();

    String rows[] = new String[sceRows.length];
    for(int row = 0; row < rows.length; row++) {
      rows[row] = sceRows[row].toString();
    }
    return rows;
  }

  public void paint(Graphics g) {
    super.paint(g);

    if(diff.isEditable()) {
      diff.setText(text);
      diff.getCaret().moveTo(0,0);
      diff.getUndoManager().clear();
      diff.getDocument().setModified(false);
      diff.setEditable(false);
      setDividerLocation(.5);

      updateDiff();
    }
  }

  public void adjustmentValueChanged(AdjustmentEvent e) {
    JScrollBar scrollBar = (JScrollBar) e.getSource();
    if(scrollBar.getName().equals("paneH")) {
      scrollDiff.getHorizontalScrollBar().setValue(e.getValue());
    }
    if(scrollBar.getName().equals("diffH")) {
      scrollPane.getHorizontalScrollBar().setValue(e.getValue());
    }
  }

  private class SCEDiffUI extends BasicSplitPaneUI {
    public void paint(Graphics g, JComponent jc) {
      //super.paint(g, jc);
    }
  }
}
