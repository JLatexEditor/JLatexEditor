package sce.component;

import util.diff.Modification;
import util.diff.SystemDiff;
import util.diff.TokenList;

import javax.swing.*;
import javax.swing.plaf.ScrollPaneUI;
import javax.swing.plaf.basic.BasicScrollPaneUI;
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

  public static Color COLOR_ADD = new Color(189, 238, 192);
  public static Color COLOR_REMOVE = new Color(191, 190, 239);
  public static Color COLOR_CHANGE = new Color(237, 191, 188);

  private String text;
  private SCEPane pane;
  private SCEPane diff;
  private JScrollPane scrollPane;

  private int preferredLines = 0;
  private Dimension preferredSize = new Dimension();

  /**
   * Correspondence of the lines.
   */
  private List<Modification> modifications = null;
  private double[] line2Pane;
  private double[] line2Diff;

  public SCEDiff(SCEPane pane, String text, SCEPane diff) {
    super(JSplitPane.HORIZONTAL_SPLIT, pane, diff);
    setDoubleBuffered(false);
    this.text = text;
    this.pane = pane;
    this.diff = diff;

    diff.setText(text);
    diff.getCaret().moveTo(0, 0);
    diff.getUndoManager().clear();
    diff.getDocument().setModified(false);
    diff.setEditable(false);
    setDividerLocation(.5);
    
    setUI(new SCEDiffUI());
  }

  public void setScrollPane(JScrollPane scrollPane) {
    this.scrollPane = scrollPane;

    setDividerLocation((scrollPane.getVisibleRect().width - WIDTH) / 2);
    setDividerSize(WIDTH);

    Rectangle visibleRect = scrollPane.getVisibleRect();
    int visibleWidthLeft = getDividerLocation();
    int visibleWidthRight = visibleRect.width - visibleWidthLeft - WIDTH;
    double overLeft = Math.max(1, pane.getPreferredSize().width - visibleWidthLeft) / (double) visibleWidthLeft;
    double overRight = Math.max(1, diff.getPreferredSize().width - visibleWidthRight) / (double) visibleWidthRight;

    preferredSize.width = (int) (visibleRect.width * (1 + Math.max(0, Math.max(overLeft, overRight))));
    preferredSize.height = preferredLines * pane.getLineHeight() + 30;
  }

  public void updateDiff() {
    SCEDocument paneDocument = pane.getDocument();
    SCEDocument diffDocument = diff.getDocument();

    int paneRows = paneDocument.getRowsCount();
    int diffRows = diffDocument.getRowsCount();

    modifications = new SystemDiff().diff(diffDocument.getText(), paneDocument.getText());

    // create a mapping for line correspondence
    int diffLine = 0;
    int paneLine = 0;
    preferredLines = 0;
    line2Diff = new double[paneRows + diffRows];
    line2Pane = new double[paneRows + diffRows];
    for (Modification modification : modifications) {
      while (diffLine < modification.getSourceStartIndex() && paneLine < modification.getTargetStartIndex()) {
        line2Diff[preferredLines] = diffLine;
        line2Pane[preferredLines] = paneLine;
        paneLine++;
        diffLine++;
        preferredLines++;
      }

      int changeMax = Math.max(modification.getSourceLength(), modification.getTargetLength());
      double sourceFactor = modification.getSourceLength() / (double) changeMax;
      double targetFactor = modification.getTargetLength() / (double) changeMax;
      for (int lineNr = 0; lineNr < changeMax; lineNr++) {
        line2Pane[preferredLines + lineNr] = paneLine + targetFactor * lineNr;
        line2Diff[preferredLines + lineNr] = diffLine + sourceFactor * lineNr;
      }
      paneLine += modification.getTargetLength();
      diffLine += modification.getSourceLength();
      preferredLines += changeMax;
    }

    while (diffLine < diffRows && paneLine < paneRows) {
      line2Diff[paneLine] = diffLine;
      line2Pane[diffLine] = paneLine;
      paneLine++;
      diffLine++;
      preferredLines++;
    }
  }

  public void setLocation(int x, int y) {
    int lineHeight = pane.getLineHeight();
    int line = -y / lineHeight;
    double lineFraction = (-y - line * lineHeight) / (double) lineHeight;

    double paneLine = (1 - lineFraction) * line2Pane[line] + lineFraction * line2Pane[line + 1];
    double diffLine = (1 - lineFraction) * line2Diff[line] + lineFraction * line2Diff[line + 1];
    pane.setLocation(pane.getX(), (int) (-paneLine * lineHeight));
    diff.setLocation(diff.getX(), (int) (-diffLine * lineHeight));

    repaint();
  }

  public void setLocation(Point p) {
    setLocation(p.x, p.y);
  }

  public Dimension getPreferredSize() {
    return preferredSize;
  }

  private TokenList[] getRows(SCEDocument document) {
    SCEDocumentRow[] sceRows = document.getRows();

    TokenList rows[] = new TokenList[sceRows.length];
    for (int row = 0; row < rows.length; row++) {
      rows[row] = new TokenList(sceRows[row].toString(), true);
    }
    return rows;
  }

  public void paint(Graphics g) {
    super.paint(g);
  }

  public void adjustmentValueChanged(AdjustmentEvent e) {
    JScrollBar scrollBar = (JScrollBar) e.getSource();
    /*
    if(scrollBar.getName().equals("paneV") && !paneScrollBarAdjusting) {
      if(linesMapPaneDiff == null) return;
      
      int paneY = pane.getVisibleRect().y + pane.getVisibleRect().height/2;
      int paneRow = pane.viewToModel(0,paneY).getRow();
      double paneRowFraction = (paneY - pane.modelToView(paneRow, 0).y) / (double) pane.getLineHeight();

      int diffRow = (int) linesMapPaneDiff[paneRow];
      int diffY = diff.modelToView(diffRow, 0).y + (int) ((paneRowFraction) * diff.getLineHeight());

      JScrollBar bar = scrollDiff.getVerticalScrollBar();
      diffScrollBarAdjusting = true;
      bar.setValue(diffY - diff.getVisibleRect().height/2);
      diffScrollBarAdjusting = false;
      repaint();
    }
    if(scrollBar.getName().equals("diffV") && !diffScrollBarAdjusting) {
      if(linesMapDiffPane == null) return;

      int diffY = diff.getVisibleRect().y + diff.getVisibleRect().height/2;
      int diffRow = diff.viewToModel(0,diffY).getRow();
      double diffRowFraction = (diffY - diff.modelToView(diffRow, 0).y) / (double) diff.getLineHeight();

      int paneRow = (int) linesMapDiffPane[diffRow];
      int paneY = diff.modelToView(paneRow, 0).y + (int) ((diffRowFraction) * diff.getLineHeight());

      JScrollBar bar = scrollPane.getVerticalScrollBar();
      paneScrollBarAdjusting = true;
      bar.setValue(paneY - pane.getVisibleRect().height/2);
      paneScrollBarAdjusting = false;
      repaint();
    }
    */
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
      if (modifications == null) return;

      int left = 0;
      int right = getWidth();

      int paneOffsetY = pane.getVisibleRect().y;
      int paneFirstRow = pane.viewToModel(0, paneOffsetY).getRow();
      int diffOffsetY = diff.getVisibleRect().y;
      int diffFirstRow = diff.viewToModel(0, diffOffsetY).getRow();
      int visibleRows = pane.getVisibleRowsCount() + 1;

      int[] xpoints = new int[]{left, right, right, left};
      int[] ypoints = new int[4];
      for (Modification modification : modifications) {
        int sourceStart = modification.getSourceStartIndex();
        int sourceLength = modification.getSourceLength();
        int targetStart = modification.getTargetStartIndex();
        int targetLength = modification.getTargetLength();

        if (sourceStart + sourceLength < diffFirstRow && targetStart + targetLength < paneFirstRow) continue;
        if (sourceStart > diffFirstRow + visibleRows && targetStart > paneFirstRow + visibleRows) continue;
        int paneYStart = pane.modelToView(targetStart, 0).y - paneOffsetY;
        int paneYEnd = pane.modelToView(targetStart + targetLength, 0).y - paneOffsetY;
        int diffYStart = pane.modelToView(sourceStart, 0).y - diffOffsetY;
        int diffYEnd = pane.modelToView(sourceStart + sourceLength, 0).y - diffOffsetY;
        ypoints[0] = paneYStart;
        ypoints[1] = diffYStart;
        ypoints[2] = diffYEnd;
        ypoints[3] = paneYEnd;

        switch (modification.getType()) {
          case Modification.TYPE_ADD:
            g.setColor(COLOR_ADD);
            break;
          case Modification.TYPE_REMOVE:
            g.setColor(COLOR_REMOVE);
            break;
          case Modification.TYPE_CHANGED:
            g.setColor(COLOR_CHANGE);
            break;
        }
        g.fillPolygon(xpoints, ypoints, 4);
      }

      /*
      for(int rowNr = 0; rowNr < pane.getVisibleRowsCount() + 1; rowNr++) {
        int paneRow = paneFirstRow + rowNr;
        if(paneRow >= linesMapPaneDiff.length) break;
        int diffRow = (int) linesMapPaneDiff[paneRow];
        int paneY = pane.modelToView(paneRow, 0).y + (lineHeight / 2);
        int diffY = diff.modelToView(diffRow, 0).y + (int) (lineHeight * (linesMapPaneDiff[paneRow] - diffRow));
        g.drawLine(left, paneY - paneOffsetY, right, diffY - diffOffsetY);
      }
      */
    }
  }

  public static class SCEDiffScrollPane extends JScrollPane {
    public SCEDiffScrollPane(SCEDiff view) {
      super(view);
      view.updateDiff();
      setViewport(new SCEDiffViewPort());
      setViewportView(view);

      getVerticalScrollBar().setUnitIncrement(30);
    }
  }

  public static class SCEDiffViewPort extends JViewport {
    private Point viewPosition = new Point();

    public void setViewPosition(Point p) {
      viewPosition.x = p.x;
      viewPosition.y = p.y;

      Component view = getView();
      if(view != null) view.setLocation(-viewPosition.x, -viewPosition.y);
    }

    public Point getViewPosition() {
      return viewPosition;
    }
  }
}
