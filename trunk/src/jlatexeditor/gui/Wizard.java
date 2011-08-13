package jlatexeditor.gui;

import de.endrullis.utils.BetterProperties2;
import jlatexeditor.gproperties.GProperties;
import util.StreamUtils;
import util.SystemUtils;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Wizard extends JDialog implements WindowListener {
  public static final ProgramWithParameters[] VIEWERS = new ProgramWithParameters[] {
          new ProgramWithParameters("kdvi", "kdvi", "--unique \"file:%file.dvi#src:%line&nbsp;%texfile\""),
          new ProgramWithParameters("okular", "okular", "--unique \"file:%file.pdf#src:%line&nbsp;%texfile\""),
          new ProgramWithParameters("Skim", "/Applications/Skim.app/Contents/SharedSupport/displayline", "%line \"%file.pdf\" \"%texfile\""),
          new ProgramWithParameters("xdvi", "xdvi", "-sourceposition \"%line&nbsp;%file.dvi\" -nofork"),
          new ProgramWithParameters("SumatraPDF", "SumatraPDF.exe", "TODO"),
  };


  private String message =
          "<font size=+2><bf>Welcome to JLatexEditor!</bf></font><br><br> " +
          "This wizard allows for a quick setup of the user interface " +
          "and external tools.";

  private HashMap<String,ArrayList<File>> toolLocations = new HashMap<String, ArrayList<File>>();
  {{
    toolLocations.put("aspell" , new ArrayList<File>());
    toolLocations.put("pdflatex" , new ArrayList<File>());
    toolLocations.put("latex" , new ArrayList<File>());
    toolLocations.put("dvips" , new ArrayList<File>());
    toolLocations.put("ps2pdf" , new ArrayList<File>());
    toolLocations.put("bibtex" , new ArrayList<File>());

    if(SystemUtils.isLinuxOS()) {
      toolLocations.put("xdvi" , new ArrayList<File>());
      toolLocations.put("kdvi" , new ArrayList<File>());
      toolLocations.put("okular" , new ArrayList<File>());
    } else
    if(SystemUtils.isMacOS()) {
      toolLocations.put("Skim.app/Contents/SharedSupport/displayline" , new ArrayList<File>());
    } else
    if(SystemUtils.isLinuxOS()) {
      toolLocations.put("SumatraPDF" , new ArrayList<File>());
    }
  }}

  private JComboBox aspell = new JComboBox();
  private JComboBox pdfLatex = new JComboBox();
  private JComboBox latex = new JComboBox();
  private JComboBox dvips = new JComboBox();
  private JComboBox ps2pdf = new JComboBox();
  private JComboBox bibtex = new JComboBox();
  private JComboBox viewer = new JComboBox();
  private JTextField viewerParameters = new JTextField();

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

    JPanel main = new JPanel();
    JScrollPane scrollPane = new JScrollPane(main);

    getContentPane().setLayout(new BorderLayout());

    JLabel welcome = new JLabel("<html>" + message + "</html>");
    welcome.setBorder(BorderFactory.createEmptyBorder(0,5,15,5));
    welcome.setOpaque(false);
    getContentPane().add(welcome, BorderLayout.NORTH);
    getContentPane().add(scrollPane, BorderLayout.CENTER);

    GridBagLayout layout = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    main.setLayout(layout);

    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new Insets(5,15,5,15);
    gbc.ipadx = 5; gbc.ipady = 5;

    gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.gridheight = 1;
    gbc.weightx = 1; gbc.weighty = .2;

    JPanel keystrokesPanel = new JPanel(new BorderLayout());
    TitledBorder border = new TitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "KeyStroke Settings");
    border.setTitleFont(border.getTitleFont().deriveFont(Font.BOLD));
    keystrokesPanel.setBorder(border);

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

    main.add(keystrokesPanel, gbc);

    gbc.gridy++;
    main.add(new ProgramPanel("Aspell", "aspell.executable", aspell,
            "The program 'aspell' is required for the live spell checker."), gbc);
    gbc.gridy++;
    main.add(new ProgramPanel("PdfLatex", "compiler.pdflatex.executable", pdfLatex,
            "The program 'pdflatex' is required for generating PDF files."), gbc);
    gbc.gridy++;
    main.add(new ProgramPanel("Latex", "compiler.latex.executable", latex,
            "The program 'latex' is required for generating DVI files."), gbc);
    gbc.gridy++;
    main.add(new ProgramPanel("DviPs", "compiler.dvips.executable", dvips,
            "The program 'dvips' is required for converting DVI files to PS files."), gbc);
    gbc.gridy++;
    main.add(new ProgramPanel("Ps2Pdf", "compiler.ps2pdf.executable", ps2pdf,
            "The program 'ps2pdf' is required for converting PS files to PDF files."), gbc);
    gbc.gridy++;
    main.add(new ProgramPanel("BibTex", "compiler.bibtex.executable", bibtex,
            "The program 'bibtex' is required for managing references."), gbc);

    if(SystemUtils.isLinuxOS()) {
      toolLocations.put("xdvi" , new ArrayList<File>());
      toolLocations.put("kdvi" , new ArrayList<File>());
      toolLocations.put("okular" , new ArrayList<File>());
    } else
    if(SystemUtils.isMacOS()) {
      toolLocations.put("Skim" , new ArrayList<File>());
    } else
    if(SystemUtils.isWinOS()) {
      toolLocations.put("SumatraPDF" , new ArrayList<File>());
    }

    gbc.gridy++;
    main.add(new ParameterisedProgramPanel(
            "PDF/DVI Viewer", "forward search.viewer", viewer,
            "<html>" +
            "This configuration is required for synchronization between the editor<br>" +
            "and your preferred viewer. Pressing `control shift F' in the editor will<br>" +
            "prompt the viewer to display the part that corresponds to the current<br>" +
            "position in the editor. The `prompting' is done by executing the<br>" +
             "command provided here." +
            "</html>",
            viewerParameters,
            "<html>" +
            "The viewer will be passed the following parameters:<br>" +
            "<ul>" +
            "<li>%file (the PDF or DVI file)</li>" +
            "<li>%texfile (the current source file), and</li>" +
            "<li>%line (the current source line).</li>" +
            "</html>"),
            gbc);

    setModal(true);
    pack();

    // place window in the center of the screen
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension windowSize = getSize();
    setLocation(screenSize.width / 2 - (windowSize.width / 2), screenSize.height / 2 - (windowSize.height / 2));

    // guessing suitable properties file
    if(!GProperties.hasChanges()) {
      if(SystemUtils.isMacOS()) keyStrokesList.setSelectedIndex(1);
      if(SystemUtils.isWinOS()) keyStrokesList.setSelectedIndex(3);
    }

    // search for tools
    searchThread.start();
  }

  private static class ProgramPanel extends JPanel {
    private String programName;
    private String gproperty;

    private JLabel messageLabel;
    private JComboBox fileBox;
    private JButton openButton;
    private JLabel resultLabel;

    private ExecutableChecker checker;

    public ProgramPanel(String title, String gproperty, JComboBox fileBox, String message) {
      this(title, gproperty, fileBox, message, true);
    }


    public ProgramPanel(String title, String gproperty, JComboBox fileBox, String message, boolean layout) {
      this.programName = title.toLowerCase();
      if(SystemUtils.isWinOS()) programName = programName + ".exe";
      this.gproperty = gproperty;
      this.fileBox = fileBox;

      TitledBorder border = new TitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), title);
      border.setTitleFont(border.getTitleFont().deriveFont(Font.BOLD));
      setBorder(border);

      ComboBoxEditor editor = fileBox.getEditor();
      DefaultComboBoxModel model = new DefaultComboBoxModel();
      fileBox.setEditable(true);
      fileBox.setModel(model);

      String file = GProperties.getString(gproperty);
      if(SystemUtils.isWinOS() && !file.toLowerCase().endsWith(".exe")) {
        file = file + ".exe";
        GProperties.set(gproperty, file);
      }

      model.addElement(file);
      editor.setItem(file);

      messageLabel = new JLabel(message);
      openButton = new JButton("...");
      resultLabel = new JLabel();

      checker = new ExecutableChecker(fileBox, resultLabel);
      checker.run();

      editor.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          check();
        }
      });

      fileBox.addItemListener(new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
          check();
        }
      });

      if(layout) setupLayout();
    }

    protected void setupLayout() {
      setLayout(new BorderLayout());

      add(fileBox, BorderLayout.CENTER);
      add(openButton, BorderLayout.EAST);
      add(messageLabel, BorderLayout.NORTH);
      add(resultLabel, BorderLayout.SOUTH);
    }

    public String getGproperty() {
      return gproperty;
    }

    public String getProgramName() {
      return programName;
    }

    private void check() {
      SwingUtilities.invokeLater(checker);
    }
  }

  private static class ParameterisedProgramPanel extends ProgramPanel {
    protected JTextField parameterField;
    protected JLabel parameterMessageLabel;

    public ParameterisedProgramPanel(String title, String gproperty, JComboBox fileBox, String message, JTextField parameterField, String parameterMessage) {
      super(title, gproperty, fileBox, message, false);

      this.parameterField = parameterField;
      parameterMessageLabel = new JLabel(parameterMessage);
    }
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

  private static class ExecutableChecker implements Runnable {
    private JComboBox comboBox;
    private JLabel resultLabel;

    private static String[] PATH;
    static {
      String path = System.getenv("PATH");
      if(path == null) path = System.getenv("Path");
      if(path == null) path = System.getenv("path");
      if(path == null) path = "";

      PATH = path.split(File.pathSeparator);
    }

    private ExecutableChecker(JComboBox comboBox, JLabel resultLabel) {
      this.comboBox = comboBox;
      this.resultLabel = resultLabel;
    }

    public void run() {
      String path = comboBox.getEditor().getItem().toString();

      if(checkExecutable(path)) {
        resultLabel.setText("<html><font color=green>File found.</font></html>");
      } else {
        resultLabel.setText("<html><font color=red><bf>File not found.</bf></font></html>");
      }
    }

    public static boolean checkExecutable(String executable) {
      File file = new File(executable);
      if(file.exists() && file.canExecute()) return true;

      for(String dirName : PATH) {
        File dir = new File(dirName);
        if(!dir.exists()) continue;

        file = new File(dir, executable);
        if(file.exists() && file.canExecute()) return true;
      }

      return false;
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

  public static class ProgramWithParameters {
    private String name;
    private String executable;
    private String parameters;

    public ProgramWithParameters(String name, String executable, String parameters) {
      this.name = name;
      this.executable = executable;
      this.parameters = parameters;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getExecutable() {
      return executable;
    }

    public void setExecutable(String executable) {
      this.executable = executable;
    }

    public String getParameters() {
      return parameters;
    }

    public void setParameters(String parameters) {
      this.parameters = parameters;
    }
  }
}
