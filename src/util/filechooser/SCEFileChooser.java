package util.filechooser;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Timer;

/**
 * Custom FileChooser since the standard JFileChooser is total crap.
 * Features:
 * - navigation via keyboard
 * - list of recent directories
 * - ...
 */
public class SCEFileChooser extends JPanel implements ListSelectionListener, MouseListener, ActionListener, KeyListener {
  public static Color COLOR_EVEN = new Color(233,246,255);
  public static Color COLOR_SELECTION_TOP = new Color(99, 136, 248);
  public static Color COLOR_SELECTION_BOTTOM = new Color(13, 83, 236);

  public static Color HIGHLIGHT_OK = new Color(133,239,176);
  public static Color HIGHLIGHT_BAD = new Color(239,163,163);

  public static Color COLOR_DIRECTORY_LABEL_TOP = new Color(202,230,255);
  public static Color COLOR_DIRECTORY_LABEL_BOTTOM = new Color(118,193,255);
  public static Color COLOR_DIRECTORY_LABEL_BORDER = new Color(106,154,206);

  public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm");

  public static FileNameExtensionFilter FILTER_ALL = new FileNameExtensionFilter("All files (*.*)", "all");
  public static FileNameExtensionFilter FILTER_TEX =
          new FileNameExtensionFilter(
                  "LaTeX files (*.tex, *.def, *.bib)",
                  "tex", "def", "bib"
          );

  public static final int RESULT_OK = 1;
  public static final int RESULT_CANCEL = 2;

  public static final int TYPE_OPEN_DIALOG = 1;
  public static final int TYPE_SAVE_DIALOG = 2;

  public static ImageIcon ICON_DIRECTORY;
  public static ImageIcon ICON_TEXFILE;
  public static ImageIcon ICON_EMPTY;

  /**
   * Parent frame.
   */
  private Frame frame;

  /**
   * Dialog title.
   */
  private String title = "Super FileChooser";

  /**
   * OK/Cancel.
   */
  private int result = RESULT_CANCEL;

  /**
   * Current directory.
   */
  private File directory = File.listRoots()[0];

  /**
   * File list.
   */
  private DefaultTableModel fileListModel = new DefaultTableModel();
  private DefaultListSelectionModel fileListSelectionModel = new DefaultListSelectionModel();
  private JTable fileList = new JTable(fileListModel);
  private TableRowSorter<TableModel> sorter;

  /**
   * File name.
   */
  private JTextField fileName = new JTextField();
  private DefaultHighlighter highlighter = new DefaultHighlighter();
  private DefaultHighlighter.DefaultHighlightPainter highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(HIGHLIGHT_OK);

  /**
   * Filter for the file list.
   */
  private DefaultComboBoxModel fileFilterModel = new DefaultComboBoxModel();
  private JComboBox fileFilter = new JComboBox(fileFilterModel);

  /**
   * Buttons.
   */
  private JButton buttonOK = new JButton("OK");
  private JButton buttonCancel = new JButton("Cancel");

  /**
   * Actual dialog that is displayed.
   */
  private JDialog dialog = null;

  /**
   * Panel for viewing the path of all parents.
   */
  private JPanel parentPath = new JPanel();

  /**
   * Quick search.
   */
  private String filePrefix = "";
  private long keyTimestamp = 0;
  private long filePrefixReset = 5000;

  /**
   * Last selected file.
   */
  private HashMap<String,String> directory2fileName = new HashMap<String, String>();

