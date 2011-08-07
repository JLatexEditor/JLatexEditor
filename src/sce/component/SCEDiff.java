package sce.component;

import jlatexeditor.SCEManager;
import util.diff.Diff;
import util.diff.levenstein.Modification;
import util.diff.levenstein.TokenList;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Diff component.
 */
public class SCEDiff extends JPanel implements ComponentListener, PropertyChangeListener {
  private static final int WIDTH = 90;

  public static Color COLOR_ADD = new Color(189, 238, 192);
  public static Color COLOR_REMOVE = new Color(191, 190, 239);
  public static Color COLOR_CHANGE = new Color(237, 191, 188);

  private Point location = new Point();

  private DiffHeader header;

  private JSplitPane splitPane;
  private JScrollPane scrollPane;

  private String leftTitle;
  private SCEPane left;
  private JViewport leftViewport;
  private String rightTitle;
  private SCEPane right;
  private JViewport rightViewport;

  private SCEMarkerBar markerBar;

  private KeyListener paneKeyListener, diffKeyListener;

  private int preferredLines = 0;
  private Dimension preferredSize = new Dimension();
  private double preferredWidthFactor = 1;

  /**
   * Correspondence of the lines.
   */
  private List<Modification<String>> modifications = null;
  private double[] line2Pane;
  private double[] line2Diff;

  public SCEDiff(String leftTitle, final SCEPane left, String rightTitle, String rightText, SCEMarkerBar markerBar) {
    setLayout(new BorderLayout());
    setDoubleBuffered(false);
    this.leftTitle = leftTitle;
    this.left = left;
    this.rightTitle = rightTitle;
    this.right = new SCEPane(left.getSourceCodeEditor());
    SCEManager.setupLatexSCEPane(this.right);
    this.markerBar = markerBar;

    right.setText(rightText);
    updateDiff();

    leftViewport = new SCEDiffViewport(left, this);
    rightViewport = new SCEDiffViewport(right, this);

    splitPane = new SCEDiffSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftViewport, rightViewport);
    scrollPane = new SCEDiff.SCEDiffScrollPane(splitPane);
    add(scrollPane, BorderLayout.CENTER);
    addComponentListener(this);
    splitPane.addPropertyChangeListener(this);

    header = new DiffHeader();
    splitPane.addPropertyChangeListener(header);
    add(header, BorderLayout.NORTH);

    splitPane.setUI(new SCEDiffUI());

    right.getCaret().moveTo(0, 0, false);
    right.getUndoManager().clear();
    right.getDocument().setModified(false);
    right.getDocument().setEditable(false);

