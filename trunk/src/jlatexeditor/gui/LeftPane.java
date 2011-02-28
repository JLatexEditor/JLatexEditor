package jlatexeditor.gui;

import jlatexeditor.gproperties.GProperties;
import sce.component.ImageButton;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LeftPane extends JPanel implements ActionListener {
  private JPanel leftPanel;
  private ImageButton buttonSymbols;
  private ImageButton buttonStructure;

  private JComponent main;
  private JComponent symbolsPanel;
  private JComponent structureView;

  private JSplitPane splitPane;

  // currently shown component
  private JComponent view = null;
  private int dividerLocation = GProperties.getInt("main_window.symbols_panel.width");

  public LeftPane(JComponent main, JComponent symbolsPanel, JComponent structureView) {
    this.main = main;
    this.symbolsPanel = symbolsPanel;
    this.structureView = structureView;

    setLayout(new BorderLayout());

    createLeftPanel();
    add(leftPanel, BorderLayout.WEST);

    splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
    splitPane.setContinuousLayout(false);
    splitPane.setOneTouchExpandable(true);

    add(main, BorderLayout.CENTER);
  }

  private void createLeftPanel() {
    leftPanel = new JPanel();
    leftPanel.setOpaque(true);
    leftPanel.setBackground((Color) UIManager.get("TabbedPane.background"));
    leftPanel.setLayout(new FlowLayout());
    leftPanel.setBorder(
            BorderFactory.createCompoundBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                    BorderFactory.createEmptyBorder(1, 1, 2, 2)
            ));

    buttonSymbols = new ImageButton(new ImageIcon(getClass().getResource("/images/leftPane/symbols.png")));
    buttonSymbols.addActionListener(this);
    buttonStructure = new ImageButton(new ImageIcon(getClass().getResource("/images/leftPane/structure.png")));
    buttonStructure.addActionListener(this);

    GroupLayout layout = new GroupLayout(leftPanel);
    leftPanel.setLayout(layout);

    layout.setAutoCreateGaps(true);

    leftPanel.add(buttonSymbols);
    leftPanel.add(buttonStructure);

    GroupLayout.Group groupHorizontal =
            layout.createParallelGroup()
                    .addComponent(buttonSymbols)
                    .addComponent(buttonStructure);

    GroupLayout.Group groupVertical =
            layout.createSequentialGroup()
                    .addGap(5)
                    .addComponent(buttonSymbols)
                    .addComponent(buttonStructure)
                    .addGap(5);

    layout.setHorizontalGroup(groupHorizontal);
    layout.setVerticalGroup(groupVertical);
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == buttonSymbols) changeView(symbolsPanel);
    if (e.getSource() == buttonStructure) changeView(structureView);
  }

  public void changeView(JComponent nview) {
    if (nview == view) nview = null;

    if (view != null && nview == null) {
      dividerLocation = splitPane.getDividerLocation();
      remove(splitPane);
      add(main, BorderLayout.CENTER);
    } else if (view == null && nview != null) {
      remove(main);
      add(splitPane, BorderLayout.CENTER);
    }

    if (nview != null) {
      splitPane.setLeftComponent(nview);
      splitPane.setRightComponent(main);
    } else {
      splitPane.setLeftComponent(null);
      splitPane.setRightComponent(null);
    }
    splitPane.setDividerLocation(dividerLocation);

    invalidate();
    revalidate();
    view = nview;

  }
}