  public SCEFileChooser(Frame frame) {
    this.frame = frame;

    fileListModel.setColumnCount(1);
    fileListModel.setColumnIdentifiers(new Object[]{"Name", "Date Modified"});
    fileList.setAutoCreateRowSorter(false);
    fileList.setShowGrid(false);
    fileList.setIntercellSpacing(new Dimension(0, 0));
    fileList.setRowHeight(fileList.getRowHeight() + 1);
    fileList.setCellSelectionEnabled(false);
    fileList.setRowSelectionAllowed(true);
    fileList.setDefaultRenderer(Object.class, new Renderer(fileList));
    fileList.setDefaultEditor(Object.class, null);
    fileList.setSelectionModel(fileListSelectionModel);
    fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    fileList.setSelectionBackground(COLOR_SELECTION_TOP);
    fileList.setSelectionForeground(Color.WHITE);
    sorter = new TableRowSorter<TableModel>();
    fileList.setRowSorter(sorter);
    sorter.setModel(fileListModel);
    sorter.setComparator(0, new FileEntryComparator());
    sorter.setSortsOnUpdates(true);

    ArrayList<RowSorter.SortKey> sortingKeys = new ArrayList<RowSorter.SortKey>();
    sortingKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
    sorter.setSortKeys(sortingKeys);

    fileName.setHighlighter(highlighter);

    ICON_DIRECTORY = new ImageIcon(getClass().getResource("/images/icons/directory.png"));
    ICON_TEXFILE = new ImageIcon(getClass().getResource("/images/icons/texfile.png"));
    ICON_EMPTY = new ImageIcon(getClass().getResource("/images/icons/empty.png"));

    addChoosableFileFilter(FILTER_ALL);

    /**
     * Layout.
     */
    setLayout(new GridBagLayout());

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1; gbc.weighty = 0;
    gbc.insets = new Insets(0,5,0,5);

    gbc.gridwidth = 2;
    gbc.gridx = 0; gbc.gridy = 0;
    add(parentPath, gbc);

    gbc.gridx = 0; gbc.gridy++;
    gbc.weighty = 1;
    add(new JScrollPane(fileList), gbc);
    gbc.weighty = 0;

    gbc.gridwidth=1;
    gbc.gridx = 0; gbc.gridy++;
    gbc.weightx = 0;
    add(new JLabel("File Name:"), gbc);
    gbc.weightx = 1;
    gbc.gridx++;
    add(fileName, gbc);

    gbc.gridx = 0; gbc.gridy++;
    gbc.weightx = 0;
    add(new JLabel("Files of Types:"), gbc);
    gbc.weightx = 1;
    gbc.gridx++;
    add(fileFilter, gbc);

    JPanel southPanel = new JPanel();
    southPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    southPanel.add(buttonOK);
    southPanel.add(buttonCancel);

    gbc.gridwidth=2;
    gbc.gridx = 0; gbc.gridy++;
    add(southPanel, gbc);

    FlowLayout parentPathLayout = new FlowLayout();
    parentPath.setLayout(parentPathLayout);
    parentPathLayout.setHgap(4);
    parentPathLayout.setAlignment(FlowLayout.LEFT);

    /**
     * Listeners.
     */
    fileList.getSelectionModel().addListSelectionListener(this);
    fileList.addMouseListener(this);
    fileList.setFocusable(true);
    fileList.addKeyListener(this);
    fileList.requestFocus();

    fileFilter.addActionListener(this);

    buttonOK.addActionListener(this);
    buttonCancel.addActionListener(this);
  }

  public void setDialogTitle(String title) {
    this.title = title;
  }

  public void setDialogType(int type) {

  }

  public void setCurrentDirectory(File directory) {
    if(directory == null || !directory.isDirectory()) return;

    // remember last selected file
    directory2fileName.put(this.directory.getAbsolutePath(), fileName.getText());
    // which file to select in the new directory?
    String select = directory2fileName.get(directory.getAbsolutePath());
    if(select == null) select = "..";
    int selectIndex = 0;

    this.directory = directory;
    filePrefix = "";

    // remove all entries
    while(fileListModel.getRowCount() > 0) {
      fileListModel.removeRow(fileListModel.getRowCount()-1);
    }

    File parent = directory.getParentFile();
    Object[] row;
    if(parent != null) {
      row = new Object[] {
              new FileEntry("..", parent),
              new NamedEntry("", new Date(0)) };
      fileListModel.addRow(row);
    }

    FileFilter filter = (FileFilter) ((NamedEntry) fileFilterModel.getSelectedItem()).getObject();
    for(File file : directory.listFiles()) {
      if(filter != FILTER_ALL && !filter.accept(file)) continue;
      if(filter != FILTER_ALL && file.getName().startsWith(".")) continue;

      Date modified = new Date(file.lastModified());
      row = new Object[] {
              new FileEntry(file),
              new NamedEntry(DATE_FORMAT.format(modified), modified)};
      fileListModel.addRow(row);

      if(file.getName().equals(select)) selectIndex = fileListModel.getRowCount()-1;
    }

    if(fileList.getRowCount() > 0) {
      selectIndex = sorter.convertRowIndexToView(selectIndex);
      fileListSelectionModel.setSelectionInterval(selectIndex,selectIndex);
    }

    parentPath.removeAll();
    while(true) {
      parentPath.add(new DirectoryLabel(directory),0);

      File next = directory.getParentFile();
      if(next == null) break;

      directory2fileName.put(next.getAbsolutePath(), directory.getName());

      directory = next;
      if(directory != null) parentPath.add(new DirectorySpacer(),0);
    }
    parentPath.revalidate();
    parentPath.repaint();

    fileList.requestFocus();
  }

