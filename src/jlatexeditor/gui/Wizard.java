package jlatexeditor.gui;

import de.endrullis.utils.BetterProperties2;
import jlatexeditor.SCEManager;
import jlatexeditor.gproperties.GProperties;
import util.ProcessUtil;
import util.StreamUtils;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;

public class Wizard extends JDialog implements WindowListener {
  private String message =
          "<font size=+2><bf>Welcome to JLatexEditor!</bf></font><br><br> " +
          "This wizard allows for a quick setup of the user interface " +
          "and external tools.";

  private HashMap<String,ArrayList<File>> toolLocations = new HashMap<String, ArrayList<File>>();
  {{
    toolLocations.put("aspell" , new ArrayList<File>());
    toolLocations.put("pdflatex" , new ArrayList<File>());
    toolLocations.put("latex" , new ArrayList<File>());
    toolLocations.put("bibtex" , new ArrayList<File>());
  }}

  private JTextField aspell = new JTextField();
  private JTextField pdfLatex = new JTextField();
  private JTextField latex = new JTextField();
  private JTextField bibtex = new JTextField();

  private String[][] importantKeyStrokes = new String[][] {
          new String[] {"open", "Open File"},
          new String[] {"save", "Save File"},
          new String[] {"close", "Close Current Tab"},
          new String[] {"undo", "Undo"},
          new String[] {"find", "Find"},
          new String[] {"copy", "Copy"},
          new String[] {"paste", "Past"},
          new String[] {"cut", "Cut"},
          new String[] {"jump left", "Jump Left"},
          new String[] {"jump right", "Jump Right"},
          new String[] {"pdf", "Compile: pdflatex + bibtex"},
  };

  private DefaultComboBoxModel keyStrokesListModel = new DefaultComboBoxModel();
  private JComboBox keyStrokesList = new JComboBox(keyStrokesListModel);
  private JButton showKeyStrokes = new JButton("Show");

  private SearchToolsThread searchThread = new SearchToolsThread();