    left.addKeyListener(paneKeyListener = new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getModifiers() == KeyEvent.ALT_MASK) {
          // alt+up
          if (e.getKeyCode() == KeyEvent.VK_UP) {
            jumpToPreviousTargetModification();
            e.consume();
          }
          // alt+down
          if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            jumpToNextTargetModification();
            e.consume();
          }
        }
      }
    });
    right.addKeyListener(diffKeyListener = new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getModifiers() == KeyEvent.ALT_MASK) {
          // alt+up
          if (e.getKeyCode() == KeyEvent.VK_UP) {
            jumpToPreviousSourceModification();
            e.consume();
          }
          // alt+down
          if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            jumpToNextSourceModification();
            e.consume();
          }
        }
      }
    });

    left.setAddToPreferredSize(new Dimension(2560,0));
    right.setAddToPreferredSize(new Dimension(2560, 0));

    left.setTransparentTextBackground(true);
    right.setTransparentTextBackground(true);

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        updateLayout(true);
      }
    });
  }

  public int getVirtualLines() {
    JViewport viewport = scrollPane.getViewport();
    if (viewport == null) return 40;
    Component view = viewport.getView();
    if (view == null) return 40;
    return view.getHeight() / Math.max(left.getLineHeight(), 1);
  }

  public void jumpToPreviousTargetModification() {
    SCECaret caret = left.getCaret();
    int currRow = caret.getRow();

    for (int modi = modifications.size() - 1; modi >= 0; modi--) {
      if (modifications.get(modi).getTargetStartIndex() < currRow) {
        left.getCaret().moveTo(modifications.get(modi).getTargetStartIndex(), 0, false);
        break;
      }
    }
  }

  public void jumpToNextTargetModification() {
    SCECaret caret = left.getCaret();
    int currRow = caret.getRow();

    for (Modification modification : modifications) {
      if (modification.getTargetStartIndex() > currRow) {
        left.getCaret().moveTo(modification.getTargetStartIndex(), 0, false);
        break;
      }
    }
  }

  public void jumpToPreviousSourceModification() {
    SCECaret caret = right.getCaret();
    int currRow = caret.getRow();

    for (int modi = modifications.size() - 1; modi >= 0; modi--) {
      if (modifications.get(modi).getSourceStartIndex() < currRow) {
        right.getCaret().moveTo(modifications.get(modi).getSourceStartIndex(), 0, false);
        break;
      }
    }
  }

  public void updateLayout(boolean setDividerLocation) {
    final Rectangle visibleRect = scrollPane.getVisibleRect();

    if(setDividerLocation) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          splitPane.setDividerLocation((visibleRect.width - WIDTH) / 2);
          splitPane.setDividerSize(WIDTH);
        }
      });
    }

    int visibleWidthLeft = splitPane.getDividerLocation();
    int visibleWidthRight = visibleRect.width - visibleWidthLeft - WIDTH;
    double overLeft = Math.max(1, left.getPreferredSize().width - left.getAddToPreferredSize().width - visibleWidthLeft) / (double) visibleWidthLeft;
    double overRight = Math.max(1, right.getPreferredSize().width - right.getAddToPreferredSize().width - visibleWidthRight) / (double) visibleWidthRight;

    preferredWidthFactor = 1 + Math.max(0, Math.max(overLeft, overRight));
    preferredSize.width = (int) (visibleRect.width * preferredWidthFactor);
    preferredSize.height = preferredLines * left.getLineHeight() + 30;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        validate();
      }
    });
  }

  private boolean firstInvalidate = true;
  public void invalidate() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        updateLayout(firstInvalidate);
        firstInvalidate = false;
      }
    });

    super.invalidate();
  }

  public void jumpToNextSourceModification() {
    SCECaret caret = right.getCaret();
    int currRow = caret.getRow();

    for (Modification modification : modifications) {
      if (modification.getSourceStartIndex() > currRow) {
        right.getCaret().moveTo(modification.getSourceStartIndex(), 0, false);
        break;
      }
    }
  }

  public void updateDiff() {
    markerBar.clear();
    SCEDocument paneDocument = left.getDocument();
    SCEDocument diffDocument = right.getDocument();

    int paneRows = paneDocument.getRowsModel().getRowsCount();
    int diffRows = diffDocument.getRowsModel().getRowsCount();

    modifications = Diff.diff(diffDocument.getText(), paneDocument.getText());

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

      int markerType;
      switch (modification.getType()) {
        case Modification.TYPE_ADD:
          markerType = SCEMarkerBar.TYPE_SVN_ADD;
          break;
        case Modification.TYPE_REMOVE:
          markerType = SCEMarkerBar.TYPE_SVN_REMOVE;
          break;
        default:
          markerType = SCEMarkerBar.TYPE_SVN_CHANGE;
      }

      int centerLine = preferredLines + changeMax / 2;
      final int visibleTop = (centerLine - left.getVisibleRowsCount() / 2) * left.getLineHeight();
      markerBar.addMarker(new SCEMarkerBar.Marker(markerType, preferredLines, preferredLines + changeMax,
              new Runnable() {
                public void run() {
                  JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
                  scrollBar.setValue(visibleTop);
                }
              }));

      preferredLines += changeMax;
    }

    while (diffLine < diffRows && paneLine < paneRows) {
      line2Diff[preferredLines] = diffLine;
      line2Pane[preferredLines] = paneLine;
      paneLine++;
      diffLine++;
      preferredLines++;
    }

    left.removeAllRowHighlights();
    right.removeAllRowHighlights();
    for (Modification modification : modifications) {
      Color color = null;
      if (modification.getType() == Modification.TYPE_ADD) color = COLOR_ADD;
      if (modification.getType() == Modification.TYPE_REMOVE) color = COLOR_REMOVE;
      if (modification.getType() == Modification.TYPE_CHANGED) color = COLOR_CHANGE;

      int sourceStart = modification.getSourceStartIndex();
      int sourceEnd = sourceStart + modification.getSourceLength();
      for (int line = sourceStart; line < sourceEnd; line++) {
        right.addRowHighlight(new SCERowHighlight(right, line, color, false, line == sourceStart, line == sourceEnd-1));
      }
      if (sourceEnd == sourceStart) {
        right.addRowHighlight(new SCERowHighlight(right, sourceStart, color, true, true, true));
      }

      int targetStart = modification.getTargetStartIndex();
      int targetEnd = targetStart + modification.getTargetLength();
      for (int line = targetStart; line < targetEnd; line++) {
        left.addRowHighlight(new SCERowHighlight(left, line, color, false, line == targetStart, line == targetEnd-1));
      }
      if (targetEnd == targetStart) {
        left.addRowHighlight(new SCERowHighlight(left, targetStart, color, true, true, true));
      }
    }

    invalidate();
  }

  public SCEPane getTextPane() {
    return left;
  }

  public SCEPane getDiffPane() {
    return right;
  }

  private TokenList[] getRows(SCEDocument document) {
    SCEDocumentRow[] sceRows = document.getRowsModel().getRows();

    TokenList rows[] = new TokenList[sceRows.length];
    for (int row = 0; row < rows.length; row++) {
      rows[row] = new TokenList(sceRows[row].toString(), true);
    }
    return rows;
  }

  public void paint(Graphics g) {
    super.paint(g);
  }

  /**
   * ComponentListener.
   */
  public void componentResized(ComponentEvent e) {
    updateLayout(false);
  }

  public void componentMoved(ComponentEvent e) {
  }

  public void componentShown(ComponentEvent e) {
    updateLayout(true);
  }

  public void componentHidden(ComponentEvent e) {
  }

  /**
   * ActionListener: close button.
   */
  public void actionPerformed(ActionEvent e) {
    close();
  }

  /**
   * PropertyChangeListener: moving of the divider.
   */
  public void propertyChange(PropertyChangeEvent evt) {
    updateLayout(false);
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

    public void paint(Graphics graphics) {
      super.paint(graphics);

      Graphics2D g = (Graphics2D) graphics;
      if (modifications == null) return;

      int left = 0;
      int right = getWidth();

      int paneOffsetY = -SCEDiff.this.left.getY();
      int paneFirstRow = SCEDiff.this.left.viewToModel(0, paneOffsetY).getRow();
      int diffOffsetY = -SCEDiff.this.right.getY();
      int diffFirstRow = SCEDiff.this.right.viewToModel(0, diffOffsetY).getRow();
      int visibleRows = SCEDiff.this.left.getVisibleRowsCount() + 1;

      int[] xpoints = new int[]{left-5, left + 20, right - 20, right+5, right+5, right - 20, left + 20, left-5};
      int[] ypoints = new int[8];
      for (Modification modification : modifications) {
        int sourceStart = modification.getSourceStartIndex();
        int sourceLength = modification.getSourceLength();
        int targetStart = modification.getTargetStartIndex();
        int targetLength = modification.getTargetLength();

        if (sourceStart + sourceLength < diffFirstRow && targetStart + targetLength < paneFirstRow) continue;
        if (sourceStart > diffFirstRow + visibleRows && targetStart > paneFirstRow + visibleRows) continue;
        int paneYStart = SCEDiff.this.left.modelToView(targetStart, 0).y - paneOffsetY;
        int paneYEnd = SCEDiff.this.left.modelToView(targetStart + targetLength, 0).y - paneOffsetY;
        int diffYStart = SCEDiff.this.left.modelToView(sourceStart, 0).y - diffOffsetY;
        int diffYEnd = SCEDiff.this.left.modelToView(sourceStart + sourceLength, 0).y - diffOffsetY;
        if (targetLength == 0) {
          paneYStart--;
          paneYEnd += 2;
        }
        if (sourceLength == 0) {
          diffYStart--;
          diffYEnd += 2;
        }
        ypoints[0] = paneYStart-1;
        ypoints[1] = paneYStart-1;
        ypoints[2] = diffYStart-1;
        ypoints[3] = diffYStart-1;
        ypoints[4] = diffYEnd-2;
        ypoints[5] = diffYEnd-2;
        ypoints[6] = paneYEnd-2;
        ypoints[7] = paneYEnd-2;

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
        g.fillPolygon(xpoints, ypoints, 8);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.GRAY);
        g.drawPolygon(xpoints, ypoints, 8);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
      }
    }
  }

  /**
   * Closes the diff.
   */
  public void close() {
    left.removeKeyListener(paneKeyListener);
    right.removeKeyListener(diffKeyListener);
    removeComponentListener(this);

    leftViewport.setView(null);
    rightViewport.setView(null);
    splitPane.setLeftComponent(null);
    splitPane.setRightComponent(null);

    left.setAddToPreferredSize(new Dimension(0,0));
    right.setAddToPreferredSize(new Dimension(0,0));

    left.setTransparentTextBackground(false);
    right.setTransparentTextBackground(false);
  }

  private class SCEDiffSplitPane extends JSplitPane {
    public SCEDiffSplitPane(int newOrientation, Component newLeftComponent, Component newRightComponent) {
      super(newOrientation, newLeftComponent, newRightComponent);
    }

    public void setLocation(int x, int y) {
      location.x = x;
      location.y = y;

      int visibleHeight = scrollPane.getVisibleRect().height;
      int lineHeight = left.getLineHeight();
      int halfLines = visibleHeight / 2 / lineHeight;

      int lineOffset = Math.min(halfLines, line2Pane.length-1);
      while (lineOffset < line2Pane.length-1 && line2Pane[lineOffset] < halfLines && line2Diff[lineOffset] < halfLines) lineOffset++;
      int yCorrection = lineHeight * (int) Math.max(line2Pane[lineOffset], line2Diff[lineOffset]);

      y = y - lineOffset * lineHeight;
      int line = Math.min(Math.max(0, -y / lineHeight), line2Pane.length-2);
      double lineFraction = (-y - line * lineHeight) / (double) lineHeight;

      double paneLine = (1 - lineFraction) * line2Pane[line] + lineFraction * line2Pane[line + 1];
      double diffLine = (1 - lineFraction) * line2Diff[line] + lineFraction * line2Diff[line + 1];
      int rx = (int) (x / preferredWidthFactor);
      leftViewport.setViewPosition(new Point(-rx, (int) (paneLine * lineHeight) - yCorrection));
      rightViewport.setViewPosition(new Point(-rx, (int) (diffLine * lineHeight) - yCorrection));

      repaint();
    }

    public Point getLocation(Point rv) {
      return location;
    }

    public void setLocation(Point p) {
      setLocation(p.x, p.y);
    }

    public Dimension getPreferredSize() {
      return preferredSize;
    }
  }

  public static class SCEDiffScrollPane extends JScrollPane {
    public SCEDiffScrollPane(JComponent view) {
      super(view);
      setViewport(new SCEDiffScrollPaneViewport());
      setViewportView(view);

      getVerticalScrollBar().setUnitIncrement(30);
    }
  }

  public static class SCEDiffScrollPaneViewport extends JViewport {
    private Point viewPosition = new Point();

    public void setViewPosition(Point p) {
      viewPosition.x = p.x;
      viewPosition.y = p.y;

      Component view = getView();
      if (view != null) view.setLocation(-viewPosition.x, -viewPosition.y);
    }

    public Point getViewPosition() {
      return viewPosition;
    }

    public Rectangle getViewRect() {
      Dimension size = getViewSize();
      return new Rectangle(viewPosition.x, viewPosition.y, size.width, size.height);
    }
  }

  private class SCEDiffViewport extends JViewport {
    private SCEDiff diffView;

    private SCEDiffViewport(Component view, SCEDiff diffView) {
      setView(view);
      this.diffView = diffView;
      setLayout(new ViewportLayout() {
        public void layoutContainer(Container parent) {
          JViewport vp = (JViewport) parent;
          if (!getViewSize().equals(vp.getSize())) setViewSize(vp.getSize());
          if (getViewPosition().x < 0) setViewPosition(new Point(0, 0));
        }
      });
    }

    public void setView(Component view) {
      super.setView(view);
      setViewPosition(new Point(-1, -1));
    }

    public void scrollRectToVisible(Rectangle rectangle) {
      JScrollBar vbar = scrollPane.getVerticalScrollBar();
      JScrollBar hbar = scrollPane.getHorizontalScrollBar();

      JViewport currentViewport = diffView.getDiffPane().hasFocus() ? rightViewport : leftViewport;
      SCEPane currentPane = diffView.getDiffPane().hasFocus() ? diffView.getDiffPane() : diffView.getTextPane();

      double[] lineMap = diffView.getDiffPane().hasFocus() ? line2Diff : line2Pane;

      Rectangle visible = currentViewport.getVisibleRect();
      if (visible.getY() > rectangle.getY() || visible.getY() + visible.getHeight() < rectangle.getY() + rectangle.getHeight()) {
        boolean up = rectangle.getY() < visible.getY();
        int lineHeight = currentPane.getLineHeight();
        int line = -(location.y - (int) visible.getHeight() / 2) / lineHeight;

        int nline = line;
        if (up) {
          int distance = (int) (visible.getY() - rectangle.getY());
          while (nline >= 0 && (lineMap[line] - lineMap[nline]) * lineHeight < distance) nline--;
        } else {
          int distance = (int) (rectangle.getY() + rectangle.getHeight() - visible.getY() - visible.getHeight());
          while (nline < lineMap.length && (lineMap[nline] - lineMap[line]) * lineHeight < distance) nline++;
        }
        vbar.setValue(vbar.getValue() + (nline - line) * lineHeight);
      }

      if (visible.getX() > rectangle.getX() || visible.getX() + visible.getWidth() < rectangle.getX() + rectangle.getWidth()) {
        boolean left = rectangle.getX() < visible.getX();
        int distance;
        if (left) {
          distance = (int) (rectangle.getX() - visible.getX());
        } else {
          distance = (int) (rectangle.getX() + rectangle.getWidth() - visible.getX() - visible.getWidth());
        }
        hbar.setValue(vbar.getValue() + (int) (distance * preferredWidthFactor));
      }
    }
  }

  private class DiffHeader extends JPanel implements PropertyChangeListener {
    public void paint(Graphics g) {
      super.paint(g);

      g.drawString(leftTitle, 5, 15);
      g.drawString(rightTitle, splitPane.getDividerLocation() + WIDTH + 5, 15);
    }

    public Dimension getPreferredSize() {
      return new Dimension(2600, 19);
    }

    public void propertyChange(PropertyChangeEvent evt) {
      repaint();
    }
  }
}