  public void addChoosableFileFilter(FileFilter filter) {
    fileFilterModel.addElement(new NamedEntry(filter.getDescription(), filter));

    // select the first non-all entry
    if(fileFilterModel.getSize() == 2) fileFilter.setSelectedIndex(1);

    // update listing
    setCurrentDirectory(directory);
  }

  public File getSelectedFile() {
    if(fileName.getText().equals("..")) {
      return directory.getParentFile();
    } else {
      return new File(directory, fileName.getText());
    }
  }

  private void openSelectedFile() {
    File file = getSelectedFile();

    if(file.isDirectory()) {
      setCurrentDirectory(file);
    } else {
      result = RESULT_OK;
      dialog.hide();
    }
  }

  public synchronized int showDialog() {
    result = RESULT_CANCEL;

    if(dialog != null) dialog.dispose();

    dialog = new JDialog(frame);
    dialog.setTitle(title);
    dialog.getContentPane().setLayout(new BorderLayout());
    dialog.getContentPane().add(this);
    dialog.setModal(true);
    dialog.pack();

    Timer timer = new Timer();
    timer.scheduleAtFixedRate(new PrefixTimer(), 500, 500);

    dialog.show();
    dialog.removeKeyListener(this);

    timer.cancel();

    return result;
  }

  public void valueChanged(ListSelectionEvent listSelectionEvent) {
    if(listSelectionEvent.getValueIsAdjusting()) return;

    int indexView = fileListSelectionModel.getAnchorSelectionIndex();
    if(indexView < 0 || indexView >= fileList.getRowCount()) return;

    int indexModel = sorter.convertRowIndexToModel(indexView);
    if(indexModel < 0 || indexModel >= fileList.getRowCount()) return;

    String name = fileListModel.getValueAt(indexModel,0).toString();
    fileName.setText(name);

    if(filePrefix.length() > 0 && name.toLowerCase().startsWith(filePrefix.toLowerCase())) {
      try {
        highlighter.removeAllHighlights();
        highlighter.addHighlight(0, filePrefix.length(), highlightPainter);
      } catch (BadLocationException e) {
      }
    } else {
      if(filePrefix.length() > 0) repaint();
      filePrefix = "";
    }
  }

  public void mouseClicked(MouseEvent mouseEvent) {
    // double click
    if(mouseEvent.getClickCount() >= 2) {
      openSelectedFile();
    }
  }

  public void mousePressed(MouseEvent mouseEvent) {
  }

  public void mouseReleased(MouseEvent mouseEvent) {
  }

  public void mouseEntered(MouseEvent mouseEvent) {
  }

  public void mouseExited(MouseEvent mouseEvent) {
  }

  public void actionPerformed(ActionEvent actionEvent) {
    if(actionEvent.getSource() == buttonOK) {
      openSelectedFile();
    }
    if(actionEvent.getSource() == buttonCancel) {
      dialog.hide();
    }
    if(actionEvent.getSource() == fileFilter) {
      setCurrentDirectory(directory);
    }
  }

  public void keyTyped(KeyEvent keyEvent) {
  }

