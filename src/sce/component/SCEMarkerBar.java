package sce.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

/**
 * Markers right of the scroll pane.
 */
public class SCEMarkerBar extends JPanel implements SCEDocumentListener, MouseMotionListener, MouseListener {
  public static final int TYPES_COUNT  = 7;

  public static final int TYPE_ERROR   = 0;
  public static final int TYPE_WARNING = 1;
  public static final int TYPE_HBOX    = 2;
  public static final int TYPE_SEARCH  = 3;
  public static final int TYPE_SVN_ADD    = 4;
  public static final int TYPE_SVN_REMOVE = 5;
  public static final int TYPE_SVN_CHAGE  = 6;

  public static Color COLORS[] = new Color[] {
          Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN,
          SCEDiff.COLOR_ADD, SCEDiff.COLOR_REMOVE, SCEDiff.COLOR_CHANGE };
  private ArrayList<ArrayList<Marker>> markers;

  // rows count
  private int rowsCount = -1;
  private SourceCodeEditor editor = null;

  // layout
  private int width = 11;
  private double heightOffset = 18;
  private double heightFactor = 1;

  private JPanel toolTipComponent = new JPanel();

  private ImageButton buttonClose;


  public SCEMarkerBar(SourceCodeEditor editor) {
    this.editor = editor;
    SCEDocument document = editor.getTextPane().getDocument();
    rowsCount = editor.getVirtualLines();

    markers = new ArrayList<ArrayList<Marker>>();
    for(int type = 0; type < TYPES_COUNT; type++) markers.add(new ArrayList<Marker>());

    document.addSCEDocumentListener(this);

    setPreferredSize(new Dimension(15,600));
    addMouseMotionListener(this);
    addMouseListener(this);

    createInputMap();

    setLayout(null);
    buttonClose = new ImageButton(
            new ImageIcon(getClass().getResource("/images/buttons/close_mini.png")),
            new ImageIcon(getClass().getResource("/images/buttons/close_mini_highlight.png")),
            new ImageIcon(getClass().getResource("/images/buttons/close_mini_press.png")));
    add(buttonClose);
    buttonClose.setSize(buttonClose.getPreferredSize());
    buttonClose.setLocation(0,0);
    buttonClose.setVisible(false);
  }

  private void createInputMap() {
    add(toolTipComponent);
    toolTipComponent.setOpaque(false);
    toolTipComponent.setSize(1,1);

    InputMap inputMap = toolTipComponent.getInputMap();
    if(inputMap.keys() == null || inputMap.keys().length == 0) {
      inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, 0), "backSlash");
    }
  }

  public synchronized void clear() {
    for(ArrayList<Marker> list : markers) list.clear();
    repaint();
  }

  public synchronized void clear(int type) {
    markers.get(type).clear();
    repaint();
  }

  public ImageButton getButtonClose() {
    return buttonClose;
  }

  public synchronized void addMarker(Marker marker) {
    markers.get(marker.type).add(marker);
  }

  private void updateLayout() {
    rowsCount = editor.getVirtualLines();
    width = getWidth() - 2;
    heightOffset = 18;
    heightFactor = (getHeight() - 2*heightOffset) / Math.max(1, rowsCount);
  }

  private int getPosition(int row) {
    return (int) (heightOffset + heightFactor * row);
  }

  public synchronized void paint(Graphics g) {
    super.paint(g);

    updateLayout();
    for(int type = 0; type < TYPES_COUNT; type++) {
      Color color = COLORS[type];
      Color colorDark = color.darker();
      for(Marker marker : markers.get(type)) {
        int yStart = getPosition(marker.getRowStart());
        int yEnd = Math.max(getPosition(marker.getRowEnd()), yStart + 2);
        g.setColor(color);
        g.fillRect(1, yStart, width, yEnd - yStart);
        g.setColor(colorDark);
        g.drawLine(1, yEnd, width+1, yEnd);
        g.drawLine(width+1, yEnd, width+1, yStart);
      }
    }
  }

  public void documentChanged(SCEDocument sender, SCEDocumentEvent event) {
    if(editor.getVirtualLines() != rowsCount) repaint();
  }

  public void mouseDragged(MouseEvent e) {
  }

  boolean toolTip = false;
  public void mouseMoved(MouseEvent e) {
    int mx = e.getX();
    int my = e.getY();

    for(int type = 0; type < TYPES_COUNT; type++) {
      for(Marker marker : markers.get(type)) {
        int y = getPosition(marker.getRowStart());
        if(mx >= 1 && mx < getWidth()-1 && my >= y && my <= y+2) {
          setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

          Object message = marker.getMessage();
          if(message != null) {
            if(!toolTip) {
              toolTipComponent.setLocation(mx+10,my+2);
              toolTipComponent.setToolTipText(message.toString());
              Action action = toolTipComponent.getActionMap().get("postTip");
              action.actionPerformed(new ActionEvent(toolTipComponent, ActionEvent.ACTION_PERFORMED, "postTip"));
            }
            toolTip = true;
          }

          return;
        }
      }
    }
    
    toolTip = false;
    toolTipComponent.setToolTipText(null);

    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }

  public void mouseClicked(MouseEvent e) {
    int mx = e.getX();
    int my = e.getY();

    for(int type = 0; type < TYPES_COUNT; type++) {
      for(Marker marker : markers.get(type)) {
        int y = getPosition(marker.getRowStart());
        if(mx >= 1 && mx < getWidth()-1 && my >= y && my <= y+2) {
          Runnable action = marker.getAction();
          if(action == null) {
            editor.moveTo(marker.getRowStart(), 0);
          } else {
            action.run();
          }
          return;
        }
      }
    }
  }

  public void mousePressed(MouseEvent e) {
  }

  public void mouseReleased(MouseEvent e) {
  }

  public void mouseEntered(MouseEvent e) {
  }

  public void mouseExited(MouseEvent e) {
  }

  public static class Marker {
    private int type = TYPE_ERROR;
    private int rowStart = 0;
    private int rowEnd = 0;
    private int column = 0;
    private Object message = null;
    private Runnable action = null;

    public Marker(int type, int rowStart, int rowEnd, Runnable action) {
      this.type = type;
      this.rowStart = rowStart;
      this.rowEnd = rowEnd;
      this.action = action;
    }

    public Marker(int type, int row, Object message) {
      this.type = type;
      this.rowStart = row;
      this.rowEnd = row+1;
      this.message = message;
    }

    public Marker(int type, int row, int column, Object message) {
      this(type, row, message);
      this.column = column;
    }

    public int getType() {
      return type;
    }

    public int getRowStart() {
      return rowStart;
    }

    public int getRowEnd() {
      return rowEnd;
    }

    public int getColumn() {
      return column;
    }

    public Object getMessage() {
      return message;
    }

    public Runnable getAction() {
      return action;
    }
  }
}