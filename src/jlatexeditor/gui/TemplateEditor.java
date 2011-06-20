package jlatexeditor.gui;

import jlatexeditor.EmptyResource;
import jlatexeditor.SCEManager;
import sce.codehelper.CHCommand;
import sce.component.AbstractResource;
import sce.component.SourceCodeEditor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

public class TemplateEditor extends JDialog {
  private ArrayList<CHCommand> commands;

  private DefaultListModel commandListModel = new DefaultListModel();
  private JList commandList = new JList(commandListModel);
  private SourceCodeEditor editor = SCEManager.createLatexSourceCodeEditor();

  private DefaultTableModel attributesTableModel = new DefaultTableModel();
  private JTable attributesTable = new JTable(attributesTableModel);

  public TemplateEditor(JFrame owner) {
    super(owner, "Template Editor");

    Container cp = getContentPane();
    GridBagLayout layout = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    cp.setLayout(layout);

    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.gridheight = 2;
    gbc.weightx = .2; gbc.weighty = 1;
    cp.add(new JScrollPane(commandList), gbc);

    gbc.gridx = 1; gbc.gridwidth = 1; gbc.gridheight = 1;
    gbc.weightx = .8; gbc.weighty = .7;
    cp.add(editor, gbc);

    gbc.gridy++; gbc.gridwidth = 1; gbc.gridheight = 1;
    gbc.weightx = .8; gbc.weighty = .3;
    cp.add(attributesTable, gbc);

    attributesTableModel.setColumnCount(7);
    attributesTableModel.setColumnIdentifiers(new String[] {"Name", "Value", "Values", "Optional", "Type", "Completion", "Hint"});

    // populate commandList
    commands = SCEManager.getTabCompletions().getCommands().getObjectsIterable("").toList();
    for(CHCommand command : commands) {
      commandListModel.addElement(command);
    }

    setModal(true);
    pack();
  }
}