  public void keyPressed(KeyEvent keyEvent) {
    if(keyEvent.getKeyCode() == KeyEvent.VK_UP || keyEvent.getKeyCode() == KeyEvent.VK_DOWN) {
      int direction = keyEvent.getKeyCode() == KeyEvent.VK_UP ? -1 : 1;

      int indexView = searchFilePrefixView(fileList.getSelectedRow() + direction, direction);
      if(indexView != -1) {
        fileListSelectionModel.setSelectionInterval(indexView, indexView);
      }

      keyEvent.consume();
    }

    if(keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
      openSelectedFile();
      keyEvent.consume();
    }

    if(keyEvent.getKeyCode() == KeyEvent.VK_LEFT
            || (keyEvent.getKeyCode() == KeyEvent.VK_BACK_SPACE && filePrefix.length() == 0) ) {
      File parent = directory.getParentFile();
      if(parent != null) setCurrentDirectory(parent);
      keyEvent.consume();
    }

    if(keyEvent.getKeyCode() == KeyEvent.VK_RIGHT) {
      File selected = getSelectedFile();
      if(selected.isDirectory()) setCurrentDirectory(selected);
      keyEvent.consume();
    }

    char c = keyEvent.getKeyChar();
    long time = System.currentTimeMillis();

    boolean backspace =
            keyEvent.getKeyChar() == KeyEvent.VK_BACK_SPACE
                    || keyEvent.getKeyChar() == KeyEvent.VK_DELETE;

    boolean stop =
            keyEvent.getKeyChar() == KeyEvent.VK_ESCAPE;

    if(Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-'
            || backspace || stop) {
      if(!backspace && !stop) {
        filePrefix += c;
      }

      if(backspace && filePrefix.length() > 0) {
        filePrefix = filePrefix.substring(0,filePrefix.length()-1);
      }

      if(stop && filePrefix.length() > 0) {
        filePrefix = "";
      }

      // select first file with this prefix
      int indexView = searchFilePrefixView(fileList.getSelectedRow(), 1);
      if(indexView == -1) indexView = searchFilePrefixView(fileList.getSelectedRow()-1, -1);
      int indexModel = -1;
      if(indexView != -1) indexModel = sorter.convertRowIndexToModel(indexView);

      if(indexModel != -1) {
        FileEntry entry = (FileEntry) fileListModel.getValueAt(indexModel,0);
        fileListSelectionModel.setSelectionInterval(indexView, indexView);

        fileName.setText(entry.getName());
        fileName.setBackground(Color.WHITE);
      } else {
        fileName.setText(filePrefix);
        fileName.setBackground(HIGHLIGHT_BAD);
      }

      try {
        highlighter.removeAllHighlights();
        highlighter.addHighlight(0, filePrefix.length(), highlightPainter);
      } catch (BadLocationException e) {
      }

      fileList.repaint();
    }

    keyTimestamp = time;

    fileList.scrollRectToVisible(fileList.getCellRect(fileList.getSelectedRow(), 0, true));
  }

  private int searchFilePrefixView(int startView, int increaseView) {
    for(int rowView = startView; rowView >= 0 && rowView < fileList.getRowCount(); rowView += increaseView) {
      int rowModel = sorter.convertRowIndexToModel(rowView);

      FileEntry entry = (FileEntry) fileListModel.getValueAt(rowModel, 0);
      if(entry.getName().toLowerCase().startsWith(filePrefix.toLowerCase())) {
        return rowView;
      }
    }

    return -1;
  }

  public void keyReleased(KeyEvent keyEvent) {
  }

  private class NamedEntry {
    private String name;
    private Object object;

    private NamedEntry(String name, Object object) {
      this.name = name;
      this.object = object;
    }

    public String getName() {
      return name;
    }

    public Object getObject() {
      return object;
    }

    public String toString() {
      return name;
    }
  }

  private class FileEntry extends NamedEntry {
    private boolean isDirectory;
    private boolean isTexFile;

    private FileEntry(File file) {
      this(file.getName(), file);
    }

    private FileEntry(String name, File file) {
      super(name, file);

      isDirectory = file.isDirectory();
      isTexFile = FILTER_TEX.accept(file);
    }

    public boolean isDirectory() {
      return isDirectory;
    }

    public boolean isTexFile() {
      return isTexFile;
    }
  }

