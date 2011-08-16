package jlatexeditor.gui;

import jlatexeditor.SCEManager;
import sce.codehelper.*;
import sce.component.SourceCodeEditor;
import util.Function1;
import util.SimpleTrie;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

/**
 * Live template editor.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class TemplateEditor extends JDialog {
	public JPanel mainPanel;
	public JList templateList;
	public SourceCodeEditor editor;
	public JCheckBox enabledCheckBox;
	public JButton restoreSystemTemplateButton;
	public JButton addTemplateButton;
	public JButton deleteTemplateButton;
	public MinimalTable argumentsTable;
	public JButton saveButton;
	public JButton cancelButton;
	public MinimalTable generatorTable;
	public JPanel generatorPanel;
	public JButton copyTemplateButton;
	public JButton renameTemplateButton;
	public JButton moveArgumentUpButton;
	public JButton moveArgumentDownButton;
	public JButton deleteArgumentButton;
	public JButton deleteGeneratorButton;
	public TemplateListModel templateListModel;
	public ArgumentsTableModel argumentsTableModel;
	public GeneratorTableModel generatorTableModel;

	private CHCommand oldTemplate;
	private CHCommand newTemplate;
	private JFrame owner;

	public TemplateEditor(final JFrame owner) {
		super(owner, "Template Editor");
		this.owner = owner;

		$$$setupUI$$$();
		setContentPane(mainPanel);

		// populate commandList
		templateList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) return;

				saveTemplateIfChanged();
				load(getSelectedTemplate());
				pack();
			}
		});
		reloadTemplateList();
		selectTemplate(0);
		templateList.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getModifiers() == 0) {
					if (e.getKeyCode() == KeyEvent.VK_DELETE) {
						askToDeleteSelectedTemplate();
						e.consume();
					} else if (e.getKeyCode() == KeyEvent.VK_INSERT) {
						askToAddTemplate();
						e.consume();
					} else if (e.getKeyCode() == KeyEvent.VK_F2) {
						askToRenameTemplate();
						e.consume();
					}
				} else if (e.getModifiers() == KeyEvent.CTRL_MASK) {
					if (e.getKeyCode() == KeyEvent.VK_C) {
						askToCopyTemplate();
						e.consume();
					}
				}
			}
		});

		// set up attributesTable
		argumentsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				reloadGenerators();
			}
		});
		argumentsTable.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getModifiers() == KeyEvent.CTRL_MASK) {
					if (e.getKeyCode() == KeyEvent.VK_UP) {
						moveArgumentUp();
					} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
						moveArgumentDown();
					}
				} else if (e.getModifiers() == 0) {
					if (e.getKeyCode() == KeyEvent.VK_DELETE) {
						deleteArgument();
					}
				}
			}
		});
		generatorTable.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getModifiers() == 0) {
					if (e.getKeyCode() == KeyEvent.VK_DELETE) {
						deleteGenerator();
					}
				}
			}
		});

		// replace button text by icons if possible
		try {
			moveArgumentUpButton.setIcon(SCEManager.getMappedImageIcon("move up"));
			moveArgumentUpButton.setText("");
			moveArgumentDownButton.setIcon(SCEManager.getMappedImageIcon("move down"));
			moveArgumentDownButton.setText("");
			deleteArgumentButton.setIcon(SCEManager.getMappedImageIcon("delete from list"));
			deleteArgumentButton.setText("");
			deleteGeneratorButton.setIcon(SCEManager.getMappedImageIcon("delete from list"));
			deleteGeneratorButton.setText("");
		} catch (Exception ignored) {
		}

		addTemplateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				askToAddTemplate();
			}
		});
		copyTemplateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				askToCopyTemplate();
			}
		});
		renameTemplateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				askToRenameTemplate();
			}
		});
		deleteTemplateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				askToDeleteSelectedTemplate();
			}
		});
		moveArgumentUpButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveArgumentUp();
			}
		});
		moveArgumentDownButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveArgumentDown();
			}
		});
		deleteArgumentButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				deleteArgument();
			}
		});
		deleteGeneratorButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				deleteGenerator();
			}
		});

		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (newTemplate != null) {
					saveTemplate();
				}
			}
		});
		restoreSystemTemplateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (getSystemTemplates().contains(newTemplate.getName())) {
					load(getSystemTemplates().get(newTemplate.getName()).clone());
				}
			}
		});
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				CHCommand selectedTemplate = getSelectedTemplate();
				if (selectedTemplate != null) {
					load(selectedTemplate);
				}
			}
		});

		setModal(true);
		setSize(800, 600);
	}

	private void deleteGenerator() {
		CHArgumentGenerator selectedGenerator = getSelectedGenerator();
		if (selectedGenerator == null) return;

		getSelectedArgument().getGenerators().remove(selectedGenerator);
		reloadGenerators();
	}

	private void deleteArgument() {
		CHCommandArgument selectedArgument = getSelectedArgument();
		if (selectedArgument == null) return;

		removeTemplateArgument(selectedArgument.getName());
	}

	private void moveArgumentDown() {
		CHCommandArgument selectedArgument = getSelectedArgument();
		if (selectedArgument == null) return;

		newTemplate.getArgumentsHashMap().moveArgDown(selectedArgument);
		reloadArguments();
		selectArgument(selectedArgument);
	}

	private void moveArgumentUp() {
		CHCommandArgument selectedArgument = getSelectedArgument();
		if (selectedArgument == null) return;

		newTemplate.getArgumentsHashMap().moveArgUp(selectedArgument);
		reloadArguments();
		selectArgument(selectedArgument);
	}

	private void saveTemplate() {
		newTemplate.setUsage(editor.getText());
		getTemplates().add(newTemplate.getName(), newTemplate.clone());
		// TODO: save templates to file
	}

	private void saveTemplateIfChanged() {
		if (newTemplate != null) {
			newTemplate.setUsage(editor.getText());
			if (!getTemplates().get(newTemplate.getName()).deepEquals(newTemplate)) {
				if (JOptionPane.showConfirmDialog(owner, "Do you want to save your changes for template \"" + newTemplate.getName() + "\"", "Save changes?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					saveTemplate();
				}
			}
		}
	}

	private void selectArgument(CHCommandArgument selectedArgument) {
		int index = argumentsTableModel.getArguments().indexOf(selectedArgument);
		argumentsTable.getSelectionModel().setSelectionInterval(index, index);
	}

	private void selectTemplate(int index) {
		if (index >= 0 && index < templateListModel.getSize()) {
			templateList.setSelectedIndex(index);
		}
	}

	private void selectTemplate(String templateName) {
		selectTemplate(templateListModel.getIndexOf(templateName));
	}

	private void reloadTemplateList() {
		templateList.clearSelection();
		templateListModel = new TemplateListModel(getTemplates());
		templateList.setModel(templateListModel);
	}

	private void askToAddTemplate() {
		String templateName = JOptionPane.showInputDialog(owner, "Template name: ", "Add new template", JOptionPane.QUESTION_MESSAGE);
		if (templateName != null) {
			CHCommand template = new CHCommand(templateName);
			addTemplate(template, owner);
		}
	}

	private void askToCopyTemplate() {
		CHCommand selectedTemplate = getSelectedTemplate();
		if (selectedTemplate == null) return;

		String templateName = JOptionPane.showInputDialog(owner, "New template name: ", "Copy template", JOptionPane.QUESTION_MESSAGE);
		if (templateName != null) {
			CHCommand template = selectedTemplate.clone();
			template.setName(templateName);
			addTemplate(template, owner);
		}
	}

	private void askToRenameTemplate() {
		CHCommand selectedTemplate = getSelectedTemplate();
		if (selectedTemplate == null) return;

		String templateName = JOptionPane.showInputDialog(owner, "New template name: ", "Rename template", JOptionPane.QUESTION_MESSAGE);
		if (templateName != null) {
			String oldTemplateName = selectedTemplate.getName();
			selectedTemplate.setName(templateName);
			if (addTemplate(selectedTemplate, owner)) {
				getTemplates().remove(oldTemplateName);
				reloadTemplateList();
				selectTemplate(templateName);
			}
		}
	}

	private void askToDeleteSelectedTemplate() {
		CHCommand selectedTemplate = getSelectedTemplate();
		if (selectedTemplate == null) return;

		if (JOptionPane.showConfirmDialog(owner, "Are you sure you want to delete this template?") == JOptionPane.YES_OPTION) {
			getTemplates().remove(selectedTemplate.getName());
			int selectedIndex = templateList.getSelectedIndex();
			reloadTemplateList();
			selectTemplate(Math.min(selectedIndex, templateListModel.getSize() - 1));
			// TODO: save changes to file
		}
	}

	private boolean addTemplate(CHCommand template, JFrame owner) {
		if (getTemplates().contains(template.getName())) {
			JOptionPane.showMessageDialog(owner, "A template with this name already exists.");
			return false;
		}

		getTemplates().add(template.getName(), template);
		reloadTemplateList();
		selectTemplate(template.getName());
		// TODO: save changes to file

		return true;
	}

	/**
	 * Returns the selected template or null if no template is selected.
	 *
	 * @return selected template or null if no template is selected
	 */
	private CHCommand getSelectedTemplate() {
		if (templateList.getSelectedIndex() < 0) return null;

		return getTemplates().get(templateListModel.getElementAt(templateList.getSelectedIndex()));
	}

	/**
	 * Returns the selected template argument or null if no argument is selected.
	 *
	 * @return selected template argument or null if no argument is selected
	 */
	private CHCommandArgument getSelectedArgument() {
		if (argumentsTable.getSelectedRow() < 0) return null;

		return argumentsTableModel.getArguments().get(argumentsTable.getSelectedRow());
	}

	/**
	 * Returns the selected template argument or null if no argument is selected.
	 *
	 * @return selected template argument or null if no argument is selected
	 */
	private CHArgumentGenerator getSelectedGenerator() {
		if (generatorTable.getSelectedRow() < 0) return null;

		return generatorTableModel.getGenerators().get(generatorTable.getSelectedRow());
	}

	private SimpleTrie<CHCommand> getSystemTemplates() {
		return SCEManager.getSystemTabCompletion();
	}

	private SimpleTrie<CHCommand> getTemplates() {
		return SCEManager.getTabCompletion();
	}

	private void load(CHCommand template) {
		if (template == null) return;

		oldTemplate = template;
		newTemplate = template.clone();
		editor.setText(newTemplate.getUsage());
		reloadArguments();
		/*
		TableColumn col = attributesTable.getColumnModel().getColumn(ArgumentsTableModel.C_Type);
		col.setCellEditor(new ComboBoxCellEditor(CHArgumentType.TYPES));
		col.setCellRenderer(new ComboBoxCellRenderer(CHArgumentType.TYPES));
		*/
		generatorPanel.setVisible(false);
		saveButton.setEnabled(true);
		cancelButton.setEnabled(true);

		restoreSystemTemplateButton.setEnabled(getSystemTemplates().contains(template.getName()));
	}

	private void reloadArguments() {
		argumentsTable.getSelectionModel().clearSelection();
		argumentsTable.setModel(argumentsTableModel = new ArgumentsTableModel(newTemplate));
	}

	private void reloadGenerators() {
		CHCommandArgument argument = getSelectedArgument();

		if (argument == null || argument.getGenerators().isEmpty()) {
			generatorPanel.setVisible(false);
		} else {
			generatorTable.getSelectionModel().clearSelection();
			generatorPanel.setVisible(true);
			generatorTable.setRowHeight(new JComboBox().getPreferredSize().height);
			generatorTable.setModel(generatorTableModel = new GeneratorTableModel(argument));
			String[] argNames = getArgumentNames();
			TableColumn nameCol = generatorTable.getColumnModel().getColumn(GeneratorTableModel.C_Name);
			nameCol.setCellEditor(new ComboBoxCellEditor(argNames));
			nameCol.setCellRenderer(new ComboBoxCellRenderer(argNames));
			String[] functionNames = getFunctionsNames();
			TableColumn functionCol = generatorTable.getColumnModel().getColumn(GeneratorTableModel.C_Function);
			functionCol.setCellEditor(new ComboBoxCellEditor(functionNames));
			functionCol.setCellRenderer(new ComboBoxCellRenderer(functionNames));
			pack();
		}
	}

	private void createUIComponents() {
		editor = SCEManager.createTemplateSourceCodeEditor(this);
	}

	public ArgumentsTableModel getArgumentsTableModel() {
		return argumentsTableModel;
	}

	public GeneratorTableModel getGeneratorTableModel() {
		return generatorTableModel;
	}

	public boolean hasTemplateArgument(String argument) {
		return newTemplate.hasArgument(argument);
	}

	public void addTemplateArgument(String argumentName) {
		newTemplate.getArgumentsHashMap().put(argumentName, new CHCommandArgument(argumentName, argumentName, false));
		reloadArguments();
		updateSyntaxHighlighting();
		pack();
	}

	public void removeTemplateArgument(String argumentName) {
		newTemplate.getArgumentsHashMap().remove(argumentName);
		reloadArguments();
		updateSyntaxHighlighting();
		pack();
	}

	protected void updateSyntaxHighlighting() {
		editor.getTextPane().getDocument().invalidateSyntaxHighlighting();
	}

	public String[] getArgumentNames() {
		String[] argumentNames = new String[argumentsTableModel.getRowCount()];
		for (int i = 0; i < argumentNames.length; i++) {
			argumentNames[i] = argumentsTableModel.getArguments().get(i).getName();
		}
		return argumentNames;
	}

	public void askForDerivedArgumentValue(String baseArgumentName) {
		JComboBox derivedArgument = new JComboBox(getArgumentNames());
		JComboBox functionName = new JComboBox(getFunctionsNames());

		Object[] message = {
			"Derived argument:", derivedArgument,
			"\nFunction:", functionName
		};
		int resp = JOptionPane.showConfirmDialog(owner, message, "Declare command", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

		if (resp != JOptionPane.OK_OPTION) return;

		CHCommandArgument baseArgument = newTemplate.getArgumentsHashMap().get(baseArgumentName);
		String derivedArgumentName = derivedArgument.getSelectedItem().toString();
		Function1<String, String> function = CHFunctions.get(functionName.getSelectedItem().toString());
		CHArgumentGenerator gen = new CHArgumentGenerator(derivedArgumentName, function);
		gen.setArgument(newTemplate.getArgumentsHashMap().get(derivedArgumentName));
		baseArgument.getGenerators().add(gen);

		selectArgument(baseArgument);
		reloadGenerators();
	}

	private String[] getFunctionsNames() {
		ArrayList<String> functionNames = CHFunctions.getFunctionNames();
		return functionNames.toArray(new String[functionNames.size()]);
	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer
	 * >>> IMPORTANT!! <<<
	 * DO NOT edit this method OR call it in your code!
	 *
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		createUIComponents();
		mainPanel = new JPanel();
		mainPanel.setLayout(new GridBagLayout());
		final JSplitPane splitPane1 = new JSplitPane();
		splitPane1.setDividerLocation(179);
		GridBagConstraints gbc;
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.add(splitPane1, gbc);
		final JPanel panel1 = new JPanel();
		panel1.setLayout(new GridBagLayout());
		splitPane1.setLeftComponent(panel1);
		panel1.setBorder(BorderFactory.createTitledBorder("Template Name"));
		templateList = new JList();
		templateList.setSelectionMode(0);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 3;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(0, 4, 4, 4);
		panel1.add(templateList, gbc);
		addTemplateButton = new JButton();
		addTemplateButton.setText("Add");
		addTemplateButton.setMnemonic('A');
		addTemplateButton.setDisplayedMnemonicIndex(0);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 4, 4, 4);
		panel1.add(addTemplateButton, gbc);
		copyTemplateButton = new JButton();
		copyTemplateButton.setText("Copy");
		copyTemplateButton.setMnemonic('O');
		copyTemplateButton.setDisplayedMnemonicIndex(1);
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.weightx = 1.2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 4, 4);
		panel1.add(copyTemplateButton, gbc);
		renameTemplateButton = new JButton();
		renameTemplateButton.setText("Rename");
		renameTemplateButton.setMnemonic('R');
		renameTemplateButton.setDisplayedMnemonicIndex(0);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 4, 4, 4);
		panel1.add(renameTemplateButton, gbc);
		deleteTemplateButton = new JButton();
		deleteTemplateButton.setText("Delete");
		deleteTemplateButton.setMnemonic('D');
		deleteTemplateButton.setDisplayedMnemonicIndex(0);
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 4, 4);
		panel1.add(deleteTemplateButton, gbc);
		final JPanel panel2 = new JPanel();
		panel2.setLayout(new GridBagLayout());
		splitPane1.setRightComponent(panel2);
		final JPanel panel3 = new JPanel();
		panel3.setLayout(new GridBagLayout());
		panel3.setMinimumSize(new Dimension(200, 200));
		panel3.setPreferredSize(new Dimension(200, 200));
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		panel2.add(panel3, gbc);
		panel3.setBorder(BorderFactory.createTitledBorder("Template Code"));
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(0, 4, 4, 4);
		panel3.add(editor, gbc);
		final JPanel panel4 = new JPanel();
		panel4.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.BOTH;
		panel2.add(panel4, gbc);
		panel4.setBorder(BorderFactory.createTitledBorder("Arguments"));
		final JScrollPane scrollPane1 = new JScrollPane();
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		gbc.gridheight = 3;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(0, 4, 4, 4);
		panel4.add(scrollPane1, gbc);
		argumentsTable = new TemplateEditor.MinimalTable();
		scrollPane1.setViewportView(argumentsTable);
		moveArgumentUpButton = new JButton();
		moveArgumentUpButton.setText("Move up");
		gbc = new GridBagConstraints();
		gbc.gridx = 2;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel4.add(moveArgumentUpButton, gbc);
		moveArgumentDownButton = new JButton();
		moveArgumentDownButton.setText("Move down");
		gbc = new GridBagConstraints();
		gbc.gridx = 2;
		gbc.gridy = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel4.add(moveArgumentDownButton, gbc);
		deleteArgumentButton = new JButton();
		deleteArgumentButton.setText("Delete");
		gbc = new GridBagConstraints();
		gbc.gridx = 2;
		gbc.gridy = 3;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel4.add(deleteArgumentButton, gbc);
		final JLabel label1 = new JLabel();
		label1.setText("Arguments of the from \"@argName@\" can be added via ALT+ENTER in the editor");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(0, 4, 4, 4);
		panel4.add(label1, gbc);
		generatorPanel = new JPanel();
		generatorPanel.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.fill = GridBagConstraints.BOTH;
		panel2.add(generatorPanel, gbc);
		generatorPanel.setBorder(BorderFactory.createTitledBorder("Derived argument values"));
		final JScrollPane scrollPane2 = new JScrollPane();
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(0, 4, 4, 4);
		generatorPanel.add(scrollPane2, gbc);
		generatorTable = new TemplateEditor.MinimalTable();
		scrollPane2.setViewportView(generatorTable);
		deleteGeneratorButton = new JButton();
		deleteGeneratorButton.setText("Delete");
		gbc = new GridBagConstraints();
		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		generatorPanel.add(deleteGeneratorButton, gbc);
		final JPanel panel5 = new JPanel();
		panel5.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.BOTH;
		panel2.add(panel5, gbc);
		saveButton = new JButton();
		saveButton.setText("Save");
		saveButton.setMnemonic('S');
		saveButton.setDisplayedMnemonicIndex(0);
		gbc = new GridBagConstraints();
		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 4, 4, 4);
		panel5.add(saveButton, gbc);
		cancelButton = new JButton();
		cancelButton.setText("Cancel");
		cancelButton.setMnemonic('C');
		cancelButton.setDisplayedMnemonicIndex(0);
		gbc = new GridBagConstraints();
		gbc.gridx = 3;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 4, 4);
		panel5.add(cancelButton, gbc);
		enabledCheckBox = new JCheckBox();
		enabledCheckBox.setEnabled(false);
		enabledCheckBox.setText("Enabled");
		enabledCheckBox.setMnemonic('E');
		enabledCheckBox.setDisplayedMnemonicIndex(0);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(0, 4, 4, 4);
		panel5.add(enabledCheckBox, gbc);
		restoreSystemTemplateButton = new JButton();
		restoreSystemTemplateButton.setText("Restore system template");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(0, 4, 4, 4);
		panel5.add(restoreSystemTemplateButton, gbc);
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return mainPanel;
	}

	public class ArgumentsTableModel implements TableModel {
		private static final int C_Name = 0, C_Value = 1, C_Completion = 2, C_Hint = 3;
		private String[] columns = new String[]{"Name", "Value", "Completion", "Hint"};
		private Class[] columnClasses = new Class[]{String.class, String.class, Boolean.class, String.class};
		private CHCommand.ArgumentsHashMap arguments;

		public ArgumentsTableModel(CHCommand template) {
			this.arguments = template.getArgumentsHashMap();
		}

		public List<CHCommandArgument> getArguments() {
			return arguments.getList();
		}

		@Override
		public int getRowCount() {
			return arguments.size();
		}

		@Override
		public int getColumnCount() {
			return columns.length;
		}

		@Override
		public String getColumnName(int columnIndex) {
			return columns[columnIndex];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnClasses[columnIndex];
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return true;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			CHCommandArgument arg = getArguments().get(rowIndex);
			switch (columnIndex) {
				case C_Name:
					return arg.getName();
				case C_Value:
					return arg.getInitialValue();
				case C_Completion:
					return arg.isCompletion();
				/*
				case C_Type:
					return arg.getType() == null ? CHArgumentType.NO_TYPE : arg.getType();
				*/
				case C_Hint:
					return arg.getHint();
			}
			return null;
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			CHCommandArgument arg = getArguments().get(rowIndex);
			switch (columnIndex) {
				case C_Name:
					renameTemplateArgument(arg, value.toString());
					break;
				case C_Value:
					arg.setInitialValue(value.toString());
					break;
				case C_Completion:
					arg.setCompletion((Boolean) value);
					break;
				/*
				case C_Type:
					arg.setType(value.equals(CHArgumentType.NO_TYPE) ? null : new CHArgumentType(value.toString()));
					break;
				*/
				case C_Hint:
					arg.setHint(value.toString());
					break;
			}
		}

		private void renameTemplateArgument(CHCommandArgument arg, String newName) {
			String oldName = arg.getName();
			String escOldName = oldName.replaceAll("([\\*\\+\\[\\]\\{\\}\\.\\-])", "\\\\$1");
			editor.setText(editor.getText().replaceAll("@" + escOldName + "@", "@" + newName + "@"));
			newTemplate.getArgumentsHashMap().remove(oldName);
			arg.setName(newName);
			newTemplate.getArgumentsHashMap().put(arg.getName(), arg);
			reloadArguments();
		}

		@Override
		public void addTableModelListener(TableModelListener l) {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public void removeTableModelListener(TableModelListener l) {
			//To change body of implemented methods use File | Settings | File Templates.
		}
	}

	public class GeneratorTableModel implements TableModel {
		private static final int C_Name = 0, C_Function = 1;
		private String[] columns = new String[]{"Attribute Name", "Function"};
		private Class[] columnClasses = new Class[]{String.class, String.class};
		private ArrayList<CHArgumentGenerator> generators = new ArrayList<CHArgumentGenerator>();

		public GeneratorTableModel(CHCommandArgument arg) {
			generators = arg.getGenerators();
		}

		public ArrayList<CHArgumentGenerator> getGenerators() {
			return generators;
		}

		@Override
		public int getRowCount() {
			return generators.size();
		}

		@Override
		public int getColumnCount() {
			return columns.length;
		}

		@Override
		public String getColumnName(int columnIndex) {
			return columns[columnIndex];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnClasses[columnIndex];
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return true;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			CHArgumentGenerator gen = generators.get(rowIndex);
			switch (columnIndex) {
				case C_Name:
					return gen.getArgument().getName();
				case C_Function:
					return CHFunctions.getName(gen.getFunction());
			}
			return null;
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			CHArgumentGenerator gen = generators.get(rowIndex);
			switch (columnIndex) {
				case C_Name:
					gen.setArgument(newTemplate.getArgumentsHashMap().get(value.toString()));
					break;
				case C_Function:
					gen.setFunction(CHFunctions.get(value.toString()));
					break;
			}
		}

		@Override
		public void addTableModelListener(TableModelListener l) {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public void removeTableModelListener(TableModelListener l) {
			//To change body of implemented methods use File | Settings | File Templates.
		}
	}

	public class ComboBoxCellRenderer extends JComboBox implements TableCellRenderer {
		public ComboBoxCellRenderer(String[] items) {
			super(items);
		}

		public Component getTableCellRendererComponent(JTable table, Object value,
		                                               boolean isSelected, boolean hasFocus, int row, int column) {
			if (isSelected) {
				setForeground(table.getSelectionForeground());
				setBackground(table.getSelectionBackground());
			} else {
				setForeground(table.getForeground());
				setBackground(table.getBackground());
			}

			// Select the current value
			setSelectedItem(value);
			return this;
		}
	}

	public class ComboBoxCellEditor extends DefaultCellEditor {
		public ComboBoxCellEditor(String[] items) {
			super(new JComboBox(items));
		}
	}

	public static class MinimalTable extends JTable {
		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}
	}

	public static class TemplateListModel extends AbstractListModel {
		List<String> templateNames;

		public TemplateListModel(SimpleTrie<CHCommand> templates) {
			templateNames = templates.getObjectsIterable("").map(new Function1<CHCommand, String>() {
				public String apply(CHCommand a1) {
					return a1.getName();
				}
			}).toList();
		}

		/**
		 * Returns index of template name or -1 if template name is not in this list.
		 *
		 * @param templateName template name
		 * @return index of template name or -1 if template name is not in this list
		 */
		public int getIndexOf(String templateName) {
			return templateNames.indexOf(templateName);
		}

		public int getSize() {
			return templateNames.size();
		}

		public String getElementAt(int index) {
			return templateNames.get(index);
		}
	}
}