package jlatexeditor.gui;

import jlatexeditor.SCEManager;
import jlatexeditor.gproperties.GProperties;
import sce.codehelper.CHCommand;
import util.ProcessUtil;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class Wizard extends JDialog {
  private String message =
          "<font size=+2><bf>Welcome to JLatexEditor!</bf></font><br><br> " +
          "This wizard allows for a quick setup of the user interface " +
          "and external tools.";

  private JTextField aspell = new JTextField();
  private JTextField pdfLatex = new JTextField();
  private JTextField latex = new JTextField();
  private JTextField bibtex = new JTextField();

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
}
