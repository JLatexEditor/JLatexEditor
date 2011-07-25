package jlatexeditor.gui;

import jlatexeditor.translation.I18n;

import javax.swing.*;
import java.awt.*;

/**
 * About dialog.
 *
 * @author Stefan Endrullis
 */
public class AcknowledgementsDialog extends JFrame {
  private JEditorPane content;

  public AcknowledgementsDialog(String version) {
    super(I18n.getString("acknowledgements_dialog.title"));

    Container cp = getContentPane();
    cp.setLayout(new BorderLayout());

    // setup GUI components
    content = new JEditorPane();
    content.setContentType("text/html");
    content.setText(I18n.getString("acknowledgements_dialog.text"));
    content.setEditable(false);
    cp.add(new JScrollPane(content), BorderLayout.CENTER);

    // set window size and location
    setSize(600, 700);
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension windowSize = getSize();
    setLocation(screenSize.width / 2 - (windowSize.width / 2), screenSize.height / 2 - (windowSize.height / 2));
  }
}
