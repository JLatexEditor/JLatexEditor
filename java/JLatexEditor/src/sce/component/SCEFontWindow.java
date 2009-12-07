package sce.component;

import jlatexeditor.GProperties;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.*;

/**
 * Window for choosing a font for displaying the latex source text.
 * @author Rena
 */
public class SCEFontWindow extends JFrame implements ListSelectionListener, ActionListener {
  private JList fontList;
  private String fontName;
  private String prevFontName;

  public static void main(String[] args){
    JFrame frame = new SCEFontWindow("Monospaced");
    frame.setVisible(true);
  }


  public SCEFontWindow(String fontName) {
    super("Font Configuration");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    this.fontName = this.prevFontName = fontName;

    Container cp = getContentPane();
    GridBagLayout layout = new GridBagLayout();
    cp.setLayout(layout);
    GridBagConstraints constraints = new GridBagConstraints();

    fontList = new JList(GProperties.getMonospaceFonts().toArray());
    fontList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    fontList.setLayoutOrientation(JList.VERTICAL);
    fontList.setVisibleRowCount(-1);
    fontList.getSelectionModel().addListSelectionListener(this);

    JScrollPane fontScroller = new JScrollPane(fontList);
    fontScroller.setPreferredSize(new Dimension(250,225));
    fontScroller.setAlignmentX(LEFT_ALIGNMENT);

    DefaultListModel sizeModel = new DefaultListModel();
    JList sizeList = new JList(sizeModel);
    sizeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    sizeList.setLayoutOrientation(JList.VERTICAL);
    sizeList.setVisibleRowCount(-1);
    for(int size = 6; size <= 48; size++) {
      sizeModel.addElement(size + "");
    }

    JScrollPane sizeScroller = new JScrollPane(sizeList);
    sizeScroller.setAlignmentX(LEFT_ALIGNMENT);

    DefaultComboBoxModel aaModel = new DefaultComboBoxModel();
    JComboBox aaList = new JComboBox(aaModel);
    for(String key : GProperties.TEXT_ANTIALIAS_KEYS) {
      aaModel.addElement(key);
    }

    JLabel fontLabel = new JLabel("Font");
    fontLabel.setLabelFor(fontList);

    JLabel sizeLabel = new JLabel("Size");
    sizeLabel.setLabelFor(sizeList);

    JLabel antiasLabel = new JLabel("Antialias");
    antiasLabel.setLabelFor(aaList);

    JButton ok = new JButton("OK");
    ok.addActionListener(this);
    ok.setActionCommand("okFont");
    JButton cancel = new JButton("Cancel");
    cancel.addActionListener(this);
    cancel.setActionCommand("cancelFont");

    //adding the components to Frame
    constraints.weightx = 1;
    constraints.fill = GridBagConstraints.BOTH;

    constraints.weighty = 0;
    constraints.insets = new Insets(3, 5, 3, 5);
    cp.add(fontLabel, constraints);
    constraints.gridx = 1;
    constraints.gridwidth = 2;
    cp.add(sizeLabel, constraints);
    constraints.weighty = 1;
    constraints.gridwidth = 1;
    constraints.gridx = 0;
    constraints.gridy = 1;
    cp.add(fontScroller, constraints);
    constraints.gridwidth = 2;
    constraints.gridx = 1;
    constraints.weightx = 0.3;
    cp.add(sizeScroller, constraints);
    constraints.weighty = 0;
    constraints.gridwidth = 1;
    constraints.gridy ++ ;
    cp.add(antiasLabel, constraints);
    constraints.gridx ++ ;
    cp.add(aaList, constraints);
    //constraints.gridx = 0;
    constraints.gridy++;
    constraints.gridx--;
    cp.add(ok, constraints);
    constraints.gridx ++;
    cp.add(cancel, constraints);

    pack();
    setVisible(true);
  }

  public void valueChanged(ListSelectionEvent e) {
    if(e.getSource() == fontList) {
      fontName = fontList.getSelectedValue().toString();
      //actionListener.actionEvent(..);
    }
  }

  public void actionPerformed(ActionEvent e) {
    if(e.getActionCommand().equals("okFont")){
      dispose();
    } else
    if (e.getActionCommand().equals("cancelFont")) {
      dispose();
    }
  }
  
}
