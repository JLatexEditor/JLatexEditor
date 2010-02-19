package jlatexeditor.gui;

import sce.component.ImageButton;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LeftPane extends JPanel implements ActionListener {
  private JPanel leftPanel;
  private ImageButton buttonSymbols;
  private ImageButton buttonStructure;

  private JComponent main;
  private JComponent symbolsPanel;

  private JSplitPane splitPane;

  // currently shown component
  private JComponent view = null;
  private int dividerLocation = 220;

  public LeftPane(JComponent main, JComponent symbolsPanel) {
    this.main = main;
    this.symbolsPanel = symbolsPanel;
    
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
                    BorderFactory.createEmptyBorder(1,1,2,2)
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
    JComponent nview = null;
    if(e.getSource() == buttonSymbols) nview = symbolsPanel;

    if(nview == view) nview = null;

    if(view != null && nview == null) {
      dividerLocation = splitPane.getDividerLocation();
      remove(splitPane);
      add(main, BorderLayout.CENTER);
    } else
    if(view == null && nview != null) {
      remove(main);
      add(splitPane, BorderLayout.CENTER);
      splitPane.setDividerLocation(dividerLocation);
    }

    if(nview != null) {
      splitPane.setLeftComponent(nview);
      splitPane.setRightComponent(main);
    } else {
      splitPane.setLeftComponent(null);
      splitPane.setRightComponent(null);
    }


    invalidate();
    revalidate();
    view = nview;
  }
}
