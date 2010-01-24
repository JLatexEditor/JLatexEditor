package sce.component;

import util.diff.Modification;
import util.diff.SystemDiff;
import util.diff.TokenList;

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
public class SCEDiff extends JPanel implements ComponentListener {
  private static final int WIDTH = 70;

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

  public SCEDiff(String leftTitle, final SCEPane left, String rightTitle, String rightText) {
    setLayout(new BorderLayout());
    setDoubleBuffered(false);
    this.leftTitle = leftTitle;
    this.left = left;
    this.rightTitle = rightTitle;
    this.right = new SCEPane();

    right.setText(rightText);
    updateDiff();

    leftViewport = new SCEDiffViewport(left, this);
    rightViewport = new SCEDiffViewport(right, this);

    splitPane = new SCEDiffSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftViewport, rightViewport);
    scrollPane = new SCEDiff.SCEDiffScrollPane(splitPane);
    add(scrollPane, BorderLayout.CENTER);
    updateLayout();
    addComponentListener(this);

    header = new DiffHeader();
    splitPane.addPropertyChangeListener(header);
    add(header, BorderLayout.NORTH);

    splitPane.setUI(new SCEDiffUI());

    right.getCaret().moveTo(0, 0);
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
  }

	public void jumpToPreviousTargetModification() {
		SCECaret caret = left.getCaret();
		int currRow = caret.getRow();

		for (int modi = modifications.size()-1; modi >= 0; modi--) {
			if (modifications.get(modi).getTargetStartIndex() < currRow) {
				left.getCaret().moveTo(modifications.get(modi).getTargetStartIndex(), 0);
				break;
			}
		}
	}

	public void jumpToNextTargetModification() {
		SCECaret caret = left.getCaret();
		int currRow = caret.getRow();

		for (Modification modification : modifications) {
			if (modification.getTargetStartIndex() > currRow) {
				left.getCaret().moveTo(modification.getTargetStartIndex(), 0);
				break;
			}
		}
	}

	public void jumpToPreviousSourceModification() {
		SCECaret caret = right.getCaret();
		int currRow = caret.getRow();

		for (int modi = modifications.size()-1; modi >= 0; modi--) {
			if (modifications.get(modi).getSourceStartIndex() < currRow) {
				right.getCaret().moveTo(modifications.get(modi).getSourceStartIndex(), 0);
				break;
			}
		}
	}

	public void jumpToNextSourceModification() {
		SCECaret caret = right.getCaret();
		int currRow = caret.getRow();

		for (Modification modification : modifications) {
			if (modification.getSourceStartIndex() > currRow) {
				right.getCaret().moveTo(modification.getSourceStartIndex(), 0);
				break;
			}
		}
	}

  public void updateLayout() {
    splitPane.setDividerLocation((scrollPane.getVisibleRect().width - WIDTH) / 2);
    splitPane.setDividerSize(WIDTH);

    Rectangle visibleRect = scrollPane.getVisibleRect();
    int visibleWidthLeft = splitPane.getDividerLocation();
    int visibleWidthRight = visibleRect.width - visibleWidthLeft - WIDTH;
    double overLeft = Math.max(1, left.getPreferredSize().width - visibleWidthLeft) / (double) visibleWidthLeft;
    double overRight = Math.max(1, right.getPreferredSize().width - visibleWidthRight) / (double) visibleWidthRight;

    preferredWidthFactor = 1 + Math.max(0, Math.max(overLeft, overRight));
    preferredSize.width = (int) (visibleRect.width * preferredWidthFactor);
    preferredSize.height = preferredLines * left.getLineHeight() + 30;
  }

  public void updateDiff() {
    SCEDocument paneDocument = left.getDocument();
    SCEDocument diffDocument = right.getDocument();

    int paneRows = paneDocument.getRowsCount();
    int diffRows = diffDocument.getRowsCount();

    modifications = SystemDiff.diff(diffDocument.getText(), paneDocument.getText());

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

    left.removeAllRowHighlights();
    right.removeAllRowHighlights();
    for(Modification modification : modifications) {
      Color color = null;
      if(modification.getType() == Modification.TYPE_ADD) color = COLOR_ADD;
      if(modification.getType() == Modification.TYPE_REMOVE) color = COLOR_REMOVE;
      if(modification.getType() == Modification.TYPE_CHANGED) color = COLOR_CHANGE;
      
      int sourceStart = modification.getSourceStartIndex();
      int sourceEnd = sourceStart + modification.getSourceLength();
      for(int line = sourceStart; line < sourceEnd; line++) {
        right.addRowHighlight(new SCERowHighlight(right, line, color, false));
      }
      if(sourceEnd == sourceStart) right.addRowHighlight(new SCERowHighlight(right, sourceStart, color, true));

      int targetStart = modification.getTargetStartIndex();
      int targetEnd = targetStart + modification.getTargetLength();
      for(int line = targetStart; line < targetEnd; line++) {
        left.addRowHighlight(new SCERowHighlight(left, line, color, false));
      }
      if(targetEnd == targetStart) left.addRowHighlight(new SCERowHighlight(left, targetStart, color, true));
    }
  }

  public SCEPane getTextPane() {
    return left;
  }

  public SCEPane getDiffPane() {
    return right;
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

  public void componentResized(ComponentEvent e) {
    updateLayout();
  }

  public void componentMoved(ComponentEvent e) {
  }

  public void componentShown(ComponentEvent e) {
    updateLayout();
  }

  public void componentHidden(ComponentEvent e) {
  }

  public void actionPerformed(ActionEvent e) {
    close();
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

      int paneOffsetY = -SCEDiff.this.left.getY();
      int paneFirstRow = SCEDiff.this.left.viewToModel(0, paneOffsetY).getRow();
      int diffOffsetY = -SCEDiff.this.right.getY();
      int diffFirstRow = SCEDiff.this.right.viewToModel(0, diffOffsetY).getRow();
      int visibleRows = SCEDiff.this.left.getVisibleRowsCount() + 1;

      int[] xpoints = new int[]{left, left + 15, right - 15, right, right, right - 15, left + 15, left};
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
        if(targetLength == 0) { paneYStart--; paneYEnd += 2; }
        if(sourceLength == 0) { diffYStart--; diffYEnd += 2; }
        ypoints[0] = paneYStart;
        ypoints[1] = paneYStart;
        ypoints[2] = diffYStart;
        ypoints[3] = diffYStart;
        ypoints[4] = diffYEnd;
        ypoints[5] = diffYEnd;
        ypoints[6] = paneYEnd;
        ypoints[7] = paneYEnd;

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
      int halfLines = visibleHeight/2/lineHeight;

      int lineOffset = halfLines;
      while(line2Pane[lineOffset] < halfLines && line2Diff[lineOffset] < halfLines) lineOffset++;
      int yCorrection = lineHeight * (int) Math.max(line2Pane[lineOffset], line2Diff[lineOffset]);

      y = y - lineOffset*lineHeight;
      int line = Math.max(0, -y/lineHeight);
      double lineFraction = (-y - line * lineHeight) / (double) lineHeight;

      double paneLine = (1 - lineFraction) * line2Pane[line] + lineFraction * line2Pane[line + 1];
      double diffLine = (1 - lineFraction) * line2Diff[line] + lineFraction * line2Diff[line + 1];
      int rx = (int) (x / preferredWidthFactor);
      leftViewport.setViewPosition(new Point(-rx, (int) (paneLine * lineHeight) - yCorrection));
      rightViewport.setViewPosition(new Point(-rx, (int) (diffLine * lineHeight) - yCorrection));

      left.setSize(leftViewport.getWidth() - rx, left.getHeight());
      right.setSize(rightViewport.getWidth() - rx, right.getHeight());

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

  private class SCEDiffViewport extends JViewport {
    private SCEDiff diffView;

    private SCEDiffViewport(Component view, SCEDiff diffView) {
      setView(view);
      this.diffView = diffView;
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

    public void scrollRectToVisible(Rectangle rectangle) {
      JScrollBar vbar = scrollPane.getVerticalScrollBar();
      JScrollBar hbar = scrollPane.getHorizontalScrollBar();

      JViewport currentViewport = diffView.getDiffPane().hasFocus() ? rightViewport : leftViewport;
      SCEPane currentPane = diffView.getDiffPane().hasFocus() ? diffView.getDiffPane() : diffView.getTextPane();

      double[] lineMap = diffView.getDiffPane().hasFocus() ? line2Diff : line2Pane;

      Rectangle visible = currentViewport.getVisibleRect(); //currentPane.getVisibleRect();
      if(visible.getY() > rectangle.getY() || visible.getY() + visible.getHeight() < rectangle.getY() + rectangle.getHeight()) {
        boolean up = rectangle.getY() < visible.getY();
        int lineHeight = currentPane.getLineHeight();
        int line = -(location.y - (int) visible.getHeight()/2) / lineHeight;

        int nline = line;
        if(up) {
          int distance = (int) (visible.getY() - rectangle.getY());
          while(line >= 0 && (lineMap[line] - lineMap[nline]) * lineHeight < distance) nline--;
        } else {
          int distance = (int) (rectangle.getY() + rectangle.getHeight() - visible.getY() - visible.getHeight());
          while(line < lineMap.length && (lineMap[nline] - lineMap[line]) * lineHeight < distance) nline++;
        }
        vbar.setValue(vbar.getValue() + (nline - line) * lineHeight);
      }

      if(visible.getX() > rectangle.getX() || visible.getX() + visible.getWidth() < rectangle.getX() + rectangle.getWidth()) {
        boolean left = rectangle.getX() < visible.getX();
        int distance;
        if(left) {
          distance = (int) (rectangle.getX() - visible.getX());
        } else {
          distance = (int) (rectangle.getX() + rectangle.getWidth() - visible.getX() - visible.getWidth());
        }
        hbar.setValue(vbar.getValue() + (int) (distance*preferredWidthFactor));
      }
    }
  }

  private class DiffHeader extends JPanel implements PropertyChangeListener {
    public void paint(Graphics g) {
      super.paint(g);

      g.drawString(leftTitle, 5,15);
      g.drawString(rightTitle, splitPane.getDividerLocation() + WIDTH + 5,15);
    }

    public Dimension getPreferredSize() {
      return new Dimension(1800, 19);
    }

    public void propertyChange(PropertyChangeEvent evt) {
      repaint();
    }
  }
}
