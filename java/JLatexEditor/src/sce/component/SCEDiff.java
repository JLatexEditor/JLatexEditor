package sce.component;

import util.diff.Diff;
import util.diff.Modification;
import util.diff.SystemDiff;
import util.diff.TokenList;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.List;

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

  /**
   * Forwards and backwards correspondence of the lines.
   */
  private double[] linesMapPaneDiff;
  private double[] linesMapDiffPane;

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

    int paneRows = paneDocument.getRowsCount();
    int diffRows = diffDocument.getRowsCount();

    List<Modification> modifications = new SystemDiff().diff(diffDocument.getText(), paneDocument.getText());

    // create a mapping for line correspondence
    int diffLine = 0;
    int paneLine = 0;
    linesMapPaneDiff = new double[paneRows];
    linesMapDiffPane = new double[diffRows];
    for(Modification modification : modifications) {
      while(diffLine < modification.getSourceStartIndex() && paneLine < modification.getTargetStartIndex()) {
        linesMapPaneDiff[paneLine] = diffLine + .5;
        linesMapDiffPane[diffLine] = paneLine + .5;
        paneLine++;
        diffLine++;
      }

      double sourceOffset = modification.getSourceLength() > 0 ? .5 : 0;
      double targetOffset = modification.getTargetLength() > 0 ? .5 : 0;
      double sourceFactor = Math.max(0, modification.getSourceLength() - 1) / (double) Math.max(1, modification.getTargetLength() - 1);
      double targetFactor = Math.max(0, modification.getTargetLength() - 1) / (double) Math.max(1, modification.getSourceLength() - 1);
      for(int lineNr = 0; lineNr < modification.getSourceLength(); lineNr++) {
        linesMapDiffPane[diffLine + lineNr] = paneLine + targetOffset + targetFactor*lineNr;
      }
      for(int lineNr = 0; lineNr < modification.getSourceLength(); lineNr++) {
        linesMapPaneDiff[paneLine + lineNr] = diffLine + sourceOffset + sourceFactor*lineNr;
      }
      paneLine += modification.getTargetLength();
      diffLine += modification.getSourceLength();
    }

    while(diffLine < diffRows && paneLine < paneRows) {
      linesMapPaneDiff[paneLine] = diffLine + .5;
      linesMapDiffPane[diffLine] = paneLine + .5;
      paneLine++;
      diffLine++;
    }
  }

  private TokenList[] getRows(SCEDocument document) {
    SCEDocumentRow[] sceRows = document.getRows();

    TokenList rows[] = new TokenList[sceRows.length];
    for(int row = 0; row < rows.length; row++) {
      rows[row] = new TokenList(sceRows[row].toString(), true);
    }
    return rows;
  }

  public void paint(Graphics g) {
    super.paint(g);
  }

  public void adjustmentValueChanged(AdjustmentEvent e) {
    JScrollBar scrollBar = (JScrollBar) e.getSource();
    if(scrollBar.getName().equals("paneH")) {
      scrollDiff.getHorizontalScrollBar().setValue(e.getValue());
    }
    if(scrollBar.getName().equals("diffH")) {
      scrollPane.getHorizontalScrollBar().setValue(e.getValue());
    }
    if(scrollBar.getName().equals("paneV")) {
      if(linesMapPaneDiff == null) return;
      
      int paneY = pane.getVisibleRect().y + pane.getVisibleRect().height/2;
      int paneRow = pane.viewToModel(0,paneY).getRow();
      double paneRowFraction = (paneY - pane.modelToView(paneRow, 0).y) / (double) pane.getLineHeight();

      int diffRow = (int) linesMapPaneDiff[paneRow];
      int diffY = diff.modelToView(diffRow, 0).y + (int) ((paneRowFraction) * diff.getLineHeight());

      scrollDiff.getVerticalScrollBar().setValue(diffY - diff.getVisibleRect().height/2);
    }
    if(scrollBar.getName().equals("diffV")) {
      repaint();
    }
  }

  private class SCEDiffUI extends BasicSplitPaneUI {
    public BasicSplitPaneDivider createDefaultDivider() {
      return new SCEDiffDivider(this);
    }
  }

  private class SCEDiffDivider extends BasicSplitPaneDivider {
    public SCEDiffDivider(BasicSplitPaneUI basicSplitPaneUI) {
      super(basicSplitPaneUI);
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

      int left = 0;
      int right = getWidth();

      int paneOffsetY = pane.getVisibleRect().y;
      int paneFirstRow = pane.viewToModel(0,paneOffsetY).getRow();
      int diffOffsetY = diff.getVisibleRect().y;
      int lineHeight = pane.getLineHeight();
      for(int rowNr = 0; rowNr < pane.getVisibleRowsCount() + 1; rowNr++) {
        int paneRow = paneFirstRow + rowNr;
        if(paneRow >= linesMapPaneDiff.length) break;
        int diffRow = (int) linesMapPaneDiff[paneRow];
        int paneY = pane.modelToView(paneRow, 0).y + (lineHeight / 2);
        int diffY = diff.modelToView(diffRow, 0).y + (int) (lineHeight * (linesMapPaneDiff[paneRow] - diffRow));
        g.drawLine(left, paneY - paneOffsetY, right, diffY - diffOffsetY);
      }
    }
  }
}
