package util.filechooser;

import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsICharsetDetectionObserver;
import org.mozilla.intl.chardet.nsPSMDetector;
import util.GraphicsUtil;
import util.StreamUtils;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

public class CharsetComboBox extends JComboBox {
  private Collection<Entry> availableCharsets = new ArrayList<Entry>();
  private DefaultComboBoxModel model;

  private boolean noDetection = false;
  private Entry UTF8;
  private Entry selectCharset;
  private Collection<Entry> detectedCharsets = new ArrayList<Entry>();

  private Properties charsetDescriptions = new Properties() {{
    try {
      load(StreamUtils.getInputStream("data/charsetDescriptions.properties"));
    } catch (IOException ignored) {}
  }};

  public CharsetComboBox() {
    super(new DefaultComboBoxModel());
    model = (DefaultComboBoxModel) getModel();
    setRenderer(new Renderer());

    // add lowercase keys as well
    for(Object key : new ArrayList<Object>(charsetDescriptions.keySet())) {
      charsetDescriptions.put(key.toString().toLowerCase(), charsetDescriptions.get(key));
    }

    // get available charsets
    for(Charset charset : Charset.availableCharsets().values()) {
      if(!charset.canEncode()) continue;

      String name = charset.name();
      String alias = null;

      String description = charsetDescriptions.getProperty(name.toLowerCase());
      if(description == null) {
        for(String charsetAlias : charset.aliases()) {
          description = charsetDescriptions.getProperty(charsetAlias.toLowerCase());
          if(description != null) {
            alias = charsetAlias;
            break;
          }
        }
      }

      availableCharsets.add(new Entry(charset, alias, description));
    }
    detectedCharsets.addAll(availableCharsets);

    UTF8 = getCharsetEntryForName("UTF-8");
    selectCharset = UTF8;

    updateModel();
  }

  DetectorThread detectorThread = null;
  public void detect(File file) {
    if(detectorThread != null) detectorThread.terminate();
    if(noDetection || !file.isFile()) return;

    // we prefer UTF-8 by default
    selectCharset = UTF8;

    detectorThread = new DetectorThread(file);
    detectorThread.start();
  }

  private void updateModel() {
    Collection<Entry> entries = (noDetection || detectedCharsets == null) ? availableCharsets : detectedCharsets;
    model.removeAllElements();

    for(Entry entry : entries) model.addElement(entry);
    if(selectCharset != null) setSelectedItem(selectCharset);
  }

  public void setDetectedCharsets(String names[]) {
    ArrayList<Entry> detected = new ArrayList<Entry>();
    boolean hasUTF8 = false;
    for(String name : names) {
      Entry charsetEntry = getCharsetEntryForName(name);
      hasUTF8 = hasUTF8 || charsetEntry == UTF8;
      if(charsetEntry != null) detected.add(charsetEntry);
    }
    if(detected.size() == 0) return;

    detectedCharsets = detected;

    if(hasUTF8) {
      selectCharset = UTF8;
    } else {
      selectCharset = detected.get(0);
    }
    updateModel();
  }

  public void setNoDetection(boolean noDetection) {
    this.noDetection = noDetection;
    updateModel();
  }

  public Entry getCharsetEntryForName(String name) {
    name = name.toLowerCase();

    for(Entry entry : availableCharsets) {
      Charset charset = entry.charset;

      if(charset.name().toLowerCase().equals(name)) return entry;

      for(String charsetAlias : charset.aliases()) {
        if(charsetAlias.toLowerCase().equals(name)) return entry;
      }
    }

    return null;
  }

  /**
   * Thread for detecting the character encoding of the file.
   */
  private class DetectorThread extends Thread implements nsICharsetDetectionObserver {
    private File file;
    private boolean terminated = false;

    private DetectorThread(File file) {
      this.file = file;
      setPriority(Thread.MIN_PRIORITY);
    }

    public void run() {
      nsDetector detector = new nsDetector(nsPSMDetector.ALL) ;

      detector.Init(this);

      BufferedInputStream in = null;
      try {
        in = new BufferedInputStream(new FileInputStream(file));

        byte[] buffer = new byte[1024] ;
        int length;
        boolean done = false ;
        boolean isAscii = true ;

        while(!terminated && (length = in.read(buffer,0,buffer.length)) != -1) {
          isAscii = isAscii && detector.isAscii(buffer,length);

          if(!isAscii && !done) {
            done = detector.DoIt(buffer, length, false);
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
        terminate();
      }

      detector.DataEnd();

      synchronized (this) {
        if(!terminated) setDetectedCharsets(detector.getProbableCharsets());
      }
    }

    public synchronized void terminate() {
      terminated = true;
    }

    public void Notify(String name) {
    }
  }

  public static class Entry {
    private Charset charset;
    private String alias;
    private String description;

    public Entry(Charset charset, String alias, String description) {
      this.charset = charset;
      this.alias = alias;
      this.description = description;
    }

    public Charset getCharset() {
      return charset;
    }

    public String getAlias() {
      return alias;
    }

    public String getDescription() {
      return description;
    }

    public String toString() {
      return charset.displayName() + alias + description;
    }
  }

  private static class Renderer extends DefaultListCellRenderer {
    private Entry entry = null;
    private boolean selected = false;

    public Component getListCellRendererComponent(JList list, Object element, int index, boolean selected, boolean focussed) {
      super.getListCellRendererComponent(list, element, index, selected, focussed);

      this.entry = (Entry) element;
      this.selected = selected;

      return this;
    }

    public void update(Graphics graphics) {
    }

    public void paint(Graphics graphics) {
      if(entry == null) return;
      Graphics2D g = (Graphics2D) graphics;

      // paint background
      g.setColor(getBackground());
      if(selected) {
        GraphicsUtil.paintSelectionBackground(this, (Graphics2D) graphics);
      } else {
        g.fillRect(0,0,getWidth(),getHeight());
      }

      // paint foreground
      Color foreground = selected ? Color.WHITE : getForeground();

      g.setColor(foreground);

      int x = 5;
      int y = getHeight()-5;
      g.setFont(getFont().deriveFont(Font.BOLD));
      g.drawString(entry.getCharset().displayName(), x, y);
      x += 5 + (int) g.getFont().getStringBounds(entry.getCharset().displayName(), g.getFontRenderContext()).getWidth();

      g.setFont(getFont());

      if(x < 120) x = 120;
      if(entry.getDescription() != null) {
        g.setColor(foreground);
        g.drawString(entry.getDescription(), x, y);
        x += 5 + (int) g.getFont().getStringBounds(entry.getDescription(), g.getFontRenderContext()).getWidth();
      }

      if(entry.getAlias() != null) {
        String alias = "(" + entry.getAlias() + ")";
        g.setColor(Color.GRAY);
        g.drawString(alias, x, y);
      }
    }
  }
}
