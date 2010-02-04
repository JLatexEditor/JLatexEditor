package jlatexeditor.gui;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import sce.component.ImageButton;
import util.gui.FlowingLayout;
import util.gui.SCETabbedPaneUI;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.IOException;

public class SymbolsPanel extends JScrollPane {
  JPanel panel = new JPanel();

  public SymbolsPanel() {
    panel.setLayout(new FlowingLayout());
    panel.setBackground(Color.WHITE);
    panel.setOpaque(true);
    setViewportView(panel);
    getVerticalScrollBar().setUnitIncrement(30);

    read(panel, "Relation", "relation");
    read(panel, "Operators", "operators");
    read(panel, "Arrows", "arrows");
    read(panel, "Miscellaneous Math", "misc-math");
    read(panel, "Miscellaneous Text", "misc-text");
    read(panel, "Delimiter", "delimiters");
    read(panel, "Greek", "greek");
    read(panel, "Special Characters", "special");
    read(panel, "Cyrillic Characters", "cyrillic");
  }

  public void doLayout() {
    panel.setMaximumSize(new Dimension(getViewport().getWidth(), 20000));
    panel.revalidate();
    super.doLayout();
  }

  private void read(JPanel panel, String title, String collection) {
    JLabel label = new JLabel(title);
    label.setPreferredSize(new Dimension(160, 30));
    label.setBackground(SCETabbedPaneUI.BLUE);
    label.setOpaque(true);
    label.setBorder(BorderFactory.createEtchedBorder());
    panel.add(new FlowingLayout.NewLine());
    panel.add(label);
    panel.add(new FlowingLayout.NewLine());

    String fileName = "/images/symbols/" + collection + ".xml";

    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(getClass().getResourceAsStream(fileName));
      doc.getDocumentElement().normalize();

      NodeList commands = doc.getElementsByTagName("commandDefinition");
      for(int imageNr = 0; imageNr < commands.getLength(); imageNr++) {
        Node command = commands.item(imageNr);
        NodeList children = command.getChildNodes();
        String latexCommand = null;
        for(int childNr = 0; childNr < children.getLength(); childNr++) {
          Node child = children.item(childNr);
          if(child.getNodeName().equals("latexCommand")) latexCommand = child.getTextContent();
        }

        ImageIcon icon = new ImageIcon(getClass().getResource("/images/symbols/" + collection + "/img" + toString(imageNr+1) + collection + ".png"));
        panel.add(new ImageButton(icon, icon, icon));
      }
    } catch (Exception ignored) {
      ignored.printStackTrace();
    }
  }

  private String toString(int imageNr) {
    String s = imageNr + "";
    while(s.length() < 3) s = "0" + s;
    return s;
  }
}