  public Wizard(JFrame owner) {
    super(owner, "Quick Setup Wizard");

    Container cp = getContentPane();
    GridBagLayout layout = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    cp.setLayout(layout);

    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new Insets(5,15,5,15);
    gbc.ipadx = 5; gbc.ipady = 5;

    gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.gridheight = 1;
    gbc.weightx = .2; gbc.weighty = 1;
    JLabel welcome = new JLabel("<html>" + message + "</html>");
    welcome.setOpaque(false);
    cp.add(welcome, gbc);

    gbc.weightx = 1; gbc.weighty = .2;

    JPanel keystrokesPanel = new JPanel(new BorderLayout());
    keystrokesPanel.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
            "Select KeyStroke Settings"));

    if(GProperties.hasChanges()) {
      keyStrokesListModel.addElement(new KeyStrokeSettings("Keep My Current Settings", (BetterProperties2) GProperties.getProperties().clone()));
    }

    for(int nr = 1; ; nr++) {
      try {
        String fileName = "data/configs/global.properties." + nr;

        // read properties
        InputStream propertiesIn = StreamUtils.getInputStream(fileName);
        BetterProperties2 properties = new BetterProperties2(GProperties.getProperties());
        properties.load(propertiesIn);
        propertiesIn.close();

        // read name
        InputStream nameIn = StreamUtils.getInputStream(fileName);
        BufferedReader nameReader = new BufferedReader(new InputStreamReader(nameIn));
        String name = nameReader.readLine().substring(1).trim();
        nameReader.close();
        nameIn.close();

        keyStrokesListModel.addElement(new KeyStrokeSettings(name, properties));
      } catch (FileNotFoundException e) {
        break;
      } catch (IOException e) {
      }
    }
    keystrokesPanel.add(keyStrokesList, BorderLayout.CENTER);

    gbc.gridy++;
    cp.add(keystrokesPanel, gbc);

    gbc.gridy++;
    cp.add(createProgramPanel("Aspell", "aspell.executable", aspell,
            "The program 'aspell' is required for the live spell checker."), gbc);
    gbc.gridy++;
    cp.add(createProgramPanel("PdfLatex", "compiler.pdflatex.executable", pdfLatex,
            "The program 'pdflatex' is required for generating PDF files."), gbc);
    gbc.gridy++;
    cp.add(createProgramPanel("Latex", "compiler.latex.executable", latex,
            "The program 'latex' is required for generating DVI files."), gbc);
    gbc.gridy++;
    cp.add(createProgramPanel("BibTex", "compiler.bibtex.executable", bibtex,
            "The program 'bibtex' is required for managing references."), gbc);

    setModal(true);
    pack();

    // guessing suitable properties file
    if(!GProperties.hasChanges()) {
      String osName= System.getProperty("os.name");
      if(osName != null && osName.equals("Mac OS X")) {
        keyStrokesList.setSelectedIndex(1);
      }
    }

    // search for tools
    searchThread.start();
  }

  private JPanel createProgramPanel(String title, String executable, JTextField textField, String message) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), title));

    textField.setText(GProperties.getString(executable));
    panel.add(textField, BorderLayout.CENTER);

    JButton button = new JButton("...");
    panel.add(button, BorderLayout.EAST);

    textField.setText(GProperties.getString(executable));
    panel.add(new JLabel(message), BorderLayout.NORTH);

    JLabel resultLabel = new JLabel();
    panel.add(resultLabel, BorderLayout.SOUTH);

    final ExecutableChecker checker = new ExecutableChecker(textField, resultLabel);
    checker.run();

    textField.getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(DocumentEvent e) {
        check();
      }

      public void removeUpdate(DocumentEvent e) {
        check();
      }

      public void changedUpdate(DocumentEvent e) {
        check();
      }

      private void check() {
        SwingUtilities.invokeLater(checker);
      }
    });

    return panel;
  }

  /**
   * WindowListener.
   */
  public void windowOpened(WindowEvent e) {
  }

  public void windowClosing(WindowEvent e) {
  }

  public void windowClosed(WindowEvent e) {
    searchThread.stop();
  }

  public void windowIconified(WindowEvent e) {
  }

  public void windowDeiconified(WindowEvent e) {
  }

  public void windowActivated(WindowEvent e) {
  }

  public void windowDeactivated(WindowEvent e) {
  }

  private class ExecutableChecker implements Runnable {
    private JTextField textField;
    private JLabel resultLabel;

    private ExecutableChecker(JTextField textField, JLabel resultLabel) {
      this.textField = textField;
      this.resultLabel = resultLabel;
    }

    public void run() {
      String path = textField.getText();

      boolean success = true;
      try {
        File dir = SCEManager.getInstance().getMainEditor().getFile().getParentFile();
        Process process = ProcessUtil.exec(path, dir);
        process.destroy();
      } catch (Throwable e) {
        success = false;
      }

      if(success) {
        resultLabel.setText("<html><font color=green>File found.</font></html>");
      } else {
        resultLabel.setText("<html><font color=red><bf>File not found.</bf></font></html>");
      }
    }
  }

  private static class KeyStrokeSettings {
    private String name;
    private BetterProperties2 settings;

    private KeyStrokeSettings(String name, BetterProperties2 settings) {
      this.name = name;
      this.settings = settings;
    }

    public String getName() {
      return name;
    }

    public BetterProperties2 getSettings() {
      return settings;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  private class SearchToolsThread extends Thread {
    ArrayList<String> toolNames = new ArrayList<String>(toolLocations.keySet());

    @Override
    public void run() {
      setPriority(Thread.MIN_PRIORITY);
      searchTools(new File("/"), 0);
      searchTools(new File("C:\\"), 0);
      searchTools(new File("D:\\"), 0);
    }

    private void searchTools(File dir, int depth) {
      if(depth > 5 || !dir.isDirectory()) return;

      File files[] = dir.listFiles();
      if(files == null) return;

      for(File file : files) {
        searchTools(file, depth+1);

        for(String name : toolNames) {
          File executable = new File(dir, name);
          if(executable.exists() && executable.isFile() && executable.canExecute()) {
            toolLocations.get(name).add(executable);
          }
        }
      }
    }
  }
}