  private class FileEntryComparator implements Comparator<FileEntry> {
    public int compare(FileEntry entry1, FileEntry entry2) {
      if(entry1.isDirectory && !entry2.isDirectory()) return -1;
      if(!entry1.isDirectory && entry2.isDirectory()) return 1;

      return entry1.getName().compareTo(entry2.getName());
    }
  }

  private class Renderer extends DefaultTableCellRenderer {
    private Font font;
    private boolean selected = false;

    public Renderer(JTable table) {
      font = table.getFont().deriveFont(table.getFont().getSize() + 1.f);
      table.setFont(font);
    }

    public Component getTableCellRendererComponent(JTable table, Object item, boolean selected, boolean focussed, int row, int column) {
      super.getTableCellRendererComponent(table, item, selected, false, row, column);
      this.selected = selected;

      FileEntry fileEntry = (FileEntry) fileListModel.getValueAt(sorter.convertRowIndexToModel(row), 0);

      if(!selected) {
        setBackground(row % 2 == 0 ? COLOR_EVEN : Color.WHITE);
        setForeground(fileEntry.getName().toLowerCase().startsWith(filePrefix.toLowerCase()) ? Color.BLACK : Color.GRAY);
      }
      setFont(font);

      if(item instanceof FileEntry) {
        fileEntry = (FileEntry) item;
        setIcon(fileEntry.isDirectory ? ICON_DIRECTORY : (fileEntry.isTexFile() ? ICON_TEXFILE : ICON_EMPTY));
      } else {
        setIcon(null);
      }

      return this;
    }

    public void paintComponent(Graphics graphics) {
      if(selected) {
        Graphics2D g = (Graphics2D) graphics;

        GradientPaint gp = new GradientPaint(0, 0, COLOR_SELECTION_TOP, 0, getHeight()-1, COLOR_SELECTION_BOTTOM);
        g.setPaint(gp);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setPaint(Color.BLACK);

        setOpaque(false);
        super.paintComponent(graphics);
        setOpaque(true);
      } else {
        super.paintComponent(graphics);
      }
    }
  }

  private class PrefixTimer extends TimerTask {
    public void run() {
      long time = System.currentTimeMillis();
      if(time - keyTimestamp > filePrefixReset) {
        filePrefix = "";

        highlighter.removeAllHighlights();
        fileName.setBackground(Color.WHITE);

        fileList.repaint();
      }
    }
  }

  private class DirectorySpacer extends JLabel {
    private int[] px = new int[] {0,4,0};
    private int[] py = new int[] {0,4,8};

    public Dimension getPreferredSize() {
      return new Dimension(5,8);
    }

    public void paint(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;

      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      g.setColor(Color.GRAY);
      g.fillPolygon(px,py,3);

      super.paint(graphics);
    }
  }

  private class DirectoryLabel extends JLabel implements MouseListener {
    private File directory;

    private DirectoryLabel(File directory) {
      super(directory.getName());
      if(getText().equals("")) setText("/");

      this.directory = directory;

      setHorizontalAlignment(JLabel.CENTER);
      setHorizontalTextPosition(JLabel.CENTER);
      setVerticalTextPosition(JLabel.TOP);

      addMouseListener(this);
    }

    public File getDirectory() {
      return directory;
    }

    public Dimension getPreferredSize() {
      Dimension size = super.getPreferredSize();
      size.width += 10;
      size.height += 4;
      return size;
    }

    public void paint(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;

      GradientPaint gp = new GradientPaint(0, 1, COLOR_DIRECTORY_LABEL_TOP, 0, getHeight()-3, COLOR_DIRECTORY_LABEL_BOTTOM);

      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      g.setColor(COLOR_DIRECTORY_LABEL_BORDER);
      g.setStroke(new BasicStroke(2f));
      g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 11, 11);

      g.setPaint(gp);
      g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 11, 11);

      super.paint(graphics);
    }

    public void mouseClicked(MouseEvent mouseEvent) {
      setCurrentDirectory(directory);
    }

    public void mousePressed(MouseEvent mouseEvent) {
    }

    public void mouseReleased(MouseEvent mouseEvent) {
    }

    public void mouseEntered(MouseEvent mouseEvent) {
    }

    public void mouseExited(MouseEvent mouseEvent) {
    }
  }
}
