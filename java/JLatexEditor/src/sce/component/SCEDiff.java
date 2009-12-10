package sce.component;

import util.diff.Modification;
import util.diff.SystemDiff;
import util.diff.TokenList;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.util.List;

/**
 * Diff component.
 */
public class SCEDiff extends JSplitPane {
  private static final int WIDTH = 50;

  public static Color COLOR_ADD = new Color(189, 238, 192);
  public static Color COLOR_REMOVE = new Color(191, 190, 239);
  public static Color COLOR_CHANGE = new Color(237, 191, 188);

  private String text;
  private SCEPane pane;
  private JViewport paneViewport;
  private SCEPane diff;
  private JViewport diffViewport;
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
    super(JSplitPane.HORIZONTAL_SPLIT, new SCEDiffViewport(pane), new SCEDiffViewport(diff));
    setDoubleBuffered(false);
    paneViewport = (JViewport) getLeftComponent();
    diffViewport = (JViewport) getRightComponent();
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
    for(Modification modification : modifications) {
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
      line2Diff[preferredLines] = diffLine;
      line2Pane[preferredLines] = paneLine;
      paneLine++;
      diffLine++;
      preferredLines++;
    }

    pane.removeAllRowHighlights();
    diff.removeAllRowHighlights();
    for(Modification modification : modifications) {
      Color color = null;
      if(modification.getType() == Modification.TYPE_ADD) color = COLOR_ADD;
      if(modification.getType() == Modification.TYPE_REMOVE) color = COLOR_REMOVE;
      if(modification.getType() == Modification.TYPE_CHANGED) color = COLOR_CHANGE;
      
      int sourceStart = modification.getSourceStartIndex();
      int sourceEnd = sourceStart + modification.getSourceLength();
      for(int line = sourceStart; line < sourceEnd; line++) {
        diff.addRowHighlight(new SCERowHighlight(diff, line, color, false));
      }
      if(sourceEnd == sourceStart) diff.addRowHighlight(new SCERowHighlight(diff, sourceStart, color, true));

      int targetStart = modification.getTargetStartIndex();
      int targetEnd = targetStart + modification.getTargetLength();
      for(int line = targetStart; line < targetEnd; line++) {
        pane.addRowHighlight(new SCERowHighlight(pane, line, color, false));
      }
      if(targetEnd == targetStart) pane.addRowHighlight(new SCERowHighlight(pane, targetStart, color, true));
    }
  }

  public void setLocation(int x, int y) {
    int visibleHeight = scrollPane.getVisibleRect().height;
    int lineHeight = pane.getLineHeight();
    int halfLines = visibleHeight/2/lineHeight;

    int lineOffset = halfLines;
    while(line2Pane[lineOffset] < halfLines && line2Diff[lineOffset] < halfLines) lineOffset++;
    int yCorrection = lineHeight * (int) Math.max(line2Pane[lineOffset], line2Diff[lineOffset]);

    y = y - lineOffset*lineHeight;
    int line = -y/lineHeight;
    double lineFraction = (-y - line * lineHeight) / (double) lineHeight;

    double paneLine = (1 - lineFraction) * line2Pane[line] + lineFraction * line2Pane[line + 1];
    double diffLine = (1 - lineFraction) * line2Diff[line] + lineFraction * line2Diff[line + 1];
    paneViewport.setViewPosition(new Point(pane.getX(), (int) (paneLine * lineHeight) - yCorrection));
    diffViewport.setViewPosition(new Point(diff.getX(), (int) (diffLine * lineHeight) - yCorrection));

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

  private class SCEDiffUI extends BasicSplitPaneUI {
    public BasicSplitPaneDivider createDefaultDivider() {
      return new SCEDiffDivider(this);
    }

    public void paint(Graphics g, JComponent jc) {
      super.paint(g, jc);
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

      int paneOffsetY = -pane.getY();
      int paneFirstRow = pane.viewToModel(0, paneOffsetY).getRow();
      int diffOffsetY = -diff.getY();
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
        if(targetLength == 0) { paneYStart--; paneYEnd += 2; }
        if(sourceLength == 0) { diffYStart--; diffYEnd += 2; }
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

    public Rectangle getViewRect() {
      Dimension size = getViewSize();
      return new Rectangle(viewPosition.x, viewPosition.y, size.width, size.height);
    }
  }

  private static class SCEDiffViewport extends JViewport {
    private SCEDiffViewport(Component view) {
      setView(view);
      setLayout(new ViewportLayout() {
        public void layoutContainer(Container parent) {
          JViewport vp = (JViewport)parent;
          if(!getViewSize().equals(vp.getSize())) setViewSize(vp.getSize());
          if(getViewPosition().x < 0) setViewPosition(new Point(0,0));
        }
      });
    }

    public void setView(Component view) {
      super.setView(view);
      setViewPosition(new Point(-1,-1));
    }
  }
}
